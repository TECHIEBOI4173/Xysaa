# 📌 ReelSaver — Android App

Save places & YouTube links from Instagram Reels and YouTube with one tap.

---

## 🚀 What It Does

When you see something amazing in an Instagram Reel or YouTube video, just **share it to ReelSaver**. The app automatically:

| Shared Content | What ReelSaver Detects |
|---|---|
| Instagram Reel with 📍 location tag | Extracts the place name + opens in Google Maps |
| YouTube link in caption / bio | Extracts video ID, shows thumbnail, saves link |
| YouTube Short | Same as above |
| Instagram Reel link | Saves reel link for later viewing |
| Google Maps link | Saves the exact location |
| Any hashtags | Saved as searchable tags |

---

## 📂 Project Structure

```
ReelSaver/
├── app/src/main/
│   ├── AndroidManifest.xml              ← Share intent filters (KEY FILE)
│   └── java/com/reelsaver/app/
│       ├── ReelSaverApp.kt              ← Application class
│       ├── model/
│       │   └── SavedItem.kt             ← Data model
│       ├── data/
│       │   └── DatabaseHelper.kt        ← SQLite DB (CRUD)
│       ├── utils/
│       │   └── ContentParser.kt         ← 🧠 The brain: detects YT links, places, hashtags
│       └── ui/
│           ├── MainActivity.kt          ← Home screen with saved items
│           ├── ShareReceiverActivity.kt ← Appears in Android share sheet
│           └── SavedItemsAdapter.kt     ← RecyclerView adapter
└── app/src/main/res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── activity_share_receiver.xml
    │   └── item_saved.xml
    ├── drawable/                         ← Badge backgrounds
    └── values/                           ← Colors, strings, themes
```

---

## 🛠 Setup Instructions

### 1. Open in Android Studio
- Open Android Studio → **File → Open** → select the `ReelSaver` folder
- Let Gradle sync finish

### 2. Add Missing Drawables
You need to add these 3 drawable files (vector icons):
- `ic_launcher.xml` — app icon (use Android Studio's Image Asset tool)
- `ic_launcher_round.xml` — round app icon
- `ic_logo.xml` — small logo for share screen header
- `ic_video_placeholder.xml` — placeholder for thumbnails
- `ic_delete.xml` — delete icon for list items

> **Quick way:** Right-click `res/drawable` → New → Vector Asset → pick icons from Material Icons

### 3. Build & Run
```bash
# From project root
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

Or press **▶ Run** in Android Studio.

---

## 📱 How to Use

### Sharing from Instagram
1. Open any Instagram Reel
2. Tap **Share** (paper plane icon)
3. Tap **More** → **ReelSaver**
4. The app detects the place or content type
5. Add optional notes → **Save**

### Sharing from YouTube
1. Tap **Share** on any YouTube video
2. Select **ReelSaver** from the share sheet
3. Thumbnail is auto-loaded
4. Tap **Save**

### Viewing Saved Items
- Open ReelSaver app
- Filter by **Places / YouTube / Reels**
- Search by name, place, or hashtag
- Tap any item to open in browser/maps/YouTube

---

## 🧠 How Content Detection Works (`ContentParser.kt`)

```
Shared text → ContentParser.parse()
                    ↓
        ┌───────────────────────┐
        │ YouTube regex match?  │ → YES → Extract video ID → YOUTUBE_VIDEO type
        └───────────────────────┘
                    ↓ NO
        ┌───────────────────────┐
        │ 📍 emoji / "location:"│ → YES → Extract place name → PLACE type
        │ Google Maps link?     │
        └───────────────────────┘
                    ↓ NO
        ┌───────────────────────┐
        │ Instagram reel URL?   │ → YES → Extract reel code → INSTAGRAM_REEL type
        └───────────────────────┘
                    ↓ NO
                UNKNOWN type (still saved)
```

**YouTube URLs supported:**
- `youtube.com/watch?v=VIDEO_ID`
- `youtu.be/VIDEO_ID`
- `youtube.com/shorts/VIDEO_ID`
- `m.youtube.com/watch?v=VIDEO_ID`

**Place detection triggers:**
- 📍 emoji followed by location name
- 📌 emoji followed by location name
- `location:`, `place:`, `at:` text prefix
- Google Maps links (`maps.google.com`, `goo.gl/maps`)
- Travel hashtags (#bali, #paris, #maldives, etc. — 60+ locations)

---

## 🔧 Extending the App

### Add Claude AI Analysis
Replace the regex parser with Claude API calls for smarter detection:

```kotlin
// In ContentParser.kt, replace parse() with:
suspend fun parseWithAI(text: String): ParseResult {
    val prompt = """
        Analyze this shared content from Instagram/YouTube:
        "$text"
        
        Extract:
        1. Any YouTube video IDs
        2. Any place/location names mentioned
        3. Any Google Maps links
        4. Key hashtags
        
        Respond in JSON: {"type":"PLACE|YOUTUBE_VIDEO|INSTAGRAM_REEL|UNKNOWN",
                          "place":"name","youtubeId":"id","hashtags":["tag1"]}
    """.trimIndent()
    
    // Call Anthropic API here
}
```

### Add Export Features
- Export all places to Google Maps list
- Export YouTube videos to a playlist
- Share saved collection with friends

---

## 📦 Dependencies

| Library | Purpose |
|---|---|
| Material Components | UI chips, cards, buttons |
| Glide 4.16 | Load YouTube thumbnails from `img.youtube.com` |
| AndroidX RecyclerView | Saved items list |
| SQLite (built-in) | Local database, no cloud needed |
| Kotlin Coroutines | Background processing |

---

## 🔐 Privacy

- **100% offline** — no data leaves your device
- No account required
- SQLite database stored locally only
- No analytics or tracking

---

## ⚠️ Known Limitations

1. **Instagram place extraction** depends on the caption text having 📍 or location keywords. Instagram's API is private and requires login — the app works with the shared text only.
2. **YouTube thumbnails** require internet to load (from `img.youtube.com`).
3. For richer metadata (video title, channel name), you would need the YouTube Data API key.

### Getting YouTube Video Titles (Optional Enhancement)
Add to `build.gradle`:
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
```
Then call:
```
GET https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=VIDEO_ID&format=json
```
This returns `title` and `author_name` without requiring an API key.
