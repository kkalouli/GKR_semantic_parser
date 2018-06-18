package sem.mapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.util.IOUtils;

import edu.stanford.nlp.coref.data.DocumentPreprocessor;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoNLLUOutputter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.EnhancedPlusPlusDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphFormatter;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreePrint;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.PropertiesUtils;
import jigsaw.JIGSAW;
import jigsaw.data.Token;
import jigsaw.data.TokenGroup;
import edu.mit.jwi.item.ISynset;

public class StanfordParser {

	private StanfordCoreNLP pipeline;
	public SemanticGraph synGraph;


	public StanfordParser() throws FileNotFoundException, UnsupportedEncodingException{
		this.pipeline = new StanfordCoreNLP(
				PropertiesUtils.asProperties(
						"annotators", "tokenize,ssplit,pos,lemma,depparse",
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
