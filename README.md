# Plant Leaf Detection App

This is a plant leaf detection app using a TensorFlow Lite model trained on YOLOv8. The app is built with Jetpack Compose and utilizes various Android libraries for camera integration, navigation, and more.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Build and Run](#build-and-run)
- [Android Studio](#android-studio)

## Features

- Detects plant leaves using a TensorFlow Lite model.
- Camera integration for real-time detection.
- Navigation with Jetpack Compose.
- Uses Room for local database storage.
- Supports AndroidX libraries and Kotlin coroutines.

## Requirements

- Android Studio
- Android SDK 24 or higher
- Java 8 or higher

## Installation

1. **Clone the repository:**
    ```sh
    git clone https://github.com/shivsoji/plant-leaf-detection-app.git
    cd plant-leaf-detection-app
    ```

2. **Open the project in Android Studio:**
    - Open Android Studio.
    - Select `Open an existing project`.
    - Navigate to the cloned repository and select it.

3. **Sync the project with Gradle:**
    - Android Studio should automatically prompt you to sync the project with Gradle. If not, click on `File > Sync Project with Gradle Files`.

## Usage

1. **Run the app:**
    - Connect an Android device or start an emulator.
    - Click on the `Run` button in Android Studio or use the `Shift + F10` shortcut.

2. **Permissions:**
    - The app requires camera permissions. Make sure to grant the necessary permissions when prompted.

## Build and Run

### Command Line

You can also build and run the app from the command line using Gradle.

1. **Build the app:**
    ```sh
    ./gradlew assembleDebug
    ```

2. **Install the APK on a connected device:**
    ```sh
    ./gradlew installDebug
    ```

### VS Code

If you are using VS Code, you can use the provided tasks and launch configurations.

1. **Build the app:**
    - Open the command palette (`Ctrl + Shift + P`).
    - Select `Tasks: Run Task`.
    - Choose `assembleDebug`.

2. **Run the app:**
    - Open the command palette (`Ctrl + Shift + P`).
    - Select `Debug: Start Debugging`.
    - Choose `Launch Android App`.

## Android Studio

1. **Build the app:**
    - Open Android Studio.
    - Click on `Build > Make Project` or use the `Ctrl + F9` shortcut.

2. **Run the app:**
    - Connect an Android device or start an emulator.
    - Click on the `Run` button in Android Studio or use the `Shift + F10` shortcut.

3. **Debug the app:**
    - Click on the `Debug` button in Android Studio or use the `Shift + F9` shortcut.
