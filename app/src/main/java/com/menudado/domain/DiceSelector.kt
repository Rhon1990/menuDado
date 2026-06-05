package com.menudado.domain

import kotlin.random.Random

object DiceSelector {
    fun select(
        menus: List<FoodMenu>,
        filter: MealType?,
        today: String? = null,
        nextIndex: (bound: Int) -> Int = { bound -> Random.nextInt(bound) }
    ): FoodMenu? {
        val candidates = if (filter == null) {
            menus
        } else {
            menus.filter { it.mealType == filter }
        }

        val availableCandidates = if (today == null) {
            candidates
        } else {
            candidates.filter { it.lastPickedDate != today }
        }

        if (availableCandidates.isEmpty()) return null

        return availableCandidates[nextIndex(availableCandidates.size)]
    }

    fun hasCandidates(menus: List<FoodMenu>, filter: MealType?): Boolean {
        return if (filter == null) {
            menus.isNotEmpty()
        } else {
            menus.any { it.mealType == filter }
        }
    }

    fun availableCandidateCount(menus: List<FoodMenu>, filter: MealType?, today: String? = null): Int {
        val candidates = if (filter == null) {
            menus
        } else {
            menus.filter { it.mealType == filter }
        }

        return if (today == null) {
            candidates.size
        } else {
            candidates.count { it.lastPickedDate != today }
        }
    }
}
