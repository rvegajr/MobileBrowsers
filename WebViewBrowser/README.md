# WebView Browser

A powerful Android web browser application with built-in developer tools and credential management, mirroring the functionality of the iOS WebKitBrowser.

## Features

### Core Browser Functionality

* Full-featured web browsing using Android WebView
* Modern address bar with URL validation
* Navigation controls (back, forward, refresh)
* Support for landscape and portrait orientations
* Handles popup windows and redirects

### Developer Tools

* HTML source code viewer with syntax highlighting
* DOM inspection capabilities
* JavaScript interaction
* Split-view developer tools interface
* Enhanced JavaScript console log viewer with:
  * Color-coded output (error, warning, info, debug)
  * Timestamped entries
  * Object and array formatting
  * Copy and clear functionality
  * Test log generation
* Source code display with proper formatting
* Tab-based interface for switching between source and console views

### Credential Management

* Secure credential storage using Android KeyStore
* Per-domain username/password management
* Automatic credential detection and filling
* Secure deletion of stored credentials

### Variable Management

* Define custom variables for form filling
* Insert variables into web forms
* Edit and manage stored variables

### History Management

* Track and store browsing history
* Search and filter history entries
* Delete individual or all history entries

## Technical Stack

* Android API 21+ (Android 5.0 Lollipop and above)
* Java
* WebView for rendering web content
* Room Database for history storage
* Android KeyStore for secure credential storage
* Material Design Components
* JSoup for HTML formatting

## Recent Updates

### April 26, 2025
* Fixed console logging initialization and display issues
* Enhanced the JavaScript interface between WebView and native app
* Improved error handling and thread safety in console log processing
* Added automatic source code loading when DevTools is first opened
* Enhanced console log entry styling with better timestamps and colors

### April 25, 2025
* Added JavaScript console log viewing functionality
* Implemented color-coded log entries with timestamp display
* Added toolbar action buttons (Clear, Copy, Test Log)
* Enhanced source code display with proper formatting

## Project Structure

```plaintext
WebViewBrowser/
├── app/                                  # Application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/noctusoft/webviewbrowser/
│   │   │   │   ├── BrowserActivity.java           # Main browser UI and logic
│   │   │   │   ├── CredentialsManager.java        # Secure credential storage
│   │   │   │   ├── HistoryManager.java            # Browsing history management
│   │   │   │   ├── VariablesManager.java          # Variable management for forms
│   │   │   │   ├── utils/                         # Utility classes
│   │   │   │   ├── model/                         # Data models
│   │   │   │   └── ui/                            # UI components
│   │   │   ├── res/                               # Resources
│   │   │   └── AndroidManifest.xml               # App configuration
│   │   └── test/                                 # Unit and UI tests
│   ├── build.gradle                              # App module build config
├── fastlane/                                     # Fastlane automation
│   ├── Fastfile                                  # Lane definitions
│   └── Appfile                                   # App configuration
├── gradle/                                       # Gradle wrapper files
├── build.gradle                                  # Project build config
├── settings.gradle                               # Project settings
├── .ai-index                                     # AI development index
└── README.md                                     # Project documentation
```

## Development Setup

### Quick Start

1. Clone the repository:

```bash
git clone https://github.com/noctusoft/MobileBrowsers.git
cd MobileBrowsers/WebViewBrowser
```

2. Open the project in Android Studio.

3. Build and run the application.

### Building with Fastlane

This project includes Fastlane to automate building and testing:

```bash
# Run unit tests
fastlane android test

# Build debug APK
fastlane android build_debug

# Build release APK
fastlane android build_release
```

## Implementation Notes

### WebView Configuration

The Android WebView is configured with:
- JavaScript enabled
- DOM storage enabled
- Zoom controls
- Safe browsing
- JavaScript interface for console logging
- Appropriate permissions for internet access

### Security Considerations

- Credentials are securely stored using Android KeyStore
- User data is kept local to the device
- No tracking or analytics included
- Secure WebView configuration to prevent common vulnerabilities

### Feature Comparison with iOS WebKitBrowser

This Android implementation mirrors the iOS WebKitBrowser with equivalent functionality:
- Both use the native web rendering engine (WebKit on iOS, WebView on Android)
- Both implement the same credential management approach (KeyChain on iOS, KeyStore on Android)
- Both provide developer tools with HTML source inspection and console logging
- Both maintain browsing history with search capabilities
- Both offer form variable management
- Both feature similar UIs with tab-based DevTools panels

## AI Development

The project includes an `.ai-index` file that provides:
* Code generation templates
* Testing strategies
* Security considerations
* Performance monitoring guidelines
* Project commands
* Future enhancement plans

Always refer to `.ai-index` before making changes to ensure consistency with project standards.
