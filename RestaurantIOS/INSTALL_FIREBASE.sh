#!/bin/bash

# Firebase Installation Script for RestaurantIOS
# This script installs Firebase via CocoaPods

echo "🔥 Installing Firebase for RestaurantIOS..."

# Check if CocoaPods is installed
if ! command -v pod &> /dev/null; then
    echo "❌ CocoaPods is not installed."
    echo "📦 Installing CocoaPods..."
    sudo gem install cocoapods
fi

# Navigate to project directory
cd "$(dirname "$0")"

# Install pods
echo "📦 Installing Firebase pods..."
pod install

if [ $? -eq 0 ]; then
    echo "✅ Firebase installed successfully!"
    echo ""
    echo "⚠️  IMPORTANT: From now on, open RestaurantIOS.xcworkspace (not .xcodeproj)"
    echo "   Open: RestaurantIOS.xcworkspace"
else
    echo "❌ Installation failed. Please check the errors above."
    exit 1
fi

