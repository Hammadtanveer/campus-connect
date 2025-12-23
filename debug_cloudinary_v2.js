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

async function testUploads() {
    try {
        // Create dummy files
        fs.writeFileSync('test.txt', 'Hello World');
        // Minimal valid PDF
        fs.writeFileSync('test.pdf', '%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000117 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n149\n%%EOF');

        console.log("--- Testing RAW upload ---");
        try {
            const rawResult = await cloudinary.uploader.upload("test.txt", {
                resource_type: "raw",
                public_id: "debug_raw_" + Date.now(),
                type: "upload"
            });
            console.log("RAW Upload Success:", rawResult.secure_url);
            await checkUrl(rawResult.secure_url);
        } catch (e) {
            console.error("RAW Upload Failed:", e.message);
        }

        console.log("\n--- Testing IMAGE upload (PDF) ---");
        try {
            const imgResult = await cloudinary.uploader.upload("test.pdf", {
                resource_type: "image",
                public_id: "debug_pdf_img_" + Date.now(),
                type: "upload"
            });
            console.log("IMAGE (PDF) Upload Success:", imgResult.secure_url);
            await checkUrl(imgResult.secure_url);
        } catch (e) {
            console.error("IMAGE (PDF) Upload Failed:", e.message);
        }

    } catch (error) {
        console.error("General Error:", error);
    }
}

testUploads();

