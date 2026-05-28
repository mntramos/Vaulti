package com.vaulti.app.ui

import java.util.Locale

object FormatUtils {
    fun formatAmount(amount: Double): String =
        String.format(Locale.getDefault(), "%,.2f", amount)

    fun formatAmountForEdit(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            String.format(Locale.getDefault(), "%.1f", amount)
        } else {
            String.format(Locale.getDefault(), "%.2f", amount)
        }
    }

    inline fun <reified T : Enum<T>> safeValueOf(name: String, default: T): T =
        try {
            java.lang.Enum.valueOf(T::class.java, name)
        } catch (_: IllegalArgumentException) {
            default
        }
}
