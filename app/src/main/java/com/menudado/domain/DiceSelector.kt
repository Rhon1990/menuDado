package com.menudado.domain

import kotlin.random.Random

object DiceSelector {
    fun select(
        menus: List<FoodMenu>,
        filter: MealType?,
        audienceFilter: MenuAudience? = null,
        today: String? = null,
        nextIndex: (bound: Int) -> Int = { bound -> Random.nextInt(bound) }
    ): FoodMenu? {
        val candidates = menus.filter { menu ->
            (filter == null || menu.mealType == filter) &&
                (audienceFilter == null || menu.audience == audienceFilter)
        }

        val availableCandidates = if (today == null) {
            candidates
        } else {
            candidates.filter { it.lastPickedDate != today }
        }

        if (availableCandidates.isEmpty()) return null

        return availableCandidates[nextIndex(availableCandidates.size)]
    }

    fun hasCandidates(menus: List<FoodMenu>, filter: MealType?, audienceFilter: MenuAudience? = null): Boolean {
        return menus.any { menu ->
            (filter == null || menu.mealType == filter) &&
                (audienceFilter == null || menu.audience == audienceFilter)
        }
    }

    fun availableCandidateCount(
        menus: List<FoodMenu>,
        filter: MealType?,
        audienceFilter: MenuAudience? = null,
        today: String? = null
    ): Int {
        val candidates = menus.filter { menu ->
            (filter == null || menu.mealType == filter) &&
                (audienceFilter == null || menu.audience == audienceFilter)
        }

        return if (today == null) {
            candidates.size
        } else {
            candidates.count { it.lastPickedDate != today }
        }
    }
}
