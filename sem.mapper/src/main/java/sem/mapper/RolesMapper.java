package sem.mapper;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.Subgraph;

import semantic.graph.SemGraph;
import semantic.graph.SemJGraphT;
import semantic.graph.SemanticEdge;
import semantic.graph.SemanticGraph;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.GraphLabels;
import semantic.graph.vetypes.RoleEdge;
import semantic.graph.vetypes.RoleEdgeContent;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.SkolemNodeContent;
import semantic.graph.vetypes.TermNode;
import semantic.graph.vetypes.TermNodeContent;


/**
 * This class maps the roles of the graph.
 * @author kkalouli
 *
 */
public class RolesMapper {
	private semantic.graph.SemanticGraph graph;
	private SemGraph depGraph;
	// will need passive for the passive graphs
	private boolean passive;
	// need it for verb coordination
	private boolean verbCoord;
	// need it for clausal coordination
	private boolean clausalCoord;
	// need it for non-verbal coordination
	boolean coord;
	// the verbal and noun forms of the main class "DepGraphToSemanticGraph"
	private ArrayList<String> verbalForms;
	private ArrayList<String> nounForms;
	// need to distinguish the subjs of the first and second verb in case of coordination
	private ArrayList<SemanticNode<?>> subjsOfFirstPred;
	private ArrayList<SemanticNode<?>> subjsOfSecondPred;
	// graphs to be processed (in case there are more because of coordination)
	private ArrayList<SemGraph> graphsToProcess;
	private ArrayList<SemGraph> graphsToAdd;
	// edges to be added to the role graph after dealing with coordination
	private ArrayList<SemanticEdge> edgesToAdd;
	


	public RolesMapper(semantic.graph.SemanticGraph graph,ArrayList<String> verbalForms,  ArrayList<String> nounForms ){
		this.graph = graph;
		this.depGraph = graph.getDependencyGraph();
		passive = false;
		verbCoord = false;
		clausalCoord = false;
		coord = false;
		this.verbalForms = verbalForms;
		this.nounForms = nounForms;
		this.subjsOfFirstPred = new ArrayList<SemanticNode<?>>();
		this.subjsOfSecondPred = new ArrayList<SemanticNode<?>>();
		this.graphsToProcess = new ArrayList<SemGraph>();
		this.graphsToAdd = new ArrayList<SemGraph>();
		this.edgesToAdd = new ArrayList<SemanticEdge>();
		
	}


	/**
	 * Integrate all the roles to one graph. Go through the graphsToProcess and 
	 * find out of there is clausal coordination or not. If no, then proceed. If there is,
	 * then split sentences to separate graphs. 
	 */
	public void integrateAllRoles(){
		SemGraph depGraph = graph.getDependencyGraph();
		// at the first run, add the current graph to the graphsToProcess
		if (graphsToProcess.isEmpty())
			graphsToProcess.add((SemGraph) graph.getDependencyGraph());

		// go through the graphs to process and check their coordination and add the corresponding roles
		for(Iterator<SemGraph> iter = graphsToProcess.iterator(); iter.hasNext();){ 
			SemGraph current = iter.next();
			Set<SemanticEdge> innerEdges = current.getEdges();
			for (SemanticEdge edge : innerEdges){
				checkCoordination(edge);
				// if there is clausal coord, remove this graph from the graphs and break the loop
				if (clausalCoord == true) {
					iter.remove();
					edgesToAdd.clear();
					break;
				}
				String edgeLabel = edge.getLabel();
				// get start and finish node of the current edge
				SemanticNode<?> start = depGraph.getStartNode(edge);
				SemanticNode<?> finish = depGraph.getEndNode(edge);
				//graph.displayRoles();
				// integrate the coordinating roles
				integrateCoordinatingRoles(start, finish, edge, edgeLabel);
				integrateBasicRoles(start, finish, edgeLabel);
				verbCoord = false;
				coord = false;
			}
			// after dealing with coordination, add all the roles of the edges of the edgesToAdd
			if (!edgesToAdd.isEmpty()){		
				integrateOtherEdges();
				edgesToAdd.clear();
			}
			// if there is clausal coord, add the seperate sentences to the graphs and set all
			// other variables to false. Then rerun this method for these new graphs. 
			if (clausalCoord == true) {
				graphsToProcess.addAll(graphsToAdd);
				passive = false;
				clausalCoord = false;
				verbCoord = false;
				coord = false;
				integrateAllRoles();
				break;
			}
			passive = false;
			clausalCoord = false;		
		}	
	}

