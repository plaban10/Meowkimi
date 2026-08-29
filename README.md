# 🐾 MeowMuscle — Mobile Workout Tracker

**MeowMuscle** is a playful, cat-mascot-themed Android workout tracking application designed to make exercise logging fun, simple, and consistent. Track daily workouts, design personalized routines, and monitor your overall progress with a clean, soft-aesthetic UI.

---

## ✨ Features

* 🐱 **Playful Theme & Mascot:** Soft rounded card designs, warm peach/coral accents, custom cat avatars, and paw-print visual cues.
* 🏋️ **Custom Routine Builder ("My Routines"):** Create, edit, and save tailored daily workout plans with specific target sets, reps, and rest timers.
* 📊 **Interactive Dashboard:** View real-time workout stats, active session duration, estimated calories burned, and daily progress.
* 📖 **Exercise Catalog:** Searchable library of exercises categorized by targeted muscle groups.
* 🗓️ **Workout History:** Detailed past session logging to review sets, volume, and performance over time.
* ⚡ **Offline Persistence & Cloud Sync:** Built-in local caching via Room Database with real-time cloud backup powered by Firebase Firestore.

---

## 🛠️ Tech Stack

* **Platform:** Android Native (Kotlin)
* **Local Database:** Room Database
* **Backend & Auth:** Firebase (Firestore, Auth)
* **Build System:** Gradle / GitHub Actions CI
* **Architecture:** MVVM (Model-View-ViewModel)

---

## 🚀 Getting Started

### Prerequisites

* Android Studio (Ladybug / Jellyfish or newer)
* JDK 17 or higher
* Android SDK (API Level 24+)

### Direct APK Download (Android Phone)

You can download and install the latest APK directly on your phone:
1. Go to the [**Releases**](https://github.com/plaban10/MeowMuscle/releases) page on GitHub.
2. Tap on the latest release and download **`MeowMuscle-debug.apk`**.
3. Open the APK file on your Android device to install.

---

### Local Development Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/plaban10/MeowMuscle.git
   cd MeowMuscle
   ```
2. **Open in Android Studio:**
   - Select **Open an Existing Project** and choose the `MeowMuscle` directory.
   - Wait for Gradle sync to finish.
3. **Run the App:**
   - Connect your Android device or start an emulator.
   - Click **Run (`Shift + F10`)** or run `./gradlew installDebug`.

