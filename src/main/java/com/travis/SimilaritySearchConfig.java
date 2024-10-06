package com.travis;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("similarity-search")
public interface SimilaritySearchConfig extends Config
{
	@ConfigItem(
		keyName = "matchingStrategy",
		name = "Matching Strategy",
		description = "What type of algorithm to use while searching. I like the Bigram Cosine or Jaro Wrinkler."
	)
	default MatchingStrategy matchingStrategy() {
		return MatchingStrategy.BIGRAM_COSINE;
	}

	@Range(
			min = 1,
			max = 100
	)
	@ConfigItem(
			keyName = "similarityThreshold",
			name = "Bank Filter Threshold",
			description = "1 - 100 Set the minimum similarity percentage for a match. Higher = strict. This only applies to bank filtering."
	)
	default double similarityThreshold() {
		return 80;
	}

	@ConfigItem(
			keyName = "grandExchangeEnabled",
			name = "Enable for Grand Exchange",
			description = "Enables this similarity searching in the Grand Exchange. The top 36 items are shown in descending order of similarity score."
	)
	default boolean grandExchangeEnabled() {
		return true;
	}
}
