package com.travis.similarity;

import org.apache.commons.text.similarity.SimilarityScore;

import java.util.HashMap;
import java.util.Map;

public class NGramPositionalCosineSimilarity implements SimilarityScore<Double> {

    private final int windowSize;

    public NGramPositionalCosineSimilarity(int windowSize) {
        this.windowSize = windowSize;
    }

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

        return dotProduct / (leftMagnitude * rightMagnitude);
    }

    private Map<CharSequence, Double> buildNGramVector(CharSequence cs) {
        Map<CharSequence, Double> nGramVector = new HashMap<>();

        for (int i = 0; i <= cs.length() - windowSize; i++) {
            CharSequence nGram = cs.subSequence(i, i + windowSize);
            double weight = 1.0 / (1 + i);  // Weight based on position
            nGramVector.put(nGram, nGramVector.getOrDefault(nGram, 0.0) + weight);
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
        double magnitude = 0.0;
        for (double freq : frequencyMap.values()) {
            magnitude += Math.pow(freq, 2);
        }
        return Math.sqrt(magnitude);
    }
}
