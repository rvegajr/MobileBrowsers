# WebKit Browser

A powerful iOS web browser application with built-in developer tools and credential management.

## Features

### Core Browser Functionality

* Full-featured web browsing using WKWebView
* Modern address bar with URL validation
* Navigation controls (back, forward, refresh)
* Support for landscape and portrait orientations
* Handles popup windows and redirects

### Developer Tools

* Built-in web inspector
* DOM element search functionality
* Live HTML source code viewer
* JavaScript console output
* Split-view developer tools interface

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

## Project Structure

```plaintext
WebKitBrowser/
├── Sources/                          # Source code files
│   ├── BrowserViewController.swift   # Main browser UI and logic
│   ├── CredentialsManager.swift      # Keychain credential management
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
git clone https://github.com/noctusoft/WebKitBrowser.git
cd WebKitBrowser
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

## Available Fastlane Lanes

* `fastlane test` - Run all tests
* `fastlane build_dev` - Build development version
* `fastlane build_release` - Build release version
* `fastlane deploy_testflight` - Deploy to TestFlight
* `fastlane deploy_appstore` - Deploy to App Store

## AI Development

The project includes an `.ai-index` file that provides:
* Code generation templates
* Testing strategies
* Security considerations
* Performance monitoring guidelines
* Project commands
* Future enhancement plans

Always refer to `.ai-index` before making changes to ensure consistency with project standards.

## Code Style

The project uses SwiftLint with custom rules defined in `.swiftlint.yml`:
* Line length limits
* Function complexity rules
* Naming conventions
* Custom logging rules

## Contributing

1. Fork the repository

2. Create a feature branch:

```bash
git checkout -b feature/your-feature-name
```

3. Make your changes

4. Ensure tests pass:

```bash
fastlane test
```

5. Run SwiftLint:

```bash
swiftlint
```

6. Submit a pull request

## Troubleshooting

### Common Issues

1. **Project won't generate**
   * Verify XcodeGen is installed
   * Check project.yml syntax
   * Run with verbose logging: `xcodegen generate --verbose`

2. **Tests fail to run**
   * Check simulator availability
   * Verify test target configuration
   * Run tests with `--verbose` flag

3. **SwiftLint errors**
   * Update SwiftLint to latest version
   * Check .swiftlint.yml configuration
   * Run auto-correct: `swiftlint --fix`

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

* Website: [https://noctusoft.com](https://noctusoft.com)
* Email: [support@noctusoft.com](mailto:support@noctusoft.com)
* Twitter: [@noctusoftdev](https://twitter.com/noctusoftdev)
