//! Mobile core bridge for Android and iOS native frontends.
//! Provides queue management, download control (start, pause, resume, cancel, remove),
//! state snapshots, persistence and FFI-safe boundaries.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;

use mdm_engine::download::{Destination, Download, Options, Request, Snapshot, State};
use mdm_engine::state::state_path;
use parking_lot::RwLock;
use serde::{Deserialize, Serialize};
use url::Url;

#[derive(Debug, thiserror::Error)]
pub enum MobileError {
    #[error("Invalid URL: {0}")]
    InvalidUrl(String),
    #[error("Invalid destination path: {0}")]
    InvalidDestination(String),
    #[error("Download not found with id: {0}")]
    NotFound(u64),
    #[error("Download already exists or is in an invalid state for this operation")]
    InvalidState,
    #[error("Engine error: {0}")]
    Engine(String),
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[repr(C)]
pub enum MobileDownloadState {
    Probing = 0,
    Downloading = 1,
    Verifying = 2,
    Completing = 3,
    Done = 4,
    Failed = 5,
    Cancelled = 6,
    Paused = 7,
    Waiting = 8,
}

impl From<State> for MobileDownloadState {
    fn from(s: State) -> Self {
        match s {
            State::Probing => MobileDownloadState::Probing,
            State::Downloading => MobileDownloadState::Downloading,
            State::Verifying => MobileDownloadState::Verifying,
            State::Completing => MobileDownloadState::Completing,
            State::Done => MobileDownloadState::Done,
            State::Failed => MobileDownloadState::Failed,
            State::Cancelled => MobileDownloadState::Cancelled,
        }
    }
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct MobileDownloadSnapshot {
    pub id: u64,
    pub url: String,
    pub file_path: Option<String>,
    pub state: MobileDownloadState,
    pub total_bytes: Option<u64>,
    pub downloaded_bytes: u64,
    pub speed_bytes_per_sec: Option<u64>,
    pub error: Option<String>,
    pub resumable: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
struct PersistedItem {
    id: u64,
    url: String,
    dest: String,
    paused: bool,
    terminal_state: Option<MobileDownloadState>,
    terminal_error: Option<String>,
    downloaded: u64,
    total: Option<u64>,
}

struct ActiveDownload {
    url: Url,
    dest: PathBuf,
    handle: Option<Download>,
    paused: bool,
    terminal_state: Option<MobileDownloadState>,
    terminal_error: Option<String>,
    last_snapshot: Option<Snapshot>,
    downloaded_checkpoint: u64,
}

pub struct MobileEngine {
    next_id: AtomicU64,
    downloads: Arc<RwLock<HashMap<u64, ActiveDownload>>>,
    queue_order: Arc<RwLock<Vec<u64>>>,
    max_active_downloads: Arc<RwLock<usize>>,
    persistence_dir: Arc<RwLock<Option<PathBuf>>>,
    runtime: Arc<tokio::runtime::Runtime>,
}

impl MobileEngine {
    pub fn new() -> Arc<Self> {
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .worker_threads(2)
            .enable_all()
            .build()
            .expect("Failed to create tokio runtime for MobileEngine");

        let engine = Arc::new(Self {
            next_id: AtomicU64::new(1),
            downloads: Arc::new(RwLock::new(HashMap::new())),
            queue_order: Arc::new(RwLock::new(Vec::new())),
            max_active_downloads: Arc::new(RwLock::new(3)),
            persistence_dir: Arc::new(RwLock::new(None)),
            runtime: Arc::new(runtime),
        });

        // Spawn background task to monitor queue advancement when downloads finish
        let engine_clone = Arc::downgrade(&engine);
        engine.runtime.spawn(async move {
            loop {
                tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
                if let Some(e) = engine_clone.upgrade() {
                    e.reconcile_queue();
                } else {
                    break;
                }
            }
        });

        engine
    }

    pub fn set_persistence_dir(&self, dir_path: &str) {
        let path = PathBuf::from(dir_path);
        let _ = std::fs::create_dir_all(&path);
        *self.persistence_dir.write() = Some(path);
    }

    pub fn set_max_concurrent_downloads(&self, max: usize) {
        *self.max_active_downloads.write() = max.max(1);
        self.reconcile_queue();
    }

    pub fn create_download(
        &self,
        url_str: &str,
        dest_path: &str,
    ) -> Result<u64, MobileError> {
        let url = Url::parse(url_str).map_err(|e| MobileError::InvalidUrl(e.to_string()))?;
        let dest = PathBuf::from(dest_path);

        let id = self.next_id.fetch_add(1, Ordering::SeqCst);
        let active = ActiveDownload {
            url,
            dest,
            handle: None,
            paused: false,
            terminal_state: None,
            terminal_error: None,
            last_snapshot: None,
            downloaded_checkpoint: 0,
        };

        {
            let mut dl_map = self.downloads.write();
            dl_map.insert(id, active);
            let mut q = self.queue_order.write();
            q.push(id);
        }

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(id)
    }

    pub fn pause_download(&self, id: u64) -> Result<(), MobileError> {
        let mut dl_map = self.downloads.write();
        let entry = dl_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;

        if entry.terminal_state.is_some() {
            return Err(MobileError::InvalidState);
        }

        if let Some(handle) = entry.handle.take() {
            let snap = handle.snapshot();
            entry.downloaded_checkpoint = snap.downloaded;
            entry.last_snapshot = Some(snap);
            handle.cancel();
        }
        entry.paused = true;
        drop(dl_map);

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn resume_download(&self, id: u64) -> Result<(), MobileError> {
        {
            let mut dl_map = self.downloads.write();
            let entry = dl_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;
            if entry.terminal_state.is_some() {
                return Err(MobileError::InvalidState);
            }
            entry.paused = false;
        }

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn cancel_download(&self, id: u64) -> Result<(), MobileError> {
        let mut dl_map = self.downloads.write();
        let entry = dl_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;

        if let Some(handle) = entry.handle.take() {
            let snap = handle.snapshot();
            entry.downloaded_checkpoint = snap.downloaded;
            entry.last_snapshot = Some(snap);
            handle.cancel();
        }
        entry.terminal_state = Some(MobileDownloadState::Cancelled);
        drop(dl_map);

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn remove_download(&self, id: u64) -> Result<(), MobileError> {
        let mut dl_map = self.downloads.write();
        let entry = dl_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;

        if let Some(handle) = entry.handle.take() {
            handle.cancel();
        }
        dl_map.remove(&id);
        drop(dl_map);

        let mut q = self.queue_order.write();
        q.retain(|&item_id| item_id != id);
        drop(q);

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn get_snapshot(&self, id: u64) -> Result<MobileDownloadSnapshot, MobileError> {
        let dl_map = self.downloads.read();
        let entry = dl_map.get(&id).ok_or(MobileError::NotFound(id))?;

        let (state, total, downloaded, speed, resumable, error) = if let Some(handle) = &entry.handle {
            let s = handle.snapshot();
            let total_speed: u64 = s.connections.iter().map(|c| c.speed).sum();
            (
                MobileDownloadState::from(s.state),
                s.size,
                s.downloaded,
                Some(total_speed),
                s.resumable.unwrap_or(true),
                s.error,
            )
        } else if let Some(term) = entry.terminal_state {
            let (total, down, res) = entry.last_snapshot.as_ref().map(|s| {
                (s.size, s.downloaded, s.resumable.unwrap_or(true))
            }).unwrap_or((None, entry.downloaded_checkpoint, true));
            (term, total, down, None, res, entry.terminal_error.clone())
        } else if entry.paused {
            let (total, down, res) = entry.last_snapshot.as_ref().map(|s| {
                (s.size, s.downloaded, s.resumable.unwrap_or(true))
            }).unwrap_or((None, entry.downloaded_checkpoint, true));
            (MobileDownloadState::Paused, total, down, None, res, None)
        } else {
            (MobileDownloadState::Waiting, None, entry.downloaded_checkpoint, None, true, None)
        };

        Ok(MobileDownloadSnapshot {
            id,
            url: entry.url.to_string(),
            file_path: Some(entry.dest.to_string_lossy().to_string()),
            state,
            total_bytes: total,
            downloaded_bytes: downloaded,
            speed_bytes_per_sec: speed,
            error,
            resumable,
        })
    }

    pub fn get_all_snapshots(&self) -> Vec<MobileDownloadSnapshot> {
        let q = self.queue_order.read().clone();
        let mut list = Vec::new();
        for id in q {
            if let Ok(snap) = self.get_snapshot(id) {
                list.push(snap);
            }
        }
        list
    }

    pub fn restore_download(
        &self,
        id: u64,
        url_str: &str,
        dest_path: &str,
        start_paused: bool,
    ) -> Result<(), MobileError> {
        let url = Url::parse(url_str).map_err(|e| MobileError::InvalidUrl(e.to_string()))?;
        let dest = PathBuf::from(dest_path);

        let active = ActiveDownload {
            url,
            dest,
            handle: None,
            paused: start_paused,
            terminal_state: None,
            terminal_error: None,
            last_snapshot: None,
            downloaded_checkpoint: 0,
        };

        let mut dl_map = self.downloads.write();
        dl_map.insert(id, active);
        let mut q = self.queue_order.write();
        if !q.contains(&id) {
            q.push(id);
        }

        // Keep AtomicU64 ahead of restored ids
        let current_max = self.next_id.load(Ordering::SeqCst);
        if id >= current_max {
            self.next_id.store(id + 1, Ordering::SeqCst);
        }

        drop(dl_map);
        drop(q);

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn restore_from_disk(&self) -> usize {
        let opt_dir = self.persistence_dir.read().clone();
        let dir = match opt_dir {
            Some(d) => d,
            None => return 0,
        };
        let meta_file = dir.join("downloads_metadata.json");
        if !meta_file.exists() {
            return 0;
        }

        let content = match std::fs::read_to_string(&meta_file) {
            Ok(c) => c,
            Err(_) => return 0,
        };

        let items: Vec<PersistedItem> = match serde_json::from_str(&content) {
            Ok(it) => it,
            Err(_) => return 0,
        };

        let count = items.len();
        let mut max_id: u64 = 0;

        for item in items {
            let parsed_url = match Url::parse(&item.url) {
                Ok(u) => u,
                Err(_) => continue,
            };
            let dest_path = PathBuf::from(&item.dest);
            if item.id > max_id {
                max_id = item.id;
            }

            let active = ActiveDownload {
                url: parsed_url,
                dest: dest_path,
                handle: None,
                paused: item.paused,
                terminal_state: item.terminal_state,
                terminal_error: item.terminal_error,
                last_snapshot: None,
                downloaded_checkpoint: item.downloaded,
            };

            let mut dl_map = self.downloads.write();
            dl_map.insert(item.id, active);
            let mut q = self.queue_order.write();
            if !q.contains(&item.id) {
                q.push(item.id);
            }
        }

        if max_id >= self.next_id.load(Ordering::SeqCst) {
            self.next_id.store(max_id + 1, Ordering::SeqCst);
        }

        self.reconcile_queue();
        count
    }

    pub fn persist_to_disk(&self) {
        let opt_dir = self.persistence_dir.read().clone();
        let dir = match opt_dir {
            Some(d) => d,
            None => return,
        };

        let dl_map = self.downloads.read();
        let q = self.queue_order.read();

        let mut items = Vec::new();
        for &id in q.iter() {
            if let Some(entry) = dl_map.get(&id) {
                let snap = self.get_snapshot(id).ok();
                items.push(PersistedItem {
                    id,
                    url: entry.url.to_string(),
                    dest: entry.dest.to_string_lossy().to_string(),
                    paused: entry.paused,
                    terminal_state: entry.terminal_state,
                    terminal_error: entry.terminal_error.clone(),
                    downloaded: snap.as_ref().map(|s| s.downloaded_bytes).unwrap_or(entry.downloaded_checkpoint),
                    total: snap.and_then(|s| s.total_bytes),
                });
            }
        }

        if let Ok(json) = serde_json::to_string_pretty(&items) {
            let tmp_file = dir.join("downloads_metadata.json.tmp");
            let final_file = dir.join("downloads_metadata.json");
            if std::fs::write(&tmp_file, json).is_ok() {
                let _ = std::fs::rename(tmp_file, final_file);
            }
        }
    }

    pub fn reconcile_queue(&self) {
        let max_running = *self.max_active_downloads.read();
        let mut dl_map = self.downloads.write();
        let q = self.queue_order.read();

        // 1. Clean up handles of downloads that finished or failed and retain terminal state
        let mut changed = false;
        for (_id, entry) in dl_map.iter_mut() {
            if let Some(handle) = &entry.handle {
                let snap = handle.snapshot();
                match snap.state {
                    State::Done => {
                        entry.downloaded_checkpoint = snap.downloaded;
                        entry.terminal_state = Some(MobileDownloadState::Done);
                        entry.last_snapshot = Some(snap);
                        entry.handle = None;
                        changed = true;
                    }
                    State::Failed => {
                        entry.downloaded_checkpoint = snap.downloaded;
                        entry.terminal_state = Some(MobileDownloadState::Failed);
                        entry.terminal_error = snap.error.clone();
                        entry.last_snapshot = Some(snap);
                        entry.handle = None;
                        changed = true;
                    }
                    State::Cancelled => {
                        entry.downloaded_checkpoint = snap.downloaded;
                        entry.terminal_state = Some(MobileDownloadState::Cancelled);
                        entry.last_snapshot = Some(snap);
                        entry.handle = None;
                        changed = true;
                    }
                    _ => {}
                }
            }
        }

        let currently_running = dl_map.values().filter(|e| e.handle.is_some()).count();
        let mut slots_available = max_running.saturating_sub(currently_running);

        if slots_available == 0 {
            if changed {
                drop(dl_map);
                drop(q);
                self.persist_to_disk();
            }
            return;
        }

        for &id in q.iter() {
            if slots_available == 0 {
                break;
            }
            if let Some(entry) = dl_map.get_mut(&id) {
                if !entry.paused && entry.terminal_state.is_none() && entry.handle.is_none() {
                    let s_file = state_path(&entry.dest);
                    let _guard = self.runtime.enter();
                    let handle = if s_file.exists() {
                        Download::resume(s_file, reqwest::header::HeaderMap::new(), Options::default())
                            .unwrap_or_else(|_| {
                                let req = Request {
                                    url: entry.url.clone(),
                                    headers: reqwest::header::HeaderMap::new(),
                                    dest: Destination::File(entry.dest.clone()),
                                };
                                Download::start(req, Options::default())
                            })
                    } else {
                        let req = Request {
                            url: entry.url.clone(),
                            headers: reqwest::header::HeaderMap::new(),
                            dest: Destination::File(entry.dest.clone()),
                        };
                        Download::start(req, Options::default())
                    };

                    entry.handle = Some(handle);
                    slots_available -= 1;
                    changed = true;
                }
            }
        }

        drop(dl_map);
        drop(q);

        if changed {
            self.persist_to_disk();
        }
    }
}

pub mod ffi;
