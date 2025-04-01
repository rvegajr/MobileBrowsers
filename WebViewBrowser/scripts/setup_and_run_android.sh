#!/bin/zsh

# Setup and Run Script for WebViewBrowser Android
# This script automates the process of setting up the Android SDK, creating an emulator,
# and running the WebViewBrowser application.

# Exit on error
set -e

# Source zshrc for environment variables
source ~/.zshrc

# Detect script directory and project root
SCRIPT_DIR="${0:A:h}"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Configuration variables
ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
EMULATOR_NAME="WebViewBrowser_Emulator"
SYSTEM_IMAGE="system-images;android-30;google_apis;x86_64"
AVD_DEVICE="pixel_4"
AVD_SKIN="1080x2340"
API_LEVEL="30"
GRADLE_TASKS="clean assembleDebug"

# Log function
log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Check if command exists
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

# Show help
show_help() {
  echo "WebViewBrowser Android Setup and Run Script"
  echo ""
  echo "Usage: ./setup_and_run_android.sh [options]"
  echo ""
  echo "Options:"
  echo "  --sdk-only                  Download and setup Android SDK only"
  echo "  --emulator-only             Create emulator without running the app"
  echo "  --run-only                  Run the app on an existing emulator"
  echo "  --clean                     Clean the project before building"
  echo "  --headless                  Run emulator in headless mode"
  echo "  --emulator-config [CONFIG]  Specify emulator configuration (e.g., 'Pixel 7 API 33')"
  echo "                              Format: '<device> API <level>' (e.g., 'Pixel 7 API 33')"
  echo "  --help                      Show this help message"
  echo ""
  echo "Example: ./setup_and_run_android.sh --clean --emulator-config 'Pixel 7 API 33'"
}

# Parse emulator configuration
parse_emulator_config() {
  local config="$1"
  
  # Extract API level
  if [[ "$config" =~ API[[:space:]]+([0-9]+) ]]; then
    API_LEVEL="${BASH_REMATCH[1]}"
    SYSTEM_IMAGE="system-images;android-$API_LEVEL;google_apis;x86_64"
  fi
  
  # Extract device model (everything before "API")
  if [[ "$config" =~ ^(.+)[[:space:]]+API ]]; then
    local device_model="${BASH_REMATCH[1]}"
    
    # Convert to lower case and replace spaces with underscores
    device_model=$(echo "$device_model" | tr '[:upper:]' '[:lower:]' | tr ' ' '_')
    
    # Map common device names to AVD device IDs
    case "$device_model" in
      pixel_7)
        AVD_DEVICE="pixel_7"
        AVD_SKIN="1080x2400"
        ;;
      pixel_6)
        AVD_DEVICE="pixel_6"
        AVD_SKIN="1080x2400"
        ;;
      pixel_5)
        AVD_DEVICE="pixel_5"
        AVD_SKIN="1080x2340"
        ;;
      pixel_4)
        AVD_DEVICE="pixel_4"
        AVD_SKIN="1080x2340"
        ;;
      pixel_3)
        AVD_DEVICE="pixel_3"
        AVD_SKIN="1080x2160"
        ;;
      *)
        log "Warning: Unknown device model '$device_model'. Using default (pixel_4)."
        AVD_DEVICE="pixel_4"
        AVD_SKIN="1080x2340"
        ;;
    esac
  fi
  
  # Update emulator name to reflect configuration
  EMULATOR_NAME="WebViewBrowser_${AVD_DEVICE}_API${API_LEVEL}"
  
  log "Using emulator configuration: Device=$AVD_DEVICE, API Level=$API_LEVEL, System Image=$SYSTEM_IMAGE"
}

