package com.tss.similarity;

import org.apache.commons.text.similarity.SimilarityScore;

import java.util.HashMap;
import java.util.Map;

public class CharNGramCosineSimilarity implements SimilarityScore<Double> {

    private final int windowSize;

    public CharNGramCosineSimilarity(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * Calculate cosine similarity between left and right CharSequence. If the left CharSequence is
     * longer than the right + 1, this implementation will penalize the score by
     * a factor of <code>right.length() / left.length()</code>.
     * @param left the first CharSequence
     * @param right the second CharSequence
     * @return A cosine similarity score.
     */
    @Override
    public Double apply(CharSequence left, CharSequence right) {
        Map<CharSequence, Double> leftVector = buildNGramVector(left);
        Map<CharSequence, Double> rightVector = buildNGramVector(right);

        double dotProduct = dotProduct(leftVector, rightVector);
        double leftMagnitude = magnitude(leftVector);
        double rightMagnitude = magnitude(rightVector);

        if (leftMagnitude == 0 || rightMagnitude == 0) {
            return 0.0;
        }

        double cosineSimilarity = dotProduct / (leftMagnitude * rightMagnitude);

        if (left.length() > right.length() + 1) {
            double rightUndersizedPenalty = (double) right.length() / left.length();
            cosineSimilarity *= rightUndersizedPenalty;
        }

        return cosineSimilarity;
    }

    private Map<CharSequence, Double> buildNGramVector(CharSequence cs) {
        Map<CharSequence, Double> nGramVector = new HashMap<>();

        for (int i = 0; i <= cs.length() - windowSize; i++) {
            CharSequence nGram = cs.subSequence(i, i + windowSize);
            nGramVector.put(nGram, nGramVector.getOrDefault(nGram, 0.0) + 1.0);
        }

        return nGramVector;
    }

    private double dotProduct(Map<CharSequence, Double> vector1, Map<CharSequence, Double> vector2) {
        double dotProduct = 0.0;
        for (CharSequence cs : vector1.keySet()) {
            if (vector2.containsKey(cs)) {
                dotProduct += vector1.get(cs) * vector2.get(cs);
            }
        }
        return dotProduct;
    }

    private double magnitude(Map<CharSequence, Double> frequencyMap) {
        double sumOfSquares = 0.0;
        for (double freq : frequencyMap.values()) {
            sumOfSquares += Math.pow(freq, 2);
        }
        return Math.sqrt(sumOfSquares);
    }
}
