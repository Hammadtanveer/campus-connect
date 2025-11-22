# Firebase Service Account Key Setup Helper
# Run this script to get guided help for setting up the service account key

Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "  Firebase Service Account Key Setup" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

# Check if the file exists
$keyPath = "D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json"

if (Test-Path $keyPath) {
    Write-Host "`n✅ Service account key found at:" -ForegroundColor Green
    Write-Host "   $keyPath" -ForegroundColor White
    Write-Host "`nYou can now run:" -ForegroundColor Yellow
    Write-Host '   $env:GOOGLE_APPLICATION_CREDENTIALS="' + $keyPath + '"' -ForegroundColor Gray
    Write-Host "   node scripts/listUsers.js`n" -ForegroundColor Gray
} else {
    Write-Host "`n❌ Service account key NOT found!" -ForegroundColor Red
    Write-Host "`nExpected location:" -ForegroundColor Yellow
    Write-Host "   $keyPath" -ForegroundColor White

    Write-Host "`n📥 TO DOWNLOAD THE KEY:" -ForegroundColor Cyan
    Write-Host "=" * 50 -ForegroundColor Cyan

    Write-Host "`n1. Open your web browser and go to:" -ForegroundColor Yellow
    Write-Host "   https://console.firebase.google.com/" -ForegroundColor White

    Write-Host "`n2. Select your 'CampusConnect' project" -ForegroundColor Yellow

    Write-Host "`n3. Click the ⚙️ gear icon (top left) and select:" -ForegroundColor Yellow
    Write-Host "   'Project Settings'" -ForegroundColor White

    Write-Host "`n4. Go to the 'Service Accounts' tab" -ForegroundColor Yellow

    Write-Host "`n5. Click the button:" -ForegroundColor Yellow
    Write-Host "   'Generate New Private Key'" -ForegroundColor White

    Write-Host "`n6. In the dialog that appears, click:" -ForegroundColor Yellow
    Write-Host "   'Generate Key'" -ForegroundColor White

    Write-Host "`n7. A JSON file will download. RENAME it to:" -ForegroundColor Yellow
    Write-Host "   serviceAccountKey.json" -ForegroundColor White

    Write-Host "`n8. MOVE the file to this location:" -ForegroundColor Yellow
    Write-Host "   $keyPath" -ForegroundColor White

    Write-Host "`n⚠️  SECURITY WARNING:" -ForegroundColor Red
    Write-Host "   • This file contains SECRET credentials" -ForegroundColor White
    Write-Host "   • NEVER share it or commit it to Git" -ForegroundColor White
    Write-Host "   • It's already added to .gitignore for safety" -ForegroundColor White

    Write-Host "`n🔄 After downloading and placing the file, run this script again:" -ForegroundColor Yellow
    Write-Host "   .\scripts\check-service-key.ps1`n" -ForegroundColor Gray

    # Offer to open the browser
    $openBrowser = Read-Host "`nWould you like to open Firebase Console now? (yes/no)"
    if ($openBrowser -eq "yes" -or $openBrowser -eq "y") {
        Start-Process "https://console.firebase.google.com/"
        Write-Host "`n✅ Browser opened. Follow the steps above to download the key.`n" -ForegroundColor Green
    } else {
        Write-Host "`n👉 Manually open: https://console.firebase.google.com/`n" -ForegroundColor Yellow
    }
}

Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "  Quick Reference" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "`nFile should be placed at:" -ForegroundColor Yellow
Write-Host "   D:\AndroidStudioProjects\CampusConnect\serviceAccountKey.json" -ForegroundColor White
Write-Host "`nFile should be named exactly:" -ForegroundColor Yellow
Write-Host "   serviceAccountKey.json" -ForegroundColor White
Write-Host "`n(The file downloaded from Firebase will have a longer name like" -ForegroundColor Gray
Write-Host " 'campusconnect-xyz123-firebase-adminsdk-abc.json' - you need to rename it)`n" -ForegroundColor Gray

