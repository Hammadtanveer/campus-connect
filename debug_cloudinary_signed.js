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
        https.get(url, (res) => {
            console.log(`URL Check: ${url} -> Status: ${res.statusCode}`);
            resolve();
        }).on('error', (e) => {
            console.error(`URL Check Error: ${e.message}`);
            resolve();
        });
    });
}

async function testSignedUrl() {
    try {
        // Minimal valid PDF
        fs.writeFileSync('test_signed.pdf', '%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000117 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n149\n%%EOF');

        console.log("--- Uploading PDF ---");
        const result = await cloudinary.uploader.upload("test_signed.pdf", {
            resource_type: "image",
            type: "upload",
            public_id: "debug_pdf_signed_" + Date.now()
        });

        console.log("Public URL (Expect 401):", result.secure_url);
        await checkUrl(result.secure_url);

        console.log("\n--- Generating Signed URL ---");
        // Generate signed URL
        const signedUrl = cloudinary.url(result.public_id, {
            resource_type: "image",
            type: "upload",
            sign_url: true,
            version: result.version,
            format: "pdf"
        });

        console.log("Signed URL:", signedUrl);
        await checkUrl(signedUrl);

    } catch (error) {
        console.error("General Error:", error);
    }
}

testSignedUrl();

