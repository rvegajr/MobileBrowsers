#!/bin/bash

# ===================================================================
# MobileBrowsers Build Verification Script
# ===================================================================
# This script checks for build tools and attempts to compile
# both Android and iOS projects to verify their build status
# ===================================================================

# Text formatting
BOLD="\033[1m"
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
BLUE="\033[34m"
RESET="\033[0m"
CHECK="✅"
CROSS="❌"
WARNING="⚠️"
INFO="ℹ️"

# Project paths
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
ANDROID_PROJECT="$PROJECT_ROOT/WebViewBrowser"
IOS_PROJECT="$PROJECT_ROOT/WebKitBrowser"

# Log functions
log_title() {
    echo -e "\n${BOLD}$1${RESET}"
    echo "=============================================="
}

log_success() {
    echo -e "${GREEN}${CHECK} $1${RESET}"
}

log_error() {
    echo -e "${RED}${CROSS} $1${RESET}"
}

log_warning() {
    echo -e "${YELLOW}${WARNING} $1${RESET}"
}

log_info() {
    echo -e "${BLUE}${INFO} $1${RESET}"
}

# Environment check function
check_command() {
    which $1 > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        log_success "Found $1: $(which $1)"
        return 0
    else
        log_error "Missing $1"
        return 1
    fi
}

check_file() {
    if [ -f "$1" ]; then
        log_success "Found $1"
        return 0
    else
        log_error "Missing $1"
        return 1
    fi
}

check_directory() {
    if [ -d "$1" ]; then
        log_success "Found $1"
        return 0
    else
        log_error "Missing $1"
        return 1
    fi
}

# Main script
echo -e "${BOLD}MobileBrowsers Build Verification Script${RESET}"
echo "==============================================="
echo "Started at: $(date)"
echo "Project root: $PROJECT_ROOT"
echo

# Check if we're on macOS
if [ "$(uname)" != "Darwin" ]; then
    log_error "This script must be run on macOS to verify both Android and iOS builds"
    exit 1
fi

# Check for Android SDK and build tools
log_title "Checking Android Environment"

# Check for Android Studio
ANDROID_STUDIO_APP="/Applications/Android Studio.app"
if [ -d "$ANDROID_STUDIO_APP" ]; then
    log_success "Found Android Studio at $ANDROID_STUDIO_APP"
    ANDROID_STUDIO_VERSION=$(defaults read "$ANDROID_STUDIO_APP/Contents/Info.plist" CFBundleShortVersionString 2>/dev/null || echo "unknown")
    log_info "Android Studio Version: $ANDROID_STUDIO_VERSION"
else
    log_error "Android Studio not found at default location"
fi

# Check common Android SDK locations
ANDROID_SDK_LOCATIONS=(
    "$HOME/Library/Android/sdk"
    "$HOME/Android/Sdk"
)

ANDROID_SDK=""
for loc in "${ANDROID_SDK_LOCATIONS[@]}"; do
    if [ -d "$loc" ]; then
        ANDROID_SDK="$loc"
        log_success "Found Android SDK at $ANDROID_SDK"
        break
    fi
done

if [ -z "$ANDROID_SDK" ]; then
    log_error "Android SDK not found in common locations"
    log_info "Attempting to get path from environment variables..."
    
    if [ ! -z "$ANDROID_HOME" ]; then
        ANDROID_SDK="$ANDROID_HOME"
        log_success "Found Android SDK from ANDROID_HOME: $ANDROID_SDK"
    elif [ ! -z "$ANDROID_SDK_ROOT" ]; then
        ANDROID_SDK="$ANDROID_SDK_ROOT"
        log_success "Found Android SDK from ANDROID_SDK_ROOT: $ANDROID_SDK"
    else
        log_error "Android SDK environment variables not found"
    fi
fi

# Check for Gradle
check_command gradle

# Check Android project
log_info "Verifying Android project structure"
check_directory "$ANDROID_PROJECT"
check_file "$ANDROID_PROJECT/build.gradle"
check_file "$ANDROID_PROJECT/settings.gradle"

# ===================================================================
# Check for iOS/Xcode build tools
log_title "Checking iOS Environment"

# Check for Xcode
XCODE_APP="/Applications/Xcode.app"
if [ -d "$XCODE_APP" ]; then
    log_success "Found Xcode at $XCODE_APP"
    XCODE_VERSION=$(defaults read "$XCODE_APP/Contents/Info.plist" CFBundleShortVersionString 2>/dev/null || echo "unknown")
    log_info "Xcode Version: $XCODE_VERSION"
else
    log_error "Xcode not found at default location"
fi

# Check for xcodebuild
check_command xcodebuild

# Check for CocoaPods
check_command pod

# Check for xcodegen
check_command xcodegen

# Check iOS project
log_info "Verifying iOS project structure"
check_directory "$IOS_PROJECT"
check_file "$IOS_PROJECT/project.yml"

# ===================================================================
# Attempt to build Android project
log_title "Building Android Project"

cd "$ANDROID_PROJECT"
if [ $? -ne 0 ]; then
    log_error "Failed to change directory to Android project"
    exit 1
fi

