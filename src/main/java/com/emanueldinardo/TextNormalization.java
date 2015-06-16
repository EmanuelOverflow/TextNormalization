package com.emanueldinardo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.language.Soundex;
import org.tartarus.snowball.ext.EnglishStemmer;

/**
 * Normalize single words trying to get its stemmed form and/or if the word is
 * emphasized try to get its true form. It provides two types of normalization:
 * <ul>
 * <li>Phonetic: use phonetic alghoritms (actually only Soundex is supported)
 * and a simple LCS mathing alghoritm to find most similar word</li>
 * <li>Combination: return all combination of possible words</li>
 * </ul>
 * 
 * @author Emanuel Di Nardo
 * @version 0.1
 * @since 0.1
 */
public class TextNormalization {

	boolean mDebug = false;
	Map<String, List<String>> mMappedDoubles;

	/**
	 * Constructor that uses the default dictionary of words with doubles i.e.
	 * hello, cool, apple, essence, toolbar
	 * 
	 * @throws IOException
	 *             FileNotFound or I/O errors
	 * 
	 * @since 0.1
	 */
	public TextNormalization() throws IOException {
		mMappedDoubles = new HashMap<>();
		BufferedReader br = new BufferedReader(new InputStreamReader(getClass()
				.getClassLoader().getResourceAsStream("wordsWithDoubles.txt")));
		fillDoubleWords(br);
		br.close();
	}

