/*
 * Copyright (C) 2025 OuterTune Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.nocturne.player.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nocturne.player.R
import com.nocturne.player.constants.AutomaticScannerKey
import com.nocturne.player.constants.DEFAULT_ENABLED_FILTERS
import com.nocturne.player.constants.DEFAULT_ENABLED_TABS
import com.nocturne.player.constants.EnabledFiltersKey
import com.nocturne.player.constants.EnabledTabsKey
import com.nocturne.player.constants.TopBarInsets
import com.nocturne.player.ui.component.ColumnWithContentPadding
import com.nocturne.player.ui.component.PreferenceGroupTitle
import com.nocturne.player.ui.component.SwitchPreference
import com.nocturne.player.ui.component.button.IconButton
import com.nocturne.player.ui.dialog.InfoLabel
import com.nocturne.player.ui.screens.settings.fragments.LocalScannerExtraFrag
import com.nocturne.player.ui.screens.settings.fragments.LocalScannerFrag
import com.nocturne.player.ui.utils.backToMain
import com.nocturne.player.utils.rememberPreference


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (autoScan, onAutoScanChange) = rememberPreference(AutomaticScannerKey, defaultValue = true)
    val (enabledFilters, onEnabledFiltersChange) = rememberPreference(
        EnabledFiltersKey,
        defaultValue = DEFAULT_ENABLED_FILTERS
    )
    val (enabledTabs, onEnabledTabsChange) = rememberPreference(EnabledTabsKey, defaultValue = DEFAULT_ENABLED_TABS)

    ColumnWithContentPadding(
        modifier = Modifier.fillMaxHeight(),
        columnModifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            // automatic scanner
            SwitchPreference(
                title = { Text(stringResource(R.string.auto_scanner_title)) },
                description = stringResource(R.string.auto_scanner_description),
                icon = { Icon(Icons.Rounded.Autorenew, null) },
                checked = autoScan,
                onCheckedChange = onAutoScanChange
            )
            InfoLabel(
                text = stringResource(R.string.auto_scanner_tooltip),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {

            PreferenceGroupTitle(
                title = stringResource(R.string.grp_manual_scanner)
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                LocalScannerFrag()
            }
            Spacer(modifier = Modifier.height(16.dp))

            PreferenceGroupTitle(
                title = stringResource(R.string.grp_extra_scanner_settings)
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                LocalScannerExtraFrag()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    /**
     * ---------------------------
     * Dialogs
     * ---------------------------
     */

    TopAppBar(
        title = { Text(stringResource(R.string.local_player_settings_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        windowInsets = TopBarInsets,
        scrollBehavior = scrollBehavior
    )
}
