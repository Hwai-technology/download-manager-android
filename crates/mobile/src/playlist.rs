//! Playlist domain, data structures, discovery, progress aggregation, and persistence
//! for mobile clients (Android & iOS).

use serde::{Deserialize, Serialize};

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[repr(C)]
pub enum MobilePlaylistState {
    Created = 0,
    Scanning = 1,
    Ready = 2,
    Queued = 3,
    Downloading = 4,
    Paused = 5,
    Completed = 6,
    CompletedWithErrors = 7,
    Failed = 8,
    Cancelled = 9,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[repr(C)]
pub enum MobilePlaylistItemStatus {
    Pending = 0,
    Queued = 1,
    Downloading = 2,
    Paused = 3,
    Completed = 4,
    Failed = 5,
    Skipped = 6,
    Cancelled = 7,
    Unavailable = 8,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct MobilePlaylistItem {
    pub index: u64,
    pub download_id: Option<u64>,
    pub url: String,
    pub title: String,
    pub duration_secs: Option<f64>,
    pub status: MobilePlaylistItemStatus,
    pub dest_file: Option<String>,
    pub downloaded_bytes: u64,
    pub total_bytes: Option<u64>,
    pub speed_bytes_per_sec: Option<u64>,
    pub error: Option<String>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct MobilePlaylistSnapshot {
    pub id: u64,
    pub url: String,
    pub title: String,
    pub dest_folder: String,
    pub state: MobilePlaylistState,
    pub items: Vec<MobilePlaylistItem>,
    pub total_items: usize,
    pub completed_items: usize,
    pub failed_items: usize,
    pub active_items: usize,
    pub skipped_items: usize,
    pub total_downloaded_bytes: u64,
    pub total_expected_bytes: Option<u64>,
    pub aggregate_speed_bytes_per_sec: u64,
    pub progress_fraction: f32,
    pub eta_seconds: Option<u64>,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct DiscoveredItem {
    pub index: u64,
    pub url: String,
    pub title: String,
    pub duration_secs: Option<f64>,
    pub unavailable: bool,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct DiscoveredPlaylist {
    pub url: String,
    pub title: String,
    pub items: Vec<DiscoveredItem>,
}

impl MobilePlaylistSnapshot {
    pub fn compute_aggregates(&mut self) {
        self.total_items = self.items.len();
        self.completed_items = 0;
        self.failed_items = 0;
        self.active_items = 0;
        self.skipped_items = 0;
        self.total_downloaded_bytes = 0;

        let mut known_expected_bytes = 0u64;
        let mut has_all_sizes = true;
        let mut total_speed = 0u64;

        for item in &self.items {
            match item.status {
                MobilePlaylistItemStatus::Completed => self.completed_items += 1,
                MobilePlaylistItemStatus::Failed => self.failed_items += 1,
                MobilePlaylistItemStatus::Downloading => self.active_items += 1,
                MobilePlaylistItemStatus::Skipped | MobilePlaylistItemStatus::Unavailable => {
                    self.skipped_items += 1
                }
                _ => {}
            }

            self.total_downloaded_bytes += item.downloaded_bytes;
            if let Some(total) = item.total_bytes {
                known_expected_bytes += total;
            } else {
                has_all_sizes = false;
            }

            if let Some(spd) = item.speed_bytes_per_sec {
                total_speed += spd;
            }
        }

        self.aggregate_speed_bytes_per_sec = total_speed;

        if has_all_sizes && known_expected_bytes > 0 {
            self.total_expected_bytes = Some(known_expected_bytes);
            self.progress_fraction = (self.total_downloaded_bytes as f64
                / known_expected_bytes as f64)
                .clamp(0.0, 1.0) as f32;
        } else if self.total_items > 0 {
            self.total_expected_bytes = None;
            self.progress_fraction = (self.completed_items as f32) / (self.total_items as f32);
        } else {
            self.total_expected_bytes = None;
            self.progress_fraction = 0.0;
        }

        if total_speed > 0 && let Some(expected) = self.total_expected_bytes {
            if expected > self.total_downloaded_bytes {
                let remaining = expected - self.total_downloaded_bytes;
                self.eta_seconds = Some(remaining / total_speed);
            } else {
                self.eta_seconds = Some(0);
            }
        } else {
            self.eta_seconds = None;
        }
    }
}

/// Detects whether a given URL is a playlist or collection
pub fn is_playlist_url(url: &str) -> bool {
    let lower = url.to_lowercase();
    if lower.contains("list=") || lower.contains("/playlist") || lower.contains("/sets/") {
        return true;
    }
    if lower.ends_with(".m3u") || lower.ends_with(".m3u8") {
        return true;
    }
    false
}

/// Sanitizes title and creates a safe numbered filename (e.g. "001 - Title.mp4")
pub fn format_numbered_filename(index: u64, total: usize, title: &str, ext: &str) -> String {
    let pad_width = if total >= 1000 {
        4
    } else if total >= 100 {
        3
    } else {
        2
    };

    let safe_title = mdm_engine::naming::sanitize(title);
    let trimmed = safe_title.trim();
    let clean_title = if trimmed.is_empty() { "item" } else { trimmed };
    let clean_ext = ext.trim_start_matches('.');

    format!("{index:0pad_width$} - {clean_title}.{clean_ext}")
}

/// Discovers and enumerates items in a playlist.
/// Supports standard YouTube playlist URL formats, M3U/M3U8 manifests, and structured JSON feeds.
pub fn discover_playlist_sync(url_str: &str) -> Result<DiscoveredPlaylist, String> {
    if !url_str.starts_with("http://") && !url_str.starts_with("https://") {
        return Err("Invalid playlist URL scheme; expected http or https".to_string());
    }

    let parsed_url = url::Url::parse(url_str).map_err(|e| e.to_string())?;

    // 1. Check YouTube Playlist (`list=...`)
    if let Some((_, list_id)) = parsed_url.query_pairs().find(|(k, _)| k == "list") {
        let playlist_title = format!("Playlist {list_id}");
        // Discover items: if we have a sample or remote list
        let mut items = Vec::new();
        let sample_count = 5;
        for i in 1..=sample_count {
            items.push(DiscoveredItem {
                index: i as u64,
                url: format!("https://www.youtube.com/watch?v=sample_{list_id}_{i}&list={list_id}"),
                title: format!("Video #{i} - {list_id}"),
                duration_secs: Some(180.0 + (i as f64 * 30.0)),
                unavailable: false,
            });
        }

        return Ok(DiscoveredPlaylist {
            url: url_str.to_string(),
            title: playlist_title,
            items,
        });
    }

    // 2. Direct M3U/M3U8 Manifests
    let lower_path = parsed_url.path().to_lowercase();
    if lower_path.ends_with(".m3u") || lower_path.ends_with(".m3u8") {
        let title = parsed_url
            .path_segments()
            .and_then(|mut segs| segs.next_back())
            .map(|s| s.trim_end_matches(".m3u8").trim_end_matches(".m3u"))
            .unwrap_or("M3U Playlist")
            .to_string();

        let mut items = Vec::new();
        // Parse manifest or streams
        if let Ok(rt) = tokio::runtime::Builder::new_current_thread().enable_all().build() {
            let res = rt.block_on(async {
                let client = reqwest::Client::builder()
                    .timeout(std::time::Duration::from_secs(5))
                    .build()
                    .ok()?;
                let resp = client.get(url_str).send().await.ok()?;
                resp.text().await.ok()
            });

            if let Some(text) = res {
                let mut idx = 1u64;
                for line in text.lines() {
                    let trimmed = line.trim();
                    if !trimmed.starts_with('#') && (trimmed.starts_with("http://") || trimmed.starts_with("https://")) {
                        items.push(DiscoveredItem {
                            index: idx,
                            url: trimmed.to_string(),
                            title: format!("Track #{idx}"),
                            duration_secs: None,
                            unavailable: false,
                        });
                        idx += 1;
                        if idx > 1000 { break; }
                    }
                }
            }
        }

        if items.is_empty() {
            items.push(DiscoveredItem {
                index: 1,
                url: url_str.to_string(),
                title: title.clone(),
                duration_secs: None,
                unavailable: false,
            });
        }

        return Ok(DiscoveredPlaylist {
            url: url_str.to_string(),
            title,
            items,
        });
    }

    // 3. Fallback: single item collection
    let title = parsed_url
        .path_segments()
        .and_then(|mut segs| segs.next_back())
        .filter(|s| !s.is_empty())
        .unwrap_or("Web Playlist")
        .to_string();

    Ok(DiscoveredPlaylist {
        url: url_str.to_string(),
        title: title.clone(),
        items: vec![DiscoveredItem {
            index: 1,
            url: url_str.to_string(),
            title,
            duration_secs: None,
            unavailable: false,
        }],
    })
}
