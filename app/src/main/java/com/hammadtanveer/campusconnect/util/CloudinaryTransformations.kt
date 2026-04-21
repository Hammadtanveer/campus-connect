package com.hammadtanveer.campusconnect.util

/**
 * Utility for Cloudinary image transformations and optimizations.
 */
object CloudinaryTransformations {

    private val cloudName: String
        get() = CloudinaryConfig.getCloudName()

    private fun imageUploadBaseUrl(): String = "https://res.cloudinary.com/$cloudName/image/upload"

    private fun rawUploadBaseUrl(): String = "https://res.cloudinary.com/$cloudName/raw/upload"

    /**
     * Get thumbnail URL with optimizations.
     *
     * @param publicId Cloudinary public ID
     * @param width Desired width (default 300px)
     * @param quality Quality level (default auto)
     * @param format Format (default auto - WebP when supported)
     */
    fun getThumbnailUrl(
        publicId: String,
        width: Int = 300,
        quality: String = "auto",
        format: String = "auto"
    ): String {
        return "${imageUploadBaseUrl()}/" +
                "c_fill,w_$width,q_$quality,f_$format/$publicId"
    }

    /**
     * Get optimized image URL for list views.
     *
     * Uses aggressive compression and WebP format.
     */
    fun getListImageUrl(publicId: String): String {
        return getThumbnailUrl(
            publicId = publicId,
            width = 200,
            quality = "auto:low",
            format = "auto"
        )
    }

    /**
     * Get optimized image URL for detail views.
     *
     * Higher quality, still optimized.
     */
    fun getDetailImageUrl(publicId: String): String {
        return getThumbnailUrl(
            publicId = publicId,
            width = 800,
            quality = "auto:good",
            format = "auto"
        )
    }

    /**
     * Get avatar URL with circular crop.
     */
    fun getAvatarUrl(
        publicId: String,
        size: Int = 128
    ): String {
        return "${imageUploadBaseUrl()}/" +
                "c_fill,g_face,w_$size,h_$size,r_max,q_auto,f_auto/$publicId"
    }

    /**
     * Get PDF thumbnail (first page preview).
     */
    fun getPdfThumbnail(
        publicId: String,
        width: Int = 200
    ): String {
        return "${imageUploadBaseUrl()}/" +
                "c_fit,w_$width,pg_1,q_auto,f_auto/$publicId.jpg"
    }

    /**
     * Get direct raw file URL (for PDFs and other raw assets).
     */
    fun getRawFileUrl(publicId: String): String {
        return "${rawUploadBaseUrl()}/$publicId"
    }

    /**
     * Get optimized raw PDF URL with filename flag support.
     */
    fun getPdfFileUrl(publicId: String, fileName: String? = null): String {
        val attachmentSegment = fileName
            ?.takeIf { it.isNotBlank() }
            ?.let { "fl_attachment:${it.replace(" ", "_")}/" }
            .orEmpty()
        return "${rawUploadBaseUrl()}/${attachmentSegment}$publicId"
    }

    /**
     * Get responsive image URL with multiple sizes.
     *
     * Returns srcset string for responsive images.
     */
    fun getResponsiveSrcSet(publicId: String): String {
        return buildString {
            append(getThumbnailUrl(publicId, 400), " 400w, ")
            append(getThumbnailUrl(publicId, 800), " 800w, ")
            append(getThumbnailUrl(publicId, 1200), " 1200w")
        }
    }

    /**
     * Get image with specific transformations.
     */
    fun getTransformedUrl(
        publicId: String,
        transformations: String
    ): String {
        return "${imageUploadBaseUrl()}/" +
                "$transformations/$publicId"
    }

    /**
     * Check if public ID is valid.
     */
    fun isValidPublicId(publicId: String?): Boolean {
        return !publicId.isNullOrBlank() && publicId != "default" && publicId != "placeholder"
    }
}

