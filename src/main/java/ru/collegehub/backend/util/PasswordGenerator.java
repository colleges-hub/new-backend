package ru.collegehub.backend.util;

import java.util.List;
import java.util.Random;

public class PasswordGenerator {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String PUNCTUATION = "!@#$%&*()_+-=[]|,./?><";
    private static final int LENGTH = 20;

    public static String generate() {
        // Variables.
        StringBuilder password = new StringBuilder(LENGTH);
        Random random = new Random(System.nanoTime());

        // Collect the categories to use.
        List<String> charCategories = List.of(LOWER, UPPER, DIGITS, PUNCTUATION);

        // Build the password.
        for (int i = 0; i < LENGTH; i++) {
            String charCategory = charCategories.get(random.nextInt(charCategories.size()));
            int position = random.nextInt(charCategory.length());
            password.append(charCategory.charAt(position));
        }
        return new String(password);
    }
}
