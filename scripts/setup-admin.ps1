# Admin Setup Helper Script for PowerShell
# This script helps you set up admin access step-by-step

Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "  CampusConnect Admin Setup Helper" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

# Step 1: Check if service account key is set
Write-Host "`nStep 1: Checking service account key..." -ForegroundColor Yellow

if (-not $env:GOOGLE_APPLICATION_CREDENTIALS) {
    Write-Host "❌ GOOGLE_APPLICATION_CREDENTIALS not set!" -ForegroundColor Red
    Write-Host "`nPlease set it with your service account key path:" -ForegroundColor White
    Write-Host '  $env:GOOGLE_APPLICATION_CREDENTIALS="D:\path\to\serviceAccountKey.json"' -ForegroundColor Gray
    Write-Host "`nThen run this script again: .\scripts\setup-admin.ps1`n" -ForegroundColor White
    exit 1
}

Write-Host "✅ Environment variable set: $env:GOOGLE_APPLICATION_CREDENTIALS" -ForegroundColor Green

# Check if file exists
if (-not (Test-Path $env:GOOGLE_APPLICATION_CREDENTIALS)) {
    Write-Host "❌ Service account key file not found at:" -ForegroundColor Red
    Write-Host "   $env:GOOGLE_APPLICATION_CREDENTIALS" -ForegroundColor Red
    Write-Host "`nPlease download it from Firebase Console and update the path.`n" -ForegroundColor White
    exit 1
}

Write-Host "✅ Service account key file found" -ForegroundColor Green

# Step 2: Check if firebase-admin is installed
Write-Host "`nStep 2: Checking firebase-admin installation..." -ForegroundColor Yellow

$npmList = npm list firebase-admin 2>&1 | Out-String
if ($npmList -like "*firebase-admin@*") {
    Write-Host "✅ firebase-admin is installed" -ForegroundColor Green
} else {
    Write-Host "❌ firebase-admin not found. Installing..." -ForegroundColor Yellow
    npm install firebase-admin
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ firebase-admin installed successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ Failed to install firebase-admin" -ForegroundColor Red
        exit 1
    }
}

# Step 3: List users
Write-Host "`nStep 3: Fetching Firebase users..." -ForegroundColor Yellow
Write-Host "Running: node scripts/listUsers.js`n" -ForegroundColor Gray

node scripts/listUsers.js

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n❌ Failed to list users. Please check your service account key." -ForegroundColor Red
    exit 1
}

# Step 4: Prompt for UID
Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "  Set Admin Claims" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

Write-Host "`nCopy the UID from the list above and paste it below." -ForegroundColor Yellow
$uid = Read-Host "`nEnter User UID"

if (-not $uid -or $uid.Length -lt 10) {
    Write-Host "❌ Invalid UID. Please run this script again and enter a valid UID." -ForegroundColor Red
    exit 1
}

# Step 5: Choose permissions
Write-Host "`nSelect permissions to grant:" -ForegroundColor Yellow
Write-Host "1. Full Admin (admin + all permissions)" -ForegroundColor White
Write-Host "2. Event Manager (event:create)" -ForegroundColor White
Write-Host "3. Notes Manager (notes:upload)" -ForegroundColor White
Write-Host "4. Custom (you choose)" -ForegroundColor White

$choice = Read-Host "`nEnter choice (1-4)"

switch ($choice) {
    "1" {
        $permissions = "admin event:create notes:upload senior:update society:manage"
        Write-Host "`n✅ Will grant: Full Admin Access" -ForegroundColor Green
    }
    "2" {
        $permissions = "event:create"
        Write-Host "`n✅ Will grant: Event Manager" -ForegroundColor Green
    }
    "3" {
        $permissions = "notes:upload"
        Write-Host "`n✅ Will grant: Notes Manager" -ForegroundColor Green
    }
    "4" {
        Write-Host "`nAvailable permissions: admin, event:create, notes:upload, senior:update, society:manage" -ForegroundColor Gray
        $permissions = Read-Host "Enter permissions (space-separated)"
        Write-Host "`n✅ Will grant: $permissions" -ForegroundColor Green
    }
    default {
        Write-Host "❌ Invalid choice. Exiting." -ForegroundColor Red
        exit 1
    }
}

# Step 6: Confirm
Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "  Confirmation" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "User UID:    $uid" -ForegroundColor White
Write-Host "Permissions: $permissions" -ForegroundColor White
$confirm = Read-Host "`nProceed? (yes/no)"

if ($confirm -ne "yes" -and $confirm -ne "y") {
    Write-Host "❌ Cancelled." -ForegroundColor Yellow
    exit 0
}

# Step 7: Set claims
Write-Host "`nSetting custom claims..." -ForegroundColor Yellow

$command = "node scripts/setCustomClaims.js `"$uid`" $permissions"
Write-Host "Running: $command`n" -ForegroundColor Gray

Invoke-Expression $command

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=====================================" -ForegroundColor Green
    Write-Host "  ✅ SUCCESS!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "`nAdmin claims set successfully!" -ForegroundColor Green
    Write-Host "`n⚠️  IMPORTANT: The user must:" -ForegroundColor Yellow
    Write-Host "   1. Sign out of the app" -ForegroundColor White
    Write-Host "   2. Sign back in" -ForegroundColor White
    Write-Host "   3. Go to Profile → Open Admin Panel`n" -ForegroundColor White
} else {
    Write-Host "`n❌ Failed to set claims. Check the error above." -ForegroundColor Red
    exit 1
}