	/**
	 * Integrate all edges that are stored in the edgesToAdd and dont involve coordination
	 */
	private void integrateOtherEdges(){
		//graph.displayRoles();
		for (SemanticEdge edge : edgesToAdd){
			// get start and finish node of the current edge
			SemanticNode<?> start = depGraph.getStartNode(edge);
			SemanticNode<?> finish = depGraph.getEndNode(edge);
			SemanticNode<?> begin = start;
			SemanticNode<?> end = finish;	
			/* if one of the nodes of the role graph is the current start or finish node,
			then get the parent of that node from within the role graph (which will be a 
			coordinated node) because this will be the new parent/child of the edge
			*/
			for (SemanticNode<?> rNode : graph.getRoleGraph().getNodes()){
				if (rNode.equals(start)){
					if (edge.getLabel().equals("amod") || edge.getLabel().equals("advmod") || edge.getLabel().equals("nmod")){
						begin = start;
					}
					else if (!graph.getRoleGraph().getInNeighbors(rNode).isEmpty()){
						SemanticNode<?> combNode = graph.getRoleGraph().getInNeighbors(rNode).iterator().next();
						begin = combNode;
					}
				} else if (rNode.equals(finish) ){
					if (!graph.getRoleGraph().getInNeighbors(rNode).isEmpty()){
						SemanticNode<?> combNode = graph.getRoleGraph().getInNeighbors(rNode).iterator().next();
						end = combNode;
					}
				}
			}
			if (!begin.equals(end)){
				integrateBasicRoles(begin,end,edge.getLabel());
			}
		}
	}

	/**
	 * This method creates a subgraph having the given node as its root and the specified subedges and subnodes as its members. The nodeToExclude
	 * is the node-child of the root node that shouldnt be included in the subgraph.  
	 * This method is called when a sentence contains clausal coordination so that it is separated into subgraphs, each subgraph with one predicate.
	 * @param node
	 * @param nodeToExclude
	 * @param listOfSubEdges
	 * @param listOfSubNodes
	 * @return
	 */
	private SemGraph createSubgraph(SemanticNode<?> node, SemanticNode<?> nodeToExclude, Set<SemanticEdge> listOfSubEdges, Set<SemanticNode<?>> listOfSubNodes){
		// got through all children edges of the specified node
		for (SemanticEdge subEdge : graph.getOutEdges(node)){
			// if the edge finishes with the nodeToEclude, move on
			if (graph.getFinishNode(subEdge).equals(nodeToExclude))
				continue;
			// otherwise, put into the list
			listOfSubEdges.add(subEdge);
			// put also the start anf finish node of that edge into the lists
			if (!listOfSubNodes.contains(graph.getStartNode(subEdge)))
				listOfSubNodes.add(graph.getStartNode(subEdge));
			if (!listOfSubNodes.contains(graph.getFinishNode(subEdge)))
				listOfSubNodes.add(graph.getFinishNode(subEdge));
			// do the same for the finish node of the current edge (recursively so that children of children are also added)
			createSubgraph(graph.getFinishNode(subEdge), nodeToExclude, listOfSubEdges, listOfSubNodes);
		}
		// create the subgraph
		return graph.getSubGraph(listOfSubNodes, listOfSubEdges);
	}
	
