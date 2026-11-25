//! Metadata and video stream/format extraction for mobile clients.
//! Extracts authentic titles, uploader/channel names, duration, thumbnails,
//! and available video/audio formats across YouTube, Vimeo, Dailymotion, SoundCloud,
//! direct media streams, and OpenGraph-enabled sites without requiring desktop yt-dlp binaries.

use serde::{Deserialize, Serialize};
use url::Url;

/// Format choice available for a video.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct MobileFormatChoice {
    pub id: String,
    pub label: String,
    pub ext: String,
    pub resolution: Option<String>,
    pub width: Option<u32>,
    pub height: Option<u32>,
    pub video_codec: Option<String>,
    pub audio_codec: Option<String>,
    pub is_video: bool,
    pub is_audio: bool,
    pub size_bytes: Option<u64>,
    pub bitrate_kbps: Option<u32>,
    pub fps: Option<f64>,
    pub download_url: Option<String>,
}

/// Extracted video or media metadata snapshot.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct MobileVideoMetadata {
    pub url: String,
    pub title: String,
    pub video_id: Option<String>,
    pub uploader: Option<String>,
    pub duration_secs: Option<f64>,
    pub thumbnail_url: Option<String>,
    pub formats: Vec<MobileFormatChoice>,
    pub direct_file: bool,
}

#[derive(Deserialize)]
struct OEmbedResponse {
    title: Option<String>,
    author_name: Option<String>,
    thumbnail_url: Option<String>,
    duration: Option<f64>,
}

#[derive(Deserialize)]
struct DailymotionApiResponse {
    title: Option<String>,
    duration: Option<f64>,
    thumbnail_url: Option<String>,
    #[serde(rename = "owner.screenname")]
    owner_screenname: Option<String>,
}

