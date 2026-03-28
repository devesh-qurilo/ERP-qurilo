package com.erp.finance_servic.util;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class AmountToWordConverter {

    private static final String[] units = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight",
            "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
            "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] tens = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    /** Backward compatible: defaults to INR words */
    public String convert(BigDecimal amount) {
        return convert(amount, "INR");
    }

    /** Currency-aware words; Indian number system (Thousand/Lakh/Crore) */
    public String convert(BigDecimal amount, String currency) {
        if (amount == null) return "Zero";
        long whole = amount.longValue();
        long fractional = amount.remainder(BigDecimal.ONE).movePointRight(2).longValue();

        String mainUnit = mainUnitName(currency);
        String subUnit  = subUnitName(currency);

        String main = convertToWords(whole) + " " + pluralize(mainUnit, whole);
        String sub = fractional > 0
                ? " and " + convertToWords(fractional) + " " + pluralize(subUnit, fractional)
                : "";
        return main + sub;
    }

    private String pluralize(String word, long count) {
        if (count == 1) return word;
        return word.endsWith("s") ? word : word + "s";
    }

    private String mainUnitName(String currency) {
        if (currency == null) return "Rupee";
        return switch (currency.toUpperCase()) {
            case "USD" -> "Dollar";
            case "EUR" -> "Euro";
            case "GBP" -> "Pound";
            case "INR" -> "Rupee";
            default -> currency; // fallback to code
        };
    }

    private String subUnitName(String currency) {
        if (currency == null) return "Paisa";
        return switch (currency.toUpperCase()) {
            case "USD", "EUR" -> "Cent";
            case "GBP" -> "Pence";
            case "INR" -> "Paisa";
            default -> "Cent";
        };
    }

    // Indian system: Thousand, Lakh, Crore
    private String convertToWords(long number) {
        if (number == 0) return "Zero";
        if (number < 20) return units[(int) number];
        if (number < 100)
            return tens[(int) (number / 10)] + ((number % 10 != 0) ? " " + units[(int) (number % 10)] : "");
        if (number < 1000)
            return units[(int) (number / 100)] + " Hundred" + ((number % 100 != 0) ? " and " + convertToWords(number % 100) : "");
        if (number < 100000)
            return convertToWords(number / 1000) + " Thousand" + ((number % 1000 != 0) ? " " + convertToWords(number % 1000) : "");
        if (number < 10000000)
            return convertToWords(number / 100000) + " Lakh" + ((number % 100000 != 0) ? " " + convertToWords(number % 100000) : "");
        return convertToWords(number / 10000000) + " Crore" + ((number % 10000000 != 0) ? " " + convertToWords(number % 10000000) : "");
    }
}
