use mdm_mobile::metadata::*;

#[test]
fn test_video_metadata_extraction_real_urls() {
    // 1. YouTube oEmbed
    let yt_url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    let yt_res = extract_metadata_sync(yt_url);
    assert!(yt_res.is_ok(), "YouTube metadata extraction failed: {:?}", yt_res);
    let yt_meta = yt_res.unwrap();
    assert!(yt_meta.title.contains("Rick Astley") || yt_meta.title.contains("Never Gonna Give You Up"), "Title was: {}", yt_meta.title);
    assert_eq!(yt_meta.video_id, Some("dQw4w9WgXcQ".to_string()));
    assert!(!yt_meta.formats.is_empty(), "Formats should not be empty");
    assert!(yt_meta.formats.iter().any(|f| f.is_video && f.resolution.as_deref() == Some("1080p")));
    assert!(yt_meta.formats.iter().any(|f| !f.is_video && f.is_audio));

    // 2. Vimeo oEmbed
    let vimeo_url = "https://vimeo.com/76979871";
    let vimeo_res = extract_metadata_sync(vimeo_url);
    assert!(vimeo_res.is_ok(), "Vimeo metadata extraction failed: {:?}", vimeo_res);
    let vimeo_meta = vimeo_res.unwrap();
    assert!(vimeo_meta.title.contains("Vimeo Player") || !vimeo_meta.title.is_empty());
    assert!(vimeo_meta.formats.iter().any(|f| f.is_video));
    assert!(vimeo_meta.formats.iter().any(|f| !f.is_video && f.is_audio));

    // 3. Direct media file fallback
    let direct_url = "https://example.com/videos/sample_presentation.mp4";
    let direct_res = extract_metadata_sync(direct_url);
    assert!(direct_res.is_ok());
    let direct_meta = direct_res.unwrap();
    assert_eq!(direct_meta.title, "sample_presentation.mp4");
}
