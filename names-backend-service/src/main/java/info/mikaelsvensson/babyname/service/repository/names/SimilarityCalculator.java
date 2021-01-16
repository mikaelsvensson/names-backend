package info.mikaelsvensson.babyname.service.repository.names;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SimilarityCalculator {
    final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    final JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
    final LongestCommonSubsequence longestCommonSubsequence = new LongestCommonSubsequence();

    public Map<String, Map<SimilarityAlgorithm, Double>> get(String refName, Map<String, String> otherNames) {
        var otherResults = new HashMap<String, Map<SimilarityAlgorithm, Double>>();
        for (Map.Entry<String, String> entry : otherNames.entrySet()) {
            final var otherId = entry.getKey();
            final var otherName = entry.getValue();
            var levenshtein = 1.0 - (1.0 * levenshteinDistance.apply(refName, otherName) / refName.length());
            if (levenshtein >= 0.75) {
                otherResults.putIfAbsent(otherId, new HashMap<>());
                otherResults.get(otherId).put(SimilarityAlgorithm.LEVENSHTEIN, levenshtein);
            }
            var jaroWinkler = jaroWinklerDistance.apply(refName, otherName);
            if (jaroWinkler >= 0.85) {
                otherResults.putIfAbsent(otherId, new HashMap<>());
                otherResults.get(otherId).put(SimilarityAlgorithm.JARO_WINKLER, jaroWinkler);
            }
            var longestSubseq = (1.0 * longestCommonSubsequence.apply(refName, otherName)) /
                    ((1.0 * refName.length() + otherName.length()) / 2);
            if (longestSubseq >= 0.75) {
                otherResults.putIfAbsent(otherId, new HashMap<>());
                otherResults.get(otherId).put(SimilarityAlgorithm.LONGEST_COMMON_SUBSEQUENCE, longestSubseq);
            }
        }

        return otherResults;
    }
}
