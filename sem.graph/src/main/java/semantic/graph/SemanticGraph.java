package semantic.graph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;
import java.io.Serializable;

import org.springframework.util.SerializationUtils;

import semantic.graph.vetypes.ContextNode;
import semantic.graph.vetypes.GraphLabels;
import semantic.graph.vetypes.LexEdge;
import semantic.graph.vetypes.PropertyEdge;
import semantic.graph.vetypes.RoleEdge;
import semantic.graph.vetypes.SenseNode;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.SkolemNodeContent;
import semantic.graph.vetypes.TermNode;
import semantic.graph.vetypes.ValueNode;



/**
 * A semantic graph, built on top of an underlying graph data structure.
 * Comprises role, context, property, lexical, and link sub graph layer.
 * The class can be extended to include further graph layers
 *
 */
public class SemanticGraph implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3300274667044499203L;
	protected SemGraph graph;
	protected SemGraph roleGraph;
	protected SemGraph contextGraph;
	protected SemGraph propertyGraph;
	protected SemGraph lexGraph;
	protected SemGraph linkGraph;
	protected SemGraph dependencyGraph;
	protected SemanticNode<?> rootNode;
	protected String name;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	enum SemanticSubGraph {ROLE, CONTEXT, PROPERTY, LEX, LINK, DEPENDENCY}
	
	/**
	 * Get underlying SemGraph for entire graph
	 * @return
	 */
	public SemGraph getGraph() {
		return graph;
	}

	/**
	 * Get underlying SemGraph for role graph
	 * @return
	 */
	public SemGraph getRoleGraph() {
		return roleGraph;
	}

	/**
	 * Get underlying SemGraph for role graph
	 * @return
	 */
	public SemGraph getContextGraph() {
		return contextGraph;
	}

	/**
	 * Get underlying SemGraph for property graph
	 * @return
	 */
	public SemGraph getPropertyGraph() {
		return propertyGraph;
	}

	/**
	 * Get underlying SemGraph for lexical graph
	 * @return
	 */
	public SemGraph getLexGraph() {
		return lexGraph;
	}

	/**
	 * Get underlying SemGraph for link graph
	 * @return
	 */
	public SemGraph getLinkGraph() {
		return linkGraph;
	}

	/**
	 * Get underlying SemGraph for dependency graph
	 * @return
	 */
	public SemGraph getDependencyGraph() {
		return dependencyGraph;
	}

	//public SemanticGraphFactory getFactory() {
	//	return factory;
	//}

	public void setRootNode(SemanticNode<?> root) {
		this.rootNode = root;
	}
	
	/**
	 * Get the root node of the graph
	 * @return
	 */
	public SemanticNode<?> getRootNode() {
		return this.rootNode;
	}

	
	/**
	 * Create an empty semantic graph
	 */
	public SemanticGraph() {
		this.graph = new SemJGraphT();
		this.roleGraph = new SemJGraphT();
		this.contextGraph = new SemJGraphT();
		this.propertyGraph = new SemJGraphT();
		this.lexGraph = new SemJGraphT();
		this.linkGraph = new SemJGraphT();
		this.dependencyGraph = new SemJGraphT();
		this.rootNode = null;
		this.name = "";
	}
	
	/**
	 * Create a semantic graph from a graph specification
	 * @param specification
	 */
	public SemanticGraph(SemanticGraphSpecification specification) {
		this();
		Map<String, SemanticNode<?>> nodeMap = specification.getNodeMap();
		for (SemanticEdge edge : specification.roleEdges) {
			addSpecifiedEdge(edge, nodeMap, SemanticSubGraph.ROLE);
		}
		for (SemanticEdge edge : specification.contextEdges) {
			addSpecifiedEdge(edge, nodeMap, SemanticSubGraph.CONTEXT);
		}
		for (SemanticEdge edge : specification.propertyEdges) {
			addSpecifiedEdge(edge, nodeMap, SemanticSubGraph.PROPERTY);
		}
		for (SemanticEdge edge : specification.lexEdges) {
			addSpecifiedEdge(edge, nodeMap, SemanticSubGraph.LEX);
		}
		for (SemanticEdge edge : specification.linkEdges) {
			addSpecifiedEdge(edge, nodeMap, SemanticSubGraph.LINK);
		}
		for (SemanticEdge edge : specification.dependencyEdges) {
			addSpecifiedEdge(edge, nodeMap, SemanticSubGraph.DEPENDENCY);
		}
		this.rootNode = specification.rootNode;
		this.name = specification.name;
	}
	
	private void addSpecifiedEdge(SemanticEdge edge,
			Map<String, SemanticNode<?>> nodeMap, SemanticSubGraph subgraph) {
		SemanticNode<?> start = nodeMap.get(edge.sourceVertexId);
		SemanticNode<?> finish = nodeMap.get(edge.destVertexId);
		addDirectedEdge(edge, start, finish, subgraph);		
	}

	/**
	 * Get the java serialization of the SemanticGraph
	 * @return A byte array representing the serialization
	 */
	public byte[] serialize() {
		return SerializationUtils.serialize(this);
	}
	
	
	/**
	 * Deserialize a byte array the serializes a semantic graph
	 * @param bytes A byte array representing the serialization of a semantic graph
	 * @return A SemanticGraph
	 */
	public static SemanticGraph deserialize(byte[] bytes) {
		if (bytes == null) {
			return null;
		} else {
			return (SemanticGraph) SerializationUtils.deserialize(bytes);
		}
	}
	
	/**
	 * Add a node to the graph (not yet linked by edges)
	 * @param node
	 */
	public void addNode(SemanticNode<?> node) {
		this.graph.addNode(node);
	}
	
	

	
	/**
	 * Add a directed  edge from start node to finish node in the role subgraph
	 * <p> Will add nodes to the graph if they have not already been added.
	 * @param edge
	 * @param start
	 * @param finish
	 */
	public void addRoleEdge(SemanticEdge edge, SemanticNode<?> start, SemanticNode<?> finish) {
		addDirectedEdge(edge, start, finish, SemanticSubGraph.ROLE);
	}
	
	/**
	 * Add a directed  edge from start node to finish node in the context subgraph
	 * <p> Will add nodes to the graph if they have not already been added.
	 * @param edge
	 * @param start
	 * @param finish
	 */
	public void addContextEdge(SemanticEdge edge, SemanticNode<?> start, SemanticNode<?> finish) {
		addDirectedEdge(edge, start, finish, SemanticSubGraph.CONTEXT);
	}
	
	/**
	 * Add a directed  edge from start node to finish node in the lexical subgraph
	 * <p> Will add nodes to the graph if they have not already been added.
	 * @param edge
	 * @param start
	 * @param finish
	 */
	public void addLexEdge(SemanticEdge edge, SemanticNode<?> start, SemanticNode<?> finish) {
		addDirectedEdge(edge, start, finish, SemanticSubGraph.LEX);
	}
	
	/**
	 * Add a directed  edge from start node to finish node in the property subgraph
	 * <p> Will add nodes to the graph if they have not already been added.
	 * @param edge
	 * @param start
	 * @param finish
	 */
	public void addPropertyEdge(SemanticEdge edge, SemanticNode<?> start, SemanticNode<?> finish) {
		addDirectedEdge(edge, start, finish, SemanticSubGraph.PROPERTY);
	}
	
	/**
	 * Add a directed  edge from start node to finish node in the link subgraph
	 * <p> Will add nodes to the graph if they have not already been added.
	 * @param edge
	 * @param start
	 * @param finish
	 */
	public void addLinkEdge(SemanticEdge edge, SemanticNode<?> start, SemanticNode<?> finish) {
		addDirectedEdge(edge, start, finish, SemanticSubGraph.LINK);
	}
	
	/**
	 * Add a directed  edge from start node to finish node in the link subgraph
	 * <p> Will add nodes to the graph if they have not already been added.
	 * @param edge
	 * @param start
	 * @param finish
	 */
	public void addDependencyEdge(SemanticEdge edge, SemanticNode<?> start, SemanticNode<?> finish) {
		addDirectedEdge(edge, start, finish, SemanticSubGraph.DEPENDENCY);
	}
	
	private void addDirectedEdge(SemanticEdge edge, SemanticNode<?> start, SemanticNode<?> finish, SemanticSubGraph subGraph) {
		if (edge == null || start == null || finish == null) {
			return;
		}
		this.graph.addEdge(start, finish, edge);
		switch (subGraph) {
		case LEX:
			this.lexGraph.addEdge(start, finish, edge);
			break;
		case PROPERTY:
			this.propertyGraph.addEdge(start, finish, edge);
			break;
		case LINK:
			this.linkGraph.addEdge(start, finish, edge);
			break;
		case DEPENDENCY:
			this.dependencyGraph.addEdge(start, finish, edge);
			break;
		case CONTEXT:
			this.contextGraph.addEdge(start, finish, edge);
			break;
		case ROLE:
			this.propertyGraph.addEdge(start, finish, edge);
			this.lexGraph.addEdge(start, finish, edge);
			this.roleGraph.addEdge(start, finish, edge);
			break;
		default:
			break;

		}
	}
	
	/**
	 * Remove an edge from the role graph
	 * @param edge
	 */
	public void removeRoleEdge(SemanticEdge edge) {
		removeEdge(edge, SemanticSubGraph.ROLE);
	}
		
	/**
	 * Remove an edge from the context graph
	 * @param edge
	 */
	public void removeContextEdge(SemanticEdge edge) {
		removeEdge(edge, SemanticSubGraph.CONTEXT);
	}
	
	/**
	 * Remove an edge from the lexical graph
	 * @param edge
	 */
	public void removeLexEdge(SemanticEdge edge) {
		removeEdge(edge, SemanticSubGraph.LEX);
	}
	
	/**
	 * Remove an edge from the property graph
	 * @param edge
	 */
	public void removePropertyEdge(SemanticEdge edge) {
		removeEdge(edge, SemanticSubGraph.PROPERTY);
	}
	
	/**
	 * Remove an edge from the link graph
	 * @param edge
	 */
	public void removeLinkEdge(SemanticEdge edge) {
		removeEdge(edge, SemanticSubGraph.LINK);
	}
	
	/**
	 * Remove an edge from the dependency graph
	 * @param edge
	 */
	public void removeDependencyEdge(SemanticEdge edge) {
		removeEdge(edge, SemanticSubGraph.DEPENDENCY);
	}
	
	private void removeEdge(SemanticEdge edge, SemanticSubGraph subGraph) {
		this.graph.removeEdge(edge);
		switch (subGraph) {
		case LEX:
			this.lexGraph.removeEdge(edge);
			break;
		case PROPERTY:
			this.propertyGraph.removeEdge(edge);
			break;
		case LINK:
			this.linkGraph.removeEdge(edge);
			break;
		case DEPENDENCY:
			this.dependencyGraph.removeEdge(edge);
			break;
		case CONTEXT:
			this.contextGraph.removeEdge(edge);
			break;
		case ROLE:
			this.propertyGraph.removeEdge(edge);
			this.lexGraph.removeEdge(edge);
			this.roleGraph.removeEdge(edge);
			break;
		default:
			break;
		
		}
	}
	
	/**
	 * Remove a node from the role graph
	 * @param edge
	 */
	public void removeRoleNode(SemanticNode<?> node) {
		removeNode(node, SemanticSubGraph.ROLE);
	}
		
	/**
	 * Remove a node from the context graph
	 * @param edge
	 */
	public void removeContextNode(SemanticNode<?> node) {
		removeNode(node, SemanticSubGraph.CONTEXT);
	}
	
	/**
	 * Remove a node from the lexical graph
	 * @param edge
	 */
	public void removeLexNode(SemanticNode<?> node) {
		removeNode(node, SemanticSubGraph.LEX);
	}
	
	/**
	 * Remove a node from the property graph
	 * @param edge
	 */
	public void removePropertyNode(SemanticNode<?> node) {
		removeNode(node, SemanticSubGraph.PROPERTY);
	}
	
	/**
	 * Remove a node from the link graph
	 * @param edge
	 */
	public void removeLinkNode(SemanticNode<?> node) {
		removeNode(node, SemanticSubGraph.LINK);
	}
	
	/**
	 * Remove an edge from the dependency graph
	 * @param edge
	 */
	public void removeDependencyNode(SemanticNode<?> node) {
		removeNode(node, SemanticSubGraph.DEPENDENCY);
	}
	
	private void removeNode(SemanticNode<?> node, SemanticSubGraph subGraph) {
		this.graph.removeNode(node);
		switch (subGraph) {
		case LEX:
			this.lexGraph.removeNode(node);
			break;
		case PROPERTY:
			this.propertyGraph.removeNode(node);
			break;
		case LINK:
			this.linkGraph.removeNode(node);
			break;
		case DEPENDENCY:
			this.dependencyGraph.removeNode(node);
			break;
		case CONTEXT:
			this.contextGraph.removeNode(node);
			break;
		case ROLE:
			this.propertyGraph.removeNode(node);
			this.lexGraph.removeNode(node);
			this.roleGraph.removeNode(node);
			break;
		default:
			break;
		
		}
	}

	/**
	 * Open a window displaying the full graph
	 */
	public void display() {
		this.graph.display();
		
	}
	
	/**
	 * Open a window displaying the role graph
	 */
	public void displayRoles() {
		this.roleGraph.display();
	}
	
	/**
	 * Open a window displaying the context graph
	 */
	public void displayContexts() {
		this.contextGraph.display();
	}
	
	/**
	 * Open a window displaying the property graph
	 */
	public void displayProperties() {
		//this.propertyGraph.display();
		Set<SemanticNode<?>> nodes = new HashSet<SemanticNode<?>>();
		Set<SemanticEdge> edges = new HashSet<SemanticEdge>();
		Map<Color, List<SemanticNode<?>>> nodeProperties = new HashMap<Color, List<SemanticNode<?>>>();
		Map<Color, List<SemanticEdge>> edgeProperties = new HashMap<Color,List<SemanticEdge>>();
		List<SemanticNode<?>> propertyNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.MAGENTA, propertyNodes);
		List<SemanticEdge> propertyEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.MAGENTA, propertyEdges);
		List<SemanticNode<?>> roleNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.BLUE, roleNodes);
		List<SemanticEdge> roleEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.BLUE, roleEdges);
		
		for (SemanticNode<?> rNode : this.propertyGraph.getNodes()) {
			nodes.add(rNode);
			if (rNode instanceof ValueNode)
				propertyNodes.add(rNode);
			else if (rNode instanceof SkolemNode)
				roleNodes.add(rNode);
		}
		for (SemanticEdge rEdge : this.propertyGraph.getEdges()) {
			edges.add(rEdge);
			if (rEdge instanceof PropertyEdge)
				propertyEdges.add(rEdge);
			else if (rEdge instanceof RoleEdge)
				roleEdges.add(rEdge);
		}
		
		SemGraph subGraph = this.graph.getSubGraph(nodes, edges);
		subGraph.display(nodeProperties, edgeProperties);
	}

	/**
	 * Open a window displaying the lexical graph
	 */
	public void displayLex() {
		//this.lexGraph.display();
		Set<SemanticNode<?>> nodes = new HashSet<SemanticNode<?>>();
		Set<SemanticEdge> edges = new HashSet<SemanticEdge>();
		Map<Color, List<SemanticNode<?>>> nodeProperties = new HashMap<Color, List<SemanticNode<?>>>();
		Map<Color, List<SemanticEdge>> edgeProperties = new HashMap<Color,List<SemanticEdge>>();
		List<SemanticNode<?>> lexNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.CYAN, lexNodes);
		List<SemanticEdge> lexEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.CYAN, lexEdges);
		List<SemanticNode<?>> roleNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.BLUE, roleNodes);
		List<SemanticEdge> roleEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.BLUE, roleEdges);
		
		for (SemanticNode<?> rNode : this.lexGraph.getNodes()) {
			nodes.add(rNode);
			if (rNode instanceof SenseNode)
				lexNodes.add(rNode);
			else if (rNode instanceof SkolemNode)
				roleNodes.add(rNode);
		}
		for (SemanticEdge rEdge : this.lexGraph.getEdges()) {
			edges.add(rEdge);
			if (rEdge instanceof LexEdge)
				lexEdges.add(rEdge);
			else if (rEdge instanceof RoleEdge)
				roleEdges.add(rEdge);
		}
		
		SemGraph subGraph = this.graph.getSubGraph(nodes, edges);
		subGraph.display(nodeProperties, edgeProperties);
	}
	
	/**
	 * Open a window displaying the dependency graph
	 */
	public void displayDependencies() {
		this.dependencyGraph.display();
	}
	
	/**
	 * Open a window displaying the role and context graphs.
	 * This is generally the most readable/useful display
	 */
	public void generalDisplay() {
		Set<SemanticNode<?>> nodes = new HashSet<SemanticNode<?>>();
		Set<SemanticEdge> edges = new HashSet<SemanticEdge>();
		Map<Color, List<SemanticNode<?>>> nodeProperties = new HashMap<Color, List<SemanticNode<?>>>();
		Map<Color, List<SemanticEdge>> edgeProperties = new HashMap<Color,List<SemanticEdge>>();
		List<SemanticNode<?>> roleNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.BLUE, roleNodes);
		List<SemanticEdge> roleEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.BLACK, roleEdges);
		List<SemanticNode<?>> contextNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.LIGHT_GRAY, contextNodes);
		List<SemanticEdge> contextEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.LIGHT_GRAY, contextEdges);
		
		for (SemanticNode<?> rNode : this.roleGraph.getNodes()) {
			nodes.add(rNode);
			roleNodes.add(rNode);
		}
		for (SemanticEdge rEdge : this.roleGraph.getEdges()) {
			edges.add(rEdge);
			roleEdges.add(rEdge);
		}
		for (SemanticNode<?> cNode : this.contextGraph.getNodes()) {
			nodes.add(cNode);
			if (!roleNodes.contains(cNode)) {
				contextNodes.add(cNode);
			}
		}
		for (SemanticEdge cEdge : this.contextGraph.getEdges()) {
			edges.add(cEdge);
			if (!roleEdges.contains(cEdge)) {
				contextEdges.add(cEdge);
			}
		}
		SemGraph subGraph = this.graph.getSubGraph(nodes, edges);
		subGraph.display(nodeProperties, edgeProperties);
	}

	public void nonLexicalDisplay() {
		Set<SemanticNode<?>> nodes = new HashSet<SemanticNode<?>>();
		Set<SemanticEdge> edges = new HashSet<SemanticEdge>();
		Map<Color, List<SemanticNode<?>>> nodeProperties = new HashMap<Color, List<SemanticNode<?>>>();
		Map<Color, List<SemanticEdge>> edgeProperties = new HashMap<Color,List<SemanticEdge>>();
		List<SemanticNode<?>> roleNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.BLUE, roleNodes);
		List<SemanticEdge> roleEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.BLACK, roleEdges);
		List<SemanticNode<?>> contextNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.DARK_GRAY, contextNodes);
		List<SemanticEdge> contextEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.CYAN, contextEdges);		
		List<SemanticEdge> linkEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.GREEN, linkEdges);
		List<SemanticEdge> propertyEdges = new ArrayList<SemanticEdge>();
		edgeProperties.put(Color.LIGHT_GRAY, propertyEdges);
		List<SemanticNode<?>> propertyNodes = new ArrayList<SemanticNode<?>>();
		nodeProperties.put(Color.LIGHT_GRAY, propertyNodes);

		for (SemanticNode<?> rNode : this.roleGraph.getNodes()) {
			nodes.add(rNode);
			roleNodes.add(rNode);
		}
		for (SemanticEdge rEdge : this.roleGraph.getEdges()) {
			edges.add(rEdge);
			roleEdges.add(rEdge);
		}
		for (SemanticNode<?> cNode : this.contextGraph.getNodes()) {
			nodes.add(cNode);
			if (!roleNodes.contains(cNode)) {
				contextNodes.add(cNode);
			}
		}
		for (SemanticEdge cEdge : this.contextGraph.getEdges()) {
			edges.add(cEdge);
			if (!roleEdges.contains(cEdge)) {
				contextEdges.add(cEdge);
			}
		}
		for (SemanticNode<?> pNode : this.propertyGraph.getNodes()) {
			nodes.add(pNode);
			if (!roleNodes.contains(pNode) && !contextNodes.contains(pNode)) {
				propertyNodes.add(pNode);
			}
		}
		for (SemanticEdge pEdge : this.propertyGraph.getEdges()) {
			edges.add(pEdge);
			if (!roleEdges.contains(pEdge) && ! contextEdges.contains(pEdge)) {
				propertyEdges.add(pEdge);
			}
		}
		for (SemanticEdge lEdge : this.linkGraph.getEdges()) {
			edges.add(lEdge);
			if (!roleEdges.contains(lEdge) && ! contextEdges.contains(lEdge) && !propertyEdges.contains(lEdge)) {
				linkEdges.add(lEdge);
			}
		}
		SemGraph subGraph = this.graph.getSubGraph(nodes, edges);
		subGraph.display(nodeProperties, edgeProperties);
	}
	
	/**
	 * Displays the whole semantic graph as a string. Suitable for testsuites. 
	 * @return
	 */
	public String displayAsString(){
		//displayRoles();
		//displayDependencies();
		//displayContexts();
		StringBuilder stringToDisplay = new StringBuilder();
		HashMap<String,List<SemanticEdge>> traversed = new HashMap<String,List<SemanticEdge>>();
		// initialize the ctx and role root
		SemanticNode<?> ctxRoot = null;
		SemanticNode<?> roleRoot = null;
		ArrayList<SemanticNode<?>> roleRoots = new ArrayList<SemanticNode<?>>();
		// get the ctx root
		for (SemanticNode<?> node : contextGraph.getNodes()){
			if (contextGraph.getInEdges(node).isEmpty())
				ctxRoot = node;
		}
		// get the root node of the role graph
		for (SemanticNode<?> node : roleGraph.getNodes()){
			if (roleGraph.getInEdges(node).isEmpty())
				roleRoots.add(node);
		}
		// go through all edges of the role graph in order to put each node to a hashmap containing the "text" of the node and its distance to the role root
		for (SemanticEdge edge : roleGraph.getEdges()){
			// get the start and finish node of each edhe
			SemanticNode<?> start = roleGraph.getStartNode(edge);
			SemanticNode<?> finish = roleGraph.getEndNode(edge);
			// go through the role roots (there can be more than one for coordinated sentences) and see if the current start node is a child of the current root
			for (SemanticNode<?> n : roleRoots){
				if (n.equals(start) || roleGraph.getOutReach(n).contains(start)){
					roleRoot = n;
					break;
				} 
			}
			if (roleRoot == null) {
				stringToDisplay.append("Sentence was parsed but not converted to string because of unconnected nodes in role graph");
				return stringToDisplay.toString();
			}			
			// get the distance of each of the nodes from the root node
			List<SemanticEdge> distOfStart = roleGraph.getShortestPath(roleRoot, start);
			List<SemanticEdge> distOfFinish = new ArrayList<SemanticEdge>();
			distOfFinish.addAll(roleGraph.getShortestPath(roleRoot, start));
			distOfFinish.addAll(roleGraph.getShortestPath(start, finish));	
			// crete the text for each node (node label plus properties in line)
			String textOfStart = getNodeAndPropertiesAsText(start, ctxRoot);
			String textOfFinish = getNodeAndPropertiesAsText(finish, ctxRoot);
			// put these texts in a hash along with their distances to the root
			traversed.put(textOfStart, distOfStart);
			if (traversed.containsKey(textOfFinish))
				traversed.put(textOfFinish+"$", distOfFinish);
			else
				traversed.put(textOfFinish, distOfFinish);
		}
		// Sort the hash by the size of the list of values. Create the own comparator
		Comparator<Entry<String, List<SemanticEdge>>> valueComparator = new Comparator<Entry<String,List<SemanticEdge>>>() { 
			
			@Override 
			// get the size of the list of each entry and compare it to the next
			public int compare(Entry<String, List<SemanticEdge>> e1, Entry<String, List<SemanticEdge>> e2){
				try {
					Integer v1 = e1.getValue().size(); 
					Integer v2 = e2.getValue().size(); 
					return v1.compareTo(v2); 
				}
				catch(NullPointerException e){
					System.out.println("No connection to top node of the Role Graph");
					//e1.setValue(value)
					return 0;
				}
			} 
		};
		
		// Sort method needs a List, so we first convert the set of entries of the hash to a list
		List<Entry<String, List<SemanticEdge>>> listOfEntries = new ArrayList<Entry<String, List<SemanticEdge>>>(traversed.entrySet()); 
		
		// sorting listOfEntries by values using comparator
		Collections.sort(listOfEntries, valueComparator); 
		// create linked hashmap to put the sorted entries in
		LinkedHashMap<String, List<SemanticEdge>> sortedByValue = new LinkedHashMap<String, List<SemanticEdge>>(listOfEntries.size()); 
		
		// copying entries from list to map 
		for(Entry<String, List<SemanticEdge>> entry : listOfEntries){ 
			sortedByValue.put(entry.getKey(), entry.getValue()); 
		} 
		// get the entryset of the new sorted linked hashmap
		Set<Entry<String, List<SemanticEdge>>> entrySetSortedByValue = sortedByValue.entrySet(); 
		
		ArrayList<String> insertedStrings = new ArrayList<String>();
		
		// go through the ordered linked hashmap and reorder the entries to write
		for(Entry<String, List<SemanticEdge>> mapping : entrySetSortedByValue){
			// initialize all variables because they wont be needed for the root node
			SemanticEdge lastEdgeOfValueList = null;
			String labelOfLastEdgeOfValueList = "";
			SemanticNode<?> parentOfLastEdgeOfValueList = null;
			int indexOfParent = 0;
			int indexAfterParent = 0;
			// get the last edge of the list of values (this is the edge which directly governs the node of the key)
			if (mapping.getValue().size() != 0)	{	
				lastEdgeOfValueList = mapping.getValue().get(mapping.getValue().size()-1);
				// get the label of this edge
				labelOfLastEdgeOfValueList = lastEdgeOfValueList.getLabel();
				// get the the start node of theis edge, i.e. the parent of the finish node
				parentOfLastEdgeOfValueList = graph.getStartNode(lastEdgeOfValueList);
				String str = getNodeAndPropertiesAsText(parentOfLastEdgeOfValueList, ctxRoot);
				// get the position of this parent from within the string sofar
				indexOfParent = stringToDisplay.indexOf(str);
				// get the position of the ) coming after the position of the parent
				indexAfterParent = indexOfParent+str.length(); 			//stringToDisplay.indexOf(")", indexOfParent)+1;
			}
			// there are as many tabs as the size of the value list
			int tabs = mapping.getValue().size();
			// create a string with so many tabs as tabs
			String tabsToAdd = new String(new char[tabs]).replace("\0", "\t");	
			String toInsert = tabsToAdd+labelOfLastEdgeOfValueList+":"+mapping.getKey().replace("$", "");
			if (!insertedStrings.contains(toInsert)){
				stringToDisplay.insert(indexAfterParent, "\n"+toInsert);
				insertedStrings.add(toInsert);
			}
		} 
		
		stringToDisplay.replace(0, 1, "");
		stringToDisplay.insert(0, getModalsAsText(stringToDisplay.toString(), ctxRoot));
		return stringToDisplay.toString();
		
	}
	
	/***
	 * Extracts the properties and the contexts of the given node and packs all information in a string. 
	 * @param node
	 * @param ctxRoot
	 * @return
	 */
	private String getNodeAndPropertiesAsText(SemanticNode<?> node, SemanticNode<?> ctxRoot){
		String nodeText = "("+node.getLabel();
		// get all properties of this node
		for (SemanticEdge prop : propertyGraph.getOutEdges(node)){
			if (GraphLabels.propertyEdgeLabels.contains(prop.getLabel()) )
				nodeText += ","+prop.getDestVertexId();
		}
		String ctx = "";
		// get the outer edge of the context graph if the node is within the context graph 
		if (contextGraph.containsNode(node)){
			SemanticNode<?> parentCtx = contextGraph.getInNeighbors(node).iterator().next();
			if (parentCtx.getLabel().equals("ctx("+node.getLabel()+")")){
				SemanticNode<?> parent = contextGraph.getInNeighbors(parentCtx).iterator().next();
				SemanticEdge link = contextGraph.getEdges(parent, parentCtx).iterator().next();
				ctx = ","+parent+"_"+link;
			}
			else if (parentCtx.getLabel().contains("ctx(")){
				SemanticEdge link = contextGraph.getEdges(parentCtx, node).iterator().next();
				ctx = ","+parentCtx+"_"+link;
			}
			else if (parentCtx.getLabel().equals("top")){
				ctx = ",top";
			}
		}
		// if the node is not within the context graph, get the context from the skolem node itself
		else if (node instanceof SkolemNode)
				ctx = ","+((SkolemNodeContent) node.getContent()).getContext()+"_veridical";

		nodeText += ctx+")";
		return nodeText;
	}
	
	private String getModalsAsText(String stringToDisplay, SemanticNode<?> ctxRoot){
		String textToAdd = "";
		for (SemanticEdge ctxE : contextGraph.getOutEdges(ctxRoot)){
			SemanticNode<?> endNode = contextGraph.getEndNode(ctxE);
			 Pattern p1 = Pattern.compile("\\("+endNode.getLabel()+",.+top_"+ctxE.getLabel()+"\\)");
			 Matcher m1 = p1.matcher(stringToDisplay);	 
			if (!ctxE.getLabel().equals("ctx_hd") && m1.find() == false){
				Pattern p2 = Pattern.compile("\\("+endNode.getLabel().substring(4,endNode.getLabel().length()-1)+",.+top_"+ctxE.getLabel()+"\\)");
				Matcher m2 = p2.matcher(stringToDisplay);
				if (m2.find() == false)
					textToAdd += "("+endNode.getLabel()+",top_"+ctxE+")\n";
			}
		}
		
		return textToAdd;
	}

	
	/**
	 * Merge with another semantic graph
	 * @param other
	 */
	public void merge(SemanticGraph other) {
		this.graph.merge(other.graph);
		this.roleGraph.merge(other.roleGraph);
		this.contextGraph.merge(other.contextGraph);
		this.propertyGraph.merge(other.propertyGraph);
		this.lexGraph.merge(other.lexGraph);
		this.linkGraph.merge(other.linkGraph);
		this.dependencyGraph.merge(other.dependencyGraph);
	}
	
	/**
	 * Does this node have any lexical nodes connected to it
	 * @param term
	 * @return
	 */
	public boolean hasLexicalNodes(TermNode term) {
		if (this.lexGraph.containsNode(term)) {
			for (SemanticEdge edge : this.lexGraph.getOutEdges(term)) {
				if (edge.getClass().equals(LexEdge.class)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Get all the term nodes in the role and context graphs.
	 * Does not include any derived terms introduced into the lexical
	 * graph by naive semantics
	 * @return
	 */
	public List<TermNode> getTermNodes() {
		List<TermNode> retval = new ArrayList<TermNode>();
		for (SemanticNode<?> semNode : this.roleGraph.getNodes()) {
			if (TermNode.class.isAssignableFrom(semNode.getClass())) {
				retval.add((TermNode)semNode);
			}
		}
		for (SemanticNode<?> ctxNode : this.contextGraph.getNodes()) {
			if (TermNode.class.isAssignableFrom(ctxNode.getClass()) &&
					!retval.contains(ctxNode)) {
				retval.add((TermNode)ctxNode);
			}
		}
		return retval;
	}
	
	/**
	 * Get all skolem nodes in the role graph, plus any skolems in the lexical graph
	 * introduced by naive semantics
	 * @return
	 */
	public List<SkolemNode> getSkolemsAndDerivedSkolems() {
		List<SkolemNode> retval = new ArrayList<SkolemNode>();
		for (SemanticNode<?> semNode : this.graph.getNodes()) {
			if (SkolemNode.class.isAssignableFrom(semNode.getClass())) {
				retval.add((SkolemNode)semNode);
			}
		}
		return retval;
	}
	
	/**
	 * Get only the skolem nodes introduced by naive semantics
	 * @return
	 */
	public List<SkolemNode> getDerivedSkolems() {
		List<SkolemNode> retval = new ArrayList<SkolemNode>();
		Set<SemanticNode<?>> lexNodes = new HashSet<SemanticNode<?>>(this.lexGraph.getNodes());
		lexNodes.removeAll(this.roleGraph.getNodes());
		for (SemanticNode<?> semNode : lexNodes) {
			if (SkolemNode.class.isAssignableFrom(semNode.getClass())) {
				retval.add((SkolemNode)semNode);
			}
		}
		return retval;
	}
	
	/**
	 * Get all the skolem nodes in the role graph
	 * @return
	 */
	public List<SkolemNode> getSkolems() {
		List<SkolemNode> retval = new ArrayList<SkolemNode>();
		for (SemanticNode<?> semNode : this.roleGraph.getNodes()) {
			if (SkolemNode.class.isAssignableFrom(semNode.getClass())) {
				retval.add((SkolemNode)semNode);
			}
		}
		return retval;
	}
	
	/**
	 * Get all the terms (skolems or contexts) that have an ingoing or outgoing link edge 
	 * @return
	 */
	public List<TermNode> getLinkedNodes() {
		List<TermNode> retval = new ArrayList<TermNode>();
		for (SemanticNode<?> semNode : this.linkGraph.getNodes()) {
			if (TermNode.class.isAssignableFrom(semNode.getClass())) {
				retval.add((TermNode)semNode);
			}
		}
		return retval;
	}
	
	/**
	 * Get all the links edge in the graph
	 * @return
	 */
	public List<SemanticEdge> getLinks() {
		List<SemanticEdge> retval = new ArrayList<SemanticEdge>();
		for (SemanticEdge edge: this.linkGraph.getEdges()) {
			retval.add(edge);
		}
		return retval;
	}

	/**
	 * Get all the link edges leaving a node
	 * @param node
	 * @return
	 */
	public List<SemanticEdge> getLinks(SemanticNode<?> node) {
		List<SemanticEdge> retval = new ArrayList<SemanticEdge>();
		if (this.linkGraph.containsNode(node)) {
			for (SemanticEdge semEdge : this.linkGraph.getOutEdges(node)) {
				retval.add(semEdge);
			}	
		}
		return retval;
	}
	
	/**
	 * Get all the link edge coming into a node
	 * @param node
	 * @return
	 */
	public List<SemanticEdge> getInLinks(SemanticNode<?> node) {
		List<SemanticEdge> retval = new ArrayList<SemanticEdge>();
		if (this.linkGraph.containsNode(node)) {
			for (SemanticEdge semEdge : this.linkGraph.getInEdges(node)) {
				retval.add(semEdge);
			}
		}
		return retval;
	}
	
	/** 
	 * Get all the context nodes in the context graph
	 * @return
	 */
	public List<ContextNode> getContextNodes() {
		List<ContextNode> retval = new ArrayList<ContextNode>();
		for (SemanticNode<?> semNode : this.contextGraph.getNodes()) {
			if (semNode.getClass().equals(ContextNode.class)) {
				retval.add((ContextNode)semNode);
			}
		}
		return retval;
	}
	
	/**
	 * Get all the derived terms in the lexical graph
	 * @return
	 */
	public List<TermNode> getDerivedTermNodes() {
		List<TermNode> retval = new ArrayList<TermNode>();
		for (SemanticNode<?> semNode : this.lexGraph.getNodes()) {
			if (TermNode.class.isAssignableFrom(semNode.getClass())) {
				retval.add((TermNode)semNode);
			}
		}
		return retval;
	}
	
	
	/**
	 * Get all the skolem nodes that directly modify the specified skolem node
	 * @param node
	 * @return
	 */
	public List<SkolemNode> getDirectModifiers(SkolemNode node) {
		List<SkolemNode> retval = new ArrayList<SkolemNode>();
		if (this.roleGraph.containsNode(node)) {		
			for (SemanticEdge role : this.roleGraph.getOutEdges(node)) {
				if (RoleEdge.class.equals(role.getClass()) &&
						// Exclude restrictions (typically from relative clauses)
						!role.getLabel().equals("rstr")) {
					SemanticNode<?> semNode = this.getFinishNode(role);	
					if (SkolemNode.class.isAssignableFrom(semNode.getClass())) {
						retval.add((SkolemNode) semNode);
					} else if (ContextNode.class.isAssignableFrom(semNode.getClass())) {
						for (SemanticNode<?> headNode : this.roleGraph.getOutNeighbors(semNode)) {
							if (SkolemNode.class.isAssignableFrom(headNode.getClass())) {
								retval.add((SkolemNode) headNode);
							}
						}
					}
				}
			}
		}
		return retval;
	}
	
	/**
	 * Get all derived (naive semantic) modifiers of a term	
	 * @param node
	 * @return
	 */
	public List<SkolemNode> getLexModifiers(SkolemNode node) {
		List<SkolemNode> retval = new ArrayList<SkolemNode>();
		if (!this.roleGraph.containsNode(node) && this.lexGraph.containsNode(node)) {
			for (SemanticNode<?> semNode : this.lexGraph.getOutNeighbors(node)) {
				if (SkolemNode.class.isAssignableFrom(semNode.getClass())) {
					retval.add((SkolemNode) semNode);
				}
			}
		} else if (this.lexGraph.containsNode(node)) {
			for (SemanticNode<?> senseNode : this.lexGraph.getOutNeighbors(node)) {
				if (SenseNode.class.isAssignableFrom(senseNode.getClass())) {
					for (SemanticNode<?> semNode : this.lexGraph.getOutNeighbors(senseNode)) {
						if (SkolemNode.class.isAssignableFrom(semNode.getClass())) {
							retval.add((SkolemNode) semNode);
						}
					}
				}				
			}
		}
		return retval;
	}
		
	/**
	 * Get all of the non-derived modifiers of the term (i.e. not ones introduced by naive semantics)
	 * @param node
	 * @return
	 */
	public List<SkolemNode> getAllModifiers(SkolemNode node) {
		List<SkolemNode> retval = new ArrayList<SkolemNode>();
		if (this.roleGraph.containsNode(node)) {
			for (SemanticNode<?> semNode : this.roleGraph.getOutReach(node)) {
				if (SkolemNode.class.isAssignableFrom(semNode.getClass())) {
					retval.add((SkolemNode) semNode);
				}
			}
		}
		return retval;
	}
	
	public List<SkolemNode> getAllLexModifiers(SkolemNode node) {
		List<SkolemNode> retval = new ArrayList<SkolemNode>();
		
		if (this.lexGraph.containsNode(node)) {
			for (SemanticNode<?> semNode : this.lexGraph.getOutReach(node)) {
				if (SkolemNode.class.isAssignableFrom(semNode.getClass())
						&& !this.roleGraph.containsNode(semNode)) {
					retval.add((SkolemNode) semNode);
				}
			}
		}
		return retval;
	}
	
	/**
	 * Get all the nodes connected to the specified one by an outgoing edge
	 * @param node
	 * @return
	 */
	public List<SemanticNode<?>> getOutNeighbors(SemanticNode<?> node) {
		List<SemanticNode<?>> retval = new ArrayList<SemanticNode<?>>();
		for (SemanticNode<?> semNode : this.graph.getOutNeighbors(node)) {
			retval.add(semNode);
		}		
		return retval;
	}
	
	/**
	 * Get all the nodes connected to the specified one by an incoming edge
	 * @param node
	 * @return
	 */
	public List<SemanticNode<?>> getInNeighbors(SemanticNode<?> node) {
		List<SemanticNode<?>> retval = new ArrayList<SemanticNode<?>>();
		for (SemanticNode<?> semNode : this.graph.getInNeighbors(node)) {
			retval.add(semNode);
		}		
		return retval;
	}
	
	/**
	 * Get all the outgoing edges from a node
	 * @param node
	 * @return
	 */
	public List<SemanticEdge> getOutEdges(SemanticNode<?> node) {
		List<SemanticEdge> retval = new ArrayList<SemanticEdge>();
		if (! this.graph.containsNode(node)) {
			return retval;
		}
		for (SemanticEdge semEdge : this.graph.getOutEdges(node)) {
			retval.add(semEdge);
		}		
		return retval;
	}
	
	/**
	 * Get all the incoming edges to a nide
	 * @param node
	 * @return
	 */
	public List<SemanticEdge> getInEdges(SemanticNode<?> node) {
		List<SemanticEdge> retval = new ArrayList<SemanticEdge>();
		for (SemanticEdge semEdge : this.graph.getInEdges(node)) {
			retval.add(semEdge);
		}		
		return retval;
	}
	
	/** 
	 * Get all the sense nodes associated with a term
	 * @param term
	 * @return
	 */
	public List<SenseNode> getSenses(TermNode term) {
		List<SenseNode> retval = new ArrayList<SenseNode>();
		if (this.lexGraph.containsNode(term)) {
			for (SemanticNode<?> semNode : this.lexGraph.getOutNeighbors(term)) {
				if (SenseNode.class.isAssignableFrom(semNode.getClass())) {
					retval.add((SenseNode) semNode);
				}
			}
		}
		return retval;
	}
	
	/**
	 * Is this node lexically co-referent
	 * @param node
	 * @return
	 */
	public boolean isLexCoRef(TermNode node) {
		if (this.lexGraph.containsNode(node) && this.linkGraph.containsNode(node)) {
			for (SemanticEdge edge : getInEdges(node)) {
				if (edge.getLabel().equals("lexCoRef")) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Is the a node that only occurs in the lexical graph
	 * @param node
	 * @return
	 */
	public boolean isLexical(SemanticNode<?> node) {		
		return this.lexGraph.containsNode(node) && !this.roleGraph.containsNode(node) ;
	}
	
	/**
	 * Get the source node of the edge
	 * @param edge
	 * @return
	 */
	public SemanticNode<?> getStartNode(SemanticEdge edge) {
		return this.graph.getStartNode(edge);
	}
	
	/** 
	 * Get the destination node of the edge
	 * @param edge
	 * @return
	 */
	public SemanticNode<?> getFinishNode(SemanticEdge edge) {
		return this.graph.getEndNode(edge);
	}
	
	/**
	 * Get the graph comprising the specified nodes and edges
	 * @param nodes
	 * @param edges
	 * @return
	 */
	public SemGraph getSubGraph(Set<SemanticNode<?>> nodes, Set<SemanticEdge> edges) {
		return this.graph.getSubGraph(nodes, edges);
	}
	


	/** 
	 * Get the shortest directed path linking two nodes
	 * @param start
	 * @param end
	 * @return
	 */
	public List<SemanticEdge> getShortestPath(SemanticNode<?> start, SemanticNode<?> end) {
		return this.graph.getShortestPath(start, end);
	}

	/**
	 * Get the shortest undirected path between two nodes
	 * @param start
	 * @param end
	 * @return
	 */
	public List<SemanticEdge> getShortestUndirectedPath(SemanticNode<?> start,
			SemanticNode<?> end) {
		return this.graph.getShortestUndirectedPath(start, end);
	}

	public boolean isEmpty() {
		return this.graph.getNodes().isEmpty();
	}








}

