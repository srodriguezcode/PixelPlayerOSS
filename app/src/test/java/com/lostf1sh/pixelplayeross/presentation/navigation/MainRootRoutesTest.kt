package com.lostf1sh.pixelplayeross.presentation.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MainRootRoutesTest {
    @Test
    fun `nested screen belongs to the nearest tab below it`() {
        val backStack = listOf(null, Screen.Home.route, Screen.Library.route, "settings", "accounts", "navidrome_dashboard")

        assertEquals(Screen.Library.route, currentTabRoute(backStack))
    }

    @Test
    fun `start tab under another tab does not own the stack`() {
        val backStack = listOf(null, Screen.Library.route, Screen.Home.route, "album_detail/{albumId}")

        assertEquals(Screen.Home.route, currentTabRoute(backStack))
    }

    @Test
    fun `tab root owns itself`() {
        assertEquals(Screen.Search.route, currentTabRoute(listOf(null, Screen.Home.route, Screen.Search.route)))
    }

    @Test
    fun `stack without a tab has no owner`() {
        assertNull(currentTabRoute(listOf(null, "setup")))
        assertNull(currentTabRoute(emptyList()))
    }

    @Test
    fun `tap on another tab switches tab`() {
        assertEquals(MainTabTap.SwitchTab, resolveMainTabTap(Screen.Library.route, Screen.Home.route, Screen.Home.route))
    }

    @Test
    fun `tap on the owning tab from a nested screen pops to the tab root`() {
        assertEquals(MainTabTap.PopToTabRoot, resolveMainTabTap(Screen.Library.route, "navidrome_dashboard", Screen.Library.route))
    }

    @Test
    fun `tap on the tab already at its root does nothing`() {
        assertEquals(MainTabTap.None, resolveMainTabTap(Screen.Library.route, Screen.Library.route, Screen.Library.route))
    }

    @Test
    fun `tap with no owning tab switches tab`() {
        assertEquals(MainTabTap.SwitchTab, resolveMainTabTap(Screen.Home.route, "setup", null))
    }
}
