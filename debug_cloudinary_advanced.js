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

async function testAdvanced() {
    try {
        // Minimal valid PDF
        fs.writeFileSync('test_adv.pdf', '%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000117 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n149\n%%EOF');

        console.log("--- Test 1: Public Upload + Signed URL + Transformation ---");
        const res1 = await cloudinary.uploader.upload("test_adv.pdf", {
            resource_type: "image",
            type: "upload",
            public_id: "debug_pdf_trans_" + Date.now()
        });

        const signedUrl1 = cloudinary.url(res1.public_id, {
            resource_type: "image",
            type: "upload",
            sign_url: true,
            version: res1.version,
            format: "pdf",
            flags: "attachment" // Force download
        });
        console.log("Signed URL 1:", signedUrl1);
        await checkUrl(signedUrl1);


        console.log("\n--- Test 2: Authenticated Upload + Signed URL ---");
        const res2 = await cloudinary.uploader.upload("test_adv.pdf", {
            resource_type: "image",
            type: "authenticated", // Explicitly authenticated
            public_id: "debug_pdf_auth_" + Date.now()
        });

        const signedUrl2 = cloudinary.url(res2.public_id, {
            resource_type: "image",
            type: "authenticated",
            sign_url: true,
            version: res2.version,
            format: "pdf"
        });
        console.log("Signed URL 2:", signedUrl2);
        await checkUrl(signedUrl2);

    } catch (error) {
        console.error("General Error:", error);
    }
}

testAdvanced();

