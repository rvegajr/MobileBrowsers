# MobileBrowsers

A collection of mobile web browsers for iOS and Android with enhanced developer features and debugging capabilities, specifically designed to help .NET MAUI developers inspect and debug web content on mobile devices.

## Purpose

MobileBrowsers was created to solve a critical challenge faced by .NET MAUI and mobile developers when working with WebView components: **the inability to easily inspect, debug, and view the source of web content on actual mobile devices**.

While desktop browsers offer powerful developer tools, mobile devices typically lack these capabilities, making it difficult to:

- View the actual HTML source rendered on a mobile device
- Debug responsive design issues specific to mobile viewports
- Troubleshoot WebView rendering differences between iOS and Android
- Identify platform-specific JavaScript or CSS issues
- Inspect network requests and responses

This toolset bridges that gap by providing dedicated browser applications with built-in developer tools for both major mobile platforms.

## Use Cases

### For .NET MAUI Developers

1. **Hybrid App Development**
   - Inspect how your MAUI WebView renders HTML content on different devices
   - Identify rendering inconsistencies between iOS and Android WebView implementations
   - Debug JavaScript interactions between your MAUI app and web content

2. **Responsive Design Testing**
   - View and copy the actual HTML/CSS as rendered on a specific mobile device
   - Troubleshoot responsive design breakpoints not working as expected on real devices
   - Compare the DOM structure on mobile vs desktop browsers

3. **Authentication Troubleshooting**
   - Debug complex authentication flows in WebViews
   - View cookies and storage to verify proper authentication state
   - Test credential management in a controlled environment

4. **Production Issue Investigation**
   - Reproduce and debug issues reported by users on specific devices
   - Inspect the exact HTML/CSS/JavaScript executing on the problematic platform
   - Save page source for offline analysis and sharing with your team

### For Mobile QA Teams

1. **Cross-Platform Verification**
   - Ensure consistent rendering across multiple devices
   - Document platform-specific differences
   - Validate responsive design implementations

2. **Regression Testing**
   - Capture and compare page source before and after updates
   - Verify DOM changes work consistently across devices
   - Test progressive web apps (PWAs) on different platforms

## How to Use WebViewBrowser (Android)

### Basic Navigation

1. **Launch the App**: Open WebViewBrowser from your app drawer
2. **Browse to Your Target Page**: Enter the URL in the address bar and tap Go
3. **Navigate**: Use the back/forward/refresh buttons for standard navigation

### Viewing Page Source

1. **Open Developer Tools**: Tap the menu button (three dots) and select "Developer Tools"
2. **View Source**: The HTML source of the current page will be displayed in a scrollable view
3. **Copy Source**: Tap the "Copy" button to copy the entire HTML source to your clipboard
4. **Analyze**: Paste the source into your preferred editor for analysis or sharing

### Using Console Logs

1. **Open Developer Tools**: Tap the menu button (three dots) and select "Developer Tools"
2. **Switch to Console Tab**: Tap the "Console" tab in the developer tools panel
3. **View Logs**: All JavaScript console logs will be displayed with timestamps and color-coding
4. **Test Logging**: Use the "Test Log" button to generate sample logs
5. **Clear Logs**: Tap the "Clear" button to clear all console logs
6. **Copy Logs**: Tap the "Copy" button to copy all console logs to your clipboard

### Managing Favorites

1. **Add a Favorite**: Navigate to a page, tap the menu button, and select "Add to Favorites"
2. **View Favorites**: Tap the star icon to view your saved favorites
3. **Select a Favorite**: Tap any favorite in the list to navigate to that page
4. **Default Favorites**: The app comes pre-loaded with useful sites for MAUI developers:
   - alliedpilots.org
   - integ.alliedpilots.org
   - expense.integ.alliedpilots.org

### Using History

1. **Access History**: Tap the history icon to view your browsing history
2. **Search History**: Use the search box to find specific pages you've visited
3. **Clear History**: Tap and hold on any history item to see deletion options

### Working with Credentials

1. **Save Credentials**: When you encounter a login form, enter your credentials and choose to save them
2. **Auto-fill**: When returning to a site with saved credentials, they will be available for auto-fill
3. **Manage Credentials**: Access saved credentials through the menu system

## Screenshots

### WebViewBrowser (Android)

