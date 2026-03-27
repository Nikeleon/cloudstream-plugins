# JavHDZ CloudStream Plugin

Plugin để xem phim JAV HD từ https://javhdz.hot/ trên ứng dụng **CloudStream 3** (Android).

---

## Cấu trúc thư mục

```
JavHDZ/
├── build.gradle.kts             ← Root Gradle (classpath cloudstream + kotlin)
├── settings.gradle.kts          ← Khai báo module JavHDZProvider
├── gradle.properties
├── repo.json                    ← Thông tin repository (upload lên hosting)
├── plugin.json                  ← Danh sách plugin (upload lên hosting)
└── JavHDZProvider/
    ├── build.gradle.kts         ← Cấu hình plugin (version, metadata)
    └── src/main/
        ├── manifest.json        ← Tự động nhúng vào .cs3 khi build
        └── kotlin/com/quyen/
            ├── JavHDZPlugin.kt  ← Entry point (@CloudstreamPlugin)
            └── JavHDZProvider.kt← Logic chính: search, load, loadLinks
```

---

## Cách build thành file .cs3

### Yêu cầu
- **Java JDK 17** hoặc mới hơn
- **Android Studio** hoặc Android SDK với `ANDROID_HOME` đã set
- Kết nối Internet để tải các Gradle dependency lần đầu

### Bước 1 — Clone template Gradle wrapper
```bash
# Clone template để lấy Gradle wrapper (gradlew)
git clone https://github.com/recloudstream/cloudstream-extensions temp-wrapper
cp -r temp-wrapper/gradle ./JavHDZ/
cp temp-wrapper/gradlew ./JavHDZ/
cp temp-wrapper/gradlew.bat ./JavHDZ/
rm -rf temp-wrapper
```

Hoặc tải `gradle wrapper` thủ công vào thư mục `JavHDZ/gradle/wrapper/`.

### Bước 2 — Build
```bash
cd JavHDZ
./gradlew JavHDZProvider:make
```

File output: `JavHDZProvider/build/outputs/JavHDZProvider.cs3`

### Bước 3 — Cài trên CloudStream
**Cách A — File trực tiếp:**
1. Copy `JavHDZProvider.cs3` vào điện thoại Android
2. CloudStream → Settings → Extensions → Cài từ file (install from file)

**Cách B — Qua Repository (online):**
1. Upload `JavHDZProvider.cs3`, `repo.json`, `plugin.json` lên GitHub/GitLab
2. Cập nhật URL trong `repo.json` và `plugin.json` trỏ đến đường dẫn raw file
3. Trong CloudStream: Settings → Extensions → Add Repository → dán URL `repo.json`

---

## Cập nhật URL hosting

Sau khi có URL hosting thực tế (GitHub raw), sửa 2 file:

**repo.json** — trường `pluginLists`:
```
https://raw.githubusercontent.com/<user>/<repo>/main/plugin.json
```

**plugin.json** — trường `url`:
```
https://raw.githubusercontent.com/<user>/<repo>/main/JavHDZProvider.cs3
```

---

## Tính năng plugin

| Tính năng | Chi tiết |
|---|---|
| Trang chủ | Mới nhất, Trending, Censored, Uncensored, Beauty |
| Tìm kiếm | Theo từ khóa |
| Xem phim | Stream HLS (m3u8) qua 1–4 server |
| Phân trang | Hỗ trợ đầy đủ |
| Ngôn ngữ | Tiếng Việt |

---

## Tác giả

- **quyen** — Plugin JavHDZ for CloudStream 3
