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
import java.util.Properties;
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
	private JIGSAW jigsaw;
	private Properties props;
	private String wnInstall;
	private String sumoInstall;
	private String jigsawProps;
	
	public SenseMappingsRetriever(File configFile){
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
			props.load(new FileReader(configFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.wnInstall = props.getProperty("wn_location");
        this.sumoInstall = props.getProperty("sumo_location");
        this.jigsawProps = props.getProperty("jigsaw_props");
		this.jigsaw = new JIGSAW(new File(jigsawProps));
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


}
