/*
 * Copyright (C) 2024 z-huang/InnerTune
 * Copyright (C) 2025 O​u​t​er​Tu​ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.nocturne.player

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.util.fastForEach
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.navigation.navArgument
import androidx.window.core.layout.WindowWidthSizeClass
import com.nocturne.player.BuildConfig
import com.nocturne.player.constants.AppBarHeight
import com.nocturne.player.constants.DEFAULT_ENABLED_TABS
import com.nocturne.player.constants.DarkMode
import com.nocturne.player.constants.DarkModeKey
import com.nocturne.player.constants.DefaultOpenTabKey
import com.nocturne.player.constants.DynamicThemeKey
import com.nocturne.player.constants.EnabledTabsKey
import com.nocturne.player.constants.HighContrastKey
import com.nocturne.player.constants.LibraryFilterKey
import com.nocturne.player.constants.MinMiniPlayerHeight
import com.nocturne.player.constants.MiniPlayerHeight
import com.nocturne.player.constants.NavigationBarAnimationSpec
import com.nocturne.player.constants.NavigationBarHeight
import com.nocturne.player.constants.OOBE_VERSION
import com.nocturne.player.constants.OobeStatusKey
import com.nocturne.player.constants.PureBlackKey
import com.nocturne.player.constants.SlimNavBarKey
import com.nocturne.player.db.MusicDatabase
import com.nocturne.player.extensions.tabMode
import com.nocturne.player.playback.DownloadUtil
import com.nocturne.player.playback.MediaControllerViewModel
import com.nocturne.player.playback.MusicService
import com.nocturne.player.playback.PlayerConnection
import com.nocturne.player.ui.component.rememberBottomSheetState
import com.nocturne.player.ui.component.BottomSheetState
import com.nocturne.player.ui.component.shimmer.ShimmerTheme
import com.nocturne.player.ui.menu.BottomSheetMenu
import com.nocturne.player.ui.menu.MenuState
import com.nocturne.player.ui.player.BottomSheetPlayer
import com.nocturne.player.ui.screens.AlbumScreen
import com.nocturne.player.ui.screens.HistoryScreen
import com.nocturne.player.ui.screens.HomeScreen
import com.nocturne.player.ui.screens.PlayerScreen
import com.nocturne.player.ui.screens.Screens
import com.nocturne.player.ui.screens.SetupWizard
import com.nocturne.player.ui.screens.StatsScreen
import com.nocturne.player.ui.screens.artist.ArtistAlbumsScreen
import com.nocturne.player.ui.screens.artist.ArtistScreen
import com.nocturne.player.ui.screens.artist.ArtistSongsScreen
import com.nocturne.player.ui.screens.library.FolderScreen
import com.nocturne.player.ui.screens.library.LibraryAlbumsScreen
import com.nocturne.player.ui.screens.library.LibraryArtistsScreen
import com.nocturne.player.ui.screens.library.LibraryFoldersScreen
import com.nocturne.player.ui.screens.library.LibraryPlaylistsScreen
import com.nocturne.player.ui.screens.library.LibraryScreen
import com.nocturne.player.ui.screens.library.LibrarySongsScreen
import com.nocturne.player.ui.screens.playlist.AutoPlaylistScreen
import com.nocturne.player.ui.screens.playlist.LocalPlaylistScreen
import com.nocturne.player.ui.screens.search.SearchBarContainer
import com.nocturne.player.ui.screens.settings.AboutScreen
import com.nocturne.player.ui.screens.settings.AppearanceSettings
import com.nocturne.player.ui.screens.settings.AttributionScreen
import com.nocturne.player.ui.screens.settings.BackupAndRestore
import com.nocturne.player.ui.screens.settings.CustomSourceManagerScreen
import com.nocturne.player.ui.screens.settings.ExperimentalSettings
import com.nocturne.player.ui.screens.settings.InterfaceSettings
import com.nocturne.player.ui.screens.settings.LibrariesScreen
import com.nocturne.player.ui.screens.settings.LibrarySettings
import com.nocturne.player.ui.screens.settings.LocalPlayerSettings
import com.nocturne.player.ui.screens.settings.RemoteSourceSettings
import com.nocturne.player.ui.screens.library.VocalSeparationScreen
import com.nocturne.player.ui.screens.library.RankScreen
import com.nocturne.player.ui.screens.settings.LyricsSettings
import com.nocturne.player.ui.screens.settings.PlayerSettings
import com.nocturne.player.ui.screens.settings.SettingsScreen
import com.nocturne.player.ui.screens.settings.StorageSettings
import com.nocturne.player.ui.theme.NocturneTheme
import com.nocturne.player.ui.utils.appBarScrollBehavior
import com.nocturne.player.utils.ActivityLauncherHelper
import com.nocturne.player.utils.lmScannerCoroutine
import com.nocturne.player.utils.rememberEnumPreference
import com.nocturne.player.utils.rememberPreference
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    val MAIN_TAG = "MainOtActivity"

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    lateinit var activityLauncher: ActivityLauncherHelper

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    // Activity-level back-handling bridge. The composition assigns these refs;
    // the global OnBackPressedCallback below (registered in onCreate, lowest
    // priority) reads them so no back event can fall through to the system
    // default (which finishes the task silently).
    private var navControllerRef: NavController? = null
    private var playerSheetStateRef: BottomSheetState? = null
    var exitDialogShown by mutableStateOf(false)

    val controllerViewModel: MediaControllerViewModel by viewModels()

    // storage permission helpers
    val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
//                Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.scanner_missing_storage_perm), Toast.LENGTH_SHORT).show()
            }
        }

    /** Unified back handling for all three back sources (navbar key, gesture, dispatcher). */
    private fun handleBackPress() {
        if (BuildConfig.DEBUG) {
            // Debug-only breadcrumb: if the user presses back and lands on the
            // launcher WITHOUT seeing this toast, the back event never reached
            // the app (system-level interception).
            Toast.makeText(this, "返回事件已进入App拦截链", Toast.LENGTH_SHORT).show()
        }
        if (exitDialogShown) {
            finish()
            return
        }
        val nc = navControllerRef
        if (nc == null) return
        val sheet = playerSheetStateRef
        if (sheet != null && !sheet.isCollapsed && !sheet.isDismissed) {
            sheet.collapseSoft()
            return
        }
        if (nc.previousBackStackEntry == null) {
            exitDialogShown = true
        } else if (!nc.navigateUp()) {
            exitDialogShown = true
        }
    }

    /**
     * Physical-layer interception for the hardware/navbar BACK key. We forward
     * it into the normal OnBackPressedDispatcher chain (so Compose BackHandlers
     * like the search bar keep their priority) and consume it, guaranteeing the
     * event can never fall through to the system default (which would finish the
     * task silently).
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        Log.i(MAIN_TAG, "onDestroy() called. isFinishing = $isFinishing")

        // https://github.com/androidx/media/issues/805
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE && (playerConnection?.player?.playWhenReady != true || playerConnection?.player?.mediaItemCount == 0)) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(MusicService.NOTIFICATION_ID)
        }
        lifecycle.removeObserver(controllerViewModel)
        playerConnection = null

        super.onDestroy()
    }

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(controllerViewModel)
        controllerViewModel.addControllerCallback(lifecycle) { controller, _ ->
            playerConnection = PlayerConnection(controllerViewModel, database)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        activityLauncher = ActivityLauncherHelper(this)

        // Global back-press safety net. Registered first (lowest priority) so
        // any back event NOT consumed by Compose BackHandlers (search bar while
        // active, player sheet while expanded, selection modes) lands here and
        // can never fall through to the system default, which would finish the
        // task silently and drop the user to the launcher.
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        // Native gesture-back callback (API 33+). Registered directly on the
        // window's OnBackInvokedDispatcher as a physical-layer fallback: even if
        // the androidx bridge misbehaves, gesture-back events still reach us.
        if (Build.VERSION.SDK_INT >= 33) {
            window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                object : OnBackInvokedCallback {
                    override fun onBackInvoked() {
                        handleBackPress()
                    }
                }
            )
        }

        // Debug-only: startup toast proving the installed build & that the back
        // interception is armed. Lets the user verify they are on the right APK.
        Handler(Looper.getMainLooper()).postDelayed({
            if (BuildConfig.DEBUG) {
                Toast.makeText(
                    this,
                    "OuterTune v${BuildConfig.VERSION_NAME} 返回拦截已启用",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, 900)

        setContent {
            Log.v(MAIN_TAG, "RC-1")
            val coroutineScope = rememberCoroutineScope()
            val haptic = LocalHapticFeedback.current
            val snackbarHostState = remember { SnackbarHostState() }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val (darkTheme, onDarkThemeChange) = rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val highContrastCompat by rememberPreference(HighContrastKey, defaultValue = false)
            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }

            val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
            val tabMode = this@MainActivity.tabMode()
            val useNavRail by remember {
                derivedStateOf {
                    windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED && !tabMode
                }
            }


            val (oobeStatus) = rememberPreference(OobeStatusKey, defaultValue = 0)

            var filter by rememberEnumPreference(LibraryFilterKey, Screens.LibraryFilter.ALL)
            val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
            val (enabledTabs) = rememberPreference(EnabledTabsKey, defaultValue = DEFAULT_ENABLED_TABS)
            val navigationItems = remember {
                Screens.getScreens(enabledTabs)
            }
            val (defaultOpenTab, onDefaultOpenTabChange) = rememberPreference(
                DefaultOpenTabKey,
                defaultValue = Screens.Songs.route
            )




            LaunchedEffect(Unit) {
                // local media & download folders auto scan
                coroutineScope.launch(lmScannerCoroutine) {
                    scanInit(
                        this@MainActivity, database, downloadUtil, coroutineScope, playerConnection,
                        snackbarHostState
                    )
                }
            }


            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }


            NocturneTheme(
                context = this@MainActivity,
                playerConnection = playerConnection,
                enableDynamicTheme = enableDynamicTheme,
                highContrastCompat = highContrastCompat,
                isSystemInDarkTheme = isSystemInDarkTheme,
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
            ) {
                Log.v(MAIN_TAG, "RC-2.1")
                val density = LocalDensity.current
                val windowsInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                val navController = rememberNavController()
                navControllerRef = navController
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                val tabOpenedFromShortcut = remember {
                    // reroute to library page for new layout is handled in NavHost section
                    when (intent?.action) {
                        ACTION_SONGS -> if (navigationItems.contains(Screens.Songs)) Screens.Songs else Screens.Library
                        ACTION_ALBUMS -> if (navigationItems.contains(Screens.Albums)) Screens.Albums else Screens.Library
                        ACTION_PLAYLISTS -> if (navigationItems.contains(Screens.Playlists)) Screens.Playlists else Screens.Library
                        else -> null
                    }
                }
                // setup filters for new layout
                if (tabOpenedFromShortcut != null && navigationItems.contains(Screens.Library)) {
                    filter = when (intent?.action) {
                        ACTION_SONGS -> Screens.LibraryFilter.SONGS
                        ACTION_ALBUMS -> Screens.LibraryFilter.ALBUMS
                        ACTION_PLAYLISTS -> Screens.LibraryFilter.PLAYLISTS
                        ACTION_SEARCH -> {
                            navController.navigate("search")
                            filter
                        } // do change filter for search
                        else -> Screens.LibraryFilter.ALL
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val maxW = maxWidth
                    Log.v(MAIN_TAG, "RC-2.2")

                    fun getNavPadding(): Dp {
                        // 现在用侧边抽屉导航，没有底部导航栏了，不需要留高度
                        return 0.dp
                    }

                    val playerBottomSheetState = rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        collapsedBound = bottomInset + MiniPlayerHeight + 4.dp,
                        expandedBound = maxHeight,
                    )

                    // Back navigation policy:
                    // - player sheet expanded/collapsed -> its own BackHandler collapses it first
                    // - non-root nav destination -> pop back to the root (main) page
                    // - root (main) page -> show "exit?" dialog; pressing back again leaves the app
                    // State lives on the Activity (exitDialogShown) so the global
                    // OnBackPressedCallback can drive it even outside composition.
                    val showExitDialog = exitDialogShown
                    val onExitDialogChange: (Boolean) -> Unit = { exitDialogShown = it }

                    BackHandler(
                        // Always-on safety net: no route/state may fall through to the
                        // default Activity back (which would finish the app silently).
                        // The player sheet and the search bar register their own handlers
                        // later (higher priority) and take over when they are active.
                        enabled = !showExitDialog
                    ) {
                        when {
                            // player sheet expanded (not collapsed, not dismissed) -> collapse it
                            !playerBottomSheetState.isCollapsed && !playerBottomSheetState.isDismissed ->
                                playerBottomSheetState.collapseSoft()
                            // root destination -> ask before exiting
                            navController.previousBackStackEntry == null -> onExitDialogChange(true)
                            // any deeper page -> pop back to the root (main) page
                            else -> {
                                val handled = navController.navigateUp()
                                // Never leave the app silently: if pop-back fails (unexpected
                                // nav state), show the exit confirmation instead of finishing.
                                if (!handled) onExitDialogChange(true)
                            }
                        }
                    }
                    // While the exit dialog is up, a further system back press exits immediately.
                    BackHandler(enabled = showExitDialog) {
                        finish()
                    }

                    if (showExitDialog) {
                        // Use a plain Dialog with back/outside-touch disabled so the system back
                        // press reaches our BackHandler(showExitDialog) -> finish(), i.e. the user
                        // can exit by pressing back again while the dialog is up.
                        Dialog(
                            onDismissRequest = { onExitDialogChange(false) },
                            properties = DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false,
                            )
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "退出软件",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "确定要退出音乐播放器吗？",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { onExitDialogChange(false) }) { Text("取消") }
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(onClick = { finish() }) { Text("退出") }
                                    }
                                }
                            }
                        }
                    }

                    // Main insets for navhost content.
                    val playerAwareWindowInsets =
                        remember(
                            bottomInset,
                            playerBottomSheetState.isDismissed,
                        ) {
                            // TODO: Navbar is shown in all screens except for oobe (which doesn't use these insets). Idk what do to tbh
                            var bottom = bottomInset + if (!useNavRail) NavigationBarHeight else 0.dp

                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight + MinMiniPlayerHeight
                            if (!tabMode) {
                                windowsInsets
                                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                    .add(
                                        WindowInsets(
                                            left = if (!useNavRail) 0.dp else NavigationBarHeight,
                                            top = AppBarHeight,
                                            bottom = bottom
                                        )
                                    )
                            } else {
                                windowsInsets
                                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                                    .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                            }
                        }

                    val scrollBehavior = appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    // TODO: can i use this for external file explorer media player?
//                    DisposableEffect(Unit) {
//                        val listener = Consumer<Intent> { intent ->
//                            val uri =
//                                intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri()
//                                ?: return@Consumer
//                            youtubeNavigator(
//                                this@MainActivity,
//                                navController,
//                                coroutineScope,
//                                playerConnection,
//                                snackbarHostState,
//                                uri
//                            )
//                        }
//
//                        addOnNewIntentListener(listener)
//                        onDispose { removeOnNewIntentListener(listener) }
//                    }

                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val drawerScope = rememberCoroutineScope()

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalMenuState provides MenuState(rememberModalBottomSheetState()),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSnackbarHostState provides snackbarHostState,
                        LocalDrawerOpen provides { drawerScope.launch { drawerState.open() } },
                    ) {

                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet(
                                    drawerContainerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Spacer(Modifier.height(40.dp))

                                    Spacer(Modifier.height(8.dp))

                                    // Theme options (vertical cards: follow system / light / dark)
                                    androidx.compose.material3.Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 3.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = if (darkTheme == DarkMode.AUTO) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        onClick = { onDarkThemeChange(DarkMode.AUTO) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Android, null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text("跟随系统")
                                        }
                                    }
                                    androidx.compose.material3.Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 3.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = if (darkTheme == DarkMode.OFF) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        onClick = { onDarkThemeChange(DarkMode.OFF) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.LightMode, null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text("浅色")
                                        }
                                    }
                                    androidx.compose.material3.Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 3.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = if (darkTheme == DarkMode.ON) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        onClick = { onDarkThemeChange(DarkMode.ON) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.DarkMode, null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text("深色")
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    androidx.compose.material3.Card(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column {
                                            DrawerItem("歌曲", Icons.Rounded.MusicNote, Color(0xFF4CAF50),
                                                navBackStackEntry?.destination?.route == Screens.Songs.route) {
                                                navController.navigate(Screens.Songs.route) { popUpTo(0) }
                                                drawerScope.launch { drawerState.close() }
                                            }
                                            DrawerItem("专辑", Icons.Rounded.Album, Color(0xFFE53935),
                                                navBackStackEntry?.destination?.route == Screens.Albums.route) {
                                                navController.navigate(Screens.Albums.route) { popUpTo(0) }
                                                drawerScope.launch { drawerState.close() }
                                            }
                                            DrawerItem("艺术家", Icons.Rounded.Person, Color(0xFFFFB300),
                                                navBackStackEntry?.destination?.route == Screens.Artists.route) {
                                                navController.navigate(Screens.Artists.route) { popUpTo(0) }
                                                drawerScope.launch { drawerState.close() }
                                            }
                                            DrawerItem("文件夹", Icons.Rounded.Folder, Color(0xFF7E57C2),
                                                navBackStackEntry?.destination?.route == Screens.Folders.route) {
                                                navController.navigate(Screens.Folders.route) { popUpTo(0) }
                                                drawerScope.launch { drawerState.close() }
                                            }
                                            DrawerItem("歌单", Icons.Rounded.QueueMusic, Color(0xFF1E88E5),
                                                navBackStackEntry?.destination?.route == Screens.Playlists.route) {
                                                navController.navigate(Screens.Playlists.route) { popUpTo(0) }
                                                drawerScope.launch { drawerState.close() }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    androidx.compose.material3.Card(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column {
                                            DrawerItem("扫描音乐", Icons.Rounded.Refresh, Color(0xFF7E57C2), false) {
                                                drawerScope.launch { drawerState.close() }
                                                coroutineScope.launch(lmScannerCoroutine) {
                                                    scanInit(
                                                        this@MainActivity, database, downloadUtil, coroutineScope, playerConnection,
                                                        snackbarHostState
                                                    )
                                                }
                                            }
                                            DrawerItem("统计", Icons.Rounded.Info, Color(0xFFE53935),
                                                navBackStackEntry?.destination?.route == "stats") {
                                                navController.navigate("stats")
                                                drawerScope.launch { drawerState.close() }
                                            }
                                            DrawerItem("设置", Icons.Rounded.Settings, Color(0xFF4CAF50),
                                                navBackStackEntry?.destination?.route == "settings") {
                                                navController.navigate("settings")
                                                drawerScope.launch { drawerState.close() }
                                            }
                                            DrawerItem("关于", Icons.Rounded.Info, Color(0xFF1E88E5), false) {
                                                navController.navigate("settings/about")
                                                drawerScope.launch { drawerState.close() }
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Log.v(MAIN_TAG, "RC-3")


                            val navHost: @Composable() (() -> Unit) = @Composable {
                                NavHost(
                                    navController = navController,
                                    startDestination = (Screens.getAllScreens()
                                        .find { it.route == defaultOpenTab })?.route
                                        ?: Screens.Songs.route,
                                    enterTransition = {
                                        fadeIn(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + slideInHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) { it / 14 }
                                    },
                                    exitTransition = {
                                        fadeOut(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { -it / 14 }
                                    },
                                    popEnterTransition = {
                                        fadeIn(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + slideInHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) { -it / 14 }
                                    },
                                    popExitTransition = {
                                        fadeOut(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + slideOutHorizontally(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { it / 14 }
                                    },
                                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                                )
                                {
                                    composable(Screens.Home.route) {
                                        HomeScreen(navController)
                                    }
                                    composable(Screens.Songs.route) {
                                        LibrarySongsScreen(navController)
                                    }
                                    composable(Screens.Folders.route) {
                                        LibraryFoldersScreen(navController, scrollBehavior)
                                    }
                                    composable(
                                        route = "${Screens.Folders.route}/{path}",
                                        arguments = listOf(
                                            navArgument("path") {
                                                type = NavType.StringType
                                            }
                                        )
                                    ) {
                                        FolderScreen(navController, scrollBehavior)
                                    }
                                    composable(Screens.Artists.route) {
                                        LibraryArtistsScreen(navController)
                                    }
                                    composable(Screens.Albums.route) {
                                        LibraryAlbumsScreen(navController)
                                    }
                                    composable(Screens.Playlists.route) {
                                        LibraryPlaylistsScreen(navController)
                                    }
                                    composable(Screens.Library.route) {
                                        LibraryScreen(navController, scrollBehavior)
                                    }
                                    composable(Screens.Player.route) {
                                        PlayerScreen(navController, bottomPadding = getNavPadding())
                                    }
                                    composable("history") {
                                        HistoryScreen(navController)
                                    }
                                    composable("stats") {
                                        StatsScreen(navController)
                                    }

                                    composable(
                                        route = "search",
                                    ) {
                                        SearchBarContainer(navController, scrollBehavior)
                                    }
                                    composable(
                                        route = "album/{albumId}",
                                        arguments = listOf(
                                            navArgument("albumId") {
                                                type = NavType.StringType
                                            },
                                        )
                                    ) {
                                        AlbumScreen(navController, scrollBehavior)
                                    }
                                    composable(
                                        route = "artist/{artistId}",
                                        arguments = listOf(
                                            navArgument("artistId") {
                                                type = NavType.StringType
                                            }
                                        )
                                    ) {
                                        ArtistScreen(navController, scrollBehavior)
                                    }
                                    composable(
                                        route = "artist/{artistId}/songs",
                                        arguments = listOf(
                                            navArgument("artistId") {
                                                type = NavType.StringType
                                            }
                                        )
                                    ) {
                                        ArtistSongsScreen(navController, scrollBehavior)
                                    }
                                    composable(
                                        route = "artist/{artistId}/albums",
                                        arguments = listOf(
                                            navArgument("artistId") {
                                                type = NavType.StringType
                                            }
                                        )
                                    ) {
                                        ArtistAlbumsScreen(navController, scrollBehavior)
                                    }
                                    composable(
                                        route = "local_playlist/{playlistId}",
                                        arguments = listOf(
                                            navArgument("playlistId") {
                                                type = NavType.StringType
                                            }
                                        )
                                    ) {
                                        LocalPlaylistScreen(navController, scrollBehavior)
                                    }
                                    composable(
                                        route = "auto_playlist/{playlistId}",
                                        arguments = listOf(
                                            navArgument("playlistId") {
                                                type = NavType.StringType
                                            }
                                        )
                                    ) {
                                        AutoPlaylistScreen(navController, scrollBehavior)
                                    }
                                    composable("settings") {
                                        SettingsScreen(navController, scrollBehavior)
                                    }
                                    composable("settings/appearance") {
                                        AppearanceSettings(navController, scrollBehavior)
                                    }
                                    composable("settings/interface") {
                                        InterfaceSettings(navController, scrollBehavior)
                                    }
                                    composable("settings/library") {
                                        LibrarySettings(navController, scrollBehavior)
                                    }
                                    composable("settings/library/lyrics") {
                                        LyricsSettings(navController, scrollBehavior)
                                    }
                                    composable("settings/player") {
                                        PlayerSettings(navController, scrollBehavior)
                                    }
                                    composable("settings/storage") {
                                        StorageSettings(navController, scrollBehavior)
                                    }
                                    composable("settings/backup_restore") {
                                        BackupAndRestore(navController, scrollBehavior)
                                    }
                                    composable("settings/local") {
                                        LocalPlayerSettings(navController, scrollBehavior)
                                    }
                                    composable("settings/remote_source") {
                                        RemoteSourceSettings(navController, scrollBehavior)
                                    }
                                    composable("vocal_separation") {
                                        VocalSeparationScreen(navController, scrollBehavior)
                                    }
                                    composable("custom_source_manager") {
                                        CustomSourceManagerScreen(navController, scrollBehavior)
                                    }
                                    composable("rank") {
                                        RankScreen(navController, scrollBehavior)
                                    }
                                    composable("settings/experimental") {
                                        ExperimentalSettings(navController, scrollBehavior)
                                    }
                                    composable("settings/about") {
                                        AboutScreen(navController, scrollBehavior)
                                    }
                                    composable("settings/about/attribution") {
                                        AttributionScreen(navController, scrollBehavior)
                                    }
                                    composable("settings/about/oss_licenses") {
                                        LibrariesScreen(navController, scrollBehavior)
                                    }

                                    composable("setup_wizard") {
                                        SetupWizard(navController)
                                    }
                                }
                            }

                            val navbar: @Composable() (() -> Unit) = @Composable {
                                val navigationBarHeight by animateDpAsState(
                                    targetValue = NavigationBarHeight,
                                    animationSpec = NavigationBarAnimationSpec,
                                    label = ""
                                )

                                NavigationBar(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .height(bottomInset + getNavPadding())
                                        .offset {
                                            if (navigationBarHeight == 0.dp) {
                                                IntOffset(
                                                    x = 0,
                                                    y = (bottomInset + NavigationBarHeight).roundToPx()
                                                )
                                            } else {
                                                val slideOffset =
                                                    (bottomInset + NavigationBarHeight) * playerBottomSheetState.progress.coerceIn(
                                                        0f,
                                                        1f
                                                    )
                                                val hideOffset =
                                                    (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                                IntOffset(
                                                    x = 0,
                                                    y = (slideOffset + hideOffset).roundToPx()
                                                )
                                            }
                                        },
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                                ) {
                                    navigationItems.fastForEach { screen ->
                                        // TODO: display selection when based on root page user entered
//                                        val isSelected = navBackStackEntry?.destination?.hierarchy?.any {
//                                            it.route?.substringBefore("?")?.substringBefore("/") == screen.route
//                                        } == true
                                        NavigationBarItem(
                                            selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true,
                                            icon = {
                                                Icon(
                                                    screen.icon,
                                                    contentDescription = null
                                                )
                                            },
                                            label = {
                                                if (!slimNav) {
                                                    Text(
                                                        text = stringResource(screen.titleId),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            onClick = {
                                                if (playerBottomSheetState.isExpanded) {
                                                    playerBottomSheetState.collapseSoft()
                                                }

                                                if (navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true) {
                                                    navBackStackEntry?.savedStateHandle?.set(
                                                        "scrollToTop",
                                                        true
                                                    )
                                                } else if (navigationItems.none { scr -> navBackStackEntry?.destination?.hierarchy?.any { it.route == scr.route } == true }) {
                                                    // this eye bleach allows you to navigate back when you tap on the navbar on a non-root page
                                                    // TODO: nav3 allows us to access back stack... maybe do indicators properly and remove this hack
                                                    navController.navigateUp()
                                                } else {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }

                                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            }
                                        )
                                    }
                                }
                            }

                            @Composable
                            fun navRail(alignment: Alignment) {
                                val layoutDirection = LocalLayoutDirection.current
                                val navigationBarHeight by animateDpAsState(
                                    targetValue = NavigationBarHeight,
                                    animationSpec = NavigationBarAnimationSpec,
                                    label = ""
                                )
                                val leftInset = remember {
                                    derivedStateOf {
                                        playerAwareWindowInsets.getLeft(density, layoutDirection).dp
                                    }
                                }
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                                    header = {
                                        Spacer(Modifier.height(8.dp))
                                        Image(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .padding(start = 8.dp),
                                            painter = painterResource(R.drawable.small_icon),
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier
                                        .align(alignment)
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState())
                                        .offset {
                                            if (navigationBarHeight == 0.dp) {
                                                IntOffset(
                                                    x = 0,
                                                    y = (bottomInset + NavigationBarHeight).roundToPx()
                                                )
                                            } else {
                                                val slideOffset =
                                                    (bottomInset + NavigationBarHeight + leftInset.value) *
                                                            playerBottomSheetState.progress.coerceIn(0f, 1f)
                                                val hideOffset =
                                                    (bottomInset + NavigationBarHeight) * (1 - navigationBarHeight / NavigationBarHeight)
                                                IntOffset(
                                                    x = -(slideOffset + hideOffset).roundToPx(),
                                                    y = 0
                                                )
                                            }
                                        },
                                ) {
                                    navigationItems.fastForEach { screen ->
                                        // TODO: display selection when based on root page user entered
//                                                val isSelected = navBackStackEntry?.destination?.hierarchy?.any {
//                                                    it.route?.substringBefore("?")?.substringBefore("/") == screen.route
//                                                } == true
                                        NavigationRailItem(
                                            selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true,
                                            icon = {
                                                Icon(
                                                    screen.icon,
                                                    contentDescription = null
                                                )
                                            },
                                            label = {
                                                if (!slimNav) {
                                                    Text(
                                                        text = stringResource(screen.titleId),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            onClick = {
                                                if (playerBottomSheetState.isExpanded) {
                                                    playerBottomSheetState.collapseSoft()
                                                }
                                                if (navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true) {
                                                    navBackStackEntry?.savedStateHandle?.set(
                                                        "scrollToTop",
                                                        true
                                                    )
                                                } else {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }

                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }

                                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            }
                                        )
                                    }
                                }
                            }

                            val bottomSheetMenu: @Composable() (() -> Unit) = @Composable {
                                BottomSheetMenu(
                                    state = LocalMenuState.current,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }

                            // phone
                            if (!tabMode) {
                                navHost()

                                SearchBarContainer(navController, scrollBehavior)

                                if (oobeStatus == OOBE_VERSION) {
                                    if (!navigationItems.contains(Screens.Player)) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController
                                        )
                                    }

                                    if (!useNavRail) {
                                        // navbar() - replaced by navigation drawer
                                    } else {
                                        navRail(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Alignment.BottomEnd else Alignment.BottomStart)
                                    }
                                }
                                bottomSheetMenu()

                                SnackbarHost(
                                    hostState = snackbarHostState,
                                    modifier = Modifier
                                        .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                                        .align(Alignment.BottomCenter)
                                )
                            } else {
                                // tabmode only enables >= 600dp (unless it's forced on). For those who wish to try down
                                // to the widescreen limit, 320dp player is the minimum acceptable size for the player
                                val playerW = (maxW.value * 0.4).coerceIn(320.0, 500.0)
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(playerW.dp)
                                    ) {
                                        if (oobeStatus == OOBE_VERSION && !navigationItems.contains(Screens.Player)) {
                                            PlayerScreen(
                                                navController = navController,
                                                windowInsets = windowsInsets.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical),
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        navHost()

                                        SearchBarContainer(
                                            navController = navController,
                                            scrollBehavior = scrollBehavior,
                                            windowInsets = windowsInsets.only(WindowInsetsSides.Top)
                                        )

                                        if (oobeStatus == OOBE_VERSION) {
                                            navbar()
                                        }
                                        bottomSheetMenu()

                                        SnackbarHost(
                                            hostState = snackbarHostState,
                                            modifier = Modifier
                                                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                                                .align(Alignment.BottomCenter)
                                        )
                                    }
                                }

                            }

                            // Setup wizard
                            LaunchedEffect(Unit) {
                                if (oobeStatus != OOBE_VERSION) {
                                    navController.navigate("setup_wizard")
                                }
                            }

                        }
                    }
                        }
                }
            }
        }
    }

    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        // sdk24 support
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.nocturne.player.action.SEARCH"
        const val ACTION_SONGS = "com.nocturne.player.action.SONGS"
        const val ACTION_ALBUMS = "com.nocturne.player.action.ALBUMS"
        const val ACTION_PLAYLISTS = "com.nocturne.player.action.PLAYLISTS"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalMenuState = staticCompositionLocalOf<MenuState> { error("No menu state provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No player WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }
val LocalDrawerOpen = staticCompositionLocalOf<() -> Unit> { error("No drawer open action provided") }

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
