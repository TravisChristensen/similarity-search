package com.tss;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.GrandExchangeSearched;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

@Slf4j
@PluginDescriptor(
	name = "Search(++)", description = "Filter your Bank or search the Grand Exchange using a variety of algorithms better than exact match. This also supports searching by common item abbreviations."
)
public class SimilaritySearchPlugin extends Plugin {
	private static final String BANK_SEARCH_FILTER_EVENT_NAME = "bankSearchFilter";
	private static final int MAX_GE_RESULTS = 3*12;
	private static final int NOTED_ITEM = 799;
	private static final HashMap<String, List<String>> ITEM_ABBREVIATIONS;
	static {
        HashMap<String, List<String>> itemAbbreviationsDefered;
        ObjectMapper objectMapper = new ObjectMapper();
		InputStream itemAbbreviationsStream = SimilaritySearchPlugin.class
				.getClassLoader().getResourceAsStream("abbreviation_dictionary.json");
        try {
            itemAbbreviationsDefered = objectMapper.readValue(itemAbbreviationsStream, new TypeReference<HashMap<String, List<String>>>() {});
        } catch (IOException e) {
			itemAbbreviationsDefered = new HashMap<>();
            log.error("Failed to load abbreviation dictionary", e);
        }
        ITEM_ABBREVIATIONS = itemAbbreviationsDefered;
    }

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
		if (input == null || input.isEmpty()) {
			return;
		}

		String itemName = itemManager.getItemComposition(itemId).getName();

		double score = getScore(input, itemName, true);
		if (score >= config.bankFilterThreshold()) {
			intStack[intStackSize - 2] = 1;
		}
    }

	@Subscribe
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

			maxHeap.offer(new SimpleEntry<>(i, getScore(input, item.getName(), false)));
		}
		int numResults = Math.min(maxHeap.size(), MAX_GE_RESULTS);
		short[] itemIds = new short[numResults];
		for (int i = 0; i < numResults; i++) {
			itemIds[i] = maxHeap.poll().getKey();
		}

		client.setGeSearchResultCount(itemIds.length);
		client.setGeSearchResultIds(itemIds);
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (!config.scoreDebug()) {
			return;
		}

		if (event.getScriptId() == ScriptID.GE_ITEM_SEARCH)
		{
			String input = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
			Widget results = client.getWidget(ComponentID.CHATBOX_GE_SEARCH_RESULTS);
			if (results == null) {
				return;
			}
			Widget[] children = results.getDynamicChildren();
			int resultCount = children.length / 3;
			for (int i = 0; i < resultCount; i++) {
				Widget itemNameWidget = children[i * 3 + 1];

				double score = getScore(input, itemNameWidget.getText(), false);
				itemNameWidget.setText(String.format("<col=ff0000>%.2f</col> %s", score, itemNameWidget.getText()));
			}
		}

		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING) {
			String input = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
			if (input.isEmpty()) {
				return;
			}

			Widget bankWidget = client.getWidget(ComponentID.BANK_ITEM_CONTAINER);
			if (bankWidget == null) {
				return;
			}

			for (Widget itemWidget : bankWidget.getDynamicChildren()) {
				if (itemWidget != null && itemWidget.getItemId() >= 0) {
					ItemComposition item = itemManager.getItemComposition(itemWidget.getItemId());
					double score = getScore(input, item.getName(), true);
					itemWidget.setName(String.format("<col=ff0000>%.2f</col> %s", score, itemWidget.getName()));
				}
			}
		}
	}

	@Provides
	SimilaritySearchConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SimilaritySearchConfig.class);
	}

	private double getScore(String input, String itemName, boolean splitItem) {
		input = input.toLowerCase();
		itemName = itemName.toLowerCase();

		List<String> textToScore = new ArrayList<>();
		textToScore.add(itemName);

		String[] itemSplits = itemName.split(" ");

		List<String> itemAbbreviations = ITEM_ABBREVIATIONS.getOrDefault(itemName, new ArrayList<>());
		if (itemAbbreviations.isEmpty()) {
			for (String itemSplit : itemSplits) {
				List<String> splitAbbreviations = ITEM_ABBREVIATIONS.get(itemSplit);
				if (splitAbbreviations != null && !splitAbbreviations.isEmpty()) {
					for (String splitAbbreviation : splitAbbreviations) {
						textToScore.add(itemName.replace(itemSplit, splitAbbreviation));
					}
				}
			}
		}

		textToScore.addAll(itemAbbreviations);

		if (splitItem) {
			textToScore.addAll(Arrays.asList(itemSplits));
		}

		double maxScore = 0.0;
		for (String text : textToScore) {
			double textScore = config.matchingStrategy().apply(input, text);
			maxScore = Math.max(maxScore, textScore);
		}
		return maxScore;
	}
}
