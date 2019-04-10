package sem.mapper;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

public class StanfordParser implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6643629409654290839L;
	private StanfordCoreNLP pipeline;
	public SemanticGraph synGraph;


	public StanfordParser() throws FileNotFoundException, UnsupportedEncodingException{
		this.pipeline = new StanfordCoreNLP(
				PropertiesUtils.asProperties(
						"annotators", "tokenize,ssplit,pos,lemma,ner,depparse,parse,mention,coref,",
						"depparse.extradependencies", "MAXIMAL",
						"tokenize.language", "en"));
	}	
	
	
	
	public SemanticGraph parseOnly(String sentence){
		//ArrayList<SemanticGraph> stanGraphs = new ArrayList<SemanticGraph>();
		Annotation document = new Annotation(sentence.substring(0,sentence.length()-1));
		// run all Annotators on this text
		pipeline.annotate(document);
		// all parsed sentences
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		//stanGraphs.add(sentences.get(0).get(EnhancedPlusPlusDependenciesAnnotation.class));
		//stanGraphs.add(sentences.get(0).get(CollapsedCCProcessedDependenciesAnnotation.class));
		return sentences.get(0).get(EnhancedPlusPlusDependenciesAnnotation.class);
	}
	
	/**
	 * Return the corefs chains of the sentence, as processed by the CoreNLP software.
	 * @param sentence
	 * @return
	 */
	public Collection<CorefChain> getCoreference(String sentence){
		//ArrayList<SemanticGraph> stanGraphs = new ArrayList<SemanticGraph>();
		Annotation document = new Annotation(sentence.substring(0,sentence.length()-1));
		// run all Annotators on this text
		pipeline.annotate(document);
		Collection<CorefChain> corefs = document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values();
		//System.out.println(corefs);
		return corefs;
	}
	

		

	@SuppressWarnings("resource")
	public static void main(String args[]) throws IOException {
		String input = "";
		String inFile = "";
		for (int i = 0; i < args.length; i++) {		
			final String arg = args[i];
			if (args.length >= i + 1) {
				if (arg.equalsIgnoreCase("-inFile")) {
					inFile = args[i + 1];
					File inputFile = new File(inFile);
					input = new Scanner(inputFile).useDelimiter("\\A").next();
				} else if (arg.equalsIgnoreCase("-inString")) {
					input = args[i + 1];
				}
			}
			System.out.println(arg);
		}	
		StanfordParser parser = new StanfordParser();
		//parser.parseTextToConllu(inFile);
		//SemanticGraph graph = parser.parseOnly(input);
		//parser.expandStanfordGraph(graph);
		//System.out.println(graph.toCompactString());
	}


}
