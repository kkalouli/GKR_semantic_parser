package sem.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;


import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import jigsaw.JIGSAW;
import jigsaw.data.TokenGroup;
import sem.graph.SemanticEdge;
import sem.graph.SemanticGraph;
import sem.graph.vetypes.SkolemNode;


public class SenseMappingsRetriever implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5353876989060718577L;
	HashMap<String,String> hashOfPOS = new HashMap<String,String>();
	public Map<String, Integer> subConcepts = new HashMap<String,Integer>();
	public Map<String, Integer> superConcepts =new HashMap<String,Integer>();
	public ArrayList<String> synonyms = new ArrayList<String>();
	public ArrayList<String> hypernyms = new ArrayList<String>();
	public ArrayList<String> hyponyms = new ArrayList<String>();
	public ArrayList<String> antonyms = new ArrayList<String>();
	public double[] embed = new double[300];
	private JIGSAW jigsaw;
	private Properties props;
	private String wnInstall;
	private String sumoInstall;
	private String jigsawProps;
	private WordVectors glove;
	private Integer indexOfPunct;
	
	public SenseMappingsRetriever(InputStream configFile){
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
		this.props = new Properties();
		try {
			props.load(new InputStreamReader(configFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.wnInstall = props.getProperty("wn_location");
        this.sumoInstall = props.getProperty("sumo_location");
        this.jigsawProps = props.getProperty("jigsaw_props");
		this.jigsaw = new JIGSAW(new File(jigsawProps));
		InputStream gloveFile = getClass().getClassLoader().getResourceAsStream("glove.6B.300d.txt");
		try {
			this.glove = WordVectorSerializer.readWord2VecModel(stream2file(gloveFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
    public static File stream2file (InputStream in) throws IOException {
        final File tempFile = File.createTempFile("stream2file.", ".txt");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }
	
	
	public double[] getEmbed() {
		return embed;
	}

	public void setEmbed(double[] embed) {
		this.embed = embed;
	}
	
	/**
	 * Get the list of synonyms associated with the sense
	 * @return
	 */
	public List<String> getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(ArrayList<String> synonyms) {
		this.synonyms = synonyms;
	}
	
	/**
	 * Get the list of hypernyms associated with the sense
	 * @return
	 */
	public List<String> getHypernyms() {
		return hypernyms;
	}

	public void setHypernyms(ArrayList<String> hypernyms) {
		this.hypernyms = hypernyms;
	}
	
	/**
	 * Get the list of hyponyms associated with the sense
	 * @return
	 */
	public List<String> getHyponyms() {
		return hyponyms;
	}

	public void setHyponyms(ArrayList<String> hyponyms) {
		this.hyponyms = hyponyms;
	}
	
	/**
	 * Get the list of antonyms associated with the sense
	 * @return
	 */
	public List<String> getAntonyms() {
		return antonyms;
	}

	public void setAntonyms(ArrayList<String> antonyms) {
		this.antonyms = antonyms;
	}
	
	public Map<String, Integer> getSuperConcepts() {
		return superConcepts;
	}

	public void setSuperConcepts(Map<String, Integer> superConcepts) {
		this.superConcepts = superConcepts;
	}
	
	public Map<String, Integer> getSubConcepts() {
		return subConcepts;
	}

	public void setSubConcepts(Map<String, Integer> subConcepts) {
		this.subConcepts = subConcepts;
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
		URL url = new URL("file", null, wnInstall);	
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
		       	listOfSenses.add(senseToWrite);
		    }
		} else {
			listOfSenses = null;
		}
	    
		dict.close();
	    return listOfSenses;		
	}
	
	/**
	 * Gets the synonyms, hypernyms, hyponyms, antonyms of the specified synset of the word lemma with the corresponding pos
	 * @param lemma
	 * @param posInitial
	 * @return
	 * @throws IOException
	 */
	public void getLexRelationsOfSynset(String lemma, String synsetID, String pos) throws IOException {
		// constructs the URL to the Wordnet dictionary directory
		URL url = new URL("file", null, wnInstall);	
		// constructs the dictionary object and opens it
		IDictionary dict = new Dictionary(url);
		dict.open ();
		IIndexWord idxWord = null;
		if (hashOfPOS.containsKey(pos))
			// look up the given word within dict
			idxWord = dict.getIndexWord (lemma, POS.valueOf(hashOfPOS.get(pos)));
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
		} 
		
		dict.close();	
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
	public String extractSUMOMappingFromSUMO(String synset, String posInitial){
		String senseToReturn = "";
		String pos = "";
		String sumo = "";
		
		if (hashOfPOS.containsKey(posInitial))
			pos = hashOfPOS.get(posInitial);
		
		// get the right local file
		if (pos.equals("NOUN")){
			sumo = sumoInstall+"/WordNetMappings/WordNetMappings30-noun.txt";			
		}
		else if (pos.equals("VERB")){
			sumo = sumoInstall+"/WordNetMappings/WordNetMappings30-verb.txt";
		}
		else if (pos.equals("ADJECTIVE")){
			sumo = sumoInstall+"/WordNetMappings/WordNetMappings30-adj.txt";
		}
		else if (pos.equals("ADVERB")){
			sumo = sumoInstall+"/WordNetMappings/WordNetMappings30-adv.txt";	
		}
		else
			return senseToReturn;
		
		String matched = getSumoFileMatchedSense(synset, sumo);
		if (matched.equals("")){
			sumo = sumoInstall+"/WordNetMappings/WordNetMappings30-all.txt";
			matched = getSumoFileMatchedSense(synset, sumo);
		}
		
		if (!matched.equals("")) {
			if (matched.contains("&%")){
				senseToReturn = matched.substring(matched.indexOf("&%")+2); //+2
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
	
	
	private String getSumoFileMatchedSense(String synset,String sumo){
		String senseMatched = "";
		// read the file into a string
		Scanner scanner;
		try {
			scanner = new Scanner(new File(sumo), "UTF-8");
			String content = scanner.useDelimiter("\\A").next();
			// match the given synset
			Pattern sensePattern = Pattern.compile("\n"+synset+".*");
			Matcher senseMatcher = sensePattern.matcher(content);
			scanner.close();
			if (senseMatcher.find())
				senseMatched = senseMatcher.group();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return senseMatched;
	}
	
	/** 
	 * Takes a sentence as input and disambiguates each word. It returns a hash containing
	 * each word mapped to a map of strings and floats. The strings (keys) are the senses of the word and
	 * the corresponding floats (values) are the probabilities of the senses. The map is sorted based on value before return,
	 * so that the most probable sense is the fist key of the inner map.
	 * The method is called from the DepGraphToSemanticGraph() class. 
	 * @param sentence
	 * @return
	 * @throws Exception
	 */
	public HashMap <String, Map<String,Float>> disambiguateSensesWithJIGSAW(String wholeCtx){
		HashMap <String, String> listOfSenses = new HashMap<String,String>();
		HashMap <String, Map<String,Float>> mapOfSensesSorted = new HashMap<String,Map<String,Float>>();
		// create a file containing the sentence to be tokenized
		String inString = "untokenized.tmp";
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(inString));
			writer.write(wholeCtx);
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
		   TokenGroup tg = null;
	       BufferedReader in = new BufferedReader(new FileReader(tmpTokenized));
	       List<String> list = new ArrayList<String>();
	       while (in.ready()) {
	    	   list.add(in.readLine());
	       }
	       in.close();
	       // write each disambiguated sense to the hashmap
	       tg = jigsaw.mapText(list.toArray(new String[list.size()]));
	       for (int i = 0; i < tg.size(); i++) {
	           listOfSenses.put(Integer.toString(i)+"_"+tg.get(i).getToken(),tg.get(i).getSyn());
	       }
	     //System.out.println(listOfSenses);
	       // go through the hashmap, split each sense to the sense and the probability and create an inner hashmap:
	       // the sense is the key and the probability is the value
	       for (String key: listOfSenses.keySet()){
	    	   String value = listOfSenses.get(key);
	    	   if (value.equals("U")) {
	    		   mapOfSensesSorted.put(key, new HashMap<String,Float>());
	    		   continue; 
	    	   }
	    	   String[] splitSenses = value.split(",");
	    	   HashMap<String,Float> mapSenseProp = new HashMap<String,Float>();
	    	   for (String senseProp : splitSenses){
	    		   String[] sensePropSplit = senseProp.split(":");
	    		   String sense = sensePropSplit[0].substring(1);
	    		   float prop = Float.parseFloat(sensePropSplit[1]);
	    		   mapSenseProp.put(sense, prop);
	    	   }
	    	   // sort this inner hashmap based on the values so that the sense with the highest probability is first
	    	   final Map<String, Float> mapSensePropSorted = mapSenseProp.entrySet().stream().sorted((Map.Entry.<String, Float>comparingByValue().reversed()))
	                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	    	   // put this hashmap as value for the current key node
	    	   mapOfSensesSorted.put(key, mapSensePropSorted);
	       }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	    
       return mapOfSensesSorted;
	}

	/**
	 * Maps the given node to a sense (PWN) and a concept (SUMO). Returns a hash with the 
	 * sense as key and the concept as value. It also checks if the node is involved in a 
	 * compound and in that case it gives back the sense and the concept of the compound as well.
	 * In that case, it does not overwrite the original sense and concept of the node but it adds an extra
	 * pair to the hashmap which contains the sense and the concept of the compound.  
	 * @param node
	 * @param retriever
	 * @return
	 */
	public HashMap<String, String> mapNodeToSenseAndConcept(SkolemNode node, SemanticGraph graph,HashMap <String, Map<String,Float>> senses){
		HashMap<String,String> lexSem = new HashMap<String,String>();
		int positionOfNode = node.getPosition();
		String keyToGet = Integer.toString(positionOfNode-1) + "_" + node.getSurface();
		Map<String,Float> senseProp = senses.get(keyToGet);
		//String sense = "";
		String concept = "";
			
		if (senseProp != null && !senseProp.isEmpty()){
			int length = senseProp.keySet().toArray().length;
			if (senseProp.keySet().toArray().length >5)
				length = 5;
			for (int i= 0; i< length; i++){
				String sense = (String) senseProp.keySet().toArray()[i];
				concept = extractSUMOMappingFromSUMO(sense, ((SkolemNode) node).getPartOfSpeech());
				lexSem.put(sense,concept);
			}
			//sense = (String) senseProp.keySet().toArray()[0];
			
		}				

		
		// check if there is a compound involved and find the sense/concept of the compound as well. Find the sense/concepts
		// of the separate words anyway in case the compound is not found or does not help for the further processing
		Set<SemanticEdge> edges = graph.getDependencyGraph().getOutEdges(node);
		for (SemanticEdge inEdge : edges){
			if (inEdge.getLabel().equals("compound")){
				String compound = inEdge.getDestVertexId().substring(0,inEdge.getDestVertexId().indexOf("_"))+ " "+ inEdge.getSourceVertexId().substring(0,inEdge.getSourceVertexId().indexOf("_"));
				String pos = ((SkolemNode) graph.getDependencyGraph().getStartNode(inEdge)).getPartOfSpeech();
				try {
					ArrayList<String> compSenses = accessPWNDBAndExtractSenses(compound,pos);
					if (compSenses != null && !compSenses.isEmpty()){
						String sense = compSenses.get(0).substring(4, compSenses.get(0).lastIndexOf("-"));		
						concept = extractSUMOMappingFromSUMO(sense, ((SkolemNode) node).getPartOfSpeech());				
						lexSem.put("cmp_"+sense,concept);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}	
		return lexSem;	
	}

	
	
	public void mapNodeToEmbed(SkolemNode node){
		String lemma = node.getStem();
        double[] wordVector = glove.getWordVector(lemma);
        this.embed = wordVector;
	}

}
