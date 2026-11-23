use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::sync::atomic::{AtomicPtr, Ordering};
use std::sync::Arc;

use crate::{MobileDownloadSnapshot, MobileDownloadState, MobileEngine};

static ENGINE_INSTANCE: AtomicPtr<Arc<MobileEngine>> = AtomicPtr::new(std::ptr::null_mut());

fn get_or_init_engine() -> Arc<MobileEngine> {
    let ptr = ENGINE_INSTANCE.load(Ordering::SeqCst);
    if !ptr.is_null() {
        unsafe { (*ptr).clone() }
    } else {
        let engine = MobileEngine::new();
        let boxed = Box::new(engine.clone());
        let new_ptr = Box::into_raw(boxed);
        if ENGINE_INSTANCE
            .compare_exchange(
                std::ptr::null_mut(),
                new_ptr,
                Ordering::SeqCst,
                Ordering::SeqCst,
            )
            .is_ok()
        {
            engine
        } else {
            // Lost race, recover memory
            unsafe {
                drop(Box::from_raw(new_ptr));
                (*ENGINE_INSTANCE.load(Ordering::SeqCst)).clone()
            }
        }
    }
}

#[repr(C)]
pub struct CMobileSnapshot {
    pub id: u64,
    pub url: *mut c_char,
    pub file_path: *mut c_char,
    pub state: MobileDownloadState,
    pub total_bytes: i64, // -1 if unknown
    pub downloaded_bytes: u64,
    pub speed_bytes_per_sec: i64, // -1 if unknown
    pub error: *mut c_char,
    pub resumable: bool,
}

#[repr(C)]
pub struct CMobileSnapshotList {
    pub items: *mut CMobileSnapshot,
    pub count: usize,
    pub capacity: usize,
}

fn to_c_string(s: Option<&str>) -> *mut c_char {
    match s {
        Some(val) => match CString::new(val) {
            Ok(c) => c.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

fn convert_snapshot(snap: MobileDownloadSnapshot) -> CMobileSnapshot {
    CMobileSnapshot {
        id: snap.id,
        url: to_c_string(Some(&snap.url)),
        file_path: to_c_string(snap.file_path.as_deref()),
        state: snap.state,
        total_bytes: snap.total_bytes.map(|v| v as i64).unwrap_or(-1),
        downloaded_bytes: snap.downloaded_bytes,
        speed_bytes_per_sec: snap.speed_bytes_per_sec.map(|v| v as i64).unwrap_or(-1),
        error: to_c_string(snap.error.as_deref()),
        resumable: snap.resumable,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn mdm_mobile_init() {
    let _ = std::panic::catch_unwind(|| {
        let _ = get_or_init_engine();
    });
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mdm_mobile_set_persistence_dir(dir_path: *const c_char) -> i32 {
    let result = std::panic::catch_unwind(|| {
        if dir_path.is_null() {
            return -1;
        }
        let dir_str = match unsafe { CStr::from_ptr(dir_path) }.to_str() {
            Ok(s) => s,
            Err(_) => return -2,
        };
        let engine = get_or_init_engine();
        engine.set_persistence_dir(dir_str);
        0
    });
    result.unwrap_or(-3)
}

#[unsafe(no_mangle)]
pub extern "C" fn mdm_mobile_restore_from_disk() -> i32 {
    let result = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        engine.restore_from_disk() as i32
    });
    result.unwrap_or(-1)
}

#[unsafe(no_mangle)]
pub extern "C" fn mdm_mobile_set_max_concurrent_downloads(max: u32) {
    let _ = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        engine.set_max_concurrent_downloads(max as usize);
    });
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mdm_mobile_create_download(
    url: *const c_char,
    dest: *const c_char,
) -> i64 {
    let result = std::panic::catch_unwind(|| {
        if url.is_null() || dest.is_null() {
            return -1;
        }
        let url_str = match unsafe { CStr::from_ptr(url) }.to_str() {
            Ok(s) => s,
            Err(_) => return -2,
        };
        let dest_str = match unsafe { CStr::from_ptr(dest) }.to_str() {
            Ok(s) => s,
            Err(_) => return -3,
        };

        let engine = get_or_init_engine();
        match engine.create_download(url_str, dest_str) {
            Ok(id) => id as i64,
            Err(_) => -4,
        }
    });
    result.unwrap_or(-5)
}

#[unsafe(no_mangle)]
pub extern "C" fn mdm_mobile_pause_download(id: u64) -> i32 {
    let result = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        match engine.pause_download(id) {
            Ok(_) => 0,
            Err(_) => -1,
        }
    });
    result.unwrap_or(-2)
}

#[unsafe(no_mangle)]
pub extern "C" fn mdm_mobile_resume_download(id: u64) -> i32 {
    let result = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        match engine.resume_download(id) {
            Ok(_) => 0,
            Err(_) => -1,
        }
    });
    result.unwrap_or(-2)
}

#[unsafe(no_mangle)]
pub extern "C" fn mdm_mobile_cancel_download(id: u64) -> i32 {
    let result = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        match engine.cancel_download(id) {
            Ok(_) => 0,
            Err(_) => -1,
        }
    });
    result.unwrap_or(-2)
}

