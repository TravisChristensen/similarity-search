package com.travis;

import com.travis.similarity.NGramPositionalCosineSimilarity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.*;

@Slf4j
public enum MatchingStrategy {
    BIGRAM_COSINE(new NGramPositionalCosineSimilarity(2)),
    JACCARD(new JaccardSimilarity()),
    JARO_WRINKLER(new JaroWinklerSimilarity()),
    LEVENSHTEIN(new LevenshteinDistance());

    private final SimilarityScore<? extends Number> scorer;

    MatchingStrategy(SimilarityScore<? extends Number> scorer) {
        this.scorer = scorer;
    }

    public double apply(String query, String textToCompare) {
        double score = 0;

        if (query == null || textToCompare == null) {
            return score;
        }

        query = query.toLowerCase();
        textToCompare = textToCompare.toLowerCase();

        double similarityResult = scorer.apply(query, textToCompare).doubleValue();
        String[] splitText = textToCompare.split(" ");
        for (String word : splitText) {
            double splitSimilarityResult = scorer.apply(query, word).doubleValue();
            similarityResult = Math.max(similarityResult, splitSimilarityResult);
        }

        // Normalize score for specific algorithms
        switch (this) {
            case JACCARD:
            case BIGRAM_COSINE:
            case JARO_WRINKLER: {
                score = similarityResult * 100;
                break;
            }
            case LEVENSHTEIN: {
                int maxLength = Math.max(query.length(), textToCompare.length());
                score = (1.d - (similarityResult / (double) maxLength)) * 100;
                break;
            }
            default:
                log.error("Unsupported algorithm. {}", this);
        }

        return score;
    }
}
