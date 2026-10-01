package com.nocturne.player.models

import com.nocturne.player.db.entities.LocalItem


data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<LocalItem>,
)
