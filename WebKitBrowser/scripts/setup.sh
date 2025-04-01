#!/bin/zsh

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "${YELLOW}Setting up WebKitBrowser development environment...${NC}"

# Check and install Homebrew if needed
if ! command -v brew &> /dev/null; then
    echo "${YELLOW}Installing Homebrew...${NC}"
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Install required tools
echo "${YELLOW}Installing required tools...${NC}"
brew install xcodegen
brew install fastlane
brew install swiftlint

# Check Xcode CLI tools
if ! command -v xcode-select &> /dev/null; then
    echo "${YELLOW}Installing Xcode Command Line Tools...${NC}"
    xcode-select --install
fi

# Create project directories if they don't exist
echo "${YELLOW}Creating project structure...${NC}"
mkdir -p Sources Tests Resources

# Generate Xcode project
echo "${YELLOW}Generating Xcode project...${NC}"
xcodegen generate

# Initialize Fastlane if not already initialized
if [ ! -d "fastlane" ]; then
    echo "${YELLOW}Initializing Fastlane...${NC}"
    fastlane init
fi

# Run SwiftLint
echo "${YELLOW}Running SwiftLint...${NC}"
swiftlint

# Build project
echo "${YELLOW}Building project...${NC}"
xcodebuild -scheme WebKitBrowser -destination "platform=iOS Simulator,name=iPhone 14" build

# Run tests
echo "${YELLOW}Running tests...${NC}"
fastlane test

echo "${GREEN}Setup complete!${NC}"
echo "${YELLOW}Next steps:${NC}"
echo "1. Update fastlane/Appfile with your Apple ID"
echo "2. Configure your development team in project.yml"
echo "3. Run 'fastlane build_dev' to build the development version"
echo "4. Run 'fastlane test' to run the test suite"
