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

    fun currencyName(code: String): String = when (code.uppercase()) {
        "AED" -> "UAE Dirham"
        "ARS" -> "Argentine Peso"
        "AUD" -> "Australian Dollar"
        "BRL" -> "Brazilian Real"
        "CAD" -> "Canadian Dollar"
        "CHF" -> "Swiss Franc"
        "CLP" -> "Chilean Peso"
        "CNY" -> "Chinese Yuan"
        "COP" -> "Colombian Peso"
        "CZK" -> "Czech Koruna"
        "DKK" -> "Danish Krone"
        "EUR" -> "Euro"
        "GBP" -> "British Pound"
        "HKD" -> "Hong Kong Dollar"
        "IDR" -> "Indonesian Rupiah"
        "ILS" -> "Israeli Shekel"
        "INR" -> "Indian Rupee"
        "JPY" -> "Japanese Yen"
        "KRW" -> "South Korean Won"
        "MXN" -> "Mexican Peso"
        "MYR" -> "Malaysian Ringgit"
        "NOK" -> "Norwegian Krone"
        "NZD" -> "New Zealand Dollar"
        "PHP" -> "Philippine Peso"
        "PLN" -> "Polish Zloty"
        "SAR" -> "Saudi Riyal"
        "SEK" -> "Swedish Krona"
        "SGD" -> "Singapore Dollar"
        "THB" -> "Thai Baht"
        "TRY" -> "Turkish Lira"
        "TWD" -> "Taiwan Dollar"
        "USD" -> "US Dollar"
        "VND" -> "Vietnamese Dong"
        "ZAR" -> "South African Rand"
        else -> "Philippine Peso"
    }

    fun currencySymbol(code: String): String = when (code.uppercase()) {
        "AED" -> "د.إ"
        "ARS" -> "$"
        "AUD" -> "A$"
        "BRL" -> "R$"
        "CAD" -> "C$"
        "CHF" -> "Fr"
        "CLP" -> "$"
        "CNY" -> "¥"
        "COP" -> "$"
        "CZK" -> "Kč"
        "DKK" -> "kr"
        "EUR" -> "€"
        "GBP" -> "£"
        "HKD" -> "HK$"
        "IDR" -> "Rp"
        "ILS" -> "₪"
        "INR" -> "₹"
        "JPY" -> "¥"
        "KRW" -> "₩"
        "MXN" -> "$"
        "MYR" -> "RM"
        "NOK" -> "kr"
        "NZD" -> "NZ$"
        "PHP" -> "₱"
        "PLN" -> "zł"
        "SAR" -> "﷼"
        "SEK" -> "kr"
        "SGD" -> "S$"
        "THB" -> "฿"
        "TRY" -> "₺"
        "TWD" -> "NT$"
        "USD" -> "$"
        "VND" -> "₫"
        "ZAR" -> "R"
        else -> "₱"
    }
}
