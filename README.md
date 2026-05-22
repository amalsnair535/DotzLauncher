# Dotz Launcher

A minimal, grid-based Android launcher designed for focus and aesthetic simplicity. Dotz replaces distracting colorful icons with a sleek, customizable dot-based interface.

![Launcher Icon](app/src/main/res/drawable/ic_launcher_foreground.xml)

## ✨ Features

- **Minimalist 8x8 Grid**: A clean, distraction-free home screen layout.
- **Icon Pack Support**: Compatibility with standard Android icon packs using `appfilter.xml` parsing.
- **Intelligent App Selection**: When remapping tiles, the launcher suggests relevant apps (e.g., dialers for the Phone tile, music players for the Music tile).
- **Detox Panel**: Quick access toggles for WiFi, Bluetooth, Mobile Data, Airplane Mode, Silent Mode, Torch, and Dark Mode.
- **Notification Badges**: Supports both notification dots and numerical counts for communication apps.
- **Customizable Appearance**:
    - Adjustable tile opacity.
    - Grayscale mode for system icons.
    - Dynamic (Material You) theme support on Android 12+.
- **Backup & Restore**: Export and import your launcher configurations as JSON files.

## 🚀 Getting Started

### Prerequisites
- Android device running API 26 (Android 8.0) or higher.
- Android Studio Ladybug or newer for development.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/amalsnair535/DotzLauncher.git
   ```
2. Open the project in Android Studio.
3. Build and run the `:app` module on your device.
4. Set **Dotz Launcher** as your default home app when prompted.

## 🛠 Built With
- **Kotlin**: Primary programming language.
- **Jetpack Compose**: Modern toolkit for building native UI.
- **DataStore**: Modern data storage solution for settings.
- **GSON**: For JSON serialization of backups.

## 📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