	/**
	 * Checks if the specified edge contains coordination. Or if one of the neighbor edges of this edge contains coordination.
	 * Sets the variables coord, verbCoord and clausalCoord. 
	 * @param edge
	 */
	private void checkCoordination(SemanticEdge edge){
		// get the start and finish node of the edge
		SemanticNode<?> start = depGraph.getStartNode(edge);
		SemanticNode<?> finish = depGraph.getEndNode(edge);
		// set the coordination  
		if (edge.getLabel().contains("conj") && !verbalForms.contains(((SkolemNodeContent) start.getContent()).getPosTag())){			
				coord = true;
		}
		// set the verb coordination and get the subjs of the coordinated verbs
		else if (edge.getLabel().contains("conj")  && verbalForms.contains(((SkolemNodeContent) start.getContent()).getPosTag()) ){
				verbCoord = true;
				for (SemanticEdge neighborEdge :graph.getOutEdges(graph.getFinishNode(edge))){ 
					if (neighborEdge.getLabel().contains("subj")){	
						if (!subjsOfSecondPred.contains(graph.getFinishNode(neighborEdge)))
							subjsOfSecondPred.add(graph.getFinishNode(neighborEdge));	
					}
				}
				for (SemanticEdge neighborEdge :graph.getOutEdges(graph.getStartNode(edge))){ 
					if (neighborEdge.getLabel().contains("subj")){	
						if (!subjsOfFirstPred.contains(graph.getFinishNode(neighborEdge)))
							subjsOfFirstPred.add(graph.getFinishNode(neighborEdge));	
					}
				}
		}
		// check if the current edge has neighbors that contain coordination
		for (SemanticEdge outEdge : depGraph.getEdges(finish)){
			if (outEdge.getLabel().contains("conj") && !outEdge.equals(edge)){
				coord = true;
				if (!edgesToAdd.contains(edge))
					edgesToAdd.add(edge);
			}
		}
		// check if the current edge has neighbors that contain coordination
		for (SemanticEdge outEdge : depGraph.getEdges(start)){
			if (outEdge.getLabel().contains("conj") && !outEdge.equals(edge)){
				coord = true;
				if (!edgesToAdd.contains(edge))
					edgesToAdd.add(edge);
			}
		}
		// if there is verb coordination but the subjects of the two corodinated verbs are different, then there is clause coordination
		if (verbCoord == true && !subjsOfFirstPred.containsAll(subjsOfSecondPred)){
			coord = false;
			verbCoord = false;
			// create the subgraph with the start node of this edge as the root node
			Set<SemanticEdge> listOfSubEdges = new HashSet<SemanticEdge>();
			Set<SemanticNode<?>> listOfSubNodes = new HashSet<SemanticNode<?>>();
			SemGraph mainSubgraph = createSubgraph(start, finish, listOfSubEdges,listOfSubNodes);
			listOfSubEdges.clear();
			listOfSubNodes.clear();
			subjsOfFirstPred.clear();
			subjsOfSecondPred.clear();
			// create the subgraph with the finish node of this edge as the root node
			SemGraph subgraph = createSubgraph(finish, start, listOfSubEdges,listOfSubNodes);
			graphsToAdd.add(mainSubgraph);
			graphsToAdd.add(subgraph); 
			clausalCoord = true;
			
			
		}
	}
	

	/**
	 * This method integrates the coordinated nodes: the two coordinated terms create a new term node which contains both of them. 
	 * @param start
	 * @param finish
	 * @param edge
	 * @param edgeLabel
	 */
	private void integrateCoordinatingRoles(SemanticNode<?> start, SemanticNode<?> finish, SemanticEdge edge, String edgeLabel){
		// the role that will have to be added at the end to the role graph
		String role = "";
	
		// only go here if there is coordination in the current edge 
		if (edgeLabel.contains("conj") ){
			// create the combined node of the coordinated terms and the edge of one of its children
			if (!graph.getRoleGraph().containsNode(start) && !graph.getRoleGraph().containsNode(finish)){
				TermNode combNode = new TermNode(start+"_"+edgeLabel.substring(5)+"_"+finish, new TermNodeContent());
				role = GraphLabels.IS_ELEMENT;
				RoleEdge combEdge1 = new RoleEdge(role, new RoleEdgeContent());
				graph.addRoleEdge(combEdge1, combNode, start);
				// create also the other edge of the other child
				RoleEdge combEdge2 = new RoleEdge(role, new RoleEdgeContent());
				graph.addRoleEdge(combEdge2, combNode, finish);	
			}
		} 
		// set the contexts of the start and finish nodes
		if (start instanceof SkolemNode && finish instanceof SkolemNode){
			// at this point set the contexts of all words that get involved to the role graph. the context is top at the moment 
			((SkolemNodeContent) start.getContent()).setContext("top");
			((SkolemNodeContent) finish.getContent()).setContext("top");
		}
	}

