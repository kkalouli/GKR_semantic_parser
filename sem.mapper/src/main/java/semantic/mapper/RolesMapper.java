package semantic.mapper;

import java.util.ArrayList;
import java.util.Set;

import semantic.graph.SemGraph;
import semantic.graph.SemanticEdge;
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
	boolean passive;
	// need it for verb coordination
	boolean verbCoord;
	// the semantic edge between a coordinated predicate and its subject
	SemanticEdge subjCoord;
	// need it for non-verbal coordination
	boolean coord;
	// the verbal and noun forms of the main class "DepGraphToSemanticGraph"
	ArrayList<String> verbalForms;
	ArrayList<String> nounForms;


	public RolesMapper(semantic.graph.SemanticGraph graph,ArrayList<String> verbalForms,  ArrayList<String> nounForms ){
		this.graph = graph;
		this.depGraph = graph.getDependencyGraph();
		passive = false;
		verbCoord = false;
		subjCoord = null;
		coord = false;
		this.verbalForms = verbalForms;
		this.nounForms = nounForms;
	}


	/**
	 * Integrate all the roles to one graph. Go through the edges of the dep graph and add the roles. Incorporate any 
	 * coordinating roles and then the basic ones. 
	 */
	public void integrateAllRoles(){
		SemGraph depGraph = graph.getDependencyGraph();
		Set<SemanticEdge> edges = depGraph.getEdges();	
		// go through the edges of the graph
		for (SemanticEdge edge : edges){
			String edgeLabel = edge.getLabel();
			// get start and finish node of the current edge
			SemanticNode<?> start = depGraph.getStartNode(edge);
			SemanticNode<?> finish = depGraph.getEndNode(edge);
			// integrate the coordinating roles
			integrateCoordinatingRoles(start, finish, edge, edgeLabel);
			// and the basic roles
			integrateBasicRoles(start, finish, edgeLabel);

		}
	}

	/**
	 * The coordinating roles have to be treated as a special case because the presence of multiple coordinations is changing
	 * the output. Generally: the two coordinated terms create a new term node which contains both of them. If there is verb coordination,
	 * then all arguments going out of each of the verbs have to be redirected to go out of the common node. If there is non-verbal
	 * coordination, then the common node has to acquire all roles that its components acquire.
	 * Following sentences work:
	 * The man and the woman are walking.
	 * The man is walking and talking.
	 * The man and the woman are walking and talking.
	 * The tall and thin man is walking.
	 * The tall and thin man is walking and talking.
	 * The man is making pasta and fish.
	 *  etc.
	 * @param start
	 * @param finish
	 * @param edge
	 * @param edgeLabel
	 */
	private void integrateCoordinatingRoles(SemanticNode<?> start, SemanticNode<?> finish, SemanticEdge edge, String edgeLabel){
		// the role that will have to be added at the end to the role graph
		String role = "";
		// go through the edges of the current finish node and see if there is conj. 
		// This loop is needed when there is non-verbal coordination so that coord can be set to true
		for (SemanticEdge sEdge : graph.getDependencyGraph().getEdges(finish)){
			if (sEdge.getLabel().contains("conj") && !edge.equals(sEdge)){			
				coord = true;
				role = "";
				break;
			}
		}
		// go through the out edges of the start node and see if there is conj.
		// This loop is needed when there is verb coordination. 
		for (SemanticEdge stEdge : graph.getDependencyGraph().getOutEdges(start)){
			// check if there is conj and also if there is really verb coordination
			if (stEdge.getLabel().contains("conj") && !edge.equals(stEdge) && verbalForms.contains(((SkolemNodeContent) start.getContent()).getPosTag()) ){
				verbCoord = true;
				// if one of the edges contains the subj, then this is stored as the subj of the coordinated predicate
			} else if (stEdge.getLabel().contains("subj")){
				subjCoord = stEdge;
			}	
		}

		// only go here if there is coordination in the current edge 
		if (edgeLabel.contains("conj")){
			// create the combined node of the coordinated terms and the edge of one of its children
			TermNode combNode = new TermNode(start+"_"+edgeLabel.substring(5)+"_"+finish, new TermNodeContent());
			role = GraphLabels.IS_ELEMENT;
			RoleEdge combEdge1 = new RoleEdge(role, new RoleEdgeContent());
			graph.addRoleEdge(combEdge1, combNode, start);
			// create also the other edge of the other child
			RoleEdge combEdge2 = new RoleEdge(role, new RoleEdgeContent());
			graph.addRoleEdge(combEdge2, combNode, finish);
			/* if there is verb coordination in the sentence and the current coordination is verbal (it might be the case that the sentence has
			/ verb coordination but that the current edge is a coordinated noun or adj) . We need this for sentences with verb coordination, even
			 * if there is also other coordination involved.
			 */
			if (verbCoord == true &&  verbalForms.contains(((SkolemNodeContent) start.getContent()).getPosTag())){
				// get the label of the subj edge that was stored before
				RoleEdge subjEdge = new RoleEdge(GraphLabels.SUBJ, new RoleEdgeContent());
				// get the real subj of that edge
				SemanticNode<?> subj = graph.getFinishNode(subjCoord);
				// in case there is also noun coordination, then take the combined noun node as the subject of the coordinated predicate
				if (graph.getRoleGraph().getInEdges(subj).iterator().hasNext()){
					SemanticEdge nodeEdge = graph.getRoleGraph().getInEdges(subj).iterator().next();
					if (nodeEdge.getLabel().equals("is_element")){
						graph.addRoleEdge(subjEdge, combNode, graph.getRoleGraph().getInNeighbors(subj).iterator().next());
					}
				} // else take the single noun as the subject
				else {
					graph.addRoleEdge(subjEdge, combNode, graph.getFinishNode(subjCoord));
				}
			}

			/* if there is noun coordination and the current edge does not involve verbs and either there is verb coordination or the coordinating
			 * terms are amod to a noun. We need this for all cases where there is no verb coordination or there is verb coordination but there are
			 * also amod coordinating so that we can add the parent on which they depend. 
			*/
			if (coord == true && !verbalForms.contains(((SkolemNodeContent) start.getContent()).getPosTag()) && 
					(verbCoord == false || graph.getDependencyGraph().getInEdges(start).iterator().next().getLabel().equals("amod")) ){	
				SemanticEdge parent = graph.getDependencyGraph().getInEdges(start).iterator().next();
				// get the parent of the coordinated nouns				
				String label = "";
				if (parent.getLabel().contains("subj")){
					label = GraphLabels.SUBJ;
				} else if (parent.getLabel().contains("amod")){
					label = GraphLabels.AMOD;
				} else if (parent.getLabel().contains("advmod")){
					label = GraphLabels.AMOD;
				} else if (parent.getLabel().contains("obj")){
					label = GraphLabels.OBJ;
				} else if (parent.getLabel().contains("ccomp")){
					label = GraphLabels.COMP;
				} else if (parent.getLabel().contains("xcomp")){
					label = GraphLabels.XCOMP;
				}
				// add the edge between the combined noun node and its parent
				RoleEdge parentEdge = new RoleEdge(label, new RoleEdgeContent());
				graph.addRoleEdge(parentEdge, graph.getDependencyGraph().getStartNode(parent), combNode);
			}
		}
		if (start instanceof SkolemNode && finish instanceof SkolemNode){
			// at this point set the contexts of all words that get involved to the role graph. the context is top at the moment 
			((SkolemNodeContent) start.getContent()).setContext("top;");
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
		// nmod can have difefrent subtypes, therefore it is put here and not within the switch
		if (edgeLabel.contains("nmod")){
			role = GraphLabels.NMOD;
		} 
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
			passive = true;
		}
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
		case "nmod:agent" : if (passive == true){ 
			role = GraphLabels.SUBJ;

		}	
		break;
		case "nmod:by" : if (passive == true){ 
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
		}
		// if the role is not empty, create the edge for this role and set the contexts
		if (!role.equals("")){
			RoleEdge roleEdge = new RoleEdge(role, new RoleEdgeContent());
			graph.addRoleEdge(roleEdge, start, finish);
			if (start instanceof SkolemNode && finish instanceof SkolemNode){
				// at this point set the contexts of all words that get involved to the role graph. the context is top at the moment 
				((SkolemNodeContent) start.getContent()).setContext("top;");
				((SkolemNodeContent) finish.getContent()).setContext("top");
			}
		}
	}


}
