package com.resumematcher.service;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares a resume's text against a job description using keyword overlap.
 * No external NLP library — just tokenization, stopword removal, and set math.
 */
@Service
public class KeywordMatchService {

    // Common English words that carry no meaning for matching.
    // Hardcoded to keep the project self-contained (no external file needed).
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "do", "does", "did",
            "will", "would", "shall", "should", "may", "might", "can", "could",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
            "into", "about", "between", "through", "during", "before", "after",
            "above", "below", "not", "no", "nor", "so", "if", "then", "than",
            "too", "very", "just", "that", "this", "it", "its", "we", "our",
            "you", "your", "they", "their", "he", "she", "his", "her", "i",
            "me", "my", "am", "up", "out", "all", "each", "every", "both",
            "such", "when", "where", "how", "what", "which", "who", "whom"
    );

    /**
     * Compares resumeText against jdText and returns match statistics.
     *
     * Algorithm:
     *   1. Lowercase both texts → makes "Java" and "java" equal.
     *   2. Tokenize (split on non-letter/digit) → ["java", "3", "years"].
     *   3. Remove stopwords → filters noise like "and", "the", "is".
     *   4. Build keyword sets for JD and resume.
     *   5. matchPercentage = (intersection size / JD keyword count) × 100.
     *
     * @return a Map with keys: matchPercentage, matchedKeywords, missingKeywords
     */
    public Map<String, Object> compare(String resumeText, String jdText) {
        Set<String> resumeKeywords = extractKeywords(resumeText);
        Set<String> jdKeywords = extractKeywords(jdText);

        // Intersection: JD keywords that ARE in the resume
        Set<String> matched = jdKeywords.stream()
                .filter(resumeKeywords::contains)      // keep only if resume has it
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Difference: JD keywords that are NOT in the resume
        Set<String> missing = jdKeywords.stream()
                .filter(kw -> !resumeKeywords.contains(kw))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Avoid division by zero if the JD has no meaningful keywords
        double percentage = jdKeywords.isEmpty()
                ? 0.0
                : (double) matched.size() / jdKeywords.size() * 100.0;

        // Round to 2 decimal places for clean display
        percentage = Math.round(percentage * 100.0) / 100.0;

        // LinkedHashMap preserves insertion order (nicer for debugging/logging)
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchPercentage", percentage);
        result.put("matchedKeywords", String.join(", ", matched));
        result.put("missingKeywords", String.join(", ", missing));
        return result;
    }

    /**
     * Converts raw text into a set of meaningful keywords.
     * Uses a LinkedHashSet to preserve the order words first appeared.
     */
    private Set<String> extractKeywords(String text) {
        return Arrays.stream(
                        text.toLowerCase()                 // step 1: normalize case
                            .split("[^a-zA-Z0-9]+"))       // step 2: split on non-alphanumeric
                .filter(word -> !word.isBlank())            // drop empty tokens from split
                .filter(word -> word.length() > 1)          // drop single chars ("a", "I", etc.)
                .filter(word -> !STOPWORDS.contains(word))  // step 3: remove stopwords
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
