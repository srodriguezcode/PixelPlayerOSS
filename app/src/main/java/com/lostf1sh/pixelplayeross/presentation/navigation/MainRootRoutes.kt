package com.lostf1sh.pixelplayeross.presentation.navigation

internal fun isMainRootRoute(route: String?): Boolean = when (route) {
    Screen.Home.route,
    Screen.Search.route,
    Screen.Library.route -> true
    else -> false
}

internal fun mainRootRouteIndex(route: String?): Int? = when (route) {
    Screen.Home.route -> 0
    Screen.Search.route -> 1
    Screen.Library.route -> 2
    else -> null
}

/** The tab whose stack is on screen: the nearest tab root below the top of [backStackRoutes]. */
internal fun currentTabRoute(backStackRoutes: List<String?>): String? =
    backStackRoutes.lastOrNull(::isMainRootRoute)

internal enum class MainTabTap { SwitchTab, PopToTabRoot, None }

internal fun resolveMainTabTap(tabRoute: String, currentRoute: String?, currentTabRoute: String?): MainTabTap = when {
    currentTabRoute != tabRoute -> MainTabTap.SwitchTab
    currentRoute == tabRoute -> MainTabTap.None
    else -> MainTabTap.PopToTabRoot
}
