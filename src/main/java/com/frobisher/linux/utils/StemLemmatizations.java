package com.frobisher.linux.utils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static edu.washington.cs.knowitall.morpha.MorphaStemmer.stem;

/*
 * author: darcy
 * date: 2018/1/15 21:39
 * description: 
*/
public class StemLemmatizations {

	public static Properties props = new Properties();
	private static StanfordCoreNLP pipeline;


	static {
		props.put("annotators","tokenize, ssplit, pos, lemma");
		pipeline = new StanfordCoreNLP(props, /*true*/ false);
	}


	/**
	 * 词干提取，词形还原。
	 * @param line
	 * @return
	 */
	public static String stemLemmatization(String line) {

		Annotation document = pipeline.process(line);

		StringBuilder sb = new StringBuilder();
		for(CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class))
		{
			for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class))
			{
				String word = token.get(CoreAnnotations.TextAnnotation.class);
				String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
				sb.append(lemma.toLowerCase() + " ");
			}
		}
		return stem(sb.toString());
	}

	/**
	 * 词形还原。
	 * @param line
	 * @return
	 */
	public static String lemmatization(String line) {

		Annotation document = pipeline.process(line);

		StringBuilder sb = new StringBuilder();
		for(CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class))
		{
			for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class))
			{
				String word = token.get(CoreAnnotations.TextAnnotation.class);
				String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
				sb.append(lemma.toLowerCase() + " ");
			}
		}
		return sb.toString();
	}

	/**
	 * 词干提取，词形还原。
	 * @param line
	 * @return
	 */
	public static String stemLemmatization1(String line) {
		/*true*/
		String text = "runners cats utilities factionally happily written used"; /* the string you want */;

		text = "In other words, Francis wants a more decentralized church and wants to hear reform ideas from small communities that sit far from Catholicism's power centers, Bellitto said.";
		text = "cats can could running ran  runs cactus cactuses cacti community communities," +
				" In other words, Francis wants a more decentralized church and wants to hear reform ideas from small communities that sit far from Catholicism's power centers, Bellitto said.";

		Annotation document = pipeline.process(line);

//		System.out.printf("%-40s%-40s\n", "text:",  line);
		StringBuilder sb = new StringBuilder();
//		IntStream.rangeClosed(1, 40).forEach(i -> System.out.print(" "));
		for(CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class))
		{
			for(CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class))
			{
				String word = token.get(CoreAnnotations.TextAnnotation.class);
				String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
//				String stem = token.get(StemAnnotation.class);
//				System.out.println("lemmatized version :" + lemma);
//				System.out.println("stem version:" + stem);
//				System.out.print(lemma + " ");
				sb.append(lemma.toLowerCase() + " ");
			}
		}
//		System.out.println();
//		System.out.printf("%-40s%-30s\n", "stem(text):" , stem(line.toLowerCase()));
		// System.out.printf("%-40s%-30s\n", "stem(sb.toString()):", stem(sb.toString()));
//		System.out.printf("%-40s%-30s\n", "MorphaStemmer.morpha(sb.toString()", MorphaStemmer.morpha(line, false));
		return stem(sb.toString());
	}


	public static void main(String[] args) {
		List<String> list = Arrays.asList("runners cats utilities factionally happily written used improvement",
				"In other words, Francis wants a more decentralized church and wants to hear" +
						" reform ideas from small communities that sit far from Catholicism's power centers, Bellitto said.",
				"cats can could running ran  runs cactus cactuses cacti community communities," +
						" In other words, Francis wants a more decentralized church and wants to hear" +
						" reform ideas from small communities that sit far from Catholicism's power centers, Bellitto said.",
				"fishing fishs fished stemming stemmer stems stemmed argue argued argues arguing argus");
		for (String line : list) {
			System.out.println(stem(line.toLowerCase()));
			System.out.println(stemLemmatization(line));
			System.out.println(lemmatization(line));
			System.out.println();
		}


	}

}
