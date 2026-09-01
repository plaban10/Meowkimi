# 🐾 MeowMuscle — Mobile Workout Tracker

**MeowMuscle** is a playful, cat-mascot-themed Android workout tracking application designed to make exercise logging fun, simple, and consistent. Track daily workouts, design personalized routines, celebrate PRs (Personal Records), and monitor your overall progress with an intuitive UI and resilient offline-first cloud synchronization.

---

## ✨ Features

* 🐱 **Playful Mascot Experience:** Clean card layouts, warm peach/coral accents, custom cat avatar milestones, and paw-print cues.
* 🏋️ **Custom Routine Builder ("My Routines"):** Create, edit, and save tailored daily workout plans with target sets, reps, and rest timers.
* 📊 **Interactive Dashboard:** View real-time workout stats, active session duration, estimated calories burned, and weekly progress.
* 📖 **Exercise Catalog:** Searchable library of exercises categorized by targeted muscle groups.
* 🏆 **Automatic PR Detection:** Real-time personal record recognition with celebratory sound and confetti animations.
* 🗓️ **Workout History:** Detailed past session logging to review sets, volume, and performance over time.
* ⚡ **Offline Persistence & Cloud Sync:** Built-in local caching via Room Database with real-time cloud backup powered by Firebase Firestore. Works without internet and syncs automatically when reconnected.

---

## 🛠️ Tech Stack

* **Platform:** Android Native (Kotlin)
* **UI Framework:** Jetpack Compose (Material Design 3)
* **Local Database:** Room Database
* **Backend & Auth:** Firebase (Cloud Firestore with Offline Cache, Firebase Anonymous Auth)
* **Build System:** Gradle (Kotlin DSL)
* **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern

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

## ☁️ Firebase Setup for Contributors & GitHub Clones

If you are cloning this repository or creating your own build, follow these steps to connect your Firebase backend:

### Step 1: Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** and name it (e.g., `Gym Cat` or `MeowMuscle`).
3. (Optional) Disable Google Analytics if not needed, then click **Create Project**.

### Step 2: Register Android App
1. In Project Overview, click the **Android** icon to add an app.
2. Enter the Android package name:
   ```
   com.aistudio.meowmuscle.wtrckr
   ```
3. Enter an App nickname (e.g., `MeowMuscle Android`).
4. Enter your **Debug SHA-1 Certificate Fingerprint** (see [SHA-1 Fingerprint Commands](#how-to-generate-sha-1-and-sha-256-fingerprints) below).
5. Click **Register app**.
6. Download `google-services.json` and place it in the `/app` directory:
   ```
   app/google-services.json
   ```
   *(A template is provided at `app/google-services.json.example`)*

### Step 3: Enable Anonymous Authentication
1. In the Firebase Console left menu, navigate to **Build > Authentication**.
2. Click **Get Started**, then select the **Sign-in method** tab.
3. Select **Anonymous** from the providers list.
4. Toggle **Enable** to ON and click **Save**.

### Step 4: Create Cloud Firestore Database
1. In the Firebase Console left menu, navigate to **Build > Firestore Database**.
2. Click **Create database**.
3. Choose a location closest to your users (e.g., `nam5 (us-central)`).
4. Select **Start in production mode** (or test mode), and click **Create**.

### Step 5: Configure Firestore Security Rules
1. In Firestore Database, open the **Rules** tab.
2. Replace all content with the rules from `firestore.rules`:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       function isAuthenticated() {
         return request.auth != null && request.auth.uid != null;
       }
       function isOwner(userId) {
         return isAuthenticated() && request.auth.uid == userId;
       }

       // User private data & subcollections
       match /users/{userId} {
         allow read, write: if isOwner(userId);
         match /{allSubcollections=**} {
           allow read, write: if isOwner(userId);
         }
       }

       // Profiles
       match /profiles/{userId} {
         allow read: if isAuthenticated();
         allow write: if isOwner(userId);
       }

       // Public exercises
       match /exercises/{exerciseId} {
         allow read: if true;
         allow write: if isAuthenticated();
       }

       // Workouts, routines, and sets
       match /{document=**} {
         allow read, write: if isAuthenticated();
       }
     }
   }
   ```
3. Click **Publish**.

---

## 🔑 How to Generate SHA-1 and SHA-256 Fingerprints

To authorize your device or build machine with Firebase Authentication, obtain your SHA fingerprints:

### Option A: Using Gradle Task (Recommended)
Run the following command inside the project root:
```bash
./gradlew signingReport
```
Look for the `Variant: debug` output and copy the `SHA1` and `SHA-256` keys.

### Option B: Using Keytool Terminal Command

* **macOS / Linux (Debug Keystore):**
  ```bash
  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```

* **Windows (Command Prompt):**
  ```cmd
  keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
  ```

* **Project-Level Debug Keystore (if applicable):**
  ```bash
  keytool -list -v -keystore ./debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```

Copy the printed `SHA1` string and add it in **Firebase Console > Project Settings > General > Your Apps > Add Fingerprint**.

---

## 📱 Verifying Sync on Real Devices

1. **Launch the App:** On launch, the app automatically signs in anonymously and restores your unique User ID across sessions.
2. **Log a Workout:** Start and complete any workout session in the **Workout** tab.
3. **Check Sync Status:** Go to the **Profile** tab. You will see:
   - Status badge: `"Connected to Firebase (X workouts synced)"`
   - Timestamp: `"Last Synced: <Date, Time>"`
4. **Manual Sync:** Tap **"Sync Now"** to force a two-way synchronization between your local Room database and Cloud Firestore.
5. **Offline Mode:** Turn on Airplane mode and log a workout. The workout is instantly saved locally. Turn Airplane mode back off and tap **"Sync Now"** — all sessions will sync seamlessly.

---

### Local Development Build

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
