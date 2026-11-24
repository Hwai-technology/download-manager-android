use mdm_mobile::playlist::*;
use mdm_mobile::MobileEngine;
use tempfile::tempdir;

#[test]
fn test_playlist_creation_and_aggregates() {
    let tmp = tempdir().unwrap();
    let engine = MobileEngine::new();
    engine.set_persistence_dir(tmp.path().to_str().unwrap());

    // 1. URL detection
    assert!(is_playlist_url("https://www.youtube.com/playlist?list=PL123456789"));
    assert!(is_playlist_url("https://example.com/stream/index.m3u8"));
    assert!(!is_playlist_url("https://example.com/file.zip"));

    // 2. Format numbered filename
    assert_eq!(format_numbered_filename(1, 10, "Intro Song", "mp3"), "01 - Intro Song.mp3");
    assert_eq!(format_numbered_filename(42, 150, "Episode 01", "mp4"), "042 - Episode 01.mp4");

    // 3. Create playlist
    let pl_id = engine
        .create_playlist(
            "https://www.youtube.com/playlist?list=PLTEST1234",
            tmp.path().join("downloads").to_str().unwrap(),
            Some("My Test Playlist"),
            None,
        )
        .expect("Should create playlist");

    assert!(pl_id > 0);

    // 4. Inspect snapshot
    let snap = engine.get_playlist_snapshot(pl_id).expect("Snapshot should exist");
    assert_eq!(snap.title, "My Test Playlist");
    assert_eq!(snap.total_items, 5);
    assert_eq!(snap.state, MobilePlaylistState::Created);

    // 5. Start playlist
    engine.start_playlist(pl_id).expect("Should start playlist");
    let started_snap = engine.get_playlist_snapshot(pl_id).unwrap();
    assert_eq!(started_snap.state, MobilePlaylistState::Downloading);

    // 6. Pause playlist
    engine.pause_playlist(pl_id).expect("Should pause playlist");
    let paused_snap = engine.get_playlist_snapshot(pl_id).unwrap();
    assert_eq!(paused_snap.state, MobilePlaylistState::Paused);

    // 7. Resume playlist
    engine.resume_playlist(pl_id).expect("Should resume playlist");
    let resumed_snap = engine.get_playlist_snapshot(pl_id).unwrap();
    assert_eq!(resumed_snap.state, MobilePlaylistState::Downloading);

    // 8. Cancel playlist
    engine.cancel_playlist(pl_id).expect("Should cancel playlist");
    let cancelled_snap = engine.get_playlist_snapshot(pl_id).unwrap();
    assert_eq!(cancelled_snap.state, MobilePlaylistState::Cancelled);

    // 9. Remove playlist
    engine.remove_playlist(pl_id, false).expect("Should remove playlist");
    assert!(engine.get_playlist_snapshot(pl_id).is_err());
}
