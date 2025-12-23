const cloudinary = require('cloudinary').v2;
const fs = require('fs');
const https = require('https');

cloudinary.config({
  cloud_name: 'dkxunmucg',
  api_key: '492784632542267',
  api_secret: '3CSXo-IjIxXX6qy-CTo-9bBSunU'
});

function checkUrl(url) {
    return new Promise((resolve) => {
        const req = https.get(url, (res) => {
            console.log(`URL Check: ${url} -> Status: ${res.statusCode}`);
            resolve(res.statusCode);
        });

        req.on('error', (e) => {
            console.error(`URL Check Error: ${e.message}`);
            resolve(0);
        });
    });
}

async function testFinalFix() {
    try {
        // Create a dummy PDF
        fs.writeFileSync('test_final.pdf', '%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000117 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n149\n%%EOF');

        console.log("--- Testing Authenticated Upload + Signed URL ---");

        // 1. Upload as authenticated
        const uploadResult = await cloudinary.uploader.upload("test_final.pdf", {
            resource_type: "auto",
            type: "authenticated",
            access_mode: "authenticated",
            public_id: "debug_final_" + Date.now()
        });

        console.log("Upload Result:");
        console.log("  Public ID:", uploadResult.public_id);
        console.log("  Resource Type:", uploadResult.resource_type);
        console.log("  Type:", uploadResult.type);
        console.log("  Version:", uploadResult.version);
        console.log("  Secure URL (Unsigned - Expect 401):", uploadResult.secure_url);

        // Check unsigned URL (Should fail)
        const statusUnsigned = await checkUrl(uploadResult.secure_url);
        if (statusUnsigned === 401) {
            console.log("  [OK] Unsigned URL is restricted (401) as expected.");
        } else {
            console.log(`  [WARN] Unsigned URL returned ${statusUnsigned}.`);
        }

        // 2. Generate Signed URL
        // We found that standard delivery URLs (res.cloudinary.com) are returning 401 for this account/setup.
        // However, the API Download URL (api.cloudinary.com) works correctly for authenticated resources.

        // Generate API Download URL
        const signedUrl = cloudinary.utils.private_download_url(uploadResult.public_id, 'pdf', {
            resource_type: uploadResult.resource_type,
            type: "authenticated",
            attachment: false, // Set to false to allow viewing in browser/app if supported
            expires_at: Math.floor(Date.now() / 1000) + 3600 // 1 hour validity
        });

        console.log("\nGenerated Signed URL (API Download):", signedUrl);

        // 3. Check Signed URL (Should succeed)
        const statusSigned = await checkUrl(signedUrl);
        if (statusSigned === 200) {
            console.log("  [SUCCESS] Signed URL is accessible (200 OK).");
        } else {
            console.error(`  [FAIL] Signed URL returned ${statusSigned}.`);
        }

    } catch (error) {
        console.error("General Error:", error);
    }
}

testFinalFix();
