# WebKit Browser

A powerful iOS web browser application with built-in developer tools and credential management.

## Features

### Core Browser Functionality

* Full-featured web browsing using WKWebView
* Modern address bar with URL validation
* Navigation controls (back, forward, refresh)
* Support for landscape and portrait orientations
* Handles popup windows and redirects
* JavaScript console log viewer with timestamped output

### Developer Tools

* Built-in web inspector
* DOM element search functionality
* Live HTML source code viewer
* Enhanced JavaScript console with:
  * Color-coded output by log type (error, warning, info, debug)
  * Timestamped entries
  * Object and array formatting
  * Copy and clear functionality
  * Test log generation
* Split-view developer tools interface
* Tab-based design for switching between source and console views

### Credential Management

* Secure credential storage using iOS Keychain
* Per-domain username/password management
* Automatic credential detection and filling
* Secure deletion of stored credentials
* Smart form field detection for autofill

### Security

* Secure credential storage using iOS Keychain
* HTTPS support
* Privacy-focused browsing
* No tracking or data collection

## Technical Stack

* iOS 14.0+
* Swift 5.0+
* WebKit Framework
* Security Framework (Keychain)

## Recent Updates

### April 26, 2025
* Synchronized console logging implementation between iOS and Android
* Improved console log styling and timestamps format
* Enhanced user interface for better developer experience
* Added consistent tab-based interface matching Android implementation

### April 25, 2025
* Fixed iOS 14.0 compatibility issues in HistoryListViewController with cell configuration API
* Added proper availability checking for iOS 14.0 APIs
* Restored CocoaPods configuration with explicit Podfile
* Integrated the following CocoaPods dependencies:
  * FLEX (debugging UI in development builds)
  * KeychainAccess (secure credential storage)
  * MBProgressHUD (activity indicators)
  * ReachabilitySwift (network connectivity)
  * SnapKit (programmatic constraints)
  * SwiftLint (code quality)
  * SwipeCellKit (swipeable table cells)
  * Toast-Swift (notifications)
  * Quick, Nimble, CwlPreconditionTesting (testing frameworks)

## Project Structure

```plaintext
WebKitBrowser/
├── Sources/                          # Source code files
│   ├── BrowserViewController.swift   # Main browser UI and logic
│   ├── CredentialsManager.swift      # Keychain credential management
│   ├── DevToolsViewController.swift  # Developer tools implementation
│   ├── ConsoleLogManager.swift       # Console log handling and display
│   ├── AppDelegate.swift            # Application lifecycle
│   ├── SceneDelegate.swift          # UI scene management
│   └── Info.plist                   # App configuration
├── Resources/                        # Interface and asset files
│   ├── LaunchScreen.storyboard      # Launch screen UI
│   └── Main.storyboard             # Main interface
├── Tests/                           # Unit and UI tests
│   └── BrowserViewControllerTests.swift
├── fastlane/                        # Fastlane automation
│   ├── Fastfile                     # Lane definitions
│   ├── Appfile                      # App configuration
│   └── Scanfile                     # Test configuration
├── scripts/                         # Development scripts
│   └── setup.sh                     # Project setup script
├── .swiftlint.yml                   # SwiftLint configuration
├── .ai-index                        # AI development index
├── project.yml                      # XcodeGen configuration
└── README.md                        # Project documentation
```

## Development Setup

### Quick Start

1. Clone the repository:

```bash
git clone https://github.com/noctusoft/MobileBrowsers.git
cd MobileBrowsers/WebKitBrowser
```

2. Run the setup script:

```bash
./scripts/setup.sh
```

This script will:
* Install required tools (xcodegen, fastlane, swiftlint)
* Create project structure
* Generate Xcode project
* Initialize Fastlane
* Run initial tests

### Manual Setup

If you prefer to set up manually, follow these steps:

1. Install required tools:

```bash
brew install xcodegen
brew install fastlane
brew install swiftlint
xcode-select --install
```

2. Generate Xcode project:

```bash
xcodegen generate
```

3. Initialize Fastlane:

```bash
fastlane init
```

4. Update Fastlane configuration:
* Edit `fastlane/Appfile` with your Apple ID
* Configure your team ID in `project.yml`

## Development Workflow

### Building the Project

1. Development build:

```bash
fastlane build_dev
```

2. Release build:

```bash
fastlane build_release
```

### Running Tests

1. Run all tests:

```bash
fastlane test
```

2. Run specific test suite:

```bash
fastlane test suite:"BrowserViewControllerTests"
```

### Code Quality

1. Run SwiftLint:

```bash
swiftlint
```

2. Auto-correct fixable issues:

```bash
swiftlint --fix
```

### Deployment

1. Deploy to TestFlight:

```bash
fastlane deploy_testflight
```

2. Deploy to App Store:

```bash
fastlane deploy_appstore
```

## Feature Comparison with Android WebViewBrowser

This iOS implementation is feature-compatible with the Android WebViewBrowser:
- Both use the native web rendering engine (WebKit on iOS, WebView on Android)
- Both implement the same credential management approach (KeyChain on iOS, KeyStore on Android)
- Both provide developer tools with HTML source inspection and console logging
- Both maintain browsing history with search capabilities
- Both offer form variable management
- Both feature similar UIs with tab-based DevTools panels
