#!/bin/bash

# Cache Verification Test Script
# This script helps verify the caching configuration locally

echo "================================="
echo "FluffyGram Cache Verification"
echo "================================="
echo ""

# Check if we're in the right directory
if [ ! -f "build.gradle" ]; then
    echo "❌ Error: Must be run from the repository root"
    exit 1
fi

echo "✅ Repository structure verified"
echo ""

# Check workflow file exists
WORKFLOW_FILE=".github/workflows/release.yml"
if [ -f "$WORKFLOW_FILE" ]; then
    echo "✅ Workflow file found: $WORKFLOW_FILE"
else
    echo "❌ Workflow file not found"
    exit 1
fi

echo ""
echo "=== Checking Cache Configuration ==="
echo ""

# Check for Gradle cache configuration
if grep -q "cache: 'gradle'" "$WORKFLOW_FILE"; then
    echo "✅ Gradle cache: CONFIGURED"
else
    echo "❌ Gradle cache: NOT CONFIGURED"
fi

# Check for NDK cache
if grep -q "Cache NDK" "$WORKFLOW_FILE"; then
    echo "✅ NDK cache: CONFIGURED"
    if grep -q "id: cache-ndk" "$WORKFLOW_FILE"; then
        echo "  ✅ Cache tracking enabled"
    else
        echo "  ⚠️  Cache tracking not enabled"
    fi
else
    echo "❌ NDK cache: NOT CONFIGURED"
fi

# Check for ccache
if grep -q "ccache-action" "$WORKFLOW_FILE"; then
    echo "✅ ccache: CONFIGURED"
else
    echo "❌ ccache: NOT CONFIGURED"
fi

# Check for CMake cache
if grep -q "Cache CMake build" "$WORKFLOW_FILE"; then
    echo "✅ CMake cache: CONFIGURED"
    if grep -q "id: cache-cmake" "$WORKFLOW_FILE"; then
        echo "  ✅ Cache tracking enabled"
    else
        echo "  ⚠️  Cache tracking not enabled"
    fi
else
    echo "❌ CMake cache: NOT CONFIGURED"
fi

echo ""
echo "=== Checking Cache Verification Steps ==="
echo ""

# Check for cache verification step
if grep -q "Verify Cache Status" "$WORKFLOW_FILE"; then
    echo "✅ Cache verification step: PRESENT"
else
    echo "⚠️  Cache verification step: MISSING"
fi

# Check for Gradle cache check
if grep -q "Check Gradle Cache" "$WORKFLOW_FILE"; then
    echo "✅ Gradle cache check: PRESENT"
else
    echo "⚠️  Gradle cache check: MISSING"
fi

# Check for ccache statistics
if grep -q "Show ccache statistics" "$WORKFLOW_FILE"; then
    echo "✅ ccache statistics: PRESENT"
else
    echo "⚠️  ccache statistics: MISSING"
fi

echo ""
echo "=== Checking Documentation ==="
echo ""

# Check for documentation
CACHE_DOC="docs/CACHING.md"
if [ -f "$CACHE_DOC" ]; then
    echo "✅ Cache documentation found: $CACHE_DOC"
    
    # Count sections in documentation
    SECTIONS=$(grep -c "^##" "$CACHE_DOC" || true)
    echo "  📄 Documentation sections: $SECTIONS"
else
    echo "⚠️  Cache documentation not found"
fi

echo ""
echo "=== Validation Summary ==="
echo ""

# Count all checks
TOTAL_CHECKS=7
PASSED_CHECKS=0

grep -q "cache: 'gradle'" "$WORKFLOW_FILE" && ((PASSED_CHECKS++))
grep -q "Cache NDK" "$WORKFLOW_FILE" && ((PASSED_CHECKS++))
grep -q "ccache-action" "$WORKFLOW_FILE" && ((PASSED_CHECKS++))
grep -q "Cache CMake build" "$WORKFLOW_FILE" && ((PASSED_CHECKS++))
grep -q "Verify Cache Status" "$WORKFLOW_FILE" && ((PASSED_CHECKS++))
grep -q "Check Gradle Cache" "$WORKFLOW_FILE" && ((PASSED_CHECKS++))
grep -q "Show ccache statistics" "$WORKFLOW_FILE" && ((PASSED_CHECKS++))

echo "Checks passed: $PASSED_CHECKS / $TOTAL_CHECKS"

if [ $PASSED_CHECKS -eq $TOTAL_CHECKS ]; then
    echo ""
    echo "🎉 All cache configurations are in place!"
    echo ""
    echo "Next steps:"
    echo "1. Push changes to GitHub"
    echo "2. Trigger workflow manually via workflow_dispatch"
    echo "3. Check workflow logs for cache verification reports"
    echo "4. Run workflow again to verify cache is being used"
    exit 0
else
    echo ""
    echo "⚠️  Some cache configurations may be missing"
    echo "Please review the checks above"
    exit 1
fi
