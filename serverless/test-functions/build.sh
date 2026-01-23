#!/bin/bash
# Build script for test functions
# Usage: ./build.sh [all|hello|sum|reverse]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/jars"

# Create output directory
mkdir -p "$OUTPUT_DIR"

build_function() {
    local func_dir=$1
    local func_name=$2
    
    echo "🔨 Building $func_name..."
    cd "$SCRIPT_DIR/$func_dir"
    mvn clean package -q -DskipTests
    cp target/*.jar "$OUTPUT_DIR/"
    echo "✅ $func_name built successfully"
}

build_all() {
    echo "🚀 Building all test functions..."
    cd "$SCRIPT_DIR"
    mvn clean package -q -DskipTests
    
    # Copy all JARs to output directory
    find . -name "*.jar" -path "*/target/*" -exec cp {} "$OUTPUT_DIR/" \;
    
    echo ""
    echo "✅ All functions built! JARs available in: $OUTPUT_DIR"
    ls -la "$OUTPUT_DIR"
}

case "${1:-all}" in
    hello)
        build_function "helloF" "HelloFunction"
        ;;
    sum)
        build_function "sumF" "SumFunction"
        ;;
    reverse)
        build_function "reverseF" "ReverseFunction"
        ;;
    all)
        build_all
        ;;
    *)
        echo "Usage: $0 [all|hello|sum|reverse]"
        exit 1
        ;;
esac
