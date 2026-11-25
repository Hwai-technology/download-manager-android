//! Mobile core bridge for Android and iOS native frontends.
//! Provides queue management, download control (start, pause, resume, cancel, remove),
//! state snapshots, persistence and FFI-safe boundaries.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};

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
    playlists: Arc<RwLock<HashMap<u64, playlist::MobilePlaylistSnapshot>>>,
    playlist_order: Arc<RwLock<Vec<u64>>>,
    max_active_playlist_items: Arc<RwLock<usize>>,
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
            playlists: Arc::new(RwLock::new(HashMap::new())),
            playlist_order: Arc::new(RwLock::new(Vec::new())),
            max_active_playlist_items: Arc::new(RwLock::new(2)),
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

    pub fn create_download(&self, url_str: &str, dest_path: &str) -> Result<u64, MobileError> {
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

        let (state, total, downloaded, speed, resumable, error) =
            if let Some(handle) = &entry.handle {
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
                let (total, down, res) = entry
                    .last_snapshot
                    .as_ref()
                    .map(|s| (s.size, s.downloaded, s.resumable.unwrap_or(true)))
                    .unwrap_or((None, entry.downloaded_checkpoint, true));
                (term, total, down, None, res, entry.terminal_error.clone())
            } else if entry.paused {
                let (total, down, res) = entry
                    .last_snapshot
                    .as_ref()
                    .map(|s| (s.size, s.downloaded, s.resumable.unwrap_or(true)))
                    .unwrap_or((None, entry.downloaded_checkpoint, true));
                (MobileDownloadState::Paused, total, down, None, res, None)
            } else {
                (
                    MobileDownloadState::Waiting,
                    None,
                    entry.downloaded_checkpoint,
                    None,
                    true,
                    None,
                )
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

    // ==========================================
    // Playlist Management Methods
    // ==========================================

    pub fn discover_playlist(&self, url_str: &str) -> Result<playlist::DiscoveredPlaylist, MobileError> {
        playlist::discover_playlist_sync(url_str).map_err(MobileError::Engine)
    }

    pub fn create_playlist(
        &self,
        url_str: &str,
        dest_parent_dir: &str,
        custom_title: Option<&str>,
        selected_indices: Option<Vec<u64>>,
    ) -> Result<u64, MobileError> {
        let discovered = self.discover_playlist(url_str)?;
        let title = custom_title.unwrap_or(&discovered.title);
        let safe_folder_name = mdm_engine::naming::sanitize(title);
        let folder_name = if safe_folder_name.trim().is_empty() { "Playlist" } else { safe_folder_name.trim() };
        let dest_folder = PathBuf::from(dest_parent_dir).join(folder_name);
        let _ = std::fs::create_dir_all(&dest_folder);

        let id = self.next_id.fetch_add(1, Ordering::SeqCst);
        let total_count = discovered.items.len();

        let mut items = Vec::new();
        for item in discovered.items {
            let should_include = match &selected_indices {
                Some(indices) => indices.contains(&item.index),
                None => !item.unavailable,
            };

            let status = if item.unavailable {
                playlist::MobilePlaylistItemStatus::Unavailable
            } else if should_include {
                playlist::MobilePlaylistItemStatus::Pending
            } else {
                playlist::MobilePlaylistItemStatus::Skipped
            };

            let filename = playlist::format_numbered_filename(item.index, total_count, &item.title, "mp4");
            let item_dest = dest_folder.join(filename);

            items.push(playlist::MobilePlaylistItem {
                index: item.index,
                download_id: None,
                url: item.url,
                title: item.title,
                duration_secs: item.duration_secs,
                status,
                dest_file: Some(item_dest.to_string_lossy().to_string()),
                downloaded_bytes: 0,
                total_bytes: None,
                speed_bytes_per_sec: None,
                error: None,
            });
        }

        let mut snapshot = playlist::MobilePlaylistSnapshot {
            id,
            url: url_str.to_string(),
            title: title.to_string(),
            dest_folder: dest_folder.to_string_lossy().to_string(),
            state: playlist::MobilePlaylistState::Created,
            items,
            total_items: 0,
            completed_items: 0,
            failed_items: 0,
            active_items: 0,
            skipped_items: 0,
            total_downloaded_bytes: 0,
            total_expected_bytes: None,
            aggregate_speed_bytes_per_sec: 0,
            progress_fraction: 0.0,
            eta_seconds: None,
        };
        snapshot.compute_aggregates();

        {
            let mut p_map = self.playlists.write();
            p_map.insert(id, snapshot);
            let mut po = self.playlist_order.write();
            po.push(id);
        }

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(id)
    }

    pub fn start_playlist(&self, id: u64) -> Result<(), MobileError> {
        let mut p_map = self.playlists.write();
        let pl = p_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;
        pl.state = playlist::MobilePlaylistState::Downloading;
        for item in pl.items.iter_mut() {
            if item.status == playlist::MobilePlaylistItemStatus::Pending || item.status == playlist::MobilePlaylistItemStatus::Paused {
                item.status = playlist::MobilePlaylistItemStatus::Queued;
            }
        }
        pl.compute_aggregates();
        drop(p_map);

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn pause_playlist(&self, id: u64) -> Result<(), MobileError> {
        let mut p_map = self.playlists.write();
        let pl = p_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;
        pl.state = playlist::MobilePlaylistState::Paused;

        let dl_ids: Vec<u64> = pl.items.iter().filter_map(|it| it.download_id).collect();
        for item in pl.items.iter_mut() {
            if item.status == playlist::MobilePlaylistItemStatus::Downloading || item.status == playlist::MobilePlaylistItemStatus::Queued {
                item.status = playlist::MobilePlaylistItemStatus::Paused;
            }
        }
        pl.compute_aggregates();
        drop(p_map);

        for dl_id in dl_ids {
            let _ = self.pause_download(dl_id);
        }

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn resume_playlist(&self, id: u64) -> Result<(), MobileError> {
        self.start_playlist(id)
    }

    pub fn cancel_playlist(&self, id: u64) -> Result<(), MobileError> {
        let mut p_map = self.playlists.write();
        let pl = p_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;
        pl.state = playlist::MobilePlaylistState::Cancelled;

        let dl_ids: Vec<u64> = pl.items.iter().filter_map(|it| it.download_id).collect();
        for item in pl.items.iter_mut() {
            if item.status != playlist::MobilePlaylistItemStatus::Completed {
                item.status = playlist::MobilePlaylistItemStatus::Cancelled;
            }
        }
        pl.compute_aggregates();
        drop(p_map);

        for dl_id in dl_ids {
            let _ = self.cancel_download(dl_id);
        }

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn retry_failed_playlist_items(&self, id: u64) -> Result<(), MobileError> {
        let mut p_map = self.playlists.write();
        let pl = p_map.get_mut(&id).ok_or(MobileError::NotFound(id))?;
        pl.state = playlist::MobilePlaylistState::Downloading;

        for item in pl.items.iter_mut() {
            if item.status == playlist::MobilePlaylistItemStatus::Failed || item.status == playlist::MobilePlaylistItemStatus::Cancelled {
                item.status = playlist::MobilePlaylistItemStatus::Queued;
                item.download_id = None;
                item.error = None;
            }
        }
        pl.compute_aggregates();
        drop(p_map);

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn remove_playlist(&self, id: u64, delete_files: bool) -> Result<(), MobileError> {
        let mut p_map = self.playlists.write();
        let pl = p_map.remove(&id).ok_or(MobileError::NotFound(id))?;
        drop(p_map);

        let mut po = self.playlist_order.write();
        po.retain(|&p_id| p_id != id);
        drop(po);

        for item in &pl.items {
            if let Some(dl_id) = item.download_id {
                let _ = self.remove_download(dl_id);
            }
        }

        if delete_files {
            let folder = PathBuf::from(&pl.dest_folder);
            if folder.exists() {
                let _ = std::fs::remove_dir_all(&folder);
            }
        }

        self.persist_to_disk();
        self.reconcile_queue();
        Ok(())
    }

    pub fn get_playlist_snapshot(&self, id: u64) -> Result<playlist::MobilePlaylistSnapshot, MobileError> {
        let p_map = self.playlists.read();
        p_map.get(&id).cloned().ok_or(MobileError::NotFound(id))
    }

    pub fn get_all_playlists(&self) -> Vec<playlist::MobilePlaylistSnapshot> {
        let po = self.playlist_order.read().clone();
        let p_map = self.playlists.read();
        let mut list = Vec::new();
        for id in po {
            if let Some(pl) = p_map.get(&id) {
                list.push(pl.clone());
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

        // Restore Playlists
        let pl_file = dir.join("playlists_metadata.json");
        if pl_file.exists() {
            if let Ok(content) = std::fs::read_to_string(&pl_file) {
                if let Ok(saved_playlists) = serde_json::from_str::<Vec<playlist::MobilePlaylistSnapshot>>(&content) {
                    let mut p_map = self.playlists.write();
                    let mut po = self.playlist_order.write();
                    for mut pl in saved_playlists {
                        if pl.id > max_id {
                            max_id = pl.id;
                        }
                        pl.compute_aggregates();
                        p_map.insert(pl.id, pl.clone());
                        if !po.contains(&pl.id) {
                            po.push(pl.id);
                        }
                    }
                }
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
                    downloaded: snap
                        .as_ref()
                        .map(|s| s.downloaded_bytes)
                        .unwrap_or(entry.downloaded_checkpoint),
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

        // Persist playlists
        let p_map = self.playlists.read();
        let po = self.playlist_order.read();
        let mut pl_list = Vec::new();
        for &id in po.iter() {
            if let Some(pl) = p_map.get(&id) {
                pl_list.push(pl.clone());
            }
        }
        if let Ok(json) = serde_json::to_string_pretty(&pl_list) {
            let tmp_pl = dir.join("playlists_metadata.json.tmp");
            let final_pl = dir.join("playlists_metadata.json");
            if std::fs::write(&tmp_pl, json).is_ok() {
                let _ = std::fs::rename(tmp_pl, final_pl);
            }
        }
    }

    pub fn reconcile_queue(&self) {
        let max_running = *self.max_active_downloads.read();
        let mut dl_map = self.downloads.write();

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

        // 2. Synchronize and schedule playlists
        {
            let mut p_map = self.playlists.write();
            let max_playlist_items = *self.max_active_playlist_items.read();

            for (_p_id, pl) in p_map.iter_mut() {
                // A. Update items from existing downloads
                for item in pl.items.iter_mut() {
                    if let Some(dl_id) = item.download_id {
                        if let Some(entry) = dl_map.get(&dl_id) {
                            if let Some(snap) = &entry.last_snapshot {
                                item.downloaded_bytes = snap.downloaded;
                                item.total_bytes = snap.size;
                            } else {
                                item.downloaded_bytes = entry.downloaded_checkpoint;
                            }

                            if let Some(handle) = &entry.handle {
                                let s = handle.snapshot();
                                item.downloaded_bytes = s.downloaded;
                                item.total_bytes = s.size;
                                let spd: u64 = s.connections.iter().map(|c| c.speed).sum();
                                item.speed_bytes_per_sec = Some(spd);
                                item.status = playlist::MobilePlaylistItemStatus::Downloading;
                            } else if let Some(term) = entry.terminal_state {
                                item.speed_bytes_per_sec = None;
                                match term {
                                    MobileDownloadState::Done => {
                                        item.status = playlist::MobilePlaylistItemStatus::Completed;
                                    }
                                    MobileDownloadState::Failed => {
                                        item.status = playlist::MobilePlaylistItemStatus::Failed;
                                        item.error = entry.terminal_error.clone();
                                    }
                                    MobileDownloadState::Cancelled => {
                                        item.status = playlist::MobilePlaylistItemStatus::Cancelled;
                                    }
                                    _ => {}
                                }
                            } else if entry.paused {
                                item.speed_bytes_per_sec = None;
                                item.status = playlist::MobilePlaylistItemStatus::Paused;
                            }
                        }
                    }
                }

                // B. If playlist is Downloading, schedule items up to limit
                if pl.state == playlist::MobilePlaylistState::Downloading {
                    let active_count = pl.items.iter().filter(|it| it.status == playlist::MobilePlaylistItemStatus::Downloading).count();
                    let mut can_schedule = max_playlist_items.saturating_sub(active_count);

                    for item in pl.items.iter_mut() {
                        if can_schedule == 0 {
                            break;
                        }
                        if item.status == playlist::MobilePlaylistItemStatus::Queued {
                            if let Some(dest_file) = &item.dest_file {
                                if let Ok(parsed_url) = Url::parse(&item.url) {
                                    let item_dl_id = self.next_id.fetch_add(1, Ordering::SeqCst);
                                    let active = ActiveDownload {
                                        url: parsed_url,
                                        dest: PathBuf::from(dest_file),
                                        handle: None,
                                        paused: false,
                                        terminal_state: None,
                                        terminal_error: None,
                                        last_snapshot: None,
                                        downloaded_checkpoint: 0,
                                    };
                                    dl_map.insert(item_dl_id, active);
                                    self.queue_order.write().push(item_dl_id);

                                    item.download_id = Some(item_dl_id);
                                    item.status = playlist::MobilePlaylistItemStatus::Downloading;
                                    can_schedule -= 1;
                                    changed = true;
                                }
                            }
                        }
                    }
                }

                // C. Recompute aggregates and determine completion
                pl.compute_aggregates();
                if pl.state == playlist::MobilePlaylistState::Downloading {
                    let all_terminal = pl.items.iter().all(|it| matches!(
                        it.status,
                        playlist::MobilePlaylistItemStatus::Completed |
                        playlist::MobilePlaylistItemStatus::Failed |
                        playlist::MobilePlaylistItemStatus::Skipped |
                        playlist::MobilePlaylistItemStatus::Unavailable |
                        playlist::MobilePlaylistItemStatus::Cancelled
                    ));
                    if all_terminal {
                        if pl.failed_items > 0 {
                            pl.state = playlist::MobilePlaylistState::CompletedWithErrors;
                        } else {
                            pl.state = playlist::MobilePlaylistState::Completed;
                        }
                        changed = true;
                    }
                }
            }
        }

        let currently_running = dl_map.values().filter(|e| e.handle.is_some()).count();
        let mut slots_available = max_running.saturating_sub(currently_running);

        if slots_available == 0 {
            if changed {
                drop(dl_map);
                self.persist_to_disk();
            }
            return;
        }

        let current_queue = self.queue_order.read().clone();
        for &id in current_queue.iter() {
            if slots_available == 0 {
                break;
            }
            if let Some(entry) = dl_map.get_mut(&id) {
                if !entry.paused && entry.terminal_state.is_none() && entry.handle.is_none() {
                    let s_file = state_path(&entry.dest);
                    let _guard = self.runtime.enter();
                    let handle = if s_file.exists() {
                        Download::resume(
                            s_file,
                            reqwest::header::HeaderMap::new(),
                            Options::default(),
                        )
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

        if changed {
            self.persist_to_disk();
        }
    }

    pub fn extract_video_metadata(&self, url_str: &str) -> Result<metadata::MobileVideoMetadata, MobileError> {
        metadata::extract_metadata_sync(url_str).map_err(MobileError::Engine)
    }
}

pub mod metadata;
pub mod playlist;
pub mod ffi;
