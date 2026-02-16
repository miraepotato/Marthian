package com.example.marthianclean.model

enum class FireType(val label: String) {
    FACTORY("공장"),
    WAREHOUSE("창고"),
    SINGLE_HOUSE("단독주택"),
    APARTMENT("공동주택"),
    COMMERCIAL("상업시설"),
    HAZMAT_PLANT("위험물제조소"),
    FOREST("산림"),
    PIG_FARM("돈사"),
    COW_FARM("우사"),
    CHICKEN_FARM("계사"),
    GREENHOUSE("비닐하우스"),
    OTHER("기타");

    companion object {
        fun from(raw: String?): FireType {
            val v = raw?.trim().orEmpty()
            if (v.isBlank()) return OTHER

            // 1) enum name 매칭 ("FACTORY")
            entries.firstOrNull { it.name.equals(v, ignoreCase = true) }?.let { return it }
            // 2) label 매칭 ("공장")
            entries.firstOrNull { it.label == v }?.let { return it }

            return OTHER
        }
    }
}
