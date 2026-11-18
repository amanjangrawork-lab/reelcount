package com.reelcount.util

/** The four apps ReelCount understands, and how they are labelled in the UI. */
object TrackedApps {

    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val FACEBOOK = "com.facebook.katana"
    const val SNAPCHAT = "com.snapchat.android"

    val ALL: List<String> = listOf(INSTAGRAM, YOUTUBE, FACEBOOK, SNAPCHAT)

    fun displayName(pkg: String): String = when (pkg) {
        INSTAGRAM -> "Instagram"
        YOUTUBE -> "YouTube"
        FACEBOOK -> "Facebook"
        SNAPCHAT -> "Snapchat"
        else -> pkg
    }

    fun isTracked(pkg: String?): Boolean = pkg != null && ALL.contains(pkg)
}
