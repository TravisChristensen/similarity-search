# Search(++)
### Search your bank or the Grand Exchange with a variety of algorithms that are better than the default exact match. Common item abbreviations are also supported.

## Configs
### Matching Strategy
* Bigram Cosine
  * Counts and compares the frequency of character bigrams in given text. (iban's staff is treated as ib, ba, an, n', 's, s , st, ta, af, ff) More sensitive to typos but order of words matter less. Frequency of the pairs is what similarity is based on.
* Jaro Wrinkler
  * Measures how many characters match between two strings, but it gives extra weight if the beginning of the strings are similar. The closer the match, especially at the start, the higher the similarity score.
* Jaccard
  * How similar two sets of characters is determined by looking at how much they overlap. It checks what percentage of items are shared between the two sets. The more they have in common, the higher the similarity score.
* Levenshtein
  * Calculates the number of changes (insertions, deletions, or substitutions) needed to turn one string into another. Fewer changes mean the strings are more similar.

All scores from each algorithm are normalized to a range of 0 to 100.

### Bank Filter Threshold %
When filtering your bank, this value is the threshold an item's similarity score needs to meet to show up. Similarity scoring applies mostly after the default exact matching doesn't find anything.

### Enable for Bank
Toggles similarity scoring when filtering your bank.

### Enable for Grand Exchange
Toggles similarity scoring when searching the Grand Exchange. When enabled, the Grand Exchange results box will have at most 36 items, ordered in descending order of similarity score.

### Enable score debug
Check this to view the computed similarity score in front of item names. In the Grand Exchange results, it appears right before the item name. In Bank filtering, it appears before the item name when you hover over it. This is useful for tuning the bank filter threshold for your chosen algorithm.