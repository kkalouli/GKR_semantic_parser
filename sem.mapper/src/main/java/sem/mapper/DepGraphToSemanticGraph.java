package sem.mapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.Pair;
import semantic.graph.EdgeContent;
import semantic.graph.NodeContent;
import semantic.graph.SemGraph;
import semantic.graph.SemJGraphT;
import semantic.graph.SemanticEdge;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.ContextHeadEdge;
import semantic.graph.vetypes.ContextNode;
import semantic.graph.vetypes.ContextNodeContent;
import semantic.graph.vetypes.GraphLabels;
import semantic.graph.vetypes.LexEdge;
import semantic.graph.vetypes.LexEdgeContent;
import semantic.graph.vetypes.PropertyEdge;
import semantic.graph.vetypes.PropertyEdgeContent;
import semantic.graph.vetypes.RoleEdge;
import semantic.graph.vetypes.RoleEdgeContent;
import semantic.graph.vetypes.SenseNode;
import semantic.graph.vetypes.SenseNodeContent;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.SkolemNodeContent;
import semantic.graph.vetypes.TermNode;
import semantic.graph.vetypes.TermNodeContent;
import semantic.graph.vetypes.ValueNode;
import semantic.graph.vetypes.ValueNodeContent;

public class DepGraphToSemanticGraph {

	private semantic.graph.SemanticGraph graph;
	private SemanticGraph stanGraph;
	public ArrayList<String> verbalForms = new ArrayList<String>();
	public ArrayList<String> nounForms = new ArrayList<String>();
	public ArrayList<String> quantifiers = new ArrayList<String>();
	private List<SemanticGraphEdge> traversed;
	private StanfordParser parser;
	private SenseMappingsRetriever retriever;


	public DepGraphToSemanticGraph() throws FileNotFoundException, UnsupportedEncodingException {
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
		quantifiers.add("few");
		quantifiers.add("plenty");
		quantifiers.add("several");
		this.graph = null;
		this.stanGraph = null;
		this.traversed = new ArrayList<SemanticGraphEdge>();
		this.parser = new StanfordParser();
		this.retriever = new SenseMappingsRetriever(new File("gkr.properties"));

	}

	/***
	 * Convert the stanford graph to a SemanticGraph with dependencies, properties, lex features, roles, etc.
	 * @param stanGraph
	 * @return
	 */
	public semantic.graph.SemanticGraph getGraph(SemanticGraph stanGraph) {
		this.stanGraph = stanGraph;
		this.graph = new semantic.graph.SemanticGraph();
		this.graph.setName(stanGraph.toRecoveredSentenceString());
		traversed.clear();
		integrateDependencies();
		//graph.displayDependencies();	
		integrateRoles();
		integrateContexts();
		integrateProperties();
		integrateLexicalFeatures();			
		return this.graph;
	}

