package com.travis;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

@Slf4j
@PluginDescriptor(
	name = "Similarity Search", description = "Filter your bank or search the Grand Exchange using a variety of algorithms better than exact match."
)
public class SimilaritySearchPlugin extends Plugin {
	private static final String BANK_SEARCH_FILTER_EVENT_NAME = "bankSearchFilter";
	private static final int MAX_GE_RESULTS = 3*12;
	public static final int NOTED_ITEM = 799;

	@Inject
	private Client client;

	@Inject
	private SimilaritySearchConfig config;

	@Inject
	private ItemManager itemManager;

    @Override
	protected void startUp() {
		log.info("Similarity Search started!");
	}

	@Override
	protected void shutDown() {
		log.info("Similarity Search stopped!");
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event) {
		if (!BANK_SEARCH_FILTER_EVENT_NAME.equals(event.getEventName())) {
			return;
		}

		int[] intStack = client.getIntStack();
		String[] stringStack = client.getStringStack();
		int intStackSize = client.getIntStackSize();
		int stringStackSize = client.getStringStackSize();

		int itemId = intStack[intStackSize - 1];
		String input = stringStack[stringStackSize - 1];
		String itemName = itemManager.getItemComposition(itemId).getName();
		if (input == null || input.isEmpty()) {
			return;
		}

		MatchingStrategy strategy = config.matchingStrategy();
		double score = strategy.apply(input, itemName);
		if (score >= config.similarityThreshold()) {
			intStack[intStackSize - 2] = 1;
		}
    }

	@Subscribe(
			priority = -99
	)
	public void onGrandExchangeSearched(GrandExchangeSearched event) {
		final String input = client.getVarcStrValue(VarClientStr.INPUT_TEXT);

		if (!config.grandExchangeEnabled() || input.isEmpty() || event.isConsumed()) {
			return;
		}

		event.consume();

		client.setGeSearchResultIndex(0);

		PriorityQueue<Map.Entry<Short, Double>> maxHeap = new PriorityQueue<>(
				(a, b) -> b.getValue().compareTo(a.getValue()) // descending
		);
		for (short i = 0; i < client.getItemCount(); i++) {
			ItemComposition item = itemManager.getItemComposition(i);
			if (!item.isTradeable() || item.getNote() == NOTED_ITEM) {
				continue;
			}

			maxHeap.offer(new SimpleEntry<>(i, config.matchingStrategy().apply(input, item.getName())));
		}
		int numResults = Math.min(maxHeap.size(), MAX_GE_RESULTS);
		short[] itemIds = new short[numResults];
		for (int i = 0; i < numResults; i++) {
			itemIds[i] = maxHeap.poll().getKey();
		}

		client.setGeSearchResultCount(itemIds.length);
		client.setGeSearchResultIds(itemIds);
	}

	@Provides
	SimilaritySearchConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SimilaritySearchConfig.class);
	}
}
