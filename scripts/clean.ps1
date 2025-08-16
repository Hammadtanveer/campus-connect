# Clean generated and cache files for this project (Windows PowerShell)
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File scripts/clean.ps1

$ErrorActionPreference = 'SilentlyContinue'

function Remove-IfExists($path) {
  if (Test-Path $path) {
    Write-Host "Removing $path"
    Remove-Item -Recurse -Force $path
  }
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
Set-Location $root

# IDE/Gradle caches
Remove-IfExists ".gradle"
Remove-IfExists ".kotlin"
Remove-IfExists ".idea"
Remove-IfExists ".externalNativeBuild"
Remove-IfExists ".cxx"
Remove-IfExists "captures"

# Module build folders
Get-ChildItem -Path . -Directory -Recurse -Filter build | ForEach-Object {
  try { Remove-IfExists $_.FullName } catch { }
}

# Specific common module path just in case
Remove-IfExists "app/build"

# NOTE: Do not delete source assets (e.g., drawables) here.
# Previously this script removed some drawable files which could revert UI/icon changes.
# If you ever need to prune truly-unused resources, use Android Studio's "Refactor > Remove Unused Resources"
# or create a separate opt-in script/flag for that.

Write-Host "Cleanup complete. If Android Studio is open, close it and re-run if some folders persist." -ForegroundColor Green
