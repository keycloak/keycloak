package org.keycloak.quarkus.runtime.configuration;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimilarityUtilTest {

    @Test
    public void exactMatch() {
        assertEquals(1.0, SimilarityUtil.cosineSimilarity("token-exchange", "token-exchange"), 0.001);
    }

    @Test
    public void similarStrings() {
        double similarity = SimilarityUtil.cosineSimilarity("tokn-exchang", "token-exchange");
        assertTrue("Expected high similarity for near-match, got " + similarity, similarity > 0.5);
    }

    @Test
    public void dissimilarStrings() {
        double similarity = SimilarityUtil.cosineSimilarity("xyz", "token-exchange");
        assertTrue("Expected low similarity for unrelated strings, got " + similarity, similarity < 0.3);
    }

    @Test
    public void emptyString() {
        assertEquals(0.0, SimilarityUtil.cosineSimilarity("", "test"), 0.001);
    }

    @Test
    public void singleChar() {
        assertEquals(0.0, SimilarityUtil.cosineSimilarity("a", "b"), 0.001);
    }

    @Test
    public void findSimilarReturnsBestMatches() {
        List<String> candidates = List.of("token-exchange", "admin-api", "account-api",
                "docker", "impersonation", "token-introspection", "passkeys");

        List<String> suggestions = SimilarityUtil.findSimilar("tokn-exchang", candidates, 3, SimilarityUtil.DEFAULT_MIN_SIMILARITY);

        assertTrue("Should suggest token-exchange", suggestions.contains("token-exchange"));
        assertTrue("Should return at most 3 suggestions", suggestions.size() <= 3);
        assertEquals("Best match should be first", "token-exchange", suggestions.get(0));
    }

    @Test
    public void findSimilarFiltersLowSimilarity() {
        List<String> candidates = List.of("token-exchange", "admin-api", "docker");

        List<String> suggestions = SimilarityUtil.findSimilar("zzzzzzzzz", candidates, 5, SimilarityUtil.DEFAULT_MIN_SIMILARITY);

        assertTrue("Should return no suggestions for unrelated input", suggestions.isEmpty());
    }

    @Test
    public void findSimilarCaseInsensitive() {
        List<String> candidates = List.of("Token-Exchange", "admin-api");

        List<String> suggestions = SimilarityUtil.findSimilar("TOKEN-EXCHANGE", candidates, 3, SimilarityUtil.DEFAULT_MIN_SIMILARITY);

        assertTrue("Should match case-insensitively", suggestions.contains("Token-Exchange"));
    }
}
