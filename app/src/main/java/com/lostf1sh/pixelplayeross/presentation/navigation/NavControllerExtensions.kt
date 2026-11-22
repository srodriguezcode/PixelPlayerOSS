package com.lostf1sh.pixelplayeross.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private fun NavController.isReadyForNavigation(): Boolean {
    return runCatching {
        currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
    }.getOrDefault(false)
}

fun NavController.navigateSafely(route: String): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
    }
    return true
}

fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = true
        builder()
    }
    return true
}

fun NavController.navigateSafelyReplacing(
    route: String,
    patternToPop: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    if (!isReadyForNavigation()) return false
    navigate(route) {
        launchSingleTop = false
        popUpTo(patternToPop) {
            inclusive = true
        }
        builder()
    }
    return true
}

fun NavController.navigateToTopLevelSafely(route: String): Boolean {
    val startDestinationId = runCatching { graph.startDestinationId }.getOrNull() ?: return false
    navigate(route) {
        popUpTo(startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    return true
}

/** False when nothing changed, so a Search double tap does not fire on a failed switch. */
internal fun NavController.selectMainTab(tap: MainTabTap, tabRoute: String): Boolean = when (tap) {
    MainTabTap.SwitchTab -> navigateToTopLevelSafely(tabRoute)
    MainTabTap.PopToTabRoot -> {
        popBackStack(tabRoute, inclusive = false)
        /** A second tap can land before recomposition, with the tab already at its root. */
        currentDestination?.route == tabRoute
    }
    MainTabTap.None -> false
}

/** `currentBackStack` is `@RestrictTo`, but no public API exposes the whole stack. */
@SuppressLint("RestrictedApi")
@Composable
internal fun NavController.currentTabRouteAsState(): State<String?> {
    val backStack = currentBackStack
    val tabRoute = remember(backStack) {
        backStack.map(::currentTabRouteOf).distinctUntilChanged()
    }
    val initialTabRoute = remember(backStack) { currentTabRouteOf(backStack.value) }
    return tabRoute.collectAsStateWithLifecycle(initialTabRoute)
}

private fun currentTabRouteOf(backStack: List<NavBackStackEntry>): String? =
    currentTabRoute(backStack.map { it.destination.route })
