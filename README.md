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
3. **Copy Source**: Tap the "Copy All" button to copy the entire HTML source to your clipboard
4. **Analyze**: Paste the source into your preferred editor for analysis or sharing

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

3. **Run in Xcode**:

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
   /bin/zsh -i -c 'source ~/.zshrc && ./gradlew installDebug'
   ```

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