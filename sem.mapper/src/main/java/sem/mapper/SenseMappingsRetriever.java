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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
//import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import com.robrua.nlp.bert.BasicTokenizer;
import com.robrua.nlp.bert.Bert;
import com.robrua.nlp.bert.FullTokenizer;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
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


/**
 * Map nodes to their senses for the lexical graph. 
 * @author Katerina Kalouli, 2017
 *
 */
public class SenseMappingsRetriever implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5353876989060718577L;
	private HashMap<String,String> hashOfPOS;
	private Map<String, Integer> subConcepts;
	private Map<String, Integer> superConcepts;
	private ArrayList<String> synonyms;
	private ArrayList<String> hypernyms;
	private ArrayList<String> hyponyms ;
	private ArrayList<String> antonyms;
	private String senseKey;
	private float[] embed;
	private Properties props;
	private String wnInstall;
	private String sumoInstall;
	private String jigsawProps;
	//private WordVectors glove;
	private Bert bert;
	private HashMap<String,float[]> embedMap;
	private JIGSAW jigsaw;
	private FullTokenizer tokenizer;
	private IRAMDictionary wnDict;
	private String sumoContent;
	private String bertVocab;
	
	/**
	 * Constructor to be used when SenseMappingsRetriever is called from the InferenceComputer
	 * (through DepGraphToSemanticGraph)
	 * In this case, bert, bertTokenizer, the PWN Dict and the SUMo content are passed as parameters
	 * so that they are not called every time a new sentence is parsed
	 * @param configFile
	 * @param bert
	 * @param tokenizer
	 * @param wnDict
	 * @param sumoContent
	 */
	public SenseMappingsRetriever(InputStream configFile, Bert bert, FullTokenizer tokenizer, 
			IRAMDictionary wnDict, String sumoContent){
		this.hashOfPOS = new HashMap<String,String>();
		this.subConcepts = new HashMap<String,Integer>();
		this.superConcepts =new HashMap<String,Integer>();
		this.synonyms = new ArrayList<String>();
		this.hypernyms = new ArrayList<String>();
		this.hyponyms = new ArrayList<String>();
		this.antonyms = new ArrayList<String>();
		this.senseKey = "";
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
		hashOfPOS.put("CD","NOUN");
		this.props = new Properties();
		InputStreamReader streamReader = new InputStreamReader(configFile);
		try {
			props.load(streamReader);
			streamReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.wnInstall = props.getProperty("wn_location");
        this.sumoInstall = props.getProperty("sumo_location");
        this.jigsawProps = props.getProperty("jigsaw_props");
        this.bert = bert;
        this.tokenizer = tokenizer;
        this.wnDict = wnDict;
        this.sumoContent = sumoContent;
		Logger.getLogger(JIGSAW.class.getName()).setLevel(Level.OFF);
		this.jigsaw = new JIGSAW(new File(jigsawProps));
		// for glove embeddings
		/*InputStream gloveFile = getClass().getClassLoader().getResourceAsStream("glove.6B.300d.txt");
		try {
			this.glove = WordVectorSerializer.readWord2VecModel(stream2file(gloveFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		this.embedMap = new HashMap<String,float[]>();
		this.embed = new float[768];
	}
	
	/**
	 * Constructor to be used when SenseMappingsRetriever is simply called from DepGraphToSemanticgraph, only to
	 * parse specific sentences (without inference). Then, all libs are initialized here.
	 * @param configFile
	 */
	public SenseMappingsRetriever(InputStream configFile){
		this.hashOfPOS = new HashMap<String,String>();
		this.subConcepts = new HashMap<String,Integer>();
		this.superConcepts =new HashMap<String,Integer>();
		this.synonyms = new ArrayList<String>();
		this.hypernyms = new ArrayList<String>();
		this.hyponyms = new ArrayList<String>();
		this.antonyms = new ArrayList<String>();
		this.senseKey = "";
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
		hashOfPOS.put("CD","NOUN");
		this.props = new Properties();
		InputStreamReader streamReader = new InputStreamReader(configFile);
		try {
			props.load(streamReader);
			streamReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.wnInstall = props.getProperty("wn_location");
        this.sumoInstall = props.getProperty("sumo_location");
        this.jigsawProps = props.getProperty("jigsaw_props");
        this.bertVocab = props.getProperty("bert_vocab");
        this.bert = Bert.load("com/robrua/nlp/easy-bert/bert-uncased-L-12-H-768-A-12");
        Logger.getLogger(JIGSAW.class.getName()).setLevel(Level.OFF);
		this.jigsaw = new JIGSAW(new File(jigsawProps));	
		this.tokenizer = new FullTokenizer(new File(bertVocab), true);
		this.wnDict = new RAMDictionary(new File(wnInstall), ILoadPolicy.NO_LOAD);
		try {
			this.wnDict.open();
			this.wnDict.load();
			Scanner scanner = new Scanner(new File(sumoInstall), "UTF-8");
			this.sumoContent = scanner.useDelimiter("\\A").next();
			scanner.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// for glove embeddings
		/*InputStream gloveFile = getClass().getClassLoader().getResourceAsStream("glove.6B.300d.txt");
		try {
			this.glove = WordVectorSerializer.readWord2VecModel(stream2file(gloveFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		this.embedMap = new HashMap<String,float[]>();
		this.embed = new float[768];
	}
	
	

	
    public static File stream2file (InputStream in) throws IOException {
        final File tempFile = File.createTempFile("stream2file.", ".txt");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }
	
	
	public float[] getEmbed() {
		return embed;
	}

	public void setEmbed(float[] embed) {
		this.embed = embed;
	}
	
	public String getSenseKey() {
		return senseKey;
	}

	public void setSenseKey(String senseKey) {
		this.senseKey = senseKey;
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
		// get the word within PWN
		IIndexWord idxWord = wnDict.getIndexWord(lemma, POS.valueOf(hashOfPOS.get(posInitial)));
		// if the word is found,
		if (idxWord != null) {
			// get all its synsets
		    for (IWordID wordID : idxWord.getWordIDs()){
		    	IWord word = wnDict.getWord (wordID);
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
	    
		//dict.close();
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
		IIndexWord idxWord = null;
		if (hashOfPOS.containsKey(pos))
			// look up the given word within dict
			idxWord = wnDict.getIndexWord (lemma, POS.valueOf(hashOfPOS.get(pos)));
		// if the word is found:
		if (idxWord != null) {
			List<IWordID> wordIDs = idxWord.getWordIDs();
			for (IWordID id : wordIDs){
				// from all ids (=synsets) only take the one that is the same as the specified synset
				if (id.getSynsetID().toString().substring(4,id.getSynsetID().toString().lastIndexOf("-")).equals(synsetID)){
					IWord word = wnDict.getWord(id);
					ISynset synset = word.getSynset();
					senseKey = word.getSenseKey().toString();
					// iterate over words associated with the synset and get the synonyms
					for(IWord w : synset.getWords()){
						synonyms.add(w.getLemma());
					}
					// get the hypernyms
					for(ISynsetID sid : synset.getRelatedSynsets(Pointer.HYPERNYM)){
						List <IWord > words ;
				    	words = wnDict.getSynset(sid).getWords();
				    	for(Iterator<IWord>i = words.iterator(); i.hasNext() ;) {
				    		hypernyms.add(i.next().getLemma());
				    	 }
			    	}
					// get the hyponyms
					for(ISynsetID sid : synset.getRelatedSynsets(Pointer.HYPONYM)){
						List <IWord > words ;
				    	words = wnDict.getSynset(sid).getWords();
				    	for(Iterator<IWord>i = words.iterator(); i.hasNext() ;) {
				    		hyponyms.add(i.next().getLemma());
				    	 }
			    	}
					/* get the antonyms (for the lexical relations we need the method getRelatedWords
					instead of getRelatedSynsets() */
					for(IWordID w : word.getRelatedWords(Pointer.ANTONYM)){
						IWord anto = wnDict.getWord(w);		
						antonyms.add(anto.getLemma());
					}
					break;
				}
			}
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
				senseToReturn = matched.substring(matched.lastIndexOf("&%")+2); //+2
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
		Pattern sensePattern = Pattern.compile("\n"+synset+".*");
		Matcher senseMatcher = sensePattern.matcher(sumoContent);
		if (senseMatcher.find())
			senseMatched = senseMatcher.group();	
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
		try {
		   TokenGroup tg = null;
	       BasicTokenizer tokenizer = new BasicTokenizer(true);
	       String[] tokens = tokenizer.tokenize(wholeCtx);
	       // write each disambiguated sense to the hashmap
	       tg = jigsaw.mapText(tokens);
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
	public HashMap<String, Map<String, Float>> mapNodeToSenseAndConcept(SkolemNode node, SemanticGraph graph,HashMap <String, Map<String,Float>> senses){
		HashMap<String,Map<String,Float>> lexSem = new HashMap<String,Map<String,Float>>();
		int positionOfNode = node.getPosition();
		String keyToGet = Integer.toString(positionOfNode-1) + "_" + node.getSurface().toLowerCase();
		Map<String,Float> senseProp = senses.get(keyToGet);
		//String sense = "";
		String concept = "";
				
		if (senseProp != null && !senseProp.isEmpty()){
			int length = senseProp.keySet().toArray().length;
			if (senseProp.keySet().toArray().length >5)
				length = 5;
			// only take the first 5 best senses
			for (int i= 0; i< length; i++){
				String sense = (String) senseProp.keySet().toArray()[i];
				concept = extractSUMOMappingFromSUMO(sense, ((SkolemNode) node).getPartOfSpeech());	
				// map to hold the concept and the score associated with that sense
				Map<String,Float> conceptAndScore = new HashMap<String,Float>();
				conceptAndScore.put(concept, senseProp.get(sense));
				lexSem.put(sense,conceptAndScore);
			}			
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
						// map to hold the concept and the score associated with that sense
						Map<String,Float> conceptAndScore = new HashMap<String,Float>();
						conceptAndScore.put(concept, (float) 1);		
						lexSem.put("cmp_"+sense,conceptAndScore);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}	
		return lexSem;	
	}

	
	/***
	 * Sets the embedding matching to the given node. Uses the glove embeddings: not used for now.
	 * @param wholeCtx
	 */
	/*public void mapNodeToEmbed(SkolemNode node){
		String lemma = node.getStem();
		double[] wordVector = glove.getWordVector(lemma);
        this.embed = wordVector;
	}*/
		
	/**
	 * Matches the original tokens (each word) to the token assumed from BERT after applying wordpiece tokenization.
	 * @param originalTokens
	 * @return
	 */
	public HashMap<String,Integer> matchOriginalTokens2BERTTokens(String[] originalTokens ){
		ArrayList<String> bertTokens = new ArrayList<String>();
		HashMap<String,Integer> orig2TokenMap = new HashMap<String,Integer>();
		// bert tokens start with CLS
		bertTokens.add("CLS");
		// counter for position of each token in the sentence
		int i = 1;
		// go through the original tokens
		for (String origToken :originalTokens ){
			// bertTokens.size() always corresponds to the "hops" that were made from the previous bert token to this one
			orig2TokenMap.put(origToken+"_"+i,bertTokens.size());
			// tokenize the current original token with the wordpiece tokenizer
			String[] tokToken = tokenizer.tokenize(origToken);
			// add each of those new tokens to the bertTokens, so that the latter increases its size
			for (String tok : tokToken){
				bertTokens.add(tok);	
			}
			i++;
		}
		// bert tokens end with SEP
		bertTokens.add("SEP");
		return orig2TokenMap;
	}
	
	/**
	 * Maps the whole context (2 sentences) to a sequence embedding with BERT. The resulting embedding of each
	 * sentence is a float[128][768] no matter the length of the sentence. There is a standard length of 128 
	 * per sentence. For this reason, we need a "decoder" which "decodes" back the exact embedding that matches to
	 * each token of the sentence (due to no one-to-one relation) 
	 * @param wholeCtx
	 */
	public void getEmbedForWholeCtx(String wholeCtx){
		String[] splitCtx = wholeCtx.split("(?<=(\\.|\\?|!))\\s");
		String str1 = splitCtx[0];
		//create a basic BERT tokenizer to tokenize the first sentence of the wholeCtx (first sent = current sent)
		BasicTokenizer tokenizer = new BasicTokenizer(true);
		String[] plainlyTokenizedSent = tokenizer.tokenize(str1);
		// match each of those original tokens to a token in the bert 128 sequence 
		HashMap<String,Integer> orig2TokenMap  = matchOriginalTokens2BERTTokens(plainlyTokenizedSent);
		// get the BERT sequence
		float[][] bertSeq = bert.embedTokens(str1);
		// match each original token to a specifc vector of the BERT sequence
		for (String key : orig2TokenMap.keySet()){
			this.embedMap.put(key,bertSeq[orig2TokenMap.get(key)]);
		}
	}
	
	/**
	 * Extracts the specific embedding of the given node from the BERT sequence embedding.
	 * @param node
	 */
	public void extractNodeEmbedFromSequenceEmbed(SkolemNode node){
		this.embed = this.embedMap.get(node.getSurface()+"_"+node.getPosition());
	}

}
