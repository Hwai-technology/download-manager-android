#ifndef MDM_MOBILE_H
#define MDM_MOBILE_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    MOBILE_STATE_PROBING = 0,
    MOBILE_STATE_DOWNLOADING = 1,
    MOBILE_STATE_VERIFYING = 2,
    MOBILE_STATE_COMPLETING = 3,
    MOBILE_STATE_DONE = 4,
    MOBILE_STATE_FAILED = 5,
    MOBILE_STATE_CANCELLED = 6,
    MOBILE_STATE_PAUSED = 7,
    MOBILE_STATE_WAITING = 8,
} MobileDownloadState;

typedef struct {
    uint64_t id;
    char *url;
    char *file_path;
    MobileDownloadState state;
    int64_t total_bytes; // -1 if unknown
    uint64_t downloaded_bytes;
    int64_t speed_bytes_per_sec; // -1 if unknown / not actively receiving
    char *error;
    bool resumable;
} CMobileSnapshot;

typedef struct {
    CMobileSnapshot *items;
    size_t count;
    size_t capacity;
} CMobileSnapshotList;

/**
 * Initialize the mobile engine singleton and async runtime.
 * Thread-safe.
 */
void mdm_mobile_init(void);

/**
 * Set directory path where download metadata should be persisted across restarts.
 */
int32_t mdm_mobile_set_persistence_dir(const char *dir_path);

/**
 * Restore downloads from previously persisted metadata and state files.
 * Returns number of restored downloads.
 */
int32_t mdm_mobile_restore_from_disk(void);

/**
 * Configure max concurrent active downloads in queue.
 */
void mdm_mobile_set_max_concurrent_downloads(uint32_t max);

/**
 * Create a new download.
 * Returns > 0 on success (download ID), or negative error code.
 */
int64_t mdm_mobile_create_download(const char *url, const char *dest);

/**
 * Pause an active or queued download.
 * Returns 0 on success, negative error code on failure.
 */
int32_t mdm_mobile_pause_download(uint64_t id);

/**
 * Resume a paused download.
 * Returns 0 on success, negative error code on failure.
 */
int32_t mdm_mobile_resume_download(uint64_t id);

/**
 * Cancel and delete an active or queued download.
 * Returns 0 on success, negative error code on failure.
 */
int32_t mdm_mobile_cancel_download(uint64_t id);

/**
 * Remove a terminal (Done/Failed/Cancelled) download from tracking.
 * Returns 0 on success, negative error code on failure.
 */
int32_t mdm_mobile_remove_download(uint64_t id);

/**
 * Restore an existing download from persisted metadata and resume file.
 * Returns 0 on success, negative error code on failure.
 */
int32_t mdm_mobile_restore_download(uint64_t id, const char *url, const char *dest, int32_t start_paused);

/**
 * Populate out_snap with point-in-time snapshot.
 * Must be released via mdm_mobile_free_snapshot().
 */
int32_t mdm_mobile_get_snapshot(uint64_t id, CMobileSnapshot *out_snap);

/**
 * Populate out_list with all tracked download snapshots.
 * Must be released via mdm_mobile_free_snapshot_list().
 */
int32_t mdm_mobile_get_all_snapshots(CMobileSnapshotList *out_list);

/**
 * Release memory allocated for a single snapshot's fields.
 */
void mdm_mobile_free_snapshot(CMobileSnapshot *snap);

/**
 * Release memory allocated for snapshot list items and strings.
 */
void mdm_mobile_free_snapshot_list(CMobileSnapshotList *list);

#ifdef __cplusplus
}
#endif

#endif /* MDM_MOBILE_H */
