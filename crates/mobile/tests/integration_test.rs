use mdm_mobile::ffi::{
    mdm_mobile_create_download, mdm_mobile_free_snapshot, mdm_mobile_free_snapshot_list,
    mdm_mobile_get_all_snapshots, mdm_mobile_get_snapshot, mdm_mobile_init, CMobileSnapshot,
    CMobileSnapshotList,
};
use mdm_mobile::{MobileDownloadState, MobileEngine};
use tempfile::tempdir;

#[test]
fn test_engine_lifecycle_queue_and_concurrency() {
    let engine = MobileEngine::new();
    let tmp = tempdir().unwrap();
    let test_file1 = tmp.path().join("dl_1.bin");
    let test_file2 = tmp.path().join("dl_2.bin");

    // Concurrency limit = 1
    engine.set_max_concurrent_downloads(1);

    let id1 = engine
        .create_download("http://127.0.0.1:9/one.bin", test_file1.to_str().unwrap())
        .expect("Create id1");
    let id2 = engine
        .create_download("http://127.0.0.1:9/two.bin", test_file2.to_str().unwrap())
        .expect("Create id2");

    assert_eq!(id1, 1);
    assert_eq!(id2, 2);

    let snap1 = engine.get_snapshot(id1).expect("snap1");
    assert_eq!(snap1.url, "http://127.0.0.1:9/one.bin");

    // Pause id1
    engine.pause_download(id1).expect("pause id1");
    let p_snap1 = engine.get_snapshot(id1).expect("paused snap1");
    assert_eq!(p_snap1.state, MobileDownloadState::Paused);

    // Resume id1
    engine.resume_download(id1).expect("resume id1");

    // Cancel id1
    engine.cancel_download(id1).expect("cancel id1");
    let c_snap1 = engine.get_snapshot(id1).expect("cancelled snap1");
    assert_eq!(c_snap1.state, MobileDownloadState::Cancelled);

    // Remove id1
    engine.remove_download(id1).expect("remove id1");
    assert!(engine.get_snapshot(id1).is_err());
}

#[test]
fn test_c_abi_ffi_and_memory_safety() {
    mdm_mobile_init();
    let tmp = tempdir().unwrap();
    let file = tmp.path().join("c_test.bin");

    let c_url = std::ffi::CString::new("http://127.0.0.1:9/test.bin").unwrap();
    let c_dest = std::ffi::CString::new(file.to_str().unwrap()).unwrap();

    let id = unsafe { mdm_mobile_create_download(c_url.as_ptr(), c_dest.as_ptr()) };
    assert!(id > 0);

    // Snapshot
    let mut snap: CMobileSnapshot = unsafe { std::mem::zeroed() };
    let code = unsafe { mdm_mobile_get_snapshot(id as u64, &mut snap) };
    assert_eq!(code, 0);
    assert_eq!(snap.id, id as u64);
    assert!(!snap.url.is_null());
    unsafe { mdm_mobile_free_snapshot(&mut snap) };

    // List of snapshots
    let mut list = CMobileSnapshotList {
        items: std::ptr::null_mut(),
        count: 0,
        capacity: 0,
    };
    let list_code = unsafe { mdm_mobile_get_all_snapshots(&mut list) };
    assert_eq!(list_code, 0);
    assert!(list.count >= 1);
    assert!(list.capacity >= list.count);
    assert!(!list.items.is_null());
    unsafe { mdm_mobile_free_snapshot_list(&mut list) };
    assert!(list.items.is_null());
    assert_eq!(list.count, 0);
    assert_eq!(list.capacity, 0);

    // Null safety test
    assert_eq!(unsafe { mdm_mobile_create_download(std::ptr::null(), std::ptr::null()) }, -1);
    unsafe { mdm_mobile_free_snapshot(std::ptr::null_mut()) };
    unsafe { mdm_mobile_free_snapshot_list(std::ptr::null_mut()) };
}
