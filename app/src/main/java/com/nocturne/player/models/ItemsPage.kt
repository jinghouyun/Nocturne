package com.nocturne.player.models

import com.nocturne.player.db.entities.LocalItem


data class ItemsPage(
    val items: List<LocalItem>,
    val continuation: String?,
)
