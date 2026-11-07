# BANKAI Anime Hub — Android APK
তোমার দেওয়া anime dashboard image-কে visual reference ধরে BANKAI Android build সাজানো হয়েছে।

## APK পাওয়া
1. GitHub repository-এর **Actions** tab খুলবে।
2. **BANKAI Android APK** workflow নির্বাচন করবে।
3. **Run workflow** চাপবে।
4. Build সফল হলে run-এর **Artifacts** থেকে **BANKAI-debug-APK** ZIP ডাউনলোড করবে।
5. ZIP extract করে `app-debug.apk` Android phone-এ install করবে।

Main branch-এ push হলেই workflow স্বয়ংক্রিয়ভাবে build চালাবে।

## Main features
- Dark purple/magenta Japanese anime dashboard
- Anime catalogue, poster, custom category
- Per-episode Watched / Unwatched
- Season ও auto-generated episode list
- Add/Remove from Weekly (Anime/Season/Episode/Video অক্ষত থাকবে)
- Separate Watchlist
- Per-episode upload, replace, delete
- Android private storage import
- LibVLC player, Media3 fallback
- VLC / MX Player external chooser
- Built-in player resume position
- Offline library
- GitHub Actions APK build

## Video storage note
Imported video app-private `files/nova_media` folder-এ কপি হয়। Gallery-র original file delete করলেও BANKAI copy থাকবে। Android app uninstall বা app data clear করলে private copy মুছে যায়; আগে backup রাখবে।

## Notes
- Workflow একটি debug APK build করে; release distribution-এর জন্য signing key লাগবে।
- External player-এ playback progress BANKAI-তে ফেরত আসা নিশ্চিত নয়; built-in player resume position save করে।
- Poster/API fetch-এর জন্য internet দরকার; local library এবং imported videos offline-এ ব্যবহার করা যাবে।
