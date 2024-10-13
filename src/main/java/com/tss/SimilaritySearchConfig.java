package com.tss;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("similarity-search")
public interface SimilaritySearchConfig extends Config {
    @ConfigItem(
            position = 1,
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
			position = 2,
            keyName = "bankFilterThreshold",
            name = "Bank Filter Threshold %",
            description = "1% - 100% Set the minimum similarity percentage for a match in Bank filtering. Higher = strict. This only applies to bank filtering."
    )
    default double bankFilterThreshold() {
        return 70;
    }

	@ConfigItem(
			position = 3,
			keyName = "bankEnabled",
			name = "Enable for Bank",
			description = "Enables similarity search in your Bank. If the default exact match doesn't find the item you're looking for, similarity search might."
	)
	default boolean bankEnabled() {
		return true;
	}

    @ConfigItem(
			position = 4,
            keyName = "grandExchangeEnabled",
            name = "Enable for Grand Exchange",
            description = "Enables similarity search in the Grand Exchange. The top 36 items are shown in descending order of similarity score."
    )
    default boolean grandExchangeEnabled() {
        return true;
    }

	@ConfigItem(
			position = 5,
			keyName = "scoreDebug",
			name = "Enable score debug",
			description = "When checked, the computed similarity score will show before the item name in the Grand Exchange results or before the item name when hovering over it when Bank filtering. Useful for tuning the Bank Filter Threshold to your liking."
	)
	default boolean scoreDebug() {
		return false;
	}
}
