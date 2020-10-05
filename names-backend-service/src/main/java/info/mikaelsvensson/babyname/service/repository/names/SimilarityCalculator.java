package info.mikaelsvensson.babyname.service.repository.names;

import info.mikaelsvensson.babyname.service.model.Name;
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

    public Map<Name, Map<SimilarityAlgorithm, Double>> get(Name refName, Iterable<Name> otherNames) {
        var otherResults = new HashMap<Name, Map<SimilarityAlgorithm, Double>>();
        for (Name otherName : otherNames) {
            var levenshtein = 1.0 - (1.0 * levenshteinDistance.apply(refName.getName(), otherName.getName()) / refName.getName().length());
            if (levenshtein >= 0.75) {
                otherResults.putIfAbsent(otherName, new HashMap<>());
                otherResults.get(otherName).put(SimilarityAlgorithm.LEVENSHTEIN, levenshtein);
            }
            var jaroWinkler = jaroWinklerDistance.apply(refName.getName(), otherName.getName());
            if (jaroWinkler >= 0.85) {
                otherResults.putIfAbsent(otherName, new HashMap<>());
                otherResults.get(otherName).put(SimilarityAlgorithm.JARO_WINKLER, jaroWinkler);
            }
            var longestSubseq = (1.0 * longestCommonSubsequence.apply(refName.getName(), otherName.getName())) /
                    ((1.0 * refName.getName().length() + otherName.getName().length()) / 2);
            if (longestSubseq >= 0.75) {
                otherResults.putIfAbsent(otherName, new HashMap<>());
                otherResults.get(otherName).put(SimilarityAlgorithm.LONGEST_COMMON_SUBSEQUENCE, longestSubseq);
            }
        }

        return otherResults;
    }
}
