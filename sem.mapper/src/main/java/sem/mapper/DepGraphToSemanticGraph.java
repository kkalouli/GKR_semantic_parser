package sem.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.jgrapht.Graph;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.IntPair;
import edu.stanford.nlp.util.Pair;
import sem.graph.EdgeContent;
import sem.graph.NodeContent;
import sem.graph.SemGraph;
import sem.graph.SemJGraphT;
import sem.graph.SemanticEdge;
import sem.graph.SemanticNode;
import sem.graph.vetypes.ContextHeadEdge;
import sem.graph.vetypes.ContextNode;
import sem.graph.vetypes.ContextNodeContent;
import sem.graph.vetypes.DefaultEdgeContent;
import sem.graph.vetypes.GraphLabels;
import sem.graph.vetypes.LexEdge;
import sem.graph.vetypes.LexEdgeContent;
import sem.graph.vetypes.LinkEdge;
import sem.graph.vetypes.PropertyEdge;
import sem.graph.vetypes.PropertyEdgeContent;
import sem.graph.vetypes.RoleEdge;
import sem.graph.vetypes.RoleEdgeContent;
import sem.graph.vetypes.SenseNode;
import sem.graph.vetypes.SenseNodeContent;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.SkolemNodeContent;
import sem.graph.vetypes.TermNode;
import sem.graph.vetypes.TermNodeContent;
import sem.graph.vetypes.ValueNode;
import sem.graph.vetypes.ValueNodeContent;