![Developer Tools View](./docs/images/webviewbrowser_dev_tools.png)
*Developer tools panel showing HTML source with Copy All button*

![Favorites Management](./docs/images/webviewbrowser_favorites.png)
*Favorites management with preloaded default sites*

### WebKitBrowser (iOS)

![Source Code View](./docs/images/webkitbrowser_source.png)
*Source code inspection on iOS*

![Developer Console](./docs/images/webkitbrowser_console.png)
*JavaScript console access on iOS*

## Future Roadmap

We're continuously improving MobileBrowsers to make it even more useful for MAUI and mobile developers:

### Planned Enhancements

1. **Enhanced Element Inspector**
   - Visual DOM tree navigation
   - Real-time CSS property inspection and modification
   - Box model visualization

2. **Network Request Monitoring**
   - Track all network requests and responses
   - View headers, payloads, and timing information
   - Filter requests by type, status, or domain

3. **JavaScript Debugging** 
   - Set breakpoints in JavaScript code 
   - Inspect variables and call stack
   - Step through code execution
   - View and analyze console logs (Implemented 2025-04-26)

4. **Cross-Browser Testing**
   - Side-by-side comparison of rendering between WebKit and WebView
   - Automated visual regression testing
   - Pixel-perfect comparison tools

5. **MAUI Integration Plugin**
   - Direct integration with Visual Studio and MAUI projects
   - Send pages from MAUI WebView to MobileBrowsers with one click
   - Synchronize debugging between your app and the browser

### Community Contributions

We welcome contributions from the community! If you're interested in contributing, please check our [CONTRIBUTING.md](./CONTRIBUTING.md) file for guidelines. Whether it's bug fixes, new features, or documentation improvements, your help is appreciated.

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

### Opening the Projects

Both iOS and Android projects are ready to use immediately after cloning the repository!

#### Opening WebKitBrowser (iOS)

1. **Double-click** the `WebKitBrowser/WebKitBrowser.xcworkspace` file
   - This will open the project directly in Xcode
   - All project settings and dependencies are pre-configured

2. **Run in Xcode**:
   - Select your target device/simulator
   - Press ⌘R to build and run

#### Opening WebViewBrowser (Android)

1. **Open Android Studio**

2. Select **Open an existing project**

3. Navigate to and select the `WebViewBrowser` directory
   - The project will open and configure itself automatically
   - Gradle will sync and download any required dependencies

4. **Build and Run**:
   - Select your target device/emulator
   - Click Run (▶) to build and deploy

> **Tip:** You can also double-click the project directory in Finder while holding the Command key and selecting "Open With > Android Studio"

### Ready to Use!

Both projects are pre-configured with all necessary build files and dependencies:
- iOS: Includes `.xcworkspace` and `.xcodeproj` files
- Android: Includes Gradle wrapper and project configuration

No additional setup steps required - just clone, open, and start coding!

### Verify Builds Script

For quick verification that both iOS and Android projects build successfully, use the included verification script:

```bash
./verify_builds.sh
```

This script:
- Checks for required development environments (Android Studio, Xcode)
- Verifies all necessary build tools are available
- Builds both projects with appropriate settings
- Reports build success or failure with detailed logging
- Automatically bypasses code signing for development builds

## Recent Updates

### April 26, 2025
- Fixed console logging initialization and display issues in Android
- Enhanced JavaScript interface between WebView and native app
- Improved error handling and thread safety in console log processing
- Added automatic source code loading when DevTools is first opened
- Synchronized console logging implementations between iOS and Android
- Enhanced console log entry styling with better timestamps and colors

### April 25, 2025
- Added JavaScript console log viewing functionality to both Android and iOS browsers
- Implemented color-coded log entries with timestamp display
- Fixed various build issues for both platforms
- Added build verification script for seamless cross-platform development
- Standardized TabView UI for switching between source and console views

## Tips for MAUI Developers

1. **Testing Hybrid Approaches**:
   - Use WebViewBrowser to test your web components independently before embedding them in MAUI
   - Compare behaviors between WebView in MobileBrowsers and WebView in your MAUI app

2. **Sharing Debug Information**:
   - Copy entire page source to share with team members for collaborative debugging
   - Document platform-specific rendering differences

3. **Design Validation**:
   - Use the browser to test responsive designs across different device form factors
   - Validate media queries and breakpoints on actual mobile viewports

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