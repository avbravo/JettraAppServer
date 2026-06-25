package com.jettra.server.core;

/**
 * Utility class for security operations like XSS sanitization.
 */
public class SecurityUtil {

    /**
     * Sanitizes a string to prevent XSS attacks by escaping HTML special characters.
     * @param input the raw input string
     * @return the sanitized string
     */
    public static String sanitizeHtml(String input) {
        if (input == null) return null;
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .replace("/", "&#x2F;");
    }
}
