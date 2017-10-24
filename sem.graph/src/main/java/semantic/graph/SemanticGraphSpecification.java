package semantic.graph;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class SemanticGraphSpecification {
	protected Map<String, SemanticNode<?>> nodeMap;
	protected SortedSet<SemanticEdge> roleEdges;
	protected SortedSet<SemanticEdge> contextEdges;
	protected SortedSet<SemanticEdge> propertyEdges;
	protected SortedSet<SemanticEdge> lexEdges;
	protected SortedSet<SemanticEdge> linkEdges;
	protected SortedSet<SemanticEdge> dependencyEdges;
	protected SemanticNode<?> rootNode;
	protected String name;

	public SemanticGraphSpecification() {
		nodeMap = new LinkedHashMap<String, SemanticNode<?>>();
		roleEdges = new TreeSet<SemanticEdge>();
		contextEdges = new TreeSet<SemanticEdge>();
		propertyEdges = new TreeSet<SemanticEdge>();
		lexEdges = new TreeSet<SemanticEdge>();
		linkEdges = new TreeSet<SemanticEdge>();
		dependencyEdges = new TreeSet<SemanticEdge>();
		rootNode = null;
		name = "";
	}
	
	public SemanticGraphSpecification(SemanticGraph graph) {
		nodeMap = new LinkedHashMap<String, SemanticNode<?>>();
		for (SemanticNode<?> n : graph.getGraph().getNodes()) {
			nodeMap.put(n.getLabel(), n);
		}
		roleEdges = new TreeSet<SemanticEdge>(graph.roleGraph.getEdges());
		contextEdges = new TreeSet<SemanticEdge>(graph.contextGraph.getEdges());
		propertyEdges = new TreeSet<SemanticEdge>(graph.propertyGraph.getEdges());
		lexEdges = new TreeSet<SemanticEdge>(graph.lexGraph.getEdges());
		linkEdges = new TreeSet<SemanticEdge>(graph.linkGraph.getEdges());
		dependencyEdges = new TreeSet<SemanticEdge>(graph.dependencyGraph.getEdges());
		rootNode = graph.rootNode;
		name = graph.name;
		
	}

	public Map<String, SemanticNode<?>> getNodeMap() {
		return nodeMap;
	}

	public void setNodeMap(Map<String, SemanticNode<?>> nodeMap) {
		this.nodeMap = nodeMap;
	}

	public SortedSet<SemanticEdge> getRoleEdges() {
		return roleEdges;
	}

	public void setRoleEdges(SortedSet<SemanticEdge> roleEdges) {
		this.roleEdges = roleEdges;
	}

	public SortedSet<SemanticEdge> getContextEdges() {
		return contextEdges;
	}

	public void setContextEdges(SortedSet<SemanticEdge> contextEdges) {
		this.contextEdges = contextEdges;
	}

	public SortedSet<SemanticEdge> getPropertyEdges() {
		return propertyEdges;
	}

	public void setPropertyEdges(SortedSet<SemanticEdge> propertyEdges) {
		this.propertyEdges = propertyEdges;
	}

	public SortedSet<SemanticEdge> getLexEdges() {
		return lexEdges;
	}

	public void setLexEdges(SortedSet<SemanticEdge> lexEdges) {
		this.lexEdges = lexEdges;
	}

	public SortedSet<SemanticEdge> getLinkEdges() {
		return linkEdges;
	}

	public void setLinkEdges(SortedSet<SemanticEdge> linkEdges) {
		this.linkEdges = linkEdges;
	}

	public SortedSet<SemanticEdge> getDependencyEdges() {
		return dependencyEdges;
	}

	public void setDependencyEdges(SortedSet<SemanticEdge> dependencyEdges) {
		this.dependencyEdges = dependencyEdges;
	}

	public SemanticNode<?> getRootNode() {
		return rootNode;
	}

	public void setRootNode(SemanticNode<?> rootNode) {
		this.rootNode = rootNode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
