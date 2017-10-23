package semantic.mapper;

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

	/**
	 * Parse a text: split, tokenize, pos-tag and parse with the enhanced dependencies.
	 * Then, add the senses of each word retrieved out of PWN. If necessary,
	 * rewrite some of the senses. Write it all as an xml file.
	 * @param input
	 * @return
	 * @throws IOException 
	 */
	/*public SemanticGraph parseTextToJson(String input) throws IOException{
		//StringBuffer xmlString = new StringBuffer();
		StringBuffer jsonString = new StringBuffer();
		// Create mapper
		SenseMappingsRetriever mapper = new SenseMappingsRetriever();
		// Create rewritter
		SensesRewritter rewritter = new SensesRewritter();
		//String outputFile = inFile.getParent()+"/"+inFile.getName()+".parsed.xml";
		String outputFile = "output.parsed.xml";
		PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
		//xmlString.append("<corpus>\n<title>SICK unique sentences</title>");
		Annotation document = new Annotation(input);
		// run all Annotators on this text
		pipeline.annotate(document);	
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		int sentNum = 1;
		// go through each sentence of the text
		for(CoreMap sentence: sentences) {
			//xmlString.append("\n<sentence id=\""+sentNum+"\" text=\""+sentence.toString()+"\">\n");
			jsonString.append("{\"sentence\":{\"id\":\""+sentNum+"\", \"text\":\""+sentence.toString()+"\",\"lexemes\":[");
			int lexeme = 0;
			sentNum ++;
			// get the enhanced dependencies of the sentence
			SemanticGraph enhPlusDep = sentence.get(EnhancedPlusPlusDependenciesAnnotation.class);
			this.synGraph = enhPlusDep;
			//System.out.println(enhPlusDep.descendants(vertex));
			HashMap<String, String> rewrittenCompounds =  rewritter.rewriteCompoundsForJson(enhPlusDep.toList());
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
				lexeme ++;
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				// this is the lemma
				String lemma = token.get(LemmaAnnotation.class);
				String senses = "";
				try {
					// get the senses of the token
					senses = mapper.extractPWNSensesToText(lemma,pos);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// if necessary, rewrite the senses
				String rewrittenSenses = rewritter.rewriteMonoSenses(word,lemma,pos,enhPlusDep.toList());
				if (!rewrittenSenses.equals(""))
					senses = rewrittenSenses;
				// write the output as xml
				//xmlString.append("<lexeme id=\""+lexeme+"\" lemma=\""+lemma+"\" pos=\""+pos+"\">"+word);
				jsonString.append("{\"id\":\""+lexeme+"\",\"lemma\":\""+lemma+"\",\"pos\":\""+pos+"\",\"text\":\""+word+"\",");
				jsonString.append(senses+"},");
			}	  
			jsonString.deleteCharAt(jsonString.length()-1);
			//xmlString.append("<synDependencies id=\"Stanford\">\n"+enhPlusDep.toList()+"</synDependencies>\n");
			jsonString.append("],\"synDependencies\":{");
			HashMap<String,ArrayList<String>> mapOfDep = new HashMap<String,ArrayList<String>>();
			for (String dep : enhPlusDep.toList().split("\n")){
				ArrayList<String> list = new ArrayList<String>();
				String label = rewritter.getDependencyLabelOfDependencyRelation(dep);
				String head = rewritter.getHeadOfDependencyRelation(dep);
				String dependent = rewritter.getDependentOfDependencyRelation(dep);
				String value = "\"head\":\""+head+"\",\"dependent\":\""+dependent+"\"";
				list.add(value);
				if (!mapOfDep.containsKey(label)){
					mapOfDep.put(label,list);
				} else{
					ArrayList<String> currentValue = mapOfDep.get(label);
					currentValue.addAll(list);
					mapOfDep.put(label, currentValue);
				}
			}
			for (String key: mapOfDep.keySet()){
				jsonString.append("\""+key+"\":[");
				for (String value : mapOfDep.get(key)){
					jsonString.append("{"+value+"},");
				}
				jsonString.deleteCharAt(jsonString.length()-1);
				jsonString.append("],");
			}

			jsonString.deleteCharAt(jsonString.length()-1);
			jsonString.append("}");
			//xmlString.append("<compounds>\n");
			jsonString.append(",\"compounds\":[[");
			for (String key: rewrittenCompounds.keySet()){
				//xmlString.append(key+rewrittenCompounds.get(key)+"</compound>\n");
				jsonString.append(key+rewrittenCompounds.get(key)+"},");
			}
			//xmlString.append("</compounds>\n</sentence>");
			jsonString.deleteCharAt(jsonString.length()-1);
			jsonString.append("]}}");
		}
		//xmlString.append("\n</corpus>");
		writer.write(jsonString.toString());
		writer.close();
		return synGraph;// jsonString.toString();
	}

	/**
	 * Parses a file which contains one sentence per line: split, tokenize, pos-tag and 
	 * parse with the enhanced dependencies.
	 * Then, adds the senses of each word retrieved out of JIGSAW. JIGSAW
	 * creates a temporary file with the senses (tmp.senses) found in the dir of this class.
	 * This temporary file is then read through to extract the senses for each word. 
	 * IMPORTANT: inFile: file with one sentence per line. 
	 * The output is in the conllu format and is written in the same dir as this class. The 
	 * senses are written in the 10th position of conllu. 
	 * @param inFile: a file with one sentence per line
	 * @return
	 * @throws IOException 
	 */
	/*public void parseTextToConllu(String inFile) throws IOException{
		String outputFileTokenized = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK_rest6000.txt";
		/*PrintWriter writerTokenizer = new PrintWriter(outputFileTokenized, "UTF-8");
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(inFile), new CoreLabelTokenFactory(), "");
	      while (ptbt.hasNext()) {
	        CoreLabel label = ptbt.next();
	        writerTokenizer.write(label.toString()+"\n");
	      }*/
		// Create rewritter
		/*SensesRewritter rewritter = new SensesRewritter();
		File inputFile = new File(inFile);
		String input = new Scanner(inputFile).useDelimiter("\\A").next();
		String outputFile = "enhanced.output.conllu";
		PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
		// access JIGSAW and extract senses
		String[] arguments = new String[] {"-i", outputFileTokenized, "-cf", "resources/jigsaw.properties",  "-o", "tmp.senses", "-m", "tokenized"};
		JIGSAW.main(arguments);
		Annotation document = new Annotation(input);
		// run all Annotators on this text
		pipeline.annotate(document);
		// all parsed sentences
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		// write them in conllu
		CoNLLUOutputter.conllUPrint(document, new FileOutputStream("output_tmp.conluu"));
		// read the just created conllu file line by line
		List<String> conlluToList = Files.readAllLines(new File("output_tmp.conluu").toPath(), Charset.defaultCharset() );
		// read the jsut created senses file line by line
		List<String> sensesToList = Files.readAllLines(new File("tmp.senses").toPath(), Charset.defaultCharset() );
		// counter for the senses file (no empty lines between sentences)
		int s = 0;
		// counter for conllu file (empty line between sentences)
		int c = s;
		// counter for sentences list
		int g = 0;
		// go through each line of the senses file
		for ( s = 0; s < sensesToList.size(); s++){	
			// get the senses column of each line
			String senses = sensesToList.get(s).split("\\s")[4];
			// get the corresponding line of the conllu file
			String conlluLine = conlluToList.get(c);
			/* if it is an empty line, go to the next conllu line (c++), move to the next
			sentence of the list (g++), write an empty line in the overall output file */
			/*if (conlluLine.isEmpty()){
				System.out.println("Line "+c);
				c ++;
				g++;
				conlluLine = conlluToList.get(c);
				
				writer.write("\n");
			}
			// get the node of the graph corresponding to the current token
			IndexedWord node = sentences.get(g).get(EnhancedPlusPlusDependenciesAnnotation.class).getNodeByWordPattern(conlluToList.get(c).split("\t")[1]);
			StringBuffer stB = new StringBuffer();
			// for testing:
			synGraph = sentences.get(g).get(EnhancedPlusPlusDependenciesAnnotation.class);
			// get the edges involving this node
			List<SemanticGraphEdge> edges = sentences.get(g).get(EnhancedPlusPlusDependenciesAnnotation.class).getIncomingEdgesSorted(node);
 			if (edges.isEmpty()){
				stB.append("_");
			} else{
				// for each of those edges, get the parent id and the relation to the current node
				for (SemanticGraphEdge edge : edges){
					IndexedWord parent = edge.getGovernor(); 
					Double pId = parent.pseudoPosition();
					GrammaticalRelation relation = edge.getRelation();
					if (relation.getShortName().contains("compound")){
						senses = rewritter.rewriteCompound(parent, edge.getDependent());
					}
					// append it to a string buffer
					stB.append(pId.intValue() + ":"+relation+"|");
				}
				stB.deleteCharAt(stB.length()-1);
			}
			String pos = node.tag();
			String morpho = getMorphFeatures(node.originalText(),pos);
			String depLabel = conlluToList.get(c).split("\t")[7];
			// rewrite some of the senses
			/*String rewrittenSenses = rewritter.rewriteMonoSenses(node.originalText(),node.lemma(),pos, depLabel);
			if (!rewrittenSenses.equals(""))
				senses = rewrittenSenses;*/
			
			// make final adjustments of the current conllu line and write it to the overall output file
			/*String replaced = conlluLine.substring(0,conlluLine.length()-3) +stB;
			String replaced2 = replaced.substring(0,replaced.length()) +"\t"+senses;
			int endOfMorph = replaced2.indexOf(replaced2.split("\t")[4])+replaced2.split("\t")[4].length();
			String replaced3 = replaced2.substring(0,endOfMorph)+"\t"+
			morpho+"\t"+replaced2.substring(endOfMorph+3);
			writer.write(replaced3+"\n");
			c++;
		}
		writer.close();
	}*/
	
	
	public void expandStanfordGraph(SemanticGraph stanGraph){
		JIGSAW jigsaw = new JIGSAW(new File("resources/jigsaw.properties"));
		TokenGroup tg;
		try {
			tg = jigsaw.mapText(stanGraph.toRecoveredSentenceString());
			for (int j = 1; j <= stanGraph.size(); j++){
				IndexedWord currentNode = stanGraph.getNodeByIndex(j);
				String word = tg.get(j-1).getToken();
				String lemma = tg.get(j-1).getLemma();
				String posTag =tg.get(j-1).getPosTag();
				String synset = tg.get(j-1).getSyn();
				IndexedWord indWord = new IndexedWord(null,0,stanGraph.size());
				indWord.setLemma(synset);
				indWord.setOriginalText(synset);
				indWord.setTag(posTag);
				indWord.setWord(synset);
				indWord.setValue(synset);
				stanGraph.addVertex(indWord);
				stanGraph.addEdge(currentNode, indWord, GrammaticalRelation.valueOf("synset"), 0, false);
				
				ISynset syn;
			
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
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
	 * Returns the morpho features of the given pos. The morpho features are only extracted
	 * from the information from the POS tag and dont involve any deeper processing.
	 * @param word
	 * @param pos
	 * @return
	 */
	public String getMorphFeatures(String word, String pos){
		String morph = "_";
		if (pos.startsWith("N")){
				if (pos.equals("NN") || pos.equals("NNP"))
					morph = "Number=Sing";
				else if (pos.equals("NNS") || pos.equals("NNPS"))
					morph = "Number=Plur";				 
		} else if (pos.startsWith("V")){
			if (pos.equals("VBN"))
				morph= "Tense=Past";
			else if (pos.equals("VBP"))
				morph = "Tense=Pres";
			else if (pos.equals("VBZ"))
				morph = "Person=3|Tense=Pres";
		} else if (pos.equals("WP$") || pos.equals("PRP$")){
			morph = "Poss=Yes";
		} else if (pos.equals("RBR") || pos.equals("JJR")){
			morph = "Degree=Cmp";
		} else if (pos.equals("RBS") || pos.equals("JJS")){
			morph = "Degree=Sup";
		} else if (pos.equals("DT")){
			if (word.equalsIgnoreCase("a"))
				morph = "Definite=Ind";
			else if (word.equalsIgnoreCase("the"))
				morph = "Definite=Def";
		} else if (pos.equalsIgnoreCase("PRP")){
			morph = "PronType=Prs";
		} else if (pos.equalsIgnoreCase("PRP$")){
			morph = "Poss=Yes|PronType=Prs";
		} else if (pos.equalsIgnoreCase("WP")){
			morph = "PronType=Int";
		} else if (pos.equalsIgnoreCase("WP$")){
			morph = "Poss=Yes|PronType=Int";
		}
			
		return morph;
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
