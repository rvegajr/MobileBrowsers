#!/bin/zsh

# Run Emulator Script for WebViewBrowser Android
# This script uses the installed Android Studio tools to run an emulator

# Source zshrc for environment variables
source ~/.zshrc

# Configuration
ANDROID_STUDIO_DIR="/Applications/Android Studio.app"
ANDROID_HOME="$HOME/Library/Android/sdk"
EMULATOR_NAME="Pixel_7_API_33"
DEVICE_ID="pixel_7"
API_LEVEL="33"

# Log function
log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Parse command line arguments
HEADLESS=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --headless)
      HEADLESS=true
      shift
      ;;
    *)
      shift
      ;;
  esac
done

# Create the emulator if it doesn't exist
create_emulator() {
  log "Checking if emulator exists..."
  
  # Set up paths to Android tools
  export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"
  
  # Check if the emulator already exists
  if "$ANDROID_HOME/emulator/emulator" -list-avds | grep -q "$EMULATOR_NAME"; then
    log "Emulator $EMULATOR_NAME already exists."
    return 0
  fi
  
  log "Creating new emulator: $EMULATOR_NAME"
  
  # Create the AVD using avdmanager
  echo "no" | "$ANDROID_HOME/tools/bin/avdmanager" create avd \
    --name "$EMULATOR_NAME" \
    --package "system-images;android-$API_LEVEL;google_apis;x86_64" \
    --device "$DEVICE_ID" \
    --force
    
  log "Emulator created successfully."
}

# Start the emulator
start_emulator() {
  log "Starting emulator: $EMULATOR_NAME"
  
  # Set environment variables
  export ANDROID_HOME="$ANDROID_HOME"
  export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
  
  # Start the emulator
  if [ "$HEADLESS" = true ]; then
    "$ANDROID_HOME/emulator/emulator" -avd "$EMULATOR_NAME" -no-window -no-boot-anim -no-audio -gpu swiftshader_indirect &
  else
    "$ANDROID_HOME/emulator/emulator" -avd "$EMULATOR_NAME" -gpu swiftshader_indirect &
  fi
  
  # Wait for emulator to boot
  log "Waiting for emulator to boot..."
  while ! adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; do
    sleep 2
  done
  
  log "Emulator is ready."
}

# Install and launch the app
install_and_launch() {
  log "Building and installing the app..."
  
  # Set up paths
  export ANDROID_HOME="$ANDROID_HOME"
  export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"
  
  # Assume gradlew is in the project root
  PROJECT_ROOT="$(dirname "$(dirname "$0")")"
  cd "$PROJECT_ROOT"
  
  # Create gradle wrapper if needed
  if [ ! -f "./gradlew" ]; then
    log "Creating gradle wrapper..."
    "$ANDROID_STUDIO_DIR/Contents/gradle/gradle-7.6/bin/gradle" wrapper
  fi
  
  # Build the project
  ./gradlew assembleDebug
  
  # Install the app
  adb install -r "app/build/outputs/apk/debug/app-debug.apk"
  
  # Launch the app
  adb shell am start -n "com.noctusoft.webviewbrowser/.BrowserActivity"
  
  log "App launched successfully!"
}

# Main execution
log "Starting Android emulator setup..."

# Create the emulator
create_emulator

# Start the emulator
start_emulator

# Install and launch the app
install_and_launch

log "Process completed successfully."

# Keep the script running
if [ "$HEADLESS" = false ]; then
  log "Press Ctrl+C to exit and stop the emulator."
  while true; do sleep 10; done
fi