log_info "Running Gradle build (this might take a while)..."
# Use './gradlew' instead of the system gradle to ensure compatibility
if [ -f "./gradlew" ]; then
    BUILD_COMMAND="./gradlew clean assembleDebug"
else
    BUILD_COMMAND="gradle clean assembleDebug"
fi

# Execute the build with environment variables to ensure proper JDK usage
JAVA_HOME=$(/usr/libexec/java_home -v 11 2>/dev/null || /usr/libexec/java_home 2>/dev/null) 
log_info "Using JAVA_HOME: $JAVA_HOME"

JAVA_HOME="$JAVA_HOME" $BUILD_COMMAND

if [ $? -eq 0 ]; then
    ANDROID_APK="$ANDROID_PROJECT/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$ANDROID_APK" ]; then
        log_success "Android build completed successfully!"
        log_info "APK location: $ANDROID_APK"
        ANDROID_BUILD_SUCCESS=true
    else
        log_warning "Build command succeeded but APK not found"
        ANDROID_BUILD_SUCCESS=false
    fi
else
    log_error "Android build failed"
    ANDROID_BUILD_SUCCESS=false
fi

# ===================================================================
# Attempt to build iOS project
log_title "Building iOS Project"

cd "$IOS_PROJECT"
if [ $? -ne 0 ]; then
    log_error "Failed to change directory to iOS project"
    exit 1
fi

# Check if project.yml needs to be regenerated
if [ -f "$IOS_PROJECT/project.yml" ]; then
    log_info "Running xcodegen to generate project..."
    xcodegen generate
    
    if [ $? -ne 0 ]; then
        log_error "Failed to generate Xcode project from project.yml"
    fi
fi

# Check for Podfile.lock and run pod install if needed
if [ -f "$IOS_PROJECT/Podfile" ]; then
    if [ ! -f "$IOS_PROJECT/Podfile.lock" ] || [ "$IOS_PROJECT/Podfile" -nt "$IOS_PROJECT/Podfile.lock" ]; then
        log_info "Running pod install to update dependencies..."
        pod install
        
        if [ $? -ne 0 ]; then
            log_error "pod install failed"
        fi
    else
        log_success "CocoaPods dependencies up to date"
    fi
fi

# Find available iOS simulators
log_info "Finding available iOS simulators..."
AVAILABLE_SIMULATORS=$(xcrun simctl list devices available -j | grep -o '"name" : "[^"]*"' | grep -o '[^"]*$' | grep -v "^name$")
FIRST_SIMULATOR=$(echo "$AVAILABLE_SIMULATORS" | head -n 1)

if [ -z "$FIRST_SIMULATOR" ]; then
    log_warning "No iOS simulators found, using generic destination"
    DESTINATION="generic/platform=iOS"
else
    log_info "Using simulator: $FIRST_SIMULATOR"
    DESTINATION="platform=iOS Simulator,name=$FIRST_SIMULATOR"
fi

# Determine Xcode workspace or project to build
XCODE_WORKSPACE="$IOS_PROJECT/WebKitBrowser.xcworkspace"
XCODE_PROJECT="$IOS_PROJECT/WebKitBrowser.xcodeproj"

if [ -d "$XCODE_WORKSPACE" ]; then
    log_info "Building using workspace: $XCODE_WORKSPACE"
    BUILD_COMMAND="xcodebuild -workspace WebKitBrowser.xcworkspace -scheme WebKitBrowser -configuration Debug -sdk iphonesimulator -destination '$DESTINATION' CODE_SIGN_IDENTITY='' CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO clean build"
elif [ -d "$XCODE_PROJECT" ]; then
    log_info "Building using project: $XCODE_PROJECT"
    BUILD_COMMAND="xcodebuild -project WebKitBrowser.xcodeproj -scheme WebKitBrowser -configuration Debug -sdk iphonesimulator -destination '$DESTINATION' CODE_SIGN_IDENTITY='' CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO clean build"
else
    log_error "Neither Xcode workspace nor project found"
    exit 1
fi

log_info "Running Xcode build (this might take a while)..."
log_info "Build command: $BUILD_COMMAND"
eval $BUILD_COMMAND

if [ $? -eq 0 ]; then
    log_success "iOS build completed successfully!"
    IOS_BUILD_SUCCESS=true
else
    log_error "iOS build failed"
    IOS_BUILD_SUCCESS=false
fi

# ===================================================================
# Summary
log_title "Build Verification Summary"

echo "Android: $([ "$ANDROID_BUILD_SUCCESS" = true ] && echo "${GREEN}SUCCESS${RESET}" || echo "${RED}FAILED${RESET}")"
echo "iOS:     $([ "$IOS_BUILD_SUCCESS" = true ] && echo "${GREEN}SUCCESS${RESET}" || echo "${RED}FAILED${RESET}")"

echo
echo "Verification completed at: $(date)"

# Return overall success
if [ "$ANDROID_BUILD_SUCCESS" = true ] && [ "$IOS_BUILD_SUCCESS" = true ]; then
    log_success "All builds successful!"
    exit 0
else
    log_error "One or more builds failed. See details above."
    exit 1
fi
