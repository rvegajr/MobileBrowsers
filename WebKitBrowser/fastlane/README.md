fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## iOS

### ios resolve_packages

```sh
[bundle exec] fastlane ios resolve_packages
```

Resolve and download all Swift packages

### ios test

```sh
[bundle exec] fastlane ios test
```

Run all tests

### ios build_dev

```sh
[bundle exec] fastlane ios build_dev
```

Build and run development version

### ios build_release

```sh
[bundle exec] fastlane ios build_release
```

Build release version

### ios deploy_testflight

```sh
[bundle exec] fastlane ios deploy_testflight
```

Deploy to TestFlight

### ios deploy_appstore

```sh
[bundle exec] fastlane ios deploy_appstore
```

Deploy to App Store

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
