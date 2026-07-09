package com.ofac.screening;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of the Python normalize_name + classify_match logic from request_script.py.
 */
public class MatchClassifier {

    /**
     * Normalize a name: lowercase, strip non-alphanumeric (except spaces), split into word set.
     * Port of Python normalize_name().
     */
    public static Set<String> normalize(String name) {
        if (name == null || name.isEmpty()) {
            return new HashSet<>();
        }
        // Use Unicode-aware \w (same as Python 3 default) so Chinese chars like 陈平 are kept.
        // (?U) enables UNICODE_CHARACTER_CLASS, making \w match [\p{L}\p{N}_].
        String cleaned = name.toLowerCase().replaceAll("(?U)[^\\w\\s]", "").trim();
        if (cleaned.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> result = new HashSet<>();
        for (String w : cleaned.split("\\s+")) {
            if (!w.isEmpty()) {
                result.add(w);
            }
        }
        return result;
    }

    /**
     * Classify the match between a query name and a list of hit names.
     * Port of Python classify_match().
     *
     * @return "Exact Match", "Partial Match", or "Not Match"
     */
    public static String classify(String queryName, List<String> hitNames) {
        Set<String> queryWords = normalize(queryName);
        if (queryWords.isEmpty()) {
            return "Not Match";
        }

        String bestType = "Not Match";

        for (String hitName : hitNames) {
            Set<String> hitWords = normalize(hitName);
            if (hitWords.isEmpty()) {
                continue;
            }

            // Exact: all query words are subset of hit words
            if (hitWords.containsAll(queryWords)) {
                return "Exact Match";
            }

            // Partial: any intersection
            for (String qw : queryWords) {
                if (hitWords.contains(qw)) {
                    bestType = "Partial Match";
                    break;
                }
            }
        }

        return bestType;
    }
}
