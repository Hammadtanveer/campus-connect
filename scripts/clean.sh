# Clean generated and cache files for this project
# Usage: bash scripts/clean.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Cleaning Gradle and IDE caches..."
rm -rf .gradle .kotlin .idea .externalNativeBuild .cxx captures || true

# Remove root/module build directories
find . -type d -name build -prune -exec rm -rf {} + || true

# Android Studio may lock files; warn if something persists
if [ -d "app/build" ]; then
  echo "Warning: app/build still exists. Close Android Studio and retry."
fi

# Delete unreferenced drawable assets
for f in \
  app/src/main/res/drawable/notes.png \
  app/src/main/res/drawable/ic_subscribe.xml \
  app/src/main/res/drawable/outline_album_24.xml \
  app/src/main/res/drawable/outline_genres_24.xml \
  app/src/main/res/drawable/outline_library_music_24.xml \
  app/src/main/res/drawable/outline_mic_24.xml \
  app/src/main/res/drawable/outline_music_note_24.xml \
  app/src/main/res/drawable/outline_play_circle_24.xml \
  app/src/main/res/drawable/sharp_playlist_play_24.xml \
  app/src/main/res/drawable/ic_account.xml
do
  if [ -f "$f" ]; then
    echo "Removing $f"
    rm -f "$f"
  fi
done

echo "Cleanup complete."
