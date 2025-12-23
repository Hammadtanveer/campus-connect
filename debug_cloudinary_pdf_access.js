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

async function testPdfAccess() {
    try {
        // Minimal valid PDF
        fs.writeFileSync('test_access.pdf', '%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000117 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n149\n%%EOF');

        console.log("--- Test 1: Image Type, Explicit Public ---");
        try {
            const res1 = await cloudinary.uploader.upload("test_access.pdf", {
                resource_type: "image",
                type: "upload",
                access_mode: "public",
                public_id: "debug_pdf_public_" + Date.now()
            });
            console.log("Upload 1 Success:", res1.secure_url);
            await checkUrl(res1.secure_url);
        } catch (e) {
            console.error("Upload 1 Failed:", e.message);
        }

        console.log("\n--- Test 2: Raw Type, Explicit Public ---");
        try {
            const res2 = await cloudinary.uploader.upload("test_access.pdf", {
                resource_type: "raw",
                type: "upload",
                access_mode: "public",
                public_id: "debug_pdf_raw_public_" + Date.now()
            });
            console.log("Upload 2 Success:", res2.secure_url);
            await checkUrl(res2.secure_url);
        } catch (e) {
            console.error("Upload 2 Failed:", e.message);
        }

        console.log("\n--- Test 3: Raw Type, .bin extension ---");
        try {
            // Rename to .bin
            fs.copyFileSync('test_access.pdf', 'test_access.bin');
            const res3 = await cloudinary.uploader.upload("test_access.bin", {
                resource_type: "raw",
                type: "upload",
                public_id: "debug_bin_raw_" + Date.now()
            });
            console.log("Upload 3 Success:", res3.secure_url);
            await checkUrl(res3.secure_url);
        } catch (e) {
            console.error("Upload 3 Failed:", e.message);
        }

    } catch (error) {
        console.error("General Error:", error);
    }
}

testPdfAccess();

