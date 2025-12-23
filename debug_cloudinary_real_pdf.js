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

async function testRealPdf() {
    try {
        console.log("--- Testing Real PDF Upload ---");
        const res = await cloudinary.uploader.upload("sample.pdf", {
            resource_type: "image",
            type: "upload",
            public_id: "debug_real_pdf_" + Date.now()
        });

        console.log("Public URL:", res.secure_url);
        await checkUrl(res.secure_url);

        const signedUrl = cloudinary.url(res.public_id, {
            resource_type: "image",
            type: "upload",
            sign_url: true,
            version: res.version,
            format: "pdf"
        });
        console.log("Signed URL:", signedUrl);
        await checkUrl(signedUrl);

    } catch (error) {
        console.error("General Error:", error);
    }
}

testRealPdf();

