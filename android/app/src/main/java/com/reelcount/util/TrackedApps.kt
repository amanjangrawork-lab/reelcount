package com.reelcount.util

/**
 * The single native source of truth for the four supported apps: package name,
 * display label and the reel container view ids used by the accessibility
 * service. `src/native/ReelCount.ts` mirrors only the package names and labels
 * for the UI; add new view ids here and nowhere else.
 */
object TrackedApps {

    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"
    const val FACEBOOK = "com.facebook.katana"
    const val SNAPCHAT = "com.snapchat.android"

    val ALL: List<String> = listOf(INSTAGRAM, YOUTUBE, FACEBOOK, SNAPCHAT)

    /**
     * Reel container view ids per package. Apps rename these without warning,
     * which is the one failure mode that silently stops counting; see
     * `README.md` ("Known limitations").
     */
    val VIEW_IDS: Map<String, List<String>> = mapOf(
        INSTAGRAM to listOf("clips_viewer_view_pager", "reel_player_page_container"),
        YOUTUBE to listOf("reel_player_page_container", "shorts_player_container"),
        FACEBOOK to listOf("reel_player_page_container", "clips_viewer_view_pager"),
        SNAPCHAT to listOf("reel_player_page_container")
    )

    fun displayName(pkg: String): String = when (pkg) {
        INSTAGRAM -> "Instagram"
        YOUTUBE -> "YouTube"
        FACEBOOK -> "Facebook"
        SNAPCHAT -> "Snapchat"
        else -> pkg
    }

    fun viewIds(pkg: String): List<String> = VIEW_IDS[pkg] ?: emptyList()

    fun isTracked(pkg: String?): Boolean = pkg != null && ALL.contains(pkg)
}
