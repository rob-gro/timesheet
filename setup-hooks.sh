#!/bin/bash
# Setup git hooks for this repository
# Run once after cloning: ./setup-hooks.sh

echo "🔧 Setting up git hooks..."

# Configure git to use .githooks directory
git config core.hooksPath .githooks

# Make hooks executable (Windows Git Bash compatible)
chmod +x .githooks/*

echo "✅ Git hooks configured successfully!"
echo ""
echo "Commit messages will now be validated against COMMIT_RULES.md"
echo ""
