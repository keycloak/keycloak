package org.keycloak.quarkus.runtime.configuration;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bigram-based cosine similarity for suggesting corrections to misspelled CLI values.
 * Used by option-name matching ({@link KcUnmatchedArgumentException}) and
 * feature-name validation ({@link org.keycloak.quarkus.runtime.configuration.mappers.FeaturePropertyMappers}).
 */
public final class SimilarityUtil {

    public static final int DEFAULT_MAX_SUGGESTIONS = 5;
    public static final double DEFAULT_MIN_SIMILARITY = 0.4;

    private SimilarityUtil() {
    }

    public static List<String> findSimilar(String input, List<String> candidates) {
        return findSimilar(input, candidates, DEFAULT_MAX_SUGGESTIONS, DEFAULT_MIN_SIMILARITY);
    }

    public static double cosineSimilarity(String a, String b) {
        Map<String, Integer> aFreq = bigramFrequency(a);
        Map<String, Integer> bFreq = bigramFrequency(b);
        double dot = dotProduct(aFreq, bFreq);
        double normA = dotProduct(aFreq, aFreq);
        double normB = dotProduct(bFreq, bFreq);
        double denominator = Math.sqrt(normA * normB);
        return denominator == 0 ? 0 : dot / denominator;
    }

    public static List<String> findSimilar(String input, List<String> candidates, int maxSuggestions, double minSimilarity) {
        String lowerInput = input.toLowerCase();
        return candidates.stream()
                .map(candidate -> Map.entry(cosineSimilarity(lowerInput, candidate.toLowerCase()), candidate))
                .filter(e -> e.getKey() >= minSimilarity)
                .sorted(Comparator.<Map.Entry<Double, String>, Double>comparing(Map.Entry::getKey).reversed())
                .limit(maxSuggestions)
                .map(Map.Entry::getValue)
                .toList();
    }

    private static Map<String, Integer> bigramFrequency(String s) {
        Map<String, Integer> freq = new HashMap<>();
        for (int i = 0; i < s.length() - 1; i++) {
            freq.merge(s.substring(i, i + 2), 1, Integer::sum);
        }
        return freq;
    }

    private static double dotProduct(Map<String, Integer> m1, Map<String, Integer> m2) {
        return m1.entrySet().stream()
                .collect(Collectors.summingDouble(e -> e.getValue() * (m2.getOrDefault(e.getKey(), 0))));
    }
}
