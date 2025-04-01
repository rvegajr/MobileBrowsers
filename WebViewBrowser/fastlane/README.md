# Fastlane Configuration for WebViewBrowser

This directory contains Fastlane configuration files for automating the build, test, and deployment processes of the WebViewBrowser Android application.

## Available Lanes

* `fastlane android test` - Run all unit tests
* `fastlane android build_debug` - Build a debug APK
* `fastlane android build_release` - Build a release APK (requires signing configuration)
* `fastlane android beta` - Deploy to Google Play Beta track
* `fastlane android production` - Deploy to Google Play Production track
* `fastlane android distribute` - Distribute via Firebase App Distribution

## Setup

### Requirements

* Ruby 2.5.0 or higher
* Bundler gem installed
* Android SDK configured
* Java Development Kit (JDK) 8 or higher

### Installation

1. Install Fastlane using Bundler:

```bash
/bin/zsh -i -c 'source ~/.zshrc && bundle install'
```

2. For release builds, configure the signing credentials as environment variables:

```bash
export KEYSTORE_PATH=/path/to/keystore.jks
export STORE_PASSWORD=your_store_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password
```

3. For Firebase App Distribution, set up the Firebase CLI and authentication:

```bash
/bin/zsh -i -c 'source ~/.zshrc && npm install -g firebase-tools'
/bin/zsh -i -c 'source ~/.zshrc && firebase login'
```

## Usage Examples

### Running Tests

```bash
/bin/zsh -i -c 'source ~/.zshrc && bundle exec fastlane android test'
```

### Building Debug APK

```bash
/bin/zsh -i -c 'source ~/.zshrc && bundle exec fastlane android build_debug'
```

### Building Release APK

```bash
/bin/zsh -i -c 'source ~/.zshrc && bundle exec fastlane android build_release'
```

### Distributing to Firebase Testing

```bash
/bin/zsh -i -c 'source ~/.zshrc && bundle exec fastlane android distribute'
```

## Customization

Edit the `Fastfile` to customize the build process based on your specific requirements. You can add additional lanes or modify existing ones as needed.
