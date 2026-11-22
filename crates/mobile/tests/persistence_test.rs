use mdm_mobile::{MobileDownloadState, MobileEngine};
use tempfile::tempdir;

#[test]
fn test_metadata_persistence_and_restore() {
    let tmp = tempdir().unwrap();
    let meta_dir = tmp.path().join("mdm_meta");
    let dest_dir = tmp.path().join("downloads");
    std::fs::create_dir_all(&dest_dir).unwrap();

    let dest1 = dest_dir.join("file1.bin");
    let dest2 = dest_dir.join("file2.bin");

    // 1. Start engine and configure persistence
    let engine = MobileEngine::new();
    engine.set_persistence_dir(meta_dir.to_str().unwrap());

    let id1 = engine
        .create_download("http://127.0.0.1:9/one.bin", dest1.to_str().unwrap())
        .expect("create id1");
    let id2 = engine
        .create_download("http://127.0.0.1:9/two.bin", dest2.to_str().unwrap())
        .expect("create id2");

    engine.pause_download(id1).expect("pause id1");

    // Verify metadata file was saved
    let meta_file = meta_dir.join("downloads_metadata.json");
    assert!(meta_file.exists());

    // 2. Simulate complete application restart with new engine instance
    let engine2 = MobileEngine::new();
    engine2.set_persistence_dir(meta_dir.to_str().unwrap());
    let restored_count = engine2.restore_from_disk();
    assert_eq!(restored_count, 2);

    let snap1 = engine2.get_snapshot(id1).expect("restored snap1");
    assert_eq!(snap1.url, "http://127.0.0.1:9/one.bin");
    assert_eq!(snap1.state, MobileDownloadState::Paused);

    let snap2 = engine2.get_snapshot(id2).expect("restored snap2");
    assert_eq!(snap2.url, "http://127.0.0.1:9/two.bin");

    // Next ID should be > 2
    let id3 = engine2
        .create_download("http://127.0.0.1:9/three.bin", dest_dir.join("file3.bin").to_str().unwrap())
        .expect("create id3");
    assert_eq!(id3, 3);
}
