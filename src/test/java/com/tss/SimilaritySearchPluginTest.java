package com.tss;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SimilaritySearchPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SimilaritySearchPlugin.class);
		RuneLite.main(args);
	}
}