	public semantic.graph.SemanticGraph getGraph() {
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
		// based on the root node, go and find all children (and children of children)
		stanChildrenToSemGraphChildren(rootN, root);
		
		/*
		 * Go through the finished dep graph and fix any cases that are dealt differently by CoreNLP than by us.
		 * For now, only change the deps of the modals ought and need so they can be treated the same as the rest of the modals.
		 * When the modals have a complement with "to", they are (correctly) considered the roots of the sentences and the main verb
		 * gets to be the xcomp, e.g. Abrams need to hire Browne (as opposed to when they are found without "to", where they are
		 * considered plain aux of the main verb, e.g. Need Abrams hire Browne?)  However, we want to treat the former cases as aux as well
		 * so that the implementation of the role graph remains the same. Therefore, in the following we remove the x/ccomp edge
		 * and add the aux edge instead.  
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
					cardinality = "singular";
					name = "common";
				} else if (pos.equals("NNP")){
					cardinality = "singular";
					name = "proper";
				} else if (pos.equals("NNPS")){
					cardinality = "plural";
					name = "proper";
				} else if (pos.equals("NNS")){
					cardinality = "plural";
					name = "common";
				}
				// checks if there is a quantification with of: five of the seven
				boolean existsQMod = false;
				// going through the out edges of this node to see if there are any specifiers
				for (SemanticEdge edge: graph.getDependencyGraph().getOutEdges(node)){
					// depending on the case, define the specifier
					String depOfDependent = edge.getLabel();			
					String determiner = edge.getDestVertexId().substring(0,edge.getDestVertexId().indexOf("_"));
					if (depOfDependent.equals("det") && existsQMod == false) {					
						if (determiner.equals("a"))
							specifier = "indef";
						else if (determiner.equals("the"))
							specifier = "def"; 
						else
							specifier = determiner; 
					// only if there is no quantification with of, assign this determiner as the cardinatlity
					} else if (depOfDependent.equals("nummod") && existsQMod == false){
						cardinality = determiner;
					// otherwise we introduce the part_of edge
					} else if (depOfDependent.equals("nummod") && existsQMod == true){
						part_of = determiner;
					// if there is det:qmod there is quantification with of. We also check if there is any quantification on the quantification, e.g.
					// "any five of the seven". In this case, the any becomes the specifier of the five
					}else if (depOfDependent.equals("det:qmod")){
							cardinality = determiner;
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
					} 
				}
				// check if there is a "none" involved: "none" is not recognized as a det:qmod so we have to look for it separately
				for (SemanticEdge edge: graph.getDependencyGraph().getInEdges(node)){
					if (edge.getLabel().equals("nmod:of") && graph.getDependencyGraph().getStartNode(edge).getLabel().toLowerCase().contains("none")){
						specifier = "none";
					}
				}
				// adding the property edge cardinality (singular, plural or exact number)
				if (!cardinality.equals("")){
					PropertyEdge cardinalityEdge = new PropertyEdge(GraphLabels.CARDINALITY, new PropertyEdgeContent());
					graph.addPropertyEdge(cardinalityEdge, node, new ValueNode(cardinality, new ValueNodeContent()));
				}
				// adding the property edge name (proper or common)
				if (!name.equals("")){
					PropertyEdge typeEdge = new PropertyEdge(GraphLabels.NTYPE, new PropertyEdgeContent());
					graph.addPropertyEdge(typeEdge, node, new ValueNode(name, new ValueNodeContent()));
				}
				// adding the property edge specifier (def, indef, many, few, etc)
				if (!specifier.equals("")){
					PropertyEdge specifierEdge = new PropertyEdge(GraphLabels.SPECIFIER, new PropertyEdgeContent());
					graph.addPropertyEdge(specifierEdge, node, new ValueNode(specifier, new ValueNodeContent()));
				}
				// adding the property edge part_of (e.g. five of seven, five is the cardinality and seven the part_of)
				if (!part_of.equals("")){
					PropertyEdge partOfEdge = new PropertyEdge(GraphLabels.PART_OF, new PropertyEdgeContent());
					graph.addPropertyEdge(partOfEdge, node, new ValueNode(part_of, new ValueNodeContent()));
				}
			} 
		}
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
	private HashMap<String, String> mapNodeToSenseAndConcept(SkolemNode node, SenseMappingsRetriever retriever,HashMap <String, String> senses){
		HashMap<String,String> lexSem = new HashMap<String,String>();
		String sense = "";
		String concept = "";
		
		sense = senses.get(((SkolemNode) node).getSurface());
		if ( sense != null && !sense.equals("U")){
			sense = sense.substring(1,senses.get(((SkolemNode) node).getSurface()).indexOf(":"));
			try {
				concept = retriever.extractSUMOMappingFromSUMO(sense, ((SkolemNode) node).getPartOfSpeech());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}				

		lexSem.put(sense,concept);
		// check if there is a compound involved and find the sense/concept of the compound as well. Find the sense/concepts
		// of the separate words anyway in case the compound is not found or does not help for the further processing
		Set<SemanticEdge> edges = graph.getDependencyGraph().getOutEdges(node);
		for (SemanticEdge inEdge : edges){
			if (inEdge.getLabel().equals("compound")){
				String compound = inEdge.getDestVertexId().substring(0,inEdge.getDestVertexId().indexOf("_"))+ " "+ inEdge.getSourceVertexId().substring(0,inEdge.getSourceVertexId().indexOf("_"));
				String pos = ((SkolemNode) graph.getDependencyGraph().getStartNode(inEdge)).getPartOfSpeech();
				try {
					ArrayList<String> compSenses = retriever.accessPWNDBAndExtractSenses(compound,pos);
					if (compSenses != null && !compSenses.isEmpty()){
						sense = compSenses.get(0).substring(4, compSenses.get(0).lastIndexOf("-"));		
						concept = retriever.extractSUMOMappingFromSUMO(sense, ((SkolemNode) node).getPartOfSpeech());				
						lexSem.put(sense,concept);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}	
		return lexSem;	
	}
	
	/**
	 * Maps each node to its lexical semantics and adds the corresponding sense nodes and lex edges to
	 * the semantic graph . For the moment, it maps the disambiguated sense of the node (this gets to be
	 * the label of the SenseNode), the concept of the node (this is set to the concept of the SenseContent),
	 * the subConcepts of the node (there are set to be the subconcepts of the SenseContent) and the 
	 * superConcepts of the node (these are set to be the superconcepts of the SenseContent). At the moment, the
	 * racs of the SenseNode are left empty. 
	 */
	private void integrateLexicalFeatures(){
		HashMap <String, String> senses = null;
		try {
			senses = retriever.disambiguateSensesWithJIGSAW(stanGraph.toRecoveredSentenceString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		SemGraph roleGraph = graph.getRoleGraph();
		Set<SemanticNode<?>> roleNodes = roleGraph.getNodes();
		for (SemanticNode<?> node: roleNodes){
			if (node instanceof SkolemNode){
				HashMap<String,String> lexSem = mapNodeToSenseAndConcept((SkolemNode) node, retriever, senses); 
				for (String key : lexSem.keySet()){
					String sense = key;
					String concept = lexSem.get(key);
					// create new sense Content
					SenseNodeContent senseContent = new SenseNodeContent(sense);
					senseContent.addConcept(concept);
					senseContent.setHierarchyPrecomputed(true);
					senseContent.setSubConcepts(retriever.subConcepts);
					senseContent.setSuperConcepts(retriever.superConcepts);
					retriever.subConcepts.clear();
					retriever.superConcepts.clear();
					
					// create new Sense Node
					SenseNode senseNode = new SenseNode(sense, senseContent);
					// create new LexEdge
					LexEdge edge = new LexEdge(GraphLabels.LEX, new LexEdgeContent());
					graph.addLexEdge(edge, node, senseNode);
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
	
	/**
	 * Returns the semantic graph of the given sentence. 
	 * It runs the stanford parser, gets the graph and turns this graph to the semantic graph.
	 * @param sentence
	 * @return
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public semantic.graph.SemanticGraph sentenceToGraph(String sentence, DepGraphToSemanticGraph semGraph) throws FileNotFoundException, UnsupportedEncodingException{	
		SemanticGraph stanGraph = parser.parseOnly(sentence);
		semantic.graph.SemanticGraph graph = semGraph.getGraph(stanGraph);
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
	
	public void processTestsuite(String file, DepGraphToSemanticGraph semConverter) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		// true stands for append = true (dont overwrite)
		BufferedWriter writer = new BufferedWriter( new FileWriter(file.substring(0,file.indexOf(".txt"))+"_processed.csv", true));
		String strLine;
		while ((strLine = br.readLine()) != null) {
			if (strLine.startsWith("####")){
				writer.write(strLine+"\n\n");
				writer.flush();
				continue;
			}
			String text = strLine.split("\t")[1];
			SemanticGraph stanGraph = parser.parseOnly(text);
			semantic.graph.SemanticGraph graph = semConverter.getGraph(stanGraph);
			//System.out.println(graph.displayAsString());
			writer.write(strLine+"\n"+graph.displayAsString()+"\n\n");
			writer.flush();
			System.out.println("Processed sentence "+ strLine.split("\t")[0]);
		}
		writer.close();
		br.close();
	}
	
	/***
	 * Process a unique sentence with GKR. 
	 * You can comment in or out the subgraphs that you want to have displayed.
	 */
	public void processSentence(String sentence, DepGraphToSemanticGraph semConverter) throws FileNotFoundException, UnsupportedEncodingException{
		semantic.graph.SemanticGraph graph = semConverter.sentenceToGraph(sentence, semConverter);
		graph.displayDependencies();
		graph.displayProperties();
		graph.displayLex();
		graph.displayContexts();
		graph.displayRoles();
		graph.generalDisplay();
		graph.display();
		System.out.println(graph.displayAsString());
		for (SemanticNode<?> node : graph.getDependencyGraph().getNodes()){
				System.out.println(node.getLabel()+((SkolemNodeContent) node.getContent()).getContext());
		}
	}


	public static void main(String args[]) throws IOException {
		DepGraphToSemanticGraph semConverter = new DepGraphToSemanticGraph();
		//semConverter.processTestsuite("/Users/kkalouli/Documents/Stanford/comp_sem/forDiss/mixed_testsuite.txt", semConverter);		
		semConverter.processSentence("The boy faked the illness.", semConverter);		
		
	}
}
