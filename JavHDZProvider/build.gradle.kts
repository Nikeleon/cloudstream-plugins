// ─────────────────────────────────────────────────────────────
// Module build file for JavHDZProvider
// The "cloudstream" block is read by the gradle plugin to:
//   - populate manifest.json inside the .cs3 zip
//   - name the output file JavHDZProvider.cs3
// ─────────────────────────────────────────────────────────────

// Version increments trigger update-notifications inside CloudStream
version = 1

cloudstream {
    // ISO 639-1 language code
    language = "vi"

    // Shown in the plugin browser
    description = "Xem phim JAV HD chất lượng cao trực tuyến"
    authors     = listOf("quyen")

    // status: 1 = active, 0 = disabled
    status = 1

    // tvTypes must match TvType names used in the provider
    tvTypes = listOf("Movie")

    iconUrl = "https://javhdz.hot/favicon.ico"
}