#[unsafe(no_mangle)]
pub extern "C" fn mdm_mobile_remove_download(id: u64) -> i32 {
    let result = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        match engine.remove_download(id) {
            Ok(_) => 0,
            Err(_) => -1,
        }
    });
    result.unwrap_or(-2)
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mdm_mobile_restore_download(
    id: u64,
    url: *const c_char,
    dest: *const c_char,
    start_paused: i32,
) -> i32 {
    let result = std::panic::catch_unwind(|| {
        if url.is_null() || dest.is_null() {
            return -1;
        }
        let url_str = match unsafe { CStr::from_ptr(url) }.to_str() {
            Ok(s) => s,
            Err(_) => return -2,
        };
        let dest_str = match unsafe { CStr::from_ptr(dest) }.to_str() {
            Ok(s) => s,
            Err(_) => return -3,
        };
        let engine = get_or_init_engine();
        match engine.restore_download(id, url_str, dest_str, start_paused != 0) {
            Ok(_) => 0,
            Err(_) => -4,
        }
    });
    result.unwrap_or(-5)
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mdm_mobile_get_snapshot(
    id: u64,
    out_snap: *mut CMobileSnapshot,
) -> i32 {
    if out_snap.is_null() {
        return -1;
    }
    let result = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        match engine.get_snapshot(id) {
            Ok(snap) => {
                unsafe {
                    *out_snap = convert_snapshot(snap);
                }
                0
            }
            Err(_) => -2,
        }
    });
    result.unwrap_or(-3)
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mdm_mobile_get_all_snapshots(
    out_list: *mut CMobileSnapshotList,
) -> i32 {
    if out_list.is_null() {
        return -1;
    }
    let result = std::panic::catch_unwind(|| {
        let engine = get_or_init_engine();
        let snaps = engine.get_all_snapshots();
        let count = snaps.len();
        let mut c_snaps: Vec<CMobileSnapshot> = snaps.into_iter().map(convert_snapshot).collect();
        let capacity = c_snaps.capacity();
        let items_ptr = c_snaps.as_mut_ptr();
        std::mem::forget(c_snaps); // Managed by mdm_mobile_free_snapshot_list

        unsafe {
            (*out_list).items = items_ptr;
            (*out_list).count = count;
            (*out_list).capacity = capacity;
        }
        0
    });
    result.unwrap_or(-2)
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mdm_mobile_free_snapshot(snap: *mut CMobileSnapshot) {
    if snap.is_null() {
        return;
    }
    let _ = std::panic::catch_unwind(|| unsafe {
        let s = &mut *snap;
        if !s.url.is_null() {
            drop(CString::from_raw(s.url));
            s.url = std::ptr::null_mut();
        }
        if !s.file_path.is_null() {
            drop(CString::from_raw(s.file_path));
            s.file_path = std::ptr::null_mut();
        }
        if !s.error.is_null() {
            drop(CString::from_raw(s.error));
            s.error = std::ptr::null_mut();
        }
    });
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mdm_mobile_free_snapshot_list(list: *mut CMobileSnapshotList) {
    if list.is_null() {
        return;
    }
    let _ = std::panic::catch_unwind(|| unsafe {
        let l = &mut *list;
        if !l.items.is_null() && l.capacity > 0 {
            let slice = std::slice::from_raw_parts_mut(l.items, l.count);
            for item in slice.iter_mut() {
                if !item.url.is_null() {
                    drop(CString::from_raw(item.url));
                    item.url = std::ptr::null_mut();
                }
                if !item.file_path.is_null() {
                    drop(CString::from_raw(item.file_path));
                    item.file_path = std::ptr::null_mut();
                }
                if !item.error.is_null() {
                    drop(CString::from_raw(item.error));
                    item.error = std::ptr::null_mut();
                }
            }
            drop(Vec::from_raw_parts(l.items, l.count, l.capacity));
            l.items = std::ptr::null_mut();
            l.count = 0;
            l.capacity = 0;
        }
    });
}

// JNI bindings for Android (Java / Kotlin integration)
#[cfg(feature = "jni")]
pub mod jni_bridge {
    use super::*;
    use jni::objects::{JClass, JString};
    use jni::sys::{jboolean, jint, jlong, jstring};
    use jni::JNIEnv;

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeInit(
        _env: JNIEnv,
        _class: JClass,
    ) {
        let _ = std::panic::catch_unwind(|| {
            mdm_mobile_init();
        });
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeInitTls(
        raw_env: *mut std::ffi::c_void,
        _class: jni::sys::jobject,
        context: jni::sys::jobject,
    ) -> jint {
        #[cfg(target_os = "android")]
        {
            let result = std::panic::catch_unwind(|| {
                if raw_env.is_null() || context.is_null() {
                    return -1;
                }
                let mut unowned = unsafe {
                    jni_022::EnvUnowned::from_raw(raw_env as *mut jni_022::sys::JNIEnv)
                };
                let outcome = unowned.with_env(|env| {
                    let jobj = unsafe {
                        jni_022::objects::JObject::from_raw(env, context as jni_022::sys::jobject)
                    };
                    rustls_platform_verifier::android::init_with_env(env, jobj)
                });
                match outcome.into_outcome() {
                    jni_022::Outcome::Ok(()) => 0,
                    _ => -2,
                }
            });
            result.unwrap_or(-3)
        }
        #[cfg(not(target_os = "android"))]
        {
            let _ = (raw_env, _class, context);
            0
        }
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeSetPersistenceDir(
        mut env: JNIEnv,
        _class: JClass,
        dir: JString,
    ) -> jint {
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let dir_str: String = match env.get_string(&dir) {
                Ok(s) => s.into(),
                Err(_) => return -1,
            };
            let engine = get_or_init_engine();
            engine.set_persistence_dir(&dir_str);
            0
        }));
        result.unwrap_or(-2)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeRestoreFromDisk(
        _env: JNIEnv,
        _class: JClass,
    ) -> jint {
        let result = std::panic::catch_unwind(|| {
            let engine = get_or_init_engine();
            engine.restore_from_disk() as jint
        });
        result.unwrap_or(-1)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeSetMaxConcurrentDownloads(
        _env: JNIEnv,
        _class: JClass,
        max: jint,
    ) {
        let _ = std::panic::catch_unwind(|| {
            let engine = get_or_init_engine();
            engine.set_max_concurrent_downloads(max.max(1) as usize);
        });
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeCreateDownload(
        mut env: JNIEnv,
        _class: JClass,
        url: JString,
        dest: JString,
    ) -> jlong {
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let url_str: String = match env.get_string(&url) {
                Ok(s) => s.into(),
                Err(_) => return -1,
            };
            let dest_str: String = match env.get_string(&dest) {
                Ok(s) => s.into(),
                Err(_) => return -2,
            };
            let engine = get_or_init_engine();
            match engine.create_download(&url_str, &dest_str) {
                Ok(id) => id as jlong,
                Err(_) => -3,
            }
        }));
        result.unwrap_or(-4)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativePauseDownload(
        _env: JNIEnv,
        _class: JClass,
        id: jlong,
    ) -> jint {
        let result = std::panic::catch_unwind(|| {
            let engine = get_or_init_engine();
            match engine.pause_download(id as u64) {
                Ok(_) => 0,
                Err(_) => -1,
            }
        });
        result.unwrap_or(-2)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeResumeDownload(
        _env: JNIEnv,
        _class: JClass,
        id: jlong,
    ) -> jint {
        let result = std::panic::catch_unwind(|| {
            let engine = get_or_init_engine();
            match engine.resume_download(id as u64) {
                Ok(_) => 0,
                Err(_) => -1,
            }
        });
        result.unwrap_or(-2)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeCancelDownload(
        _env: JNIEnv,
        _class: JClass,
        id: jlong,
    ) -> jint {
        let result = std::panic::catch_unwind(|| {
            let engine = get_or_init_engine();
            match engine.cancel_download(id as u64) {
                Ok(_) => 0,
                Err(_) => -1,
            }
        });
        result.unwrap_or(-2)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeRemoveDownload(
        _env: JNIEnv,
        _class: JClass,
        id: jlong,
    ) -> jint {
        let result = std::panic::catch_unwind(|| {
            let engine = get_or_init_engine();
            match engine.remove_download(id as u64) {
                Ok(_) => 0,
                Err(_) => -1,
            }
        });
        result.unwrap_or(-2)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeRestoreDownload(
        mut env: JNIEnv,
        _class: JClass,
        id: jlong,
        url: JString,
        dest: JString,
        start_paused: jboolean,
    ) -> jint {
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let url_str: String = match env.get_string(&url) {
                Ok(s) => s.into(),
                Err(_) => return -1,
            };
            let dest_str: String = match env.get_string(&dest) {
                Ok(s) => s.into(),
                Err(_) => return -2,
            };
            let engine = get_or_init_engine();
            match engine.restore_download(id as u64, &url_str, &dest_str, start_paused != 0) {
                Ok(_) => 0,
                Err(_) => -3,
            }
        }));
        result.unwrap_or(-4)
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeGetSnapshotJson(
        env: JNIEnv,
        _class: JClass,
        id: jlong,
    ) -> jstring {
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let engine = get_or_init_engine();
            match engine.get_snapshot(id as u64) {
                Ok(snap) => {
                    let json = serde_json::to_string(&snap).unwrap_or_default();
                    env.new_string(json).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
                }
                Err(_) => std::ptr::null_mut(),
            }
        }));
        result.unwrap_or(std::ptr::null_mut())
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_com_misterdownloadmanager_core_MdmNativeBridge_nativeGetAllSnapshotsJson(
        env: JNIEnv,
        _class: JClass,
    ) -> jstring {
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let engine = get_or_init_engine();
            let list = engine.get_all_snapshots();
            let json = serde_json::to_string(&list).unwrap_or_default();
            env.new_string(json).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
        }));
        result.unwrap_or(std::ptr::null_mut())
    }
}
