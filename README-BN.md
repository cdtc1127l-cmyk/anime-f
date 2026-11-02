# NOVA — Native Android Project

এই প্রজেক্টে তোমার দেওয়া NOVA GitHub Pages ওয়েব অ্যাসেট Android WebView-এর মধ্যে রাখা হয়েছে এবং Native Android সুবিধা যোগ করা হয়েছে।

## কী আছে
- Existing NOVA web interface (`app/src/main/assets/`)
- Native Android video import: Android file picker থেকে ভিডিও NOVA-এর private storage-এ কপি হয়
- Native offline video library: Play / Delete
- LibVLC primary player
- Media3/ExoPlayer fallback (LibVLC error হলে)
- Vertical swipe Reels feed for imported local videos (Media3)
- Storage usage display
- GitHub Actions workflow দিয়ে Debug APK build

## GitHub-এ APK তৈরি
1. এই ZIP Extract করো।
2. Extract করা `NOVA_Android_Native_Project` ফোল্ডারের ভেতরের সব ফাইল তোমার GitHub Repository-র root-এ upload করো।
3. GitHub → Actions → `NOVA Android APK` → Run workflow চাপো। অথবা `main` branch-এ push করলেই workflow চলবে।
4. Build সফল হলে workflow run খুলে `NOVA-debug-APK` Artifact ডাউনলোড করো।
5. ZIP extract করে `app-debug.apk` ফোনে Install করো।

## NOVA-তে ভিডিও রাখা ও মুছা
- অ্যাপের নিচের `＋ Import` চাপো; ফোনের ফাইল picker থেকে ভিডিও নির্বাচন করো।
- Import করলে ভিডিও NOVA-এর `files/nova_media` private folder-এ কপি হয়; মূল Gallery ফাইল অক্ষত থাকে।
- `My Videos` থেকে Play বা Delete করা যায়।
- Delete করলে NOVA-র কপিটি মুছে যায়। Gallery-র মূল ফাইল মোছা হয় না।
- `Storage`-এ NOVA-র imported video-গুলোর মোট জায়গা দেখা যায়।
- অ্যাপ uninstall করলে Android app-private storage মুছে দেয়। Backup বন্ধ রাখা হয়েছে।

## গুরুত্বপূর্ণ সীমাবদ্ধতা
- এটি প্রথম Native integration build; এখানে Android SDK/Gradle build চালিয়ে APK compile বা ফোনে runtime test করা হয়নি। GitHub Actions-এর build result-ই প্রথম compile validation হবে।
- LibVLC ও Media3 অনেক format চালাতে পারে, কিন্তু কোনো প্লেয়ারই সব codec/profile/DRM/ক্ষতিগ্রস্ত ফাইলের জন্য 100% guarantee দিতে পারে না।
- Offline Reels feed এখন Native-এ import করা ভিডিওগুলো দেখায়। Existing web app-এর browser-local data স্বয়ংক্রিয়ভাবে Native private storage-এ migrate হয় না।
- Web interface-এর কিছু feature এখনও WebView/browser storage ব্যবহার করে; Native imported video library আলাদা।
- Debug APK test/install-এর জন্য। Public release-এর আগে নিজস্ব signing key দিয়ে signed release build তৈরি করতে হবে।
- LibVLC dependency-এর license/notice এবং distribution obligations release করার আগে যাচাই করতে হবে।

## Build configuration
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Kotlin 2.0.21
- compileSdk 35 / minSdk 23
- LibVLC Android 3.6.5
- AndroidX Media3 1.5.1
