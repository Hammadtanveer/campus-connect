const cloudinary = require('cloudinary').v2;
const fs = require('fs');
const https = require('https');
const crypto = require('crypto');

cloudinary.config({
  cloud_name: 'dkxunmucg',
  api_key: '492784632542267',
  api_secret: '3CSXo-IjIxXX6qy-CTo-9bBSunU'
});

function checkUrl(url, description) {
    return new Promise((resolve) => {
        const req = https.get(url, (res) => {
            console.log(`[${res.statusCode}] ${description}`);
            if (res.statusCode === 200) {
                console.log(`    SUCCESS URL: ${url}`);
            }
            resolve(res.statusCode);
        });

        req.on('error', (e) => {
            console.error(`[ERR] ${description}: ${e.message}`);
            resolve(0);
        });
    });
}

async function testFix() {
    try {
        // Create a dummy PDF
        const filename = 'test_fix_401.pdf';
        fs.writeFileSync(filename, '%PDF-1.0\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]>>endobj\nxref\n0 4\n0000000000 65535 f\n0000000010 00000 n\n0000000060 00000 n\n0000000117 00000 n\ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n149\n%%EOF');

        console.log("--- Uploading File ---");
        const uploadResult = await cloudinary.uploader.upload(filename, {
            resource_type: "auto",
            type: "authenticated",
            access_mode: "authenticated",
            public_id: "debug_fix_" + Date.now()
        });

        console.log("Upload successful.");
        console.log("  Public ID:", uploadResult.public_id);
        console.log("  Resource Type:", uploadResult.resource_type);
        console.log("  Version:", uploadResult.version);

        const publicId = uploadResult.public_id;
        const version = uploadResult.version;
        const resourceType = uploadResult.resource_type; // likely 'image'

        // Strategy 1: Standard SDK (what failed before)
        const url1 = cloudinary.url(publicId, {
            resource_type: resourceType,
            type: "authenticated",
            sign_url: true,
            version: version
        });
        await checkUrl(url1, "Strategy 1: Standard SDK (Implicit format)");

        // Strategy 2: Explicit format 'pdf'
        const url2 = cloudinary.url(publicId, {
            resource_type: resourceType,
            type: "authenticated",
            sign_url: true,
            version: version,
            format: 'pdf'
        });
        await checkUrl(url2, "Strategy 2: Explicit format 'pdf'");

        // Strategy 3: No version
        const url3 = cloudinary.url(publicId, {
            resource_type: resourceType,
            type: "authenticated",
            sign_url: true,
            format: 'pdf'
        });
        await checkUrl(url3, "Strategy 3: No version, explicit format");

        // Strategy 4: Manual Signature Construction
        // Signature = hash( "s--<signature>--/v<version>/<public_id>.<format>" ) ? No.
        // For authenticated URLs, the signature is part of the path: /s--SIGNATURE--/
        // The signature is first 8 chars of base64(SHA1(path_after_signature + secret))

        // Let's try to construct the path we want: /v<version>/<public_id>.pdf
        const toSign = `v${version}/${publicId}.pdf`;
        const hash = crypto.createHash('sha1').update(toSign + cloudinary.config().api_secret).digest('binary');
        const base64Hash = Buffer.from(hash, 'binary').toString('base64').replace(/\+/g, '-').replace(/\//g, '_').substring(0, 8);

        const manualUrl = `https://res.cloudinary.com/${cloudinary.config().cloud_name}/${resourceType}/authenticated/s--${base64Hash}--/${toSign}`;
        await checkUrl(manualUrl, "Strategy 4: Manual Signature (v+pid+ext)");

        // Strategy 5: Private Download URL (for 'raw' or 'image' acting as file)
        // Sometimes 'private_download_url' is used for authenticated assets
        try {
             const url5 = cloudinary.utils.private_download_url(publicId, 'pdf', {
                resource_type: resourceType,
                type: "authenticated",
                attachment: true
             });
             await checkUrl(url5, "Strategy 5: Private Download URL (Attachment)");

             const url5b = cloudinary.utils.private_download_url(publicId, 'pdf', {
                resource_type: resourceType,
                type: "authenticated",
                attachment: false
             });
             await checkUrl(url5b, "Strategy 5b: Private Download URL (No Attachment)");

        } catch (e) {
            console.log("Strategy 5 failed to generate: " + e.message);
        }

        // Strategy 6: Resource Type 'raw' (if it was uploaded as image, this might fail, but let's see if we can access it as raw)
        // Usually you can't change resource_type in URL if uploaded as image.

        // Strategy 7: Upload as 'raw' instead?
        // If the user wants to store PDFs, 'raw' might be safer to avoid image processing issues.
        console.log("\n--- Attempting Upload as RAW ---");
        const uploadRaw = await cloudinary.uploader.upload(filename, {
            resource_type: "raw",
            type: "authenticated",
            access_mode: "authenticated",
            public_id: "debug_fix_raw_" + Date.now()
        });

        const rawUrl = cloudinary.url(uploadRaw.public_id, {
            resource_type: "raw",
            type: "authenticated",
            sign_url: true,
            version: uploadRaw.version
        });
        await checkUrl(rawUrl, "Strategy 7: Upload as RAW + Signed URL");


    } catch (error) {
        console.error("Error:", error);
    }
}

testFix();
