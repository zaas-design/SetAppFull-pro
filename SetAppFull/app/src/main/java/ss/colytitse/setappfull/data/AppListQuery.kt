package ss.colytitse.setappfull.data

import ss.colytitse.setappfull.ui.AppListAccess

/** A failed query never masquerades as a successful empty application list. */
internal data class AppListQueryResult<T>(
    val access: AppListAccess,
    val apps: List<T>? = null,
    val message: String,
)

internal fun <T> queryAppListAccess(
    hasPermission: () -> Boolean,
    query: () -> List<T>,
): AppListQueryResult<T> = try {
    if (!hasPermission()) {
        AppListQueryResult(AppListAccess.DENIED,
            message = "App-list access has not been granted. Check the related permission in system settings and try again.")
    } else {
        val apps = query()
        if (apps.isEmpty()) {
            // Even the querying application itself should be visible on stock Android.
            AppListQueryResult(AppListAccess.ERROR,
                message = "The system returned no apps. Check app-list access in system settings and try again.")
        } else {
            AppListQueryResult(AppListAccess.READY, apps,
                "The apps allowed by the system are available.")
        }
    }
} catch (_: SecurityException) {
    AppListQueryResult(AppListAccess.DENIED,
        message = "The system denied app-list access. Check the related permission in system settings and try again.")
} catch (failure: Exception) {
    AppListQueryResult(AppListAccess.ERROR,
        message = "Could not read the app list (${failure.javaClass.simpleName}). Try again later.")
}