	/**
	 * Integrate the basic roles. These depend on the edges they are involved in. 
	 * @param start
	 * @param finish
	 * @param edgeLabel
	 * @param role
	 */
	private void integrateBasicRoles(SemanticNode<?> start, SemanticNode<?> finish, String edgeLabel){		
		String role = "";
		// switch of the different edge labels
		switch (edgeLabel){
		case "ccomp": if (verbCoord == false && coord == false){
			role = GraphLabels.COMP;
		}
		break;
		case "xcomp": if (verbCoord == false && coord == false){
			role = GraphLabels.XCOMP;
		}
		break;
		case "dobj" : if (verbCoord == false && coord == false){
			role = GraphLabels.OBJ;
		}
		break;
		case "iobj" : if (verbCoord == false && coord == false){
			role = GraphLabels.XCOMP;
		}
		break;
		// only add it when the verb coord is false; when true, it will be added later
		case "nsubj" : if (verbCoord == false && coord == false){
			role = GraphLabels.SUBJ;
		}
		break;
		// only add it when the verb coord is false; when true, it will be added later
		case "nsubjpass" : if (verbCoord == false && coord == false){
			role = GraphLabels.OBJ;
		}
		passive = true;
		break;
		// only add it when the verb coord is false; when true, it will be added later
		case "csubj" : if (verbCoord == false && coord == false){
			role = GraphLabels.SUBJ;
		}
		break;
		// only add it when the verb coord is false; when true, it will be added later
		case "csubjpass" : if (verbCoord == false && coord == false){
			role = GraphLabels.OBJ;
		}	
		passive = true;
		break;
		case "nmod:agent" : if (verbCoord == false && coord == false && passive == true){ 
			role = GraphLabels.SUBJ;
		}	
		break;
		case "nmod:by" : if (verbCoord == false && coord == false && passive == true){ 
			role = GraphLabels.SUBJ;

		}	
		break;
		// only add it when the verb coord is false; when true, it will be added later
		case "nsubj:xsubj": if (verbCoord == false && coord == false){
			role = GraphLabels.SUBJ;
		}
		break;
		case "amod": if (verbCoord == false && coord == false){
			role = GraphLabels.AMOD;
		}
		break;
		case "advmod": if (verbCoord == false && coord == false){
			role = GraphLabels.AMOD;
		}
		break;
		case "acl:relcl": role = GraphLabels.RESTRICTION;
		break;
		case "nummod": if (verbCoord == false && coord == false){
			role = GraphLabels.RESTRICTION;
		}
		}
		// nmod can have difefrent subtypes, therefore it is put here and not within the switch 
		if (role.equals("") && verbCoord == false && coord == false && edgeLabel.contains("nmod")){
			role = GraphLabels.NMOD;
		} else if (edgeLabel.contains("acl")){
			role = GraphLabels.NMOD;	
		}
		
		// if the role is not empty, create the edge for this role and set the contexts
		if (!role.equals("")){
			RoleEdge roleEdge = new RoleEdge(role, new RoleEdgeContent());
			graph.addRoleEdge(roleEdge, start, finish);
			if (start instanceof SkolemNode && finish instanceof SkolemNode){
				// at this point set the contexts of all words that get involved to the role graph. the context is top at the moment 
				((SkolemNodeContent) start.getContent()).setContext("top");
				((SkolemNodeContent) finish.getContent()).setContext("top");
			}
		}
	}


}
