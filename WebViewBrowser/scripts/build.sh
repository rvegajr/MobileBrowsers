#!/bin/zsh

# WebViewBrowser Build Script
# This script helps build and initialize the WebViewBrowser Android project

# Exit on error
set -e

# Source zshrc for environment variables
source ~/.zshrc

# Detect script directory and project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Function to check if command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Function to display help message
show_help() {
  echo "WebViewBrowser Build Script"
  echo ""
  echo "Usage: ./build.sh [options]"
  echo ""
  echo "Options:"
  echo "  --init       Initialize the project (install dependencies)"
  echo "  --debug      Build debug APK"
  echo "  --release    Build release APK"
  echo "  --test       Run tests"
  echo "  --install    Install debug APK to connected device"
  echo "  --clean      Clean the project"
  echo "  --help       Show this help message"
  echo ""
  echo "Example: ./build.sh --init --debug"
}

# Check for Android SDK
check_android_sdk() {
  if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME environment variable is not set."
    echo "Please set it to the path of your Android SDK."
    exit 1
  fi
  
  if [ ! -d "$ANDROID_HOME" ]; then
    echo "Error: Android SDK directory not found at $ANDROID_HOME"
    exit 1
  fi
  
  echo "✓ Android SDK found at $ANDROID_HOME"
}

# Initialize the project
init_project() {
  cd "$PROJECT_ROOT"
  
  echo "Checking for required dependencies..."
  
  # Check for required tools
  if ! command_exists gradle; then
    echo "Error: Gradle not found. Please install it first."
    exit 1
  fi
  
  if ! command_exists bundle; then
    echo "Installing Bundler..."
    gem install bundler
  fi
  
  echo "Installing Ruby dependencies..."
  bundle install
  
  echo "Installing Fastlane plugins..."
  bundle exec fastlane install_plugins
  
  echo "Project initialized successfully!"
}

# Build debug APK
build_debug() {
  cd "$PROJECT_ROOT"
  echo "Building debug APK..."
  
  if command_exists bundle && [ -f "$PROJECT_ROOT/Gemfile" ]; then
    bundle exec fastlane android build_debug
  else
    ./gradlew assembleDebug
  fi
  
  echo "Debug APK built successfully!"
}

# Build release APK
build_release() {
  cd "$PROJECT_ROOT"
  echo "Building release APK..."
  
  if command_exists bundle && [ -f "$PROJECT_ROOT/Gemfile" ]; then
    bundle exec fastlane android build_release
  else
    ./gradlew assembleRelease
  fi
  
  echo "Release APK built successfully!"
}

# Run tests
run_tests() {
  cd "$PROJECT_ROOT"
  echo "Running tests..."
  
  if command_exists bundle && [ -f "$PROJECT_ROOT/Gemfile" ]; then
    bundle exec fastlane android test
  else
    ./gradlew test
  fi
  
  echo "Tests completed!"
}

# Install debug APK to connected device
install_debug() {
  cd "$PROJECT_ROOT"
  echo "Installing debug APK to connected device..."
  
  ./gradlew installDebug
  
  echo "APK installed successfully!"
}

# Clean the project
clean_project() {
  cd "$PROJECT_ROOT"
  echo "Cleaning project..."
  
  ./gradlew clean
  
  echo "Project cleaned successfully!"
}

# Check for arguments
if [ $# -eq 0 ]; then
  show_help
  exit 0
fi

# Process arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --init)
      check_android_sdk
      init_project
      shift
      ;;
    --debug)
      build_debug
      shift
      ;;
    --release)
      build_release
      shift
      ;;
    --test)
      run_tests
      shift
      ;;
    --install)
      install_debug
      shift
      ;;
    --clean)
      clean_project
      shift
      ;;
    --help)
      show_help
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      show_help
      exit 1
      ;;
  esac
done

echo "All tasks completed successfully!"
exit 0
