package com.agendaki.whatsapp;

/**
 * Converte o telefone digitado pelo cliente para o formato que a Cloud API espera:
 * só dígitos, com código do país (ex.: 5511999999999).
 *
 * As regras de número nacional (10 ou 11 dígitos = DDD + número) valem para o Brasil.
 */
public final class PhoneNumbers {

    private static final int MIN_DIGITS = 8;
    private static final int MAX_DIGITS = 15; // limite do E.164

    private PhoneNumbers() {}

    /** @return o número só com dígitos e código do país, ou null se não der para interpretar. */
    public static String toE164(String raw, String defaultCountryCode) {
        if (raw == null || raw.isBlank()) return null;

        String trimmed = raw.trim();
        String digits = trimmed.replaceAll("\\D", "");

        boolean plus = trimmed.startsWith("+");
        boolean doubleZero = trimmed.startsWith("00");
        if (plus || doubleZero) {
            // Já vem com código do país: só confere o tamanho ("00" é o prefixo internacional).
            if (doubleZero) digits = digits.substring(2);
            return validLength(digits) ? digits : null;
        }

        digits = digits.replaceFirst("^0+", ""); // zero de tronco: 011 99999-9999
        int cc = defaultCountryCode.length();

        if (digits.length() == 10 || digits.length() == 11) {
            digits = defaultCountryCode + digits;                       // DDD + número
        } else if (!(digits.startsWith(defaultCountryCode)
                && (digits.length() == cc + 10 || digits.length() == cc + 11))) {
            return null;                                                // nem nacional, nem com código do país
        }
        return validLength(digits) ? digits : null;
    }

    /** Para logs: mantém só os 4 últimos dígitos. */
    public static String mask(String phone) {
        if (phone == null) return "—";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) return "****";
        return "****" + digits.substring(digits.length() - 4);
    }

    private static boolean validLength(String digits) {
        return digits.length() >= MIN_DIGITS && digits.length() <= MAX_DIGITS;
    }
}