	/**
	 * Constructor that uses the provided dictionary of words with doubles i.e.
	 * hello, cool, apple, essence, toolbar
	 * 
	 * @param wordsWithDoublesPath
	 *            Relative or absolute path to file of words
	 * 
	 * @throws IOException
	 *             FileNotFound or I/O errors
	 * 
	 * @since 0.1
	 */
	public TextNormalization(String wordsWithDoublesPath) throws IOException {
		// TODO Auto-generated constructor stub
		mMappedDoubles = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(
				wordsWithDoublesPath));
		fillDoubleWords(br);
		br.close();
	}

	/**
	 * Show debug output
	 * 
	 * @param debug
	 *            Enable/Disable debug
	 * 
	 * @since 0.1
	 */
	public void setDebug(boolean debug) {
		mDebug = debug;
	}

	/**
	 * Fills a dictionary of words with doubles and give them their phonetic
	 * code. Overlapped phonetic words are putted in a list.
	 * 
	 * @param br
	 *            Reader
	 * 
	 * @throws IOException
	 *             I/O errors
	 * 
	 * @since 0.1
	 */
	private void fillDoubleWords(BufferedReader br) throws IOException {
		String line = null;
		Soundex encoder = new Soundex();
		while ((line = br.readLine()) != null) {
			// Avoid problems with BufferedReader capacity per line?
			String[] wordsBuf = line.split(", ");
			for (String word : wordsBuf) {
				String encodedWord = encoder.encode(word);
				if (mMappedDoubles.containsKey(encodedWord)) {
					if (mDebug) {
						System.out.println("Repeated key: ");
						System.out.println("\tKey: " + encodedWord);
						System.out.println("\tOld value: "
								+ mMappedDoubles.get(encodedWord));
						System.out.println("\tNew value: " + word);
					}
					mMappedDoubles.get(encodedWord).add(word);
				} else {
					List<String> words = new ArrayList<>();
					words.add(word);
					mMappedDoubles.put(encodedWord, words);
				}
			}
		}
	}

	/**
	 * Normalize a word. It can normalize in the following ways:
	 * <ul>
	 * <li>Unemphatize: heeeellloooo = hello | stem=false</li>
	 * <li>Unemphatize and stem: occcccccuring = occur | stem=true</li>
	 * <li>Stem: if a word is already unpemphatized, barring = bar | stem=true</li>
	 * <li>Nothing: if a word is already unpemphatized | stem=false</li>
	 * </ul>
	 * 
	 * @param token
	 *            Word to normalize
	 * 
	 * @param stem
	 *            Enable/Disable stemming
	 * 
	 * @return Normalized string
	 * 
	 * @since 0.1
	 */
	public String normalize(String token, boolean stem) {
		EnglishStemmer stemmer = new EnglishStemmer();
		String biasedString = token.toLowerCase().replaceAll("[\\W]", "");
		// i.e. barring = bar, occuring = occur, patrolling = patrol
		// stemmer.setCurrent(biasedString);
		// stemmer.stem();
		// biasedString = stemmer.getCurrent();
		// find repeated letters
		Matcher matcherString = Pattern.compile("(.)\\1+\\1").matcher(
				biasedString);
		List<String> groups = new ArrayList<>();
		while (matcherString.find()) {
			groups.add(matcherString.group());
		}
		// if repeated letters exist
		if (!groups.isEmpty()) {
			String normalizedString = biasedString;
			String unbiasedString = biasedString;
			// preprocess string, i.e heeeelllloooo
			// normalized: helo
			// unbiased: heelloo
			// biased: heeelllooo
			for (String group : groups) {
				normalizedString = normalizedString.replace(group,
						group.subSequence(0, 1));
				unbiasedString = unbiasedString.replace(group,
						group.subSequence(0, 2));
				biasedString = biasedString.replace(group,
						group.substring(0, 3));
			}
			Soundex filter = new Soundex();
			String encodedString = filter.encode(unbiasedString);
			// encode phonetic string
			if (mMappedDoubles.containsKey(encodedString)) {
				String result = "";
				int lastMatchLength = 0;
				for (String word : mMappedDoubles.get(encodedString)) {
					String res = org.apache.commons.collections4.ListUtils
							.longestCommonSubsequence(word, unbiasedString);
					if (mDebug) {
						System.out.println("word: " + word + " res: " + res
								+ " len: " + res.length());
						System.out.println("\tcurrent result: " + result);
					}
					if (res.length() > 3 && res.length() > lastMatchLength) {
						result = word;
						lastMatchLength = res.length();
					}
				}
				// if match doesn't exist maybe token is a non double-letter
				// word
				if (result.isEmpty()) {
					if (stem) {
						stemmer.setCurrent(normalizedString);
						stemmer.stem();
						result = stemmer.getCurrent();
					} else {
						result = normalizedString;
					}
				} else if (stem) {
					// token is a double-letter word
					stemmer.setCurrent(result);
					stemmer.stem();
					result = stemmer.getCurrent();
				}
				if (mDebug) {
					System.out.println();
					System.out.println("Normalized: " + result);
					System.out.println("unBiased: " + unbiasedString);
					System.out.println("Biased: " + biasedString);
				}
				return result;
			} else if (stem) {
				stemmer.setCurrent(normalizedString);
				stemmer.stem();
				return stemmer.getCurrent();
			} else {
				return normalizedString;
			}
		} else if (stem) {
			return biasedString;
		} else {
			return token;
		}
	}

	/**
	 * Normalize a word. It can normalize in the following ways:
	 * <ul>
	 * <li>Unemphatize: hellloooo = helo, heloo, helloo, hello | stem=false</li>
	 * <li>Unemphatize and stem: occcurrrring = ocur, occur, ocurr, occurr |
	 * stem=true</li>
	 * <li>Stem: if a word is already unpemphatized, barring = bar | stem=true</li>
	 * <li>Nothing: if a word is already unpemphatized | stem=false</li>
	 * </ul>
	 * 
	 * @param token
	 *            Word to normalize
	 * 
	 * @param stem
	 *            Enable/Disable stemming
	 * 
	 * @return Normalized string
	 * 
	 * @since 0.1
	 */
	public static String[] normalizeCombination(String token, boolean stem) {
		EnglishStemmer stemmer = new EnglishStemmer();
		String biasedString = token.toLowerCase().replaceAll("[\\W]", "");
		// i.e. barring = bar, occuring = occur, patrolling = patrol
		// stemmer.setCurrent(biasedString);
		// stemmer.stem();
		// biasedString = stemmer.getCurrent();
		Matcher matcherString = Pattern.compile("(.)\\1+\\1").matcher(
				biasedString);
		List<String> groups = new ArrayList<>();
		Set<String> possibilities = new HashSet<>();
		while (matcherString.find()) {
			groups.add(matcherString.group());
		}
		if (!groups.isEmpty()) {
			String unbiasedString = biasedString;
			String totwo = biasedString;
			for (String group : groups) {
				if (group.length() > 3) {
					unbiasedString = unbiasedString.replace(group,
							group.substring(0, 3));
				}
				totwo = totwo.replace(group, group.substring(0, 2));
			}
			if (stem) {
				stemmer.setCurrent(totwo);
				stemmer.stem();
				possibilities.add(stemmer.getCurrent());
			} else {
				possibilities.add(totwo);
			}
			for (int i = 0; i < groups.size(); ++i) {
				String group = groups.get(i).substring(0, 2);
				String reduction = totwo
						.replaceFirst(group, group.substring(1));
				if (stem) {
					stemmer.setCurrent(reduction);
					stemmer.stem();
					possibilities.add(stemmer.getCurrent());
				} else {
					possibilities.add(reduction);
				}
				for (String g : groups.subList(i, groups.size())) {
					g = g.substring(0, 2);
					String reduct = reduction.replaceFirst(g, g.substring(1));
					if (stem) {
						stemmer.setCurrent(reduct);
						stemmer.stem();
						possibilities.add(stemmer.getCurrent());
					} else {
						possibilities.add(reduct);
					}
				}
			}
			return possibilities.toArray(new String[] {});
		} else if (stem) {
			stemmer.setCurrent(biasedString);
			stemmer.stem();
			return new String[] { stemmer.getCurrent() };
		} else {
			return new String[] { token }; // Token doesn't need normalization
		}
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		TextNormalization tn = new TextNormalization();
		while (!(line = br.readLine()).equals("q")) {
			System.out.println("Normalize phonetic: "
					+ tn.normalize(line, true));
			System.out.println("Normalize combination: ");
			for (String s : TextNormalization.normalizeCombination(line, true)) {
				System.out.println("\t" + s);
			}
		}

	}
}
