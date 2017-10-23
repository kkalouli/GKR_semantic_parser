
package semantic.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.Color;
import java.io.Serializable;

import org.springframework.util.SerializationUtils;

import semantic.graph.vetypes.ContextNode;
import semantic.graph.vetypes.LexEdge;
import semantic.graph.vetypes.RoleEdge;
import semantic.graph.vetypes.SenseNode;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.TermNode;



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
		this.propertyGraph.display();
	}

	/**
	 * Open a window displaying the lexical graph
	 */
	public void displayLex() {
		this.lexGraph.display();
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

