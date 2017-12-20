package sem.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ILexFile;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import jigsaw.JIGSAW;
import jigsaw.data.TokenGroup;

public class SenseMappingsRetriever {
	
	HashMap<String,String> hashOfPOS = new HashMap<String,String>();
	public Map<String, Integer> subConcepts = new HashMap<String,Integer>();
	public Map<String, Integer> superConcepts =new HashMap<String,Integer>();
	
	public SenseMappingsRetriever(){
		/* fill hash with the POS tags of the Penn treebank. The POS
		have to be matched to the generic POS used in SUMO and PWN.  */
		hashOfPOS.put("JJ","ADJECTIVE");
		hashOfPOS.put("JJR","ADJECTIVE");
		hashOfPOS.put("JJS","ADJECTIVE");
		hashOfPOS.put("MD","VERB");
		hashOfPOS.put("NN","NOUN");
		hashOfPOS.put("NNP","NOUN");
		hashOfPOS.put("NNPS","NOUN");
		hashOfPOS.put("NNS","NOUN");
		hashOfPOS.put("RB","ADVERB");
		hashOfPOS.put("RBR","ADVERB");
		hashOfPOS.put("RBS","ADVERB");
		hashOfPOS.put("VB","VERB");
		hashOfPOS.put("VBD","VERB");
		hashOfPOS.put("VBG","VERB");
		hashOfPOS.put("VBN","VERB");
		hashOfPOS.put("VBP","VERB");
		hashOfPOS.put("VBZ","VERB");
	}

	/**
	 * Reads a conllu file, which has an empty 10th position and
	 * fills that position with the senses from SUMO and PWN.
	 * @param inFile
	 * @throws IOException
	 */
	public void readConlluAndMapSenses(File inFile) throws IOException {
		String outputFile = inFile.getParent()+"/"+inFile.getName()+".senseMapped";
		// opens the input file
		BufferedReader br = new BufferedReader(new InputStreamReader (new FileInputStream(inFile), "UTF-8"));
		// opens the output file
		PrintWriter writer = new PrintWriter(outputFile, "UTF-8");		
		String strLine;
		int counter = 0;
		// read file line by line
		while ((strLine = br.readLine()) != null) { 
			String toWrite = "";
			if (!strLine.equals("")){
				// split the line in each features
				String[] columns = strLine.split("\t");
				String lemma = columns[2];
				String posInitial = columns[4];
				//toWrite = extractSUMOSenses(strLine,posInitial,lemma);
				toWrite = extractPWNSensesToConllu(strLine,lemma,posInitial);
				writer.println(toWrite);
			} else {
				counter++;
				System.out.println("Writing sentence "+counter);
				writer.println(strLine);
			}		
		}
		br.close();
		writer.close();
	}
	
