package info.mikaelsvensson.babyname.service.util.similarity;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SimilarityCalculator {
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    private final JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
    private final LongestCommonSubsequence longestCommonSubsequence = new LongestCommonSubsequence();
    private final Map<String, String> otherNames;

    public SimilarityCalculator(Map<String, String> otherNames) {
        this.otherNames = otherNames;
    }

    public Map<String, Map<SimilarityAlgorithm, Double>> getData(String refName) {
        var otherResults = new HashMap<String, Map<SimilarityAlgorithm, Double>>();
        for (Map.Entry<String, String> entry : otherNames.entrySet()) {
            final var otherId = entry.getKey();
            final var otherName = entry.getValue();
            if (refName.equals(otherName)) {
                continue;
            }
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

    public List<String> getSortedList(String refName) {
        final var data = getData(refName);
        return data.entrySet()
                .stream()
                .sorted((o1, o2) ->
                        o2.getValue().values().stream().mapToInt(value -> (int) (value * 100_000)).sum() -
                                o1.getValue().values().stream().mapToInt(value -> (int) (value * 100_000)).sum())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
