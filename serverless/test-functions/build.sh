#!/bin/bash
# =============================================================================
# Build Script für Test-Functions
# =============================================================================
# Baut alle Test-Funktionen und kopiert die JARs in den jars/ Ordner
# 
# Verwendung:
#   cd serverless/test-functions
#   ./build.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  Building Test Functions"
echo "=========================================="

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Prüfen ob Maven installiert ist
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven ist nicht installiert!${NC}"
    echo "Bitte installiere Maven: https://maven.apache.org/install.html"
    exit 1
fi

# Alte Builds löschen
echo -e "${YELLOW}Lösche alte target Ordner...${NC}"
rm -rf helloF/target reverseF/target sumF/target

# Maven Build
echo -e "${YELLOW}Baue alle Test-Functions...${NC}"
mvn clean package -q

# JARs kopieren
echo -e "${YELLOW}Kopiere JARs nach jars/...${NC}"
mkdir -p jars

cp helloF/target/hello-function.jar jars/
cp reverseF/target/reverse-function.jar jars/
cp sumF/target/sum-function.jar jars/

# Erfolg
echo ""
echo -e "${GREEN}=========================================="
echo "  Build erfolgreich!"
echo "==========================================${NC}"
echo ""
echo "Erstellte JARs:"
ls -lh jars/*.jar
echo ""
echo "Nächste Schritte:"
echo "  1. cd .."
echo "  2. Functions erstellen und JARs hochladen (siehe docs/TESTING.md)"