public class DepGraphToSemanticGraph implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7281901236266524522L;
	private sem.graph.SemanticGraph graph;
	private SemanticGraph stanGraph;
	public ArrayList<String> verbalForms = new ArrayList<String>();
	public ArrayList<String> nounForms = new ArrayList<String>();
	public ArrayList<String> quantifiers = new ArrayList<String>();
	public ArrayList<String> whinterrogatives = new ArrayList<String>();
	static public boolean interrogative;
	private List<SemanticGraphEdge> traversed;
	private StanfordParser parser;
	private SenseMappingsRetriever retriever;


	public DepGraphToSemanticGraph() {
		verbalForms.add("MD");
		verbalForms.add("VB");
		verbalForms.add("VBD");
		verbalForms.add("VBG");
		verbalForms.add("VBN");
		verbalForms.add("VBP");
		verbalForms.add("VBZ");
		nounForms.add("NN");
		nounForms.add("NNP");
		nounForms.add("NNS");
		nounForms.add("NNPS");
		quantifiers.add("many");
		quantifiers.add("much");
		quantifiers.add("plenty");
		quantifiers.add("several");
		quantifiers.add("some");
		quantifiers.add("most");
		quantifiers.add("all");
		quantifiers.add("every");
		whinterrogatives.add("who");
		whinterrogatives.add("when");
		whinterrogatives.add("where");
		whinterrogatives.add("why");
		whinterrogatives.add("how");
		whinterrogatives.add("which");
		whinterrogatives.add("what");
		whinterrogatives.add("whose");
		whinterrogatives.add("whom");
		whinterrogatives.add("whether");
		whinterrogatives.add("if");
		this.graph = null;
		this.stanGraph = null;
		this.traversed = new ArrayList<SemanticGraphEdge>();
		try {
			this.parser = new StanfordParser();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStream configFile = getClass().getClassLoader().getResourceAsStream("gkr.properties");
		this.retriever = new SenseMappingsRetriever(configFile);
		this.interrogative = false;

	}

	/***
	 * Convert the stanford graph to a SemanticGraph with dependencies, properties, lex features, roles, etc.
	 * @param stanGraph
	 * @return
	 */
	public sem.graph.SemanticGraph getGraph(SemanticGraph stanGraph, String sentence, String wholeCtx) {
		this.stanGraph = stanGraph;
		this.graph = new sem.graph.SemanticGraph();
		this.graph.setName(stanGraph.toRecoveredSentenceString());
		traversed.clear();
		integrateDependencies();
		//graph.displayDependencies();	
		integrateRoles();
		integrateContexts();
		integrateProperties();
		integrateLexicalFeatures(wholeCtx);		
		integrateCoRefLinks(sentence);
		return this.graph;
	}

	public sem.graph.SemanticGraph getGraph() {
		return this.graph;
	}

	/**
	 * Converts the edges of the stanford graph to edges of the semantic graph. 
	 * It finds all children of the given parent node and for each child it
	 * adds an edge from the parent to that child and recursively does the same
	 * for each child of the child.
	 * @param parent
	 * @param parentNode
	 */
	private void stanChildrenToSemGraphChildren(IndexedWord parent, SkolemNode parentNode){
		// get the children
		List<SemanticGraphEdge> children = stanGraph.outgoingEdgeList(parent);
		// iterate through the children
		for (SemanticGraphEdge child : children){
			if (traversed.contains(child)){
				break;
			}
			traversed.add(child);
			// get the role relation of the child and create a role edge
			String role = "";
			if (child.getRelation().getSpecific() == null)
				role = child.getRelation().getShortName(); 
			else
				role = child.getRelation().getShortName()+":"+child.getRelation().getSpecific();
			RoleEdge roleEdge = new RoleEdge(role, new RoleEdgeContent());
			// get the child's lemma
			String dependent = child.getDependent().lemma(); 
			// create a SkolemNodeContent for the child and create a SkolemNode
			SkolemNodeContent dependentContent = new SkolemNodeContent();
			dependentContent.setSurface(child.getDependent().originalText());
			dependentContent.setStem(child.getDependent().lemma());
			dependentContent.setPosTag(child.getDependent().tag());
			dependentContent.setPartOfSpeech(child.getDependent().tag());
			Double positionD = child.getDependent().pseudoPosition();
			dependentContent.setPosition(positionD.intValue());
			dependentContent.setDerived(false);	
			dependentContent.setSkolem(dependent+"_"+Integer.toString(positionD.intValue()));
			SkolemNode finish = new SkolemNode(dependentContent.getSkolem(), dependentContent);
			/* check if the same node already exists and if so, if it is a verb node. If it is no verb, then use the existing node as the finish node.
			this is necessary for sentences with noun or different-verbs coordination or control/raising verbs; 
			otherwise, the coord node /controlled subj is inserted twice because of the explicit enhanced dependencies. If however it is a verb (the same verb), 
			then we have a sentence of the type John works for Mary and for Anna. where we have to assume to different (same) verbs. */
			for (SemanticNode<?> node : graph.getDependencyGraph().getNodes()){
					if (node.getLabel().equals(finish.getLabel()) && !role.contains("conj")){ //!verbalForms.contains(finish.getPartOfSpeech())){
						finish = (SkolemNode) node;
					}
			}
			// add the dependency between the parent and the current child only if the parentNode != finish, otherwise loops
			if (!parentNode.equals(finish)){
				graph.addDependencyEdge(roleEdge, parentNode, finish);
			}
			
			// recursively, go back and do the same for each of the children of the child
			stanChildrenToSemGraphChildren(child.getDependent(), finish);		
		}
	}


	/**
	 * Convert the dependencies of the stanford graph to dependencies of the semantic graph.
	 * Start from the root node and then recursively visit all children of the root and all children
	 * of the children (in-depth). 
	 */
	private void integrateDependencies(){
		// get the root node of the stanford graph
		IndexedWord rootN = stanGraph.getFirstRoot();
		//stanGraph.prettyPrint();
		// create a new node for the semantic graph and define all its features 
		SkolemNodeContent rootContent = new SkolemNodeContent();
		rootContent.setSurface(rootN.originalText());
		rootContent.setStem(rootN.lemma());
		rootContent.setPosTag(rootN.tag());
		rootContent.setPartOfSpeech(rootN.tag());
		Double positionR = rootN.pseudoPosition();
		rootContent.setPosition(positionR.intValue());
		rootContent.setDerived(false);
		rootContent.setSkolem(rootN.lemma()+"_"+Integer.toString(positionR.intValue()));
		SkolemNode root = new SkolemNode(rootContent.getSkolem(), rootContent);
		// add the node as root node to the graph
		graph.setRootNode(root);
		// if there are no children of the root node at all (sentence only with imperative intransitive verb), just add this node the dep graph
		if (stanGraph.outgoingEdgeList(rootN).isEmpty()){
			graph.getDependencyGraph().addNode(root);
		} else {
			// based on the root node, go and find all children (and children of children)
			stanChildrenToSemGraphChildren(rootN, root);
		}
		
		/*
		 * Go through the finished dep graph and fix any cases that are dealt differently by CoreNLP than by us.
		 * For now, 2 things:
		 * 1)  only change the deps of the modals ought and need so they can be treated the same as the rest of the modals.
		 * When the modals have a complement with "to", they are (correctly) considered the roots of the sentences and the main verb
		 * gets to be the xcomp, e.g. Abrams need to hire Browne (as opposed to when they are found without "to", where they are
		 * considered plain aux of the main verb, e.g. Need Abrams hire Browne?)  However, we want to treat the former cases as aux as well
		 * so that the implementation of the role graph remains the same. Therefore, in the following we remove the x/ccomp edge
		 * and add the aux edge instead.  
		 * 2) if there are any quantifiers (e.g., few, little) involved with negative monotonicity in restriction position, then add a negated node in order to
		 * capture accordingly the contexts: few people = not many people
		 * For the moment, it doesnt work with "little". The quantifier "no" (not some) is separately treated in the context mapping. 
		 */
		for (SemanticNode<?> node : graph.getDependencyGraph().getNodes()){
			if ( (((SkolemNodeContent) node.getContent()).getStem().equals("ought")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("need")) && graph.getInEdges(node).isEmpty()) {
				RoleEdge depEdge = new RoleEdge("aux", new RoleEdgeContent());
				List<SemanticEdge> outEdges = graph.getOutEdges(node);
				// if it is the x/ccomp edge, add the aux edge in its place
				for (SemanticEdge out : outEdges){
					if (out.getLabel().equals("xcomp") || out.getLabel().equals("ccomp")){
						SemanticNode<?> head = graph.getFinishNode(out);
						graph.addDependencyEdge(depEdge, head, node);	
					}
					// remove all out edges of the modal
					graph.removeDependencyEdge(out);
				}		
			} else if ( (((SkolemNodeContent) node.getContent()).getStem().equals("few"))) { 
				RoleEdge depEdge = new RoleEdge("neg", new RoleEdgeContent());
				SemanticNode<?> head = graph.getOutNeighbors(node).iterator().next();
				SkolemNodeContent notContent = new SkolemNodeContent();
				notContent.setSurface("not");
				notContent.setStem("not");
				notContent.setPosTag("RB");
				notContent.setPartOfSpeech("RB");
				int position = 0;
				notContent.setPosition(position);
				notContent.setDerived(false);
				notContent.setSkolem("not_0");
				SkolemNode not = new SkolemNode(notContent.getSkolem(), notContent);
				graph.addDependencyEdge(depEdge, head, not);	
			}
		}
	}
	
	/**
	 * Integrate the semantic roles of the graph.
	 */
	private void integrateRoles(){
		RolesMapper rolesMapper = new RolesMapper(graph,verbalForms, nounForms);
		rolesMapper.integrateAllRoles();
	}

	/**
	 * Adds the properties to the semantic graph. Searches for all nodes that are nouns or verbs
	 * and adds for each of them the relevant properties:
	 * - for nouns: cardinality, name, specifier and nmod_num
	 * - for verbs: tense and aspect
	 */
	private void integrateProperties(){
		SemGraph depGraph = graph.getDependencyGraph();
		Set<SemanticNode<?>> depNodes = depGraph.getNodes();
		// iterate through the nodes of the dep graph
		for (SemanticNode<?> node: depNodes){
			String tense = "";
			String aspect = "";
			String cardinality = "";
			String name = "";
			String specifier = "";
			String part_of = "";
			String pos = ((SkolemNode) node).getPartOfSpeech();
			// define the properties fro verbs
			if (verbalForms.contains(pos)){
				aspect = "not progressive";
				if (pos.equals("VB") || pos.equals("VBP") || pos.equals("VBZ"))
					tense = "present";
				else if (pos.equals("VBD") || pos.equals("VBN")){
					tense = "past";
				}
				else if (pos.equals("VBG"))
					aspect = "progressive";
				Set<SemanticEdge> inEdges = depGraph.getInEdges(node);
				if (!inEdges.isEmpty()){
					if (inEdges.iterator().next().getLabel().equals("aux") ){
						if (!tense.equals("")){
							PropertyEdge tenseEdge = new PropertyEdge(GraphLabels.TENSE, new PropertyEdgeContent());
							graph.addPropertyEdge(tenseEdge, depGraph.getInNeighbors(node).iterator().next(), new ValueNode(tense, new ValueNodeContent()));
						}
						continue;
					}
				}
				
				// adding the property edge tense
				if (!tense.equals("")){
					PropertyEdge tenseEdge = new PropertyEdge(GraphLabels.TENSE, new PropertyEdgeContent());
					graph.addPropertyEdge(tenseEdge, node, new ValueNode(tense, new ValueNodeContent()));
				}
				// adding the property edge aspect
				if (!aspect.equals("")){
					PropertyEdge aspectEdge = new PropertyEdge(GraphLabels.ASPECT, new PropertyEdgeContent());
					graph.addPropertyEdge(aspectEdge, node, new ValueNode(aspect, new ValueNodeContent()));
				}
			// define the properties for nouns
			} else if (nounForms.contains(pos) && !node.getLabel().toLowerCase().contains("none")){
				if (pos.equals("NN")){
					cardinality = "sg";
					name = "common";
				} else if (pos.equals("NNP")){
					cardinality = "sg";
					name = "proper";
				} else if (pos.equals("NNPS")){
					cardinality = "pl";
					name = "proper";
				} else if (pos.equals("NNS")){
					cardinality = "pl";
					name = "common";
				}
				// checks if there is a quantification with of, e.g. five of the seven
				boolean existsQMod = false;
				// going through the out edges of this node to see if there are any specifiers
				for (SemanticEdge edge: graph.getDependencyGraph().getOutEdges(node)){
					// depending on the case, define the specifier
					String depOfDependent = edge.getLabel();			
					String determiner = ((SkolemNodeContent) graph.getFinishNode(edge).getContent()).getStem(); //edge.getDestVertexId().substring(0,edge.getDestVertexId().indexOf("_"));
					if (depOfDependent.equals("det") && existsQMod == false) {					
							specifier = determiner; 
					// only if there is no quantification with of, assign this determiner as the cardinatlity
					} else if (depOfDependent.equals("nummod") && existsQMod == false){
						specifier = determiner;
					// otherwise we introduce the part_of edge
					} else if (depOfDependent.equals("nummod") && existsQMod == true){
						part_of = determiner;
					// if there is det:qmod there is quantification with of. We also check if there is any quantification on the quantification, e.g.
					// "any five of the seven". In this case,  any becomes the specifier of the five
					}else if (depOfDependent.equals("det:qmod")){
							specifier = determiner;
							existsQMod = true;
							// get the outNode (the node corresponding to the string determiner)
							SemanticNode<?> outNode = graph.getDependencyGraph().getEndNode(edge);
							// check if there are outEdges that are "det"
							for (SemanticEdge outEdge : graph.getDependencyGraph().getOutEdges(outNode)){
								if (outEdge.getLabel().equals("det")){
									specifier = outEdge.getDestVertexId().substring(0,outEdge.getDestVertexId().indexOf("_"));
								}
							}
					} else if (depOfDependent.equals("amod") && (quantifiers.contains(determiner.toLowerCase()) )  ){
						specifier = determiner;
					// do the following adjustments for quantifiers with negative monotonicity in restriction type
					} else if (determiner.equals("no")){
						specifier = "some";
					} else if (determiner.equals("few")){
						specifier = "many";
					}
				}
				// check if there is a "none" involved: "none" is not recognized as a det:qmod so we have to look for it separately
				for (SemanticEdge edge: graph.getDependencyGraph().getInEdges(node)){
					if (edge.getLabel().equals("nmod:of") && graph.getDependencyGraph().getStartNode(edge).getLabel().toLowerCase().contains("none")){
						specifier = "none";
					}
				}
				// adding the property edge cardinality (singular, plural)
				if (!cardinality.equals("")){
					PropertyEdge cardinalityEdge = new PropertyEdge(GraphLabels.CARDINAL, new PropertyEdgeContent());
					graph.addPropertyEdge(cardinalityEdge, node, new ValueNode(cardinality, new ValueNodeContent()));
				}
				// adding the property edge name (proper or common)
				if (!name.equals("")){
					PropertyEdge typeEdge = new PropertyEdge(GraphLabels.NTYPE, new PropertyEdgeContent());
					graph.addPropertyEdge(typeEdge, node, new ValueNode(name, new ValueNodeContent()));
				}
				// adding the property edge specifier (the, a, many, few, N, etc)
				if (!specifier.equals("")){
					PropertyEdge specifierEdge = new PropertyEdge(GraphLabels.SPECIFIER, new PropertyEdgeContent());
					graph.addPropertyEdge(specifierEdge, node, new ValueNode(specifier, new ValueNodeContent()));
				}
				// adding the property edge part_of (e.g. five of seven, five is the specifier and seven the part_of)
				if (!part_of.equals("")){
					PropertyEdge partOfEdge = new PropertyEdge(GraphLabels.PART_OF, new PropertyEdgeContent());
					graph.addPropertyEdge(partOfEdge, node, new ValueNode(part_of, new ValueNodeContent()));
				}
			} 
			// check if there is a direct or indirect question and add property for wh-interrogatives
			else if (interrogative == true && whinterrogatives.contains(((SkolemNodeContent) node.getContent()).getStem())){
				String stem = (String) ((SkolemNodeContent) node.getContent()).getStem();
				String label = "unk";
				if (stem.equals("who"))
					label = "personal";
				else if (stem.equals("where"))
					label = "locative";
				else if (stem.equals("when"))
					label = "temporal";
				else if (stem.equals("why"))
					label = "causal";
				else if (stem.equals("what"))
					label = "object";
				else if (stem.equals("which"))
					label = "group";
				else if (stem.equals("how"))
					label = "manner";
				else if (stem.equals("whose"))
					label = "possessive";
				else if (stem.equals("whom"))
					label = "pers.acc.";
				PropertyEdge interrEdge = new PropertyEdge(GraphLabels.SPECIFIER, new PropertyEdgeContent());
				graph.addPropertyEdge(interrEdge, node, new ValueNode(label, new ValueNodeContent()));
			}
		}
	}
	
	/**
	 * Maps each node to its lexical semantics and adds the corresponding sense nodes and lex edges to
	 * the semantic graph . For the moment, it maps the disambiguated sense of the node (this gets to be
	 * the label of the SenseNode), the concept of the node (this is set to the concept of the SenseContent),
	 * the subConcepts of the node (there are set to be the subconcepts of the SenseContent) and the 
	 * superConcepts of the node (these are set to be the superconcepts of the SenseContent). At the moment, the
	 * racs of the SenseNode are left empty. 
	 */
	private void integrateLexicalFeatures(String wholeCtx){
		HashMap <String, Map<String,Float>> senses = null;
		try {
			senses = retriever.disambiguateSensesWithJIGSAW(wholeCtx); // stanGraph.toRecoveredSentenceString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SemGraph roleGraph = graph.getRoleGraph();
		Set<SemanticNode<?>> roleNodes = roleGraph.getNodes();
		for (SemanticNode<?> node: roleNodes){
			if (node instanceof SkolemNode){
				HashMap<String,String> lexSem = retriever.mapNodeToSenseAndConcept((SkolemNode) node, graph, senses); 
				for (String key : lexSem.keySet()){
					String sense = key;
					try {
						retriever.getLexRelationsOfSynset(((SkolemNode) node).getStem(), sense, ((SkolemNode) node).getPartOfSpeech());
						retriever.mapNodeToEmbed((SkolemNode) node);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String concept = lexSem.get(key);
					// create new sense Content
					SenseNodeContent senseContent = new SenseNodeContent(sense);
					senseContent.addConcept(concept);
					senseContent.setHierarchyPrecomputed(true);
					senseContent.setSubConcepts(retriever.getSubConcepts());
					senseContent.setSuperConcepts(retriever.getSuperConcepts());
					senseContent.setSynonyms(retriever.getSynonyms());
					senseContent.setHypernyms(retriever.getHypernyms());
					senseContent.setHyponyms(retriever.getHyponyms());
					senseContent.setAntonyms(retriever.getAntonyms());
					senseContent.setEmbed(retriever.getEmbed());
					
					// create new Sense Node
					SenseNode senseNode = new SenseNode(sense, senseContent);
					// create new LexEdge

					LexEdge edge = new LexEdge(GraphLabels.LEX, new LexEdgeContent());
					graph.addLexEdge(edge, node, senseNode);
					
					retriever.setSubConcepts(new HashMap<String,Integer>());
					retriever.setSuperConcepts(new HashMap<String,Integer>());
					retriever.setSynonyms(new ArrayList<String>());
					retriever.setHypernyms(new ArrayList<String>());
					retriever.setHyponyms(new ArrayList<String>());
					retriever.setAntonyms(new ArrayList<String>());
					retriever.embed = null;
				}	
			}
		}
	}
	
	
	/*** 
	 * create the context graph by taking into account the different "markers of contexts".
	 * Once the graph is created, go through it and assign contexts to each skolem of the dependency graph. 
	 */
	private void integrateContexts(){
		ContextMapper ctxMapper = new ContextMapper(graph, verbalForms);
		ctxMapper.integrateAllContexts();
	}
	
	/***
	 * Create the link graph by resolving the coreferences. Uses the stanford CoreNLP software but also the stanford dependencies directly.
	 * @param sentence
	 */
	private void integrateCoRefLinks(String sentence){
		// ge the corefrence chains as those are given by CoreNLP
		Collection<CorefChain> corefChains = parser.getCoreference(sentence);
		for (CorefChain cc: corefChains){	
			SemanticNode<?> startNode = null;
			SemanticNode<?> finishNode = null;
			for (IntPair k : cc.getMentionMap().keySet()){
				// find in the role graph the node with the position equal to the position that the coreference element has
				for (SemanticNode<?> n : graph.getRoleGraph().getNodes()){
					if (((SkolemNodeContent) n.getContent()).getPosition() == k.getTarget()){
						// in the first pass of this chain, set the startNode, in all other ones set the finishNode (the start Node remains the same)
						if (startNode == null){
							startNode = n;
						} else {
							finishNode = n;
						}
					}
				}
				// if all passes are over and there is coreference, add the links
				if (startNode != null && finishNode != null){
					LinkEdge linkEdge = new LinkEdge(GraphLabels.PRONOUN_RESOLUTION, new DefaultEdgeContent());
					graph.addLinkEdge(linkEdge, startNode, finishNode);
				}
			}
		}
		// the coreference CoreNLP does not show the appositives; these are in the form of dependencies in the dependency graph, so we need to extract them from there
		// (the appositives are also included in the role graph as restrictions)
		SemanticNode<?> startNode = null;
		SemanticNode<?> finishNode = null;
		for (SemanticEdge depEdge : graph.getDependencyGraph().getEdges()){
			// check for the existence of appositives and add the coreference link
			if (depEdge.getLabel().equals("appos")){
				startNode = graph.getStartNode(depEdge);
				finishNode = graph.getFinishNode(depEdge);
				LinkEdge linkEdge = new LinkEdge(GraphLabels.APPOS_IDENTICAL_TO, new DefaultEdgeContent());
				graph.addLinkEdge(linkEdge, startNode, finishNode);
			}
		}
	}
	
	/**
	 * Returns the semantic graph of the given sentence. 
	 * It runs the stanford parser, gets the graph and turns this graph to the semantic graph.
	 * @param sentence
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public sem.graph.SemanticGraph sentenceToGraph(String sentence, String wholeCtx){	
		if (sentence.contains("?"))
			this.interrogative = true;
		SemanticGraph stanGraph = parser.parseOnly(sentence);
		sem.graph.SemanticGraph graph = this.getGraph(stanGraph, sentence, wholeCtx);
		return graph;
	}

	/***
	 * Process a testsuite of sentences with GKR. One sentence per line.
	 * Lines starting with # are considered comments.
	 * The output is formatted as string: in this format only the dependency graph, the
	 * concepts graph, the contextual graph and the properties graph are displayed
	 * @param file
	 * @param semConverter
	 * @throws IOException
	 */
	
	public void processTestsuite(String file) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		// true stands for append = true (dont overwrite)
		BufferedWriter writer = new BufferedWriter( new FileWriter(file.substring(0,file.indexOf(".txt"))+"_processed.csv", true));
		FileOutputStream fileSer = null;
        ObjectOutputStream writerSer = null;
		String strLine;
		ArrayList<sem.graph.SemanticGraph> semanticGraphs = new ArrayList<sem.graph.SemanticGraph>();
		while ((strLine = br.readLine()) != null) {
			if (strLine.startsWith("####")){
				writer.write(strLine+"\n\n");
				writer.flush();
				continue;
			}
			String text = strLine.split("\t")[1];
			SemanticGraph stanGraph = parser.parseOnly(text);
			sem.graph.SemanticGraph graph = this.getGraph(stanGraph, text, text);
			//System.out.println(graph.displayAsString());
			writer.write(strLine+"\n"+graph.displayAsString()+"\n\n");
			writer.flush();
			System.out.println("Processed sentence "+ strLine.split("\t")[0]);
			if (graph != null)
				semanticGraphs.add(graph);
		}
		// serialize and write to file
		try {
			fileSer = new FileOutputStream("serialized_SemanticGraphs.ser");
			writerSer = new ObjectOutputStream(fileSer);
			writerSer.writeObject(semanticGraphs); 				
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		writer.close();
		br.close();
		writerSer.close();
	}

	/***
	 * Process a single sentence with GKR. 
	 * You can comment in or out the subgraphs that you want to have displayed.
	 * @throws IOException 
	 */
	public sem.graph.SemanticGraph processSentence(String sentence, String wholeCtx) throws IOException{
		if (!sentence.endsWith("."))
			sentence = sentence+".";
		if (!wholeCtx.endsWith("."))
			wholeCtx = wholeCtx+".";
		sem.graph.SemanticGraph graph = this.sentenceToGraph(sentence, wholeCtx);
		/*graph.displayRoles();
		graph.displayDependencies();
		graph.displayProperties();
		graph.displayLex();
		graph.displayContexts();
		graph.displayRolesAndCtxs();
		graph.displayCoref();*/
		String ctxs = graph.getContextGraph().getMxGraph();
		String roles = graph.getRoleGraph().getMxGraph();
		String deps = graph.getDependencyGraph().getMxGraph();
		String props = graph.getPropertyGraph().getMxGraph();
		String lex = graph.getLexGraph().getMxGraph();
		String coref = graph.getLinkGraph().getMxGraph();
		BufferedWriter writer = new BufferedWriter( new FileWriter("-7.txt", true));
		writer.write(roles);
		writer.write("\n\n");
		writer.write(deps);
		writer.write("\n\n");
		writer.write(ctxs);
		writer.write("\n\n");
		writer.write(props);
		writer.write("\n\n");
		writer.write(lex);
		writer.write("\n\n");
		writer.write(coref);
		writer.write("\n\n");
		writer.flush();
		writer.close();
		//ImageIO.write(graph.saveDepsAsImage(),"png", new File("/Users/kkalouli/Desktop/deps.png"));
		System.out.println(graph.displayAsString());
		for (SemanticNode<?> node : graph.getDependencyGraph().getNodes()){
				System.out.println(node.getLabel()+((SkolemNodeContent) node.getContent()).getContext());
		}
		return graph;
	}
	
	
	@SuppressWarnings("unchecked")
	public ArrayList<sem.graph.SemanticGraph> deserializeFileWithComputedPairs(String file){
		ArrayList<sem.graph.SemanticGraph> semanticGraphs = null;
			try {
				FileInputStream fileIn = new FileInputStream("serialized_SemanticGraphs.ser");
				ObjectInputStream in = new ObjectInputStream(fileIn);
				semanticGraphs = (ArrayList<sem.graph.SemanticGraph>) in.readObject();
				in.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			return semanticGraphs;
	}
	


	public static void main(String args[]) throws IOException {
		DepGraphToSemanticGraph semConverter = new DepGraphToSemanticGraph();
		//semConverter.deserializeFileWithComputedPairs("/Users/kkalouli/Documents/Stanford/comp_sem/forDiss/test.txt");
		//semConverter.processTestsuite("/Users/kkalouli/Documents/Stanford/comp_sem/forDiss/test.txt");
		String sentence = "John might apply for the position.";//"A family is watching a little boy who is hitting a baseball.";
		String context = "The boy faked the illness.";
		semConverter.processSentence(sentence, sentence+" "+context);	
	}
}