	/**
	 * Extracts the senses for a given lemma and a pos. The senses are extracted and 
	 * are formatted as an xml element.
	 * @param lemma
	 * @param posInitial
	 * @return
	 * @throws IOException
	 */
	public String extractPWNSensesToText(String lemma, String posInitial) throws IOException {
		String toWrite = "";
		// if the word is one of those having a sense within PWN
		if (hashOfPOS.containsKey(posInitial)){
			ArrayList<String> senses = null;
			try {
				// access and retrieve the senses
				senses = accessPWNDBAndExtractSenses(lemma, posInitial);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// write the output to a string buffer
			StringBuffer sensesToString = new StringBuffer();
			sensesToString.append("\"senses\":[");
			if (senses != null){
				for (String sense : senses){
					// for xml
					//sensesToString.append("\n<sense "+sense+"</sense>");
					// for json
					sensesToString.append(sense+",");
				}
			}
			// for xml
			//sensesToString.append("</senses>");
			// for json
			sensesToString.deleteCharAt(sensesToString.length()-1);
			sensesToString.append("]");
			toWrite = sensesToString.toString();
		} else {
			// for xml
			//toWrite = "<senses></senses>";
			// for json
			toWrite = "\"senses\":[]";
		}
		return toWrite;
	}
	
	/**
	 * Extracts the senses based on a given lemma and pos. The senses are extracted and
	 * formatted as the 10th position of a conllu file.
	 * @param strLine
	 * @param lemma
	 * @param posInitial
	 * @return
	 */
	public String extractPWNSensesToConllu(String strLine, String lemma, String posInitial){
		String toWrite = "";
		// if the word is one of those having a sense within PWN
		if (hashOfPOS.containsKey(posInitial)){
			ArrayList<String> senses = null;
			try {
				// access and retrieve the senses
				senses = accessPWNDBAndExtractSenses(lemma, posInitial);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// write the output
			String sensesToString = "PWN="+senses;
			toWrite = strLine.substring(0,strLine.length()-1)+sensesToString;
		} else {
			toWrite = strLine.substring(0,strLine.length()-1)+"PWN=[?]";
		}
		return toWrite;
	}
	
	/**
	 * Access the PWN DB and retrieves the senses of the given lemma and pos.
	 * All senses are retrieved, meaning all synsets to which a word belongs are retrieved.
	 * They are formatted as a list of xml elements, i.e. each sense is a "sense" xml 
	 * element with id, gloss, example and lexical file.
	 * @param lemma
	 * @param posInitial
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> accessPWNDBAndExtractSenses(String lemma, String posInitial) throws IOException {
		ArrayList<String> listOfSenses = new ArrayList<String>();
		// constructs the URL to the Wordnet dictionary directory
		URL url = new URL("file", null, "/usr/local/Cellar/wordnet/3.1/dict");	
		// constructs the dictionary object and opens it
		IDictionary dict = new Dictionary(url);
		dict.open ();
		// get the word within PWN
		IIndexWord idxWord = dict.getIndexWord(lemma, POS.valueOf(hashOfPOS.get(posInitial)));
		// if the word is found,
		if (idxWord != null) {
			// get all its synsets
		    for (IWordID wordID : idxWord.getWordIDs()){
		    	IWord word = dict.getWord (wordID);
		    	ISynset synset = word.getSynset();
		    	// get gloss and example 
		    	String glossAndEx = synset.getGloss();
		    	String gloss="";
		    	String example = "";
		    	// separate gloss from example
		    	if (glossAndEx.contains("; \"")) {
		    		gloss = synset.getGloss().substring(0,synset.getGloss().indexOf("; \""));
		    		example = synset.getGloss().substring(synset.getGloss().indexOf("; \"")+2,synset.getGloss().length());
		    	} else {
		    		gloss = glossAndEx;
		    	}
		    	// get lexical file to which the word belongs
		    	String lexFile = synset.getLexicalFile().toString();
		    	String id = synset.getID().toString();
		    	String senseToWrite = id;
		    	// for json format
		    	//String senseToWrite= "{\"id\":\""+id+ "\",\"gloss\":\""+gloss.replace("\"","")+"\",\"example\":[\""+example.replace(";",",").replace("\"","")+"\"],\"lexFile\":\""+lexFile+"\"}";
		    	// for xml format
		    	//String senseToWrite= "id=\""+id+ "\" gloss=\""+gloss+"\" example=\'["+example.replace(";",",")+"]\' lexFile=\""+lexFile+"\"";
		    	listOfSenses.add(senseToWrite);
		    }
		} else {
			listOfSenses = null;
		}
	    
		dict.close();
	    return listOfSenses;
		
	}
	
	/**
	 * Gets the synonyms of the word lemma with the corresponding posInitial
	 * @param lemma
	 * @param posInitial
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> getSynonyms(String lemma, String posInitial) throws IOException {
		ArrayList<String> listOfSenses = new ArrayList<String>();
		// constructs the URL to the Wordnet dictionary directory
		URL url = new URL("file", null, "/usr/local/Cellar/wordnet/3.1/dict");	
		// constructs the dictionary object and opens it
		IDictionary dict = new Dictionary(url);
		dict.open ();
		// get the word within PWN
		IIndexWord idxWord = dict.getIndexWord(lemma, POS.valueOf(hashOfPOS.get(posInitial)));
		// if the word is found,
		if (idxWord != null) {
			// get all its synsets
		    for (IWordID wordID : idxWord.getWordIDs()){
		    	IWord word = dict.getWord (wordID);
		    	ISynset synset = word.getSynset();
		    	for( IWord w : synset.getWords()){
		    		listOfSenses.add(w.getLemma());
		    	}
		    }
		} else {
			listOfSenses = null;
		}   
		dict.close();
	    return listOfSenses;
	}
	
	/**
	 * Gets the synonyms of the specified synset of the word lemma with the corresponding pos
	 * @param lemma
	 * @param posInitial
	 * @return
	 * @throws IOException
	 */
	public ArrayList<ArrayList<String>> getLexRelationsOfSynset(String lemma, String synsetID, String pos,
			ArrayList<ArrayList<String>> listWithLexRelations) throws IOException {
		// the lex relations will be stored in a big list which contains the lists of each relation
		ArrayList<String> synonyms = new ArrayList<String>();
		ArrayList<String> hypernyms = new ArrayList<String>();
		ArrayList<String> hyponyms = new ArrayList<String>();
		ArrayList<String> antonyms = new ArrayList<String>();
		// constructs the URL to the Wordnet dictionary directory
		URL url = new URL("file", null, "/usr/local/Cellar/wordnet/3.1/dict");	
		// constructs the dictionary object and opens it
		IDictionary dict = new Dictionary(url);
		dict.open ();
		// look up the given word within dict
		IIndexWord idxWord = dict.getIndexWord (lemma, POS.valueOf(pos));
		// if the word is found:
		if (idxWord != null) {
			List<IWordID> wordIDs = idxWord.getWordIDs();
			for (IWordID id : wordIDs){
				// from all ids (=synsets) only take the one that is the same as the specified synset
				if (id.getSynsetID().toString().substring(4,id.getSynsetID().toString().lastIndexOf("-")).equals(synsetID)){
					IWord word = dict.getWord(id);
					ISynset synset = word.getSynset();
					// iterate over words associated with the synset and get the synonyms
					for(IWord w : synset.getWords()){
						synonyms.add(w.getLemma());
					}
					// get the hypernyms
					for(ISynsetID sid : synset.getRelatedSynsets(Pointer.HYPERNYM)){
						List <IWord > words ;
				    	words = dict.getSynset(sid).getWords();
				    	for(Iterator<IWord>i = words.iterator(); i.hasNext() ;) {
				    		hypernyms.add(i.next().getLemma());
				    	 }
			    	}
					// get the hyponyms
					for(ISynsetID sid : synset.getRelatedSynsets(Pointer.HYPONYM)){
						List <IWord > words ;
				    	words = dict.getSynset(sid).getWords();
				    	for(Iterator<IWord>i = words.iterator(); i.hasNext() ;) {
				    		hyponyms.add(i.next().getLemma());
				    	 }
			    	}
					/* get the antonyms (for the lexical relations we need the method getRelatedWords
					instead of getRelatedSynsets() */
					for(IWordID w : word.getRelatedWords(Pointer.ANTONYM)){
						IWord anto = dict.getWord(w);		
						antonyms.add(anto.getLemma());
					}
					break;
				}
			}
		} else {
			synonyms.add(lemma+" not found in PWN");
		}
		dict.close();
		// add all relations list to the big list
		listWithLexRelations.add(synonyms);
		listWithLexRelations.add(hypernyms);
		listWithLexRelations.add(hyponyms);
		listWithLexRelations.add(antonyms);
		return listWithLexRelations;	
	}
	
	/**
	 * Gets the hypernyms of the word lemma with the corresponding posInitial.
	 * @param lemma
	 * @param posInitial
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> getPWNHypernyms(String lemma, String posInitial) throws IOException {
		ArrayList<String> listOfSenses = new ArrayList<String>();
		// constructs the URL to the Wordnet dictionary directory
		URL url = new URL("file", null, "/usr/local/Cellar/wordnet/3.1/dict");	
		// constructs the dictionary object and opens it
		IDictionary dict = new Dictionary(url);
		dict.open ();
		// get the word within PWN
		IIndexWord idxWord = dict.getIndexWord(lemma, POS.valueOf(hashOfPOS.get(posInitial)));
		// if the word is found,
		if (idxWord != null) {
			// get all its synsets
		    for (IWordID wordID : idxWord.getWordIDs()){
		    	IWord word = dict.getWord (wordID);
		    	ISynset synset = word.getSynset();
		    	List<ISynsetID>hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
		    	List <IWord > words ;
		    	
		    	for(ISynsetID sid : hypernyms){
			    	words = dict.getSynset(sid).getWords();
			    	for(Iterator<IWord>i = words.iterator(); i.hasNext() ;) {
			    		listOfSenses.add(i.next().getLemma());
			    	 }
		    	}
		    }
		} else {
			listOfSenses = null;
		}
		dict.close();
	    return listOfSenses;	
	}
	
	/**
	 * Gets the hyponyms of the word lemma with the corresponding posInitial.
	 * @param lemma
	 * @param posInitial
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> getPWNHyponyms(String lemma, String posInitial) throws IOException {
		ArrayList<String> listOfSenses = new ArrayList<String>();
		// constructs the URL to the Wordnet dictionary directory
		URL url = new URL("file", null, "/usr/local/Cellar/wordnet/3.1/dict");	
		// constructs the dictionary object and opens it
		IDictionary dict = new Dictionary(url);
		dict.open ();
		// get the word within PWN
		IIndexWord idxWord = dict.getIndexWord(lemma, POS.valueOf(hashOfPOS.get(posInitial)));
		// if the word is found,
		if (idxWord != null) {
			// get all its synsets
		    for (IWordID wordID : idxWord.getWordIDs()){
		    	IWord word = dict.getWord (wordID);
		    	ISynset synset = word.getSynset();
		    	List<ISynsetID>hypernyms = synset.getRelatedSynsets(Pointer.HYPONYM);
		    	List <IWord > words ;
		    	
		    	for(ISynsetID sid : hypernyms){
			    	words = dict.getSynset(sid).getWords();
			    	for(Iterator<IWord>i = words.iterator(); i.hasNext() ;) {
			    		listOfSenses.add(i.next().getLemma());
			    	 }
		    	}
		    }
		} else {
			listOfSenses = null;
		}
		dict.close();
	    return listOfSenses;	
	}
	
	
	/***
	 * Takes a conllu file as input and produces an output file which contains the SUMO mapped senses
	 * in the 10th column of the conllu. It adds the SUMO mapped senses next to the scored PWN synsets
	 * which are also in the 10th column.  
	 * @param file
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public void annotateConlluWithSUMOSenses(String file) throws FileNotFoundException, UnsupportedEncodingException{
		BufferedReader br = new BufferedReader(new InputStreamReader (new FileInputStream(file), "UTF-8"));
		String outputFile= "/Users/kkalouli/Documents/Stanford/comp_sem/SUMO.mapped";
		OutputStream out = new FileOutputStream(outputFile);	
		String strLine;
		// read file line by line
		try {
			while ((strLine = br.readLine()) != null) {
				// split the line into columns
				String [] columns = strLine.split("\t");
				// get the senses column (the 10th)
				String senses = columns[columns.length-1];
				String mapping = "";
				// if the senses column actually contains something
				if (senses.contains(":")){
					// get the first synset returned
					String disambSense = senses.substring(0,senses.indexOf(":"));
					// get the pos of the current word
					String posInitial = columns[4];
					mapping = extractSUMOMappingFromSUMO(disambSense,posInitial);
					// and write it nto place
					if (!mapping.isEmpty())
						mapping = "|"+mapping.substring(2);
				}
				String stringToWrite = strLine+mapping+"\n";
				//System.out.println(stringToWrite);
				out.write(stringToWrite.getBytes(Charset.forName("UTF-8")));
			}
			out.close();
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracts the SUMO mappings from the local SUMO files. It needs a synset and the pos of the synset.
	 * 
	 * @param synset
	 * @param pos
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	public String extractSUMOMappingFromSUMO(String synset, String posInitial) throws UnsupportedEncodingException, FileNotFoundException{
		String senseToReturn = "";
		String pos = "";
		String sumo = "";
	
		if (hashOfPOS.containsKey(posInitial))
			pos = hashOfPOS.get(posInitial);
		
		// get the right local file
		if (pos.equals("NOUN")){
			sumo = "/Users/kkalouli/Documents/workspace/sumo/WordNetMappings/WordNetMappings30-noun.txt";			
		}
		else if (pos.equals("VERB")){
			sumo = "/Users/kkalouli/Documents/workspace/sumo/WordNetMappings/WordNetMappings30-verb.txt";
		}
		else if (pos.equals("ADJECTIVE")){
			sumo = "/Users/kkalouli/Documents/workspace/sumo/WordNetMappings/WordNetMappings30-adj.txt";
		}
		else if (pos.equals("ADVERB")){
			sumo = "/Users/kkalouli/Documents/workspace/sumo/WordNetMappings/WordNetMappings30-adv.txt";	
		}
		else
			return senseToReturn;
		
		String matched = getSumoFileMatchedSense(synset, sumo);
		if (matched.equals("")){
			sumo = "/Users/kkalouli/Documents/workspace/sumo/WordNetMappings/WordNetMappings30-all.txt";
			matched = getSumoFileMatchedSense(synset, sumo);
		}
		
		if (!matched.equals("")) {
			if (matched.contains("&%")){
				senseToReturn = matched.substring(matched.indexOf("&%")+2);
			} 
			if (matched.contains("@") && matched.indexOf("@") != matched.length()-1 && matched.contains("|")){
				String hypernymsStr = matched.substring(matched.indexOf("@")+1,matched.indexOf("|"));
				String[] hypernmysList = hypernymsStr.split("@");
				for (int i = 0; i < hypernmysList.length; i++){
					String hyper = hypernmysList[i].substring(0,9);
					Integer depth = i;
					String hyperConcept = getSumoFileMatchedSense(hyper.replace(" ", ""), sumo);
					if (!hyperConcept.equals(""))
						superConcepts.put(hyper, depth);
				}
			}
			if (matched.contains("~") && matched.contains("|")){
				String hyponymsStr = matched.substring(matched.indexOf("~")+1,matched.indexOf("|"));
				String[] hyponmysList = hyponymsStr.split("~");
				for (int i = 0; i < hyponmysList.length; i++){
					String hypo = hyponmysList[i].substring(1,9);
					Integer depth = i;
					String hypoConcept = getSumoFileMatchedSense(hypo, sumo);
					if (!hypoConcept.equals(""))
					subConcepts.put(hypo, depth);
				}
			}
		}
		//System.out.println(synset);
		//System.out.println(superConcepts);
		//System.out.println(subConcepts);
		return senseToReturn;
	}
	
	
	private String getSumoFileMatchedSense(String synset,String sumo) throws FileNotFoundException{
		String senseMatched = "";
		// read the file into a string
		Scanner scanner = new Scanner(new File(sumo), "UTF-8");
		String content = scanner.useDelimiter("\\A").next();
		// match the given synset
		Pattern sensePattern = Pattern.compile("\n"+synset+".*");
		Matcher senseMatcher = sensePattern.matcher(content);
		scanner.close();
		if (senseMatcher.find())
			senseMatched = senseMatcher.group();
		return senseMatched;
	}
	

	
	/**
	 * Creates a url quering the SUMO database. The url is based on the lemma of 
	 * the given word and on its POS. Returns the string to be written in the output
	 * file: the string is the new string including the old info plus the SUMO senses.
	 * @param strLine
	 * @param posInitial
	 * @param lemma
	 * @return
	 */
	public String extractSUMOSenses(String strLine, String lemma, String posInitial){
		String toWrite = "";
		String urlToParse = "";
		// if the POS is one of the above, get the value of the hash
		if (hashOfPOS.containsKey(posInitial)){
			String posNumb = "";
			String pos = hashOfPOS.get(posInitial);
			/*The pos has to be macthed to one of the SUMO ids, so that the specific
			 * pos is quired:
			1 for nouns, 2 for verbs, 3 for adjectives, 4 for adverbs. */
			if (pos.equals("NOUN"))
				posNumb = "1";
			else if (pos.equals("VERB"))
				posNumb = "2";
			else if (pos.equals("ADJECTIVE"))
				posNumb = "3";
			else if (pos.equals("ADVERB"))
				posNumb = "4";		
			// form the corresponding url for search in SUMO
			urlToParse = "http://sigma.ontologyportal.org:8080/sigma/WordNet.jsp?word="+lemma+"&POS="+posNumb;
			// read the url and extract the senses
			ArrayList<String> senses  = readSUMOUrlAndExtractSenses(urlToParse);
			// write the output
			String sensesToString = "|Sumo="+senses;
			toWrite = strLine.substring(0,strLine.length()-1)+sensesToString.replace("[","").replace("]","").replace(" ", "");
		} else {
			toWrite = strLine.substring(0,strLine.length()-1)+"|Sumo=?";
		}
		return toWrite;
	}
	
	/**
	 * Reads a given url which corresponds to a SUMO query and returns all
	 * senses found within this url.
	 * @param url
	 * @return
	 */
	public ArrayList<String> readSUMOUrlAndExtractSenses(String url) {
		ArrayList<String> listOfSenses = new ArrayList<String>();
		ArrayList<String> matches = new ArrayList<String>();
		String content = null;
		URLConnection connection = null;
		try {
		  connection =  new URL(url).openConnection();
		  Scanner scanner = new Scanner(connection.getInputStream());
		  scanner.useDelimiter("\\Z");
		  content = scanner.next();
		}catch ( Exception ex ) {
		    ex.printStackTrace();
		}
		String symbol = "";
		Pattern sensePattern = Pattern.compile("(?<=SUMO Mappings:  ).*?mapping\\)");
        Matcher matcher = sensePattern.matcher(content);
        while (matcher.find()){
        	matches.add(matcher.group());
        }
        for (String match : matches ){
        	String sense = match.substring(match.indexOf(">")+1,match.indexOf("<", 1));
        	String mapping = match.substring(match.indexOf("(")+1,match.indexOf(" mapping"));
        	if (mapping.equals("equivalent"))
        		symbol = "=";
        	else if (mapping.equals("subsuming"))
        		symbol = "+";
        	else if (mapping.equals("negated subsuming"))
        		symbol = "-";
        	else if (mapping.equals("subsuming"))
        		symbol = "@";
        	listOfSenses.add(sense+symbol);        	
        }    
        return listOfSenses;
	}
	
	/**
	 * Takes a sentence as input and disambiguates each word. It returns a hash containing
	 * each word with its list of senses in decreasing score order.
	 * The method is called from the DepGraphToSemanticGraph() class. 
	 * @param sentence
	 * @return
	 * @throws Exception
	 */
	public HashMap <String, String> disambiguateSensesWithJIGSAW(String sentence) throws Exception{
		HashMap <String, String> listOfSenses = new HashMap<String,String>();
		// create a file containing the sentence to be tokenized
		String inString = "untokenized.tmp";
		BufferedWriter writer = new BufferedWriter( new FileWriter(inString));
		writer.write(sentence);
		writer.close();
		// create a file where the tokenized sentence will be written
		String tmpTokenized= "tokenized.tmp";
		PrintWriter writerTokenizer = new PrintWriter(tmpTokenized, "UTF-8");
		PTBTokenizer<CoreLabel> ptbt = new PTBTokenizer<>(new FileReader(new File(inString)), new CoreLabelTokenFactory(), "");
	    // tokenize
		while (ptbt.hasNext()) {
	        CoreLabel label = ptbt.next();
	        writerTokenizer.write(label.toString()+"\n");
	      }
		writerTokenizer.close();
		// run the disambiguation
	   JIGSAW jigsaw = new JIGSAW(new File("/Users/kkalouli/Documents/eclipseWorkspace/Syn2Sem/resources/jigsaw.properties"));
	   TokenGroup tg = null;
       BufferedReader in = new BufferedReader(new FileReader(tmpTokenized));
       List<String> list = new ArrayList<String>();
       while (in.ready()) {
    	   list.add(in.readLine());
       }
       in.close();
       tg = jigsaw.mapText(list.toArray(new String[list.size()]));
       for (int i = 0; i < tg.size(); i++) {
           listOfSenses.put(tg.get(i).getToken(),tg.get(i).getSyn());
       }
       
       return listOfSenses;
	}
	
	
	
	public static void main(String args[]) throws IOException {
		String input = "/Users/kkalouli/Documents/Stanford/comp_sem/SICK_4001-6076.parsed_sensestaggedJIGSAW.conllu";
		SenseMappingsRetriever mapper = new SenseMappingsRetriever();
		//mapper.extractPWNSensesToText("dance","VB"); 
		mapper.annotateConlluWithSUMOSenses(input);
		//mapper.test();
	}

}
