# MobileBrowsers

A collection of mobile web browsers for iOS and Android with enhanced developer features and debugging capabilities.

## Overview

This repository contains two browser implementations:

- **WebKitBrowser** - An iOS browser built with WebKit
- **WebViewBrowser** - An Android browser built with WebView

Both browsers provide similar features including browsing history management, credential storage, HTML source viewing, and developer tools integration.

## Requirements

### iOS (WebKitBrowser)

- macOS 11.0 or later
- Xcode 13.0 or later
- iOS 14.0+ deployment target
- Swift 5.5+
- CocoaPods (for dependency management)

### Android (WebViewBrowser)

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 21+ (Android 5.0 Lollipop or higher)
- Gradle 7.0+
- Java 11+

## Getting Started

### Setting Up WebKitBrowser (iOS)

1. **Install dependencies**:

   ```bash
   cd WebKitBrowser
   /bin/zsh -i -c 'source ~/.zshrc && pod install'
   ```

2. **Open the project**:

   ```bash
   /bin/zsh -i -c 'source ~/.zshrc && open WebKitBrowser.xcworkspace'
   ```

3. **Run using Xcode**:
   - Select your target device/simulator
   - Press ⌘R to build and run

4. **Using automation scripts**:

   ```bash
   cd WebKitBrowser
   /bin/zsh -i -c 'source ~/.zshrc && ./scripts/build.sh --init --debug'
   ```

### Setting Up WebViewBrowser (Android)

1. **Setup using Android Studio**:

   ```bash
   # Open Android Studio
   /bin/zsh -i -c 'source ~/.zshrc && open -a "Android Studio" WebViewBrowser'
   ```
   
   - Let Android Studio sync the Gradle files
   - Configure an emulator via AVD Manager (Tools → AVD Manager)
   - Click the "Run" button to build and deploy

2. **Using automation scripts**:

   ```bash
   cd WebViewBrowser
   
   # Build and run on a specified emulator (e.g., Pixel 7 with API 33)
   /bin/zsh -i -c 'source ~/.zshrc && ./scripts/run_emulator.sh --emulator-config "Pixel 7 API 33"'
   
   # For headless mode (no emulator UI)
   /bin/zsh -i -c 'source ~/.zshrc && ./scripts/run_emulator.sh --headless'
   
   # Build the project only
   /bin/zsh -i -c 'source ~/.zshrc && ./scripts/build.sh --debug'
   ```

## Key Features

Both browser implementations provide these core features:

- **History Management** - Store and view browsing history with search functionality
- **Credential Management** - Securely store and retrieve website credentials
- **Variable Storage** - Define variables for form auto-filling
- **Developer Tools** - View page source, inspect elements, and view console logs
- **Navigation Controls** - Standard browser navigation (back, forward, refresh)

## Development Workflow

### iOS Development

The recommended workflow for iOS development:

1. Use the Xcode workspace (WebKitBrowser.xcworkspace)
2. Make changes to Swift files in the Sources directory
3. Build and test on a simulator or device
4. Use the scripts directory for automation tasks

### Android Development

The recommended workflow for Android development:

1. Open the project in Android Studio
2. Make changes to Java files in app/src/main/java
3. Test using the built-in emulator
4. Use the scripts directory for automation tasks
5. Use Fastlane (in the fastlane directory) for CI/CD integration

## Troubleshooting

### iOS Issues

- If you encounter issues with dependencies, run `pod update` to refresh all pods
- Clear derived data if you experience build errors: `rm -rf ~/Library/Developer/Xcode/DerivedData`

### Android Issues

- Run `./gradlew clean` to clean the build if you encounter build errors
- Check that Android SDK paths are correctly set in local.properties
- Use Logcat in Android Studio to see detailed logs

## License

This project is licensed under the MIT License - see the LICENSE file for details.