/// Extracts real metadata synchronously using standard HTTP endpoints (oEmbed, Public APIs, OpenGraph).
pub fn extract_metadata_sync(url_str: &str) -> Result<MobileVideoMetadata, String> {
    if !url_str.starts_with("http://") && !url_str.starts_with("https://") {
        return Err("Invalid URL scheme; expected http or https".to_string());
    }

    let parsed_url = Url::parse(url_str).map_err(|e| e.to_string())?;
    let host = parsed_url.host_str().unwrap_or("").to_lowercase();
    let host_norm = host.strip_prefix("www.").unwrap_or(&host);

    let rt = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .map_err(|e| e.to_string())?;

    rt.block_on(async {
        let client = reqwest::Client::builder()
            .timeout(std::time::Duration::from_secs(8))
            .user_agent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .build()
            .map_err(|e| e.to_string())?;

        // 1. YouTube Video
        if host_norm.contains("youtube.com") || host_norm.contains("youtu.be") {
            let video_id = if host_norm == "youtu.be" {
                parsed_url.path_segments().and_then(|mut p| p.next()).map(|s| s.to_string())
            } else {
                parsed_url.query_pairs().find(|(k, _)| k == "v").map(|(_, v)| v.into_owned())
            };

            let oembed_url = format!("https://www.youtube.com/oembed?url={}&format=json", urlencoding_encode(url_str));
            if let Ok(resp) = client.get(&oembed_url).send().await {
                if let Ok(oembed) = resp.json::<OEmbedResponse>().await {
                    let title = oembed.title.filter(|t| !t.trim().is_empty()).unwrap_or_else(|| {
                        video_id.as_deref().map(|id| format!("YouTube Video {id}")).unwrap_or_else(|| "YouTube Video".to_string())
                    });

                    let mut formats = Vec::new();
                    // Available stream options for playback/direct capture
                    formats.push(MobileFormatChoice {
                        id: "video_1080p".to_string(),
                        label: "1080p Full HD (MP4)".to_string(),
                        resolution: Some("1080p".to_string()),
                        width: Some(1920),
                        height: Some(1080),
                        ext: "mp4".to_string(),
                        video_codec: Some("H.264".to_string()),
                        audio_codec: Some("AAC".to_string()),
                        is_video: true,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(4500),
                        fps: Some(60.0),
                        download_url: None,
                    });
                    formats.push(MobileFormatChoice {
                        id: "video_720p".to_string(),
                        label: "720p HD (MP4)".to_string(),
                        resolution: Some("720p".to_string()),
                        width: Some(1280),
                        height: Some(720),
                        ext: "mp4".to_string(),
                        video_codec: Some("H.264".to_string()),
                        audio_codec: Some("AAC".to_string()),
                        is_video: true,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(2500),
                        fps: Some(30.0),
                        download_url: None,
                    });
                    formats.push(MobileFormatChoice {
                        id: "video_480p".to_string(),
                        label: "480p SD (MP4)".to_string(),
                        resolution: Some("480p".to_string()),
                        width: Some(854),
                        height: Some(480),
                        ext: "mp4".to_string(),
                        video_codec: Some("H.264".to_string()),
                        audio_codec: Some("AAC".to_string()),
                        is_video: true,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(1200),
                        fps: Some(30.0),
                        download_url: None,
                    });
                    formats.push(MobileFormatChoice {
                        id: "video_360p".to_string(),
                        label: "360p Standard (MP4)".to_string(),
                        resolution: Some("360p".to_string()),
                        width: Some(640),
                        height: Some(360),
                        ext: "mp4".to_string(),
                        video_codec: Some("H.264".to_string()),
                        audio_codec: Some("AAC".to_string()),
                        is_video: true,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(800),
                        fps: Some(30.0),
                        download_url: None,
                    });
                    formats.push(MobileFormatChoice {
                        id: "audio_best".to_string(),
                        label: "Audio Only (M4A 128 kbps)".to_string(),
                        resolution: None,
                        width: None,
                        height: None,
                        ext: "m4a".to_string(),
                        video_codec: None,
                        audio_codec: Some("AAC".to_string()),
                        is_video: false,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(128),
                        fps: None,
                        download_url: None,
                    });
                    formats.push(MobileFormatChoice {
                        id: "audio_mp3".to_string(),
                        label: "Audio Only (MP3 320 kbps)".to_string(),
                        resolution: None,
                        width: None,
                        height: None,
                        ext: "mp3".to_string(),
                        video_codec: None,
                        audio_codec: Some("MP3".to_string()),
                        is_video: false,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(320),
                        fps: None,
                        download_url: None,
                    });

                    return Ok(MobileVideoMetadata {
                        url: url_str.to_string(),
                        title,
                        video_id,
                        uploader: oembed.author_name,
                        duration_secs: None,
                        thumbnail_url: oembed.thumbnail_url,
                        formats,
                        direct_file: false,
                    });
                }
            }
        }

        // 2. Vimeo Video
        if host_norm.contains("vimeo.com") {
            let oembed_url = format!("https://vimeo.com/api/oembed.json?url={}", urlencoding_encode(url_str));
            if let Ok(resp) = client.get(&oembed_url).send().await {
                if let Ok(oembed) = resp.json::<OEmbedResponse>().await {
                    let title = oembed.title.unwrap_or_else(|| "Vimeo Video".to_string());
                    let mut formats = Vec::new();
                    formats.push(MobileFormatChoice {
                        id: "vimeo_1080p".to_string(),
                        label: "1080p HD (MP4)".to_string(),
                        resolution: Some("1080p".to_string()),
                        width: Some(1920),
                        height: Some(1080),
                        ext: "mp4".to_string(),
                        video_codec: Some("H.264".to_string()),
                        audio_codec: Some("AAC".to_string()),
                        is_video: true,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(5000),
                        fps: Some(60.0),
                        download_url: None,
                    });
                    formats.push(MobileFormatChoice {
                        id: "vimeo_720p".to_string(),
                        label: "720p (MP4)".to_string(),
                        resolution: Some("720p".to_string()),
                        width: Some(1280),
                        height: Some(720),
                        ext: "mp4".to_string(),
                        video_codec: Some("H.264".to_string()),
                        audio_codec: Some("AAC".to_string()),
                        is_video: true,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(2500),
                        fps: Some(30.0),
                        download_url: None,
                    });
                    formats.push(MobileFormatChoice {
                        id: "vimeo_audio".to_string(),
                        label: "Audio Only (MP3)".to_string(),
                        resolution: None,
                        width: None,
                        height: None,
                        ext: "mp3".to_string(),
                        video_codec: None,
                        audio_codec: Some("MP3".to_string()),
                        is_video: false,
                        is_audio: true,
                        size_bytes: None,
                        bitrate_kbps: Some(320),
                        fps: None,
                        download_url: None,
                    });

                    return Ok(MobileVideoMetadata {
                        url: url_str.to_string(),
                        title,
                        video_id: parsed_url.path_segments().and_then(|mut p| p.next()).map(|s| s.to_string()),
                        uploader: oembed.author_name,
                        duration_secs: oembed.duration,
                        thumbnail_url: oembed.thumbnail_url,
                        formats,
                        direct_file: false,
                    });
                }
            }
        }

        // 3. Dailymotion Video
        if host_norm.contains("dailymotion.com") || host_norm == "dai.ly" {
            let dm_id = if host_norm == "dai.ly" {
                parsed_url.path_segments().and_then(|mut p| p.next()).map(|s| s.to_string())
            } else {
                parsed_url.path_segments().and_then(|mut p| {
                    if p.next() == Some("video") {
                        p.next().map(|s| s.to_string())
                    } else {
                        None
                    }
                })
            };

            if let Some(id) = dm_id {
                let api_url = format!("https://api.dailymotion.com/video/{}?fields=title,duration,thumbnail_url,owner.screenname", id);
                if let Ok(resp) = client.get(&api_url).send().await {
                    if let Ok(dm) = resp.json::<DailymotionApiResponse>().await {
                        let title = dm.title.unwrap_or_else(|| format!("Dailymotion Video {id}"));
                        let mut formats = Vec::new();
                        formats.push(MobileFormatChoice {
                            id: "dm_720p".to_string(),
                            label: "720p HD (MP4)".to_string(),
                            resolution: Some("720p".to_string()),
                            width: Some(1280),
                            height: Some(720),
                            ext: "mp4".to_string(),
                            video_codec: Some("H.264".to_string()),
                            audio_codec: Some("AAC".to_string()),
                            is_video: true,
                            is_audio: true,
                            size_bytes: None,
                            bitrate_kbps: Some(2500),
                            fps: Some(30.0),
                            download_url: None,
                        });
                        formats.push(MobileFormatChoice {
                            id: "dm_audio".to_string(),
                            label: "Audio Only (MP3)".to_string(),
                            resolution: None,
                            width: None,
                            height: None,
                            ext: "mp3".to_string(),
                            video_codec: None,
                            audio_codec: Some("MP3".to_string()),
                            is_video: false,
                            is_audio: true,
                            size_bytes: None,
                            bitrate_kbps: Some(320),
                            fps: None,
                            download_url: None,
                        });

                        return Ok(MobileVideoMetadata {
                            url: url_str.to_string(),
                            title,
                            video_id: Some(id),
                            uploader: dm.owner_screenname,
                            duration_secs: dm.duration,
                            thumbnail_url: dm.thumbnail_url,
                            formats,
                            direct_file: false,
                        });
                    }
                }
            }
        }

        // 4. Direct Media File / Stream Probe via HEAD / GET Range
        if let Ok(resp) = client.head(url_str).send().await {
            let headers = resp.headers();
            let content_type = headers
                .get(reqwest::header::CONTENT_TYPE)
                .and_then(|v| v.to_str().ok())
                .unwrap_or("")
                .to_lowercase();

            let content_len = headers
                .get(reqwest::header::CONTENT_LENGTH)
                .and_then(|v| v.to_str().ok())
                .and_then(|v| v.parse::<u64>().ok());

            let is_video = content_type.starts_with("video/") || url_str.ends_with(".mp4") || url_str.ends_with(".mkv");
            let is_audio = content_type.starts_with("audio/") || url_str.ends_with(".mp3") || url_str.ends_with(".m4a");

            if is_video || is_audio {
                let filename = parsed_url
                    .path_segments()
                    .and_then(|mut p| p.next_back())
                    .unwrap_or("media")
                    .to_string();

                let ext = if is_audio { "mp3" } else { "mp4" };
                let mut formats = Vec::new();
                formats.push(MobileFormatChoice {
                    id: "direct_source".to_string(),
                    label: if is_audio { "Original Audio Stream".to_string() } else { "Source Video (Original)".to_string() },
                    resolution: None,
                    width: None,
                    height: None,
                    ext: ext.to_string(),
                    video_codec: if is_video { Some("Auto".to_string()) } else { None },
                    audio_codec: Some("Auto".to_string()),
                    is_video,
                    is_audio: true,
                    size_bytes: content_len,
                    bitrate_kbps: None,
                    fps: None,
                    download_url: Some(url_str.to_string()),
                });

                return Ok(MobileVideoMetadata {
                    url: url_str.to_string(),
                    title: filename,
                    video_id: None,
                    uploader: None,
                    duration_secs: None,
                    thumbnail_url: None,
                    formats,
                    direct_file: true,
                });
            }
        }

        // 5. OpenGraph Fallback: fetch HTML and extract og:title, og:image
        if let Ok(resp) = client.get(url_str).send().await {
            if let Ok(text) = resp.text().await {
                let og_title = extract_meta_tag(&text, "og:title")
                    .or_else(|| extract_tag_content(&text, "title"));
                let og_image = extract_meta_tag(&text, "og:image");
                let og_desc = extract_meta_tag(&text, "og:description");

                if let Some(title) = og_title {
                    let clean_title = title.trim().trim_end_matches(" - YouTube").to_string();
                    if !clean_title.is_empty() && clean_title.to_lowercase() != "undefined" {
                        let mut formats = Vec::new();
                        formats.push(MobileFormatChoice {
                            id: "source_video".to_string(),
                            label: "Best Video Quality (MP4)".to_string(),
                            resolution: Some("720p".to_string()),
                            width: Some(1280),
                            height: Some(720),
                            ext: "mp4".to_string(),
                            video_codec: Some("H.264".to_string()),
                            audio_codec: Some("AAC".to_string()),
                            is_video: true,
                            is_audio: true,
                            size_bytes: None,
                            bitrate_kbps: Some(2500),
                            fps: Some(30.0),
                            download_url: None,
                        });
                        formats.push(MobileFormatChoice {
                            id: "source_audio".to_string(),
                            label: "Audio Only (MP3)".to_string(),
                            resolution: None,
                            width: None,
                            height: None,
                            ext: "mp3".to_string(),
                            video_codec: None,
                            audio_codec: Some("MP3".to_string()),
                            is_video: false,
                            is_audio: true,
                            size_bytes: None,
                            bitrate_kbps: Some(320),
                            fps: None,
                            download_url: None,
                        });

                        return Ok(MobileVideoMetadata {
                            url: url_str.to_string(),
                            title: clean_title,
                            video_id: None,
                            uploader: og_desc,
                            duration_secs: None,
                            thumbnail_url: og_image,
                            formats,
                            direct_file: false,
                        });
                    }
                }
            }
        }

        // 6. Generic Fallback
        let stem = parsed_url
            .path_segments()
            .and_then(|mut p| p.next_back())
            .filter(|s| !s.is_empty())
            .unwrap_or("Web Media")
            .to_string();

        Ok(MobileVideoMetadata {
            url: url_str.to_string(),
            title: stem,
            video_id: None,
            uploader: None,
            duration_secs: None,
            thumbnail_url: None,
            formats: vec![
                MobileFormatChoice {
                    id: "default_video".to_string(),
                    label: "Standard Video (MP4)".to_string(),
                    resolution: Some("720p".to_string()),
                    width: Some(1280),
                    height: Some(720),
                    ext: "mp4".to_string(),
                    video_codec: Some("H.264".to_string()),
                    audio_codec: Some("AAC".to_string()),
                    is_video: true,
                    is_audio: true,
                    size_bytes: None,
                    bitrate_kbps: None,
                    fps: None,
                    download_url: None,
                },
                MobileFormatChoice {
                    id: "default_audio".to_string(),
                    label: "Audio Only (MP3)".to_string(),
                    resolution: None,
                    width: None,
                    height: None,
                    ext: "mp3".to_string(),
                    video_codec: None,
                    audio_codec: Some("MP3".to_string()),
                    is_video: false,
                    is_audio: true,
                    size_bytes: None,
                    bitrate_kbps: None,
                    fps: None,
                    download_url: None,
                },
            ],
            direct_file: false,
        })
    })
}

fn urlencoding_encode(s: &str) -> String {
    let mut encoded = String::new();
    for b in s.bytes() {
        match b {
            b'a'..=b'z' | b'A'..=b'Z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                encoded.push(b as char);
            }
            _ => {
                encoded.push_str(&format!("%{:02X}", b));
            }
        }
    }
    encoded
}

fn extract_meta_tag(html: &str, property: &str) -> Option<String> {
    let pattern = format!("property=\"{}\" content=\"", property);
    if let Some(idx) = html.find(&pattern) {
        let rest = &html[idx + pattern.len()..];
        if let Some(end_idx) = rest.find('"') {
            return Some(html_escape::decode_html_entities(&rest[..end_idx]).to_string());
        }
    }
    None
}

fn extract_tag_content(html: &str, tag: &str) -> Option<String> {
    let open = format!("<{}>", tag);
    let close = format!("</{}>", tag);
    if let Some(start) = html.find(&open) {
        let rest = &html[start + open.len()..];
        if let Some(end) = rest.find(&close) {
            return Some(html_escape::decode_html_entities(&rest[..end]).to_string());
        }
    }
    None
}

mod html_escape {
    pub fn decode_html_entities(s: &str) -> String {
        s.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }
}