# Download and set up Android SDK using the command line
setup_android_sdk() {
  log "Setting up Android SDK..."
  
  # Create Android SDK directory if it doesn't exist
  mkdir -p "$ANDROID_SDK_ROOT"
  
  # Get the latest command line tools
  CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip"
  TOOLS_ZIP="$ANDROID_SDK_ROOT/cmdline-tools.zip"
  
  if [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
    log "Downloading command line tools..."
    curl -o "$TOOLS_ZIP" "$CMDLINE_TOOLS_URL"
    
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
    unzip -q -o "$TOOLS_ZIP" -d "$ANDROID_SDK_ROOT/cmdline-tools"
    mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    rm "$TOOLS_ZIP"
  fi
  
  # Set environment variables
  export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
  export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
  
  # Install required SDK components
  log "Installing SDK components..."
  yes | sdkmanager --install "platform-tools" "platforms;android-$API_LEVEL" "build-tools;30.0.3" "$SYSTEM_IMAGE"
  
  # Accept licenses
  yes | sdkmanager --licenses
  
  log "Android SDK setup complete."
}

# Create Android emulator
create_emulator() {
  log "Creating Android emulator ($EMULATOR_NAME)..."
  
  # Check if emulator already exists
  if echo "no" | avdmanager list avd | grep -q "$EMULATOR_NAME"; then
    log "Emulator $EMULATOR_NAME already exists. Skipping creation."
    return 0
  fi
  
  # Create a new AVD
  echo "no" | avdmanager create avd \
    --name "$EMULATOR_NAME" \
    --package "$SYSTEM_IMAGE" \
    --device "$AVD_DEVICE" \
    --force
  
  # Configure emulator settings (optional)
  echo "hw.lcd.density=420" >> "$HOME/.android/avd/$EMULATOR_NAME.avd/config.ini"
  echo "hw.keyboard=yes" >> "$HOME/.android/avd/$EMULATOR_NAME.avd/config.ini"
  
  log "Emulator $EMULATOR_NAME created successfully."
}

# Start the emulator
start_emulator() {
  log "Starting emulator ($EMULATOR_NAME)..."
  
  # Start the emulator in the background
  if [ "$HEADLESS" = true ]; then
    "$ANDROID_SDK_ROOT/emulator/emulator" -avd "$EMULATOR_NAME" -no-window -no-boot-anim -no-audio -gpu swiftshader_indirect &
  else
    "$ANDROID_SDK_ROOT/emulator/emulator" -avd "$EMULATOR_NAME" -gpu swiftshader_indirect &
  fi
  
  EMULATOR_PID=$!
  
  # Wait for emulator to boot
  log "Waiting for emulator to boot..."
  
  while ! adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; do
    sleep 2
  done
  
  log "Emulator is now ready."
}

# Build the project
build_project() {
  log "Building WebViewBrowser project..."
  
  # Navigate to project directory
  cd "$PROJECT_ROOT"
  
  # Create gradle wrapper if it doesn't exist
  if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
    log "Creating Gradle wrapper..."
    gradle wrapper
  fi
  
  # Build the project
  ./gradlew $GRADLE_TASKS
  
  log "Build complete."
}

# Install and run the app
install_and_run() {
  log "Installing app on emulator..."
  
  # Navigate to project directory
  cd "$PROJECT_ROOT"
  
  # Install the app
  adb install -r "$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
  
  # Start the app
  log "Launching WebViewBrowser..."
  adb shell am start -n "com.noctusoft.webviewbrowser/.BrowserActivity"
  
  log "App launched successfully!"
}

# Clean up function to be called on exit
cleanup() {
  log "Cleaning up resources..."
  
  # Kill emulator if it was started by this script
  if [ -n "$EMULATOR_PID" ]; then
    kill $EMULATOR_PID 2>/dev/null || true
  fi
  
  log "Cleanup completed."
}

# Register the cleanup function to be called on exit
trap cleanup EXIT

# Parse arguments
SDK_ONLY=false
EMULATOR_ONLY=false
RUN_ONLY=false
CLEAN_BUILD=false
HEADLESS=false
CUSTOM_EMULATOR=""

if [ $# -eq 0 ]; then
  # No arguments provided, run everything
  SDK_ONLY=false
  EMULATOR_ONLY=false
  RUN_ONLY=false
else
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --sdk-only)
        SDK_ONLY=true
        shift
        ;;
      --emulator-only)
        EMULATOR_ONLY=true
        shift
        ;;
      --run-only)
        RUN_ONLY=true
        shift
        ;;
      --clean)
        CLEAN_BUILD=true
        GRADLE_TASKS="clean assembleDebug"
        shift
        ;;
      --headless)
        HEADLESS=true
        shift
        ;;
      --emulator-config)
        if [ -n "$2" ]; then
          CUSTOM_EMULATOR="$2"
          shift 2
        else
          echo "Error: --emulator-config requires an argument."
          exit 1
        fi
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
fi

# Process custom emulator configuration if provided
if [ -n "$CUSTOM_EMULATOR" ]; then
  parse_emulator_config "$CUSTOM_EMULATOR"
fi

# Main execution flow
if [ "$SDK_ONLY" = true ]; then
  setup_android_sdk
  exit 0
fi

if [ "$EMULATOR_ONLY" = true ]; then
  setup_android_sdk
  create_emulator
  exit 0
fi

if [ "$RUN_ONLY" = true ]; then
  start_emulator
  install_and_run
  exit 0
fi

# Default: run everything
setup_android_sdk
create_emulator
start_emulator
build_project
install_and_run

log "Setup and run process completed successfully."

# Keep the script running to maintain emulator
log "Press Ctrl+C to exit and stop the emulator."
while true; do sleep 10; done
