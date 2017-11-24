package sem.mapper;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import semantic.graph.EdgeContent;
import semantic.graph.SemGraph;
import semantic.graph.SemanticEdge;
import semantic.graph.SemanticGraph;
import semantic.graph.SemanticNode;
import semantic.graph.vetypes.ContextHeadEdge;
import semantic.graph.vetypes.ContextNode;
import semantic.graph.vetypes.ContextNodeContent;
import semantic.graph.vetypes.GraphLabels;
import semantic.graph.vetypes.RoleEdgeContent;
import semantic.graph.vetypes.SkolemNode;
import semantic.graph.vetypes.SkolemNodeContent;

public class ContextMapper {
	private semantic.graph.SemanticGraph graph;
	private SemGraph depGraph;
	private HashMap<SemanticNode<?>,SemanticNode<?>> negCtxs;
	private HashMap<SemanticNode<?>,String> coordCtxs;
	private boolean coord;
	private boolean verbCoord;
	private boolean clausalCoord;
	private boolean disjunction;

	public ContextMapper(semantic.graph.SemanticGraph graph){
		this.graph = graph;
		this.depGraph = this.graph.getDependencyGraph();
		this.negCtxs = new HashMap<SemanticNode<?>,SemanticNode<?>>();
		this.coordCtxs = new HashMap<SemanticNode<?>,String>();
		this.coord = false;
		this.verbCoord = false;
		this.disjunction = false;
	}

	/**
	 * Integrates all possible contexts of the sentence. 
	 */
	public void integrateAllContexts(){
		integrateClausalContexts();
		integrateCoordinatingContexts();
		integrateNegativeContexts();
		integrateModalContexts();		
		SemGraph conGraph = graph.getContextGraph();	
		// make sure that all "root" nodes existing in the role graph have been added to the context graph; if not add them now
		for (SemanticNode<?> node : graph.getRoleGraph().getNodes()){
			boolean foundNode = false;
			// do the following only for the root nodes: the ones without inNeighbors
			if (graph.getRoleGraph().getInNeighbors(node).isEmpty()){
				for (SemanticNode<?> ctxNode : graph.getContextGraph().getNodes()){
					// check if this node or its context exist in the context graph
					if (ctxNode.equals(node) || ctxNode.getLabel().equals("ctx("+node.getLabel()+")")){
						foundNode = true;
					}
				}
				// if it doesnt, add it now to the contxex graph
				if (foundNode == false)
					addSelfContextNodeAndEdgeToGraph(node);
			}

		}
		// make sure that the context graph is left with one top context; if there are more than one "tops", then
		//merge the two tops to one
		int noOfTops = 0;
		ArrayList<SemanticNode<?>> listOfTops = new ArrayList<SemanticNode<?>>();
		SemanticNode<?> firstTop = null;
		for (SemanticNode<?> cNode: conGraph.getNodes()){			
			// if no incoming edges, it is the top context
			if (conGraph.getInEdges(cNode).isEmpty()){
				noOfTops ++;
				// if there are more than one tops,
				if (noOfTops > 1){
					// get all the edges of this top and add them to the first top which was found
					for (SemanticEdge edge : graph.getContextGraph().getEdges(cNode)){
						ContextHeadEdge ctxEdge = new ContextHeadEdge(edge.getLabel(), new RoleEdgeContent());
						graph.addContextEdge(ctxEdge, firstTop, graph.getFinishNode(edge));
					}
					listOfTops.add(cNode);
					continue;
				}
				// otherwise (=if it is the first top), set the label to top
				cNode.setLabel("top");
				firstTop = cNode;
			} 
		}
		// remove all edges of the other tops
		for (SemanticNode<?> top : listOfTops){
			graph.removeContextNode(top);
		}
	}

	/**
	 * Integrates any modal contexts of the sentence. 
	 */
	private void integrateModalContexts(){
		Set<SemanticNode<?>> nodes = depGraph.getNodes();
		for (SemanticNode<?> node : nodes){
			if (((SkolemNodeContent) node.getContent()).getStem().equals("might") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("should")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("must")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("ought")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("can")
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("would")){
				// create the self node of the modal
				SemanticNode<?> modalNode = addSelfContextNodeAndEdgeToGraph(node);
				// get the head of the modal
				SemanticNode<?> head = depGraph.getInNeighbors(node).iterator().next();
				//System.out.println(head);
				SemanticNode<?> headNode  = null;
				// initializing the node of the negation of the modal (negModalNode) to see if there is negation
				// over the modal or somewhere else in the sentence
				SemanticNode<?> negModalNode = null;
				for (Entry<SemanticNode<?>, SemanticNode<?>> e : negCtxs.entrySet()) {
					SemanticNode<?> key = e.getKey();
					SemanticNode<?> value = e.getValue();
					if (value.equals(head)){
						negModalNode = key;
					}
				}		
				// create the head node and edge between the modal and its verb head only if there is no negation involved
				// or if there is no negation of the modal but of another word
				if (negCtxs.isEmpty() || negModalNode == null){
					// create the self node of the head
					headNode = addSelfContextNodeAndEdgeToGraph(head);
					String label = ((SkolemNodeContent) node.getContent()).getStem();
					ContextHeadEdge labelEdgeHead = new ContextHeadEdge(label, new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeHead, modalNode, headNode);
					// re-set the context of the head of the modal to the modal itself
					((SkolemNodeContent) head.getContent()).setContext(headNode.getLabel());
				} 
				/* if negation is involved, then there are two cases:
				 * 1. negation over the modal (can, could, would): here the neg context graph is practically 
				 * disolved and we just keep the negation context node which is then used as top for the 
				 * modal context graph to eb created
				 */
				else if (((SkolemNodeContent) node.getContent()).getStem().equals("can") ){
					// remove all edges and nodes that are dependent on the negation_context node
					for (SemanticNode<?> sNode : graph.getContextGraph().getOutNeighbors(negModalNode)){
						for (SemanticEdge sEdge : graph.getContextGraph().getEdges(sNode)){
							if (sEdge.getLabel().equals("not")){
								headNode = graph.getContextGraph().getEndNode(sEdge);
								graph.getContextGraph().removeEdge(sEdge);
							}					
						}
					}
					// create the edge between the modal and its head
					String label = ((SkolemNodeContent) node.getContent()).getStem();
					ContextHeadEdge labelEdgeHead = new ContextHeadEdge(label, new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeHead, modalNode, headNode);		
					// create and edge between the negation_context node and the modal (so that the negation is over the modal)
					ContextHeadEdge labelEdgeCan = new ContextHeadEdge("not", new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeCan, negModalNode, modalNode);
					// re-set the context of the head of the modal to the ctx of that head  
					((SkolemNodeContent) head.getContent()).setContext(headNode.getLabel());
					// re-set the context of the modal to the ctx of that modal
					((SkolemNodeContent) node.getContent()).setContext(modalNode.getLabel());	
				} 
				/*
				 * 2. negation is under the modal (all other modals): here the negation context graph is combined with the
				 * modal graph to be created in a way that the negated graph is embedded under the modal graph
				 */
				else {
					String label = ((SkolemNodeContent) node.getContent()).getStem();
					ContextHeadEdge labelEdgeHead = new ContextHeadEdge(label, new  RoleEdgeContent());
					graph.addContextEdge(labelEdgeHead, modalNode, negModalNode);
					// resetting the contexts
					for (SemanticEdge s : graph.getContextGraph().getOutEdges(negModalNode)){
						if (s.getLabel().equals("ctx_hd")){
							SemanticNode<?> negNode = graph.getFinishNode(s);
							// re-set the context of the negation to its ctx
							((SkolemNodeContent) negNode.getContent()).setContext(negModalNode.getLabel());
						} else if (s.getLabel().equals("not")){
							headNode = graph.getFinishNode(s);
							// re-set the context of the head of the modal to the ctx of that head
							((SkolemNodeContent) head.getContent()).setContext(headNode.getLabel());
						}
					}

				}
			}
		}
	}

	/**
	 * Integrates all negative contexts of the sentence. 
	 */
	private void integrateNegativeContexts(){
		SemanticNode<?> negNode = null;
		Set<SemanticNode<?>> nodes = depGraph.getNodes();
		SemanticNode<?> previousNegNode = null;
		for (SemanticNode<?> node : nodes){
			/* First if clause deals with the following negation:
			 * The dog is not carrying the stick.
			 * --> the head of the negation is a predicate and all arguments exist in top
			 */
			if (((SkolemNodeContent) node.getContent()).getStem().equals("not") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("n't") ){			
				// initialize the head of the negation
				SemanticNode<?> head = null;
				// initialize the context of the head of the negation
				SemanticNode<?> ctxHead = null;
				// get the dep head
				SemanticNode<?> depHead = depGraph.getInNeighbors(node).iterator().next();
				//If we have verbal coordination, then the combined node of the two predicates has to be negated
				if (coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("verbal")){
					// find the role head of this dep head
					head = graph.getRoleGraph().getInNeighbors(depHead).iterator().next();
					// if the negated node doesnt already exist (in cases of combined predicates), then add the self node of the negation
					if (!graph.getContextGraph().containsNode(previousNegNode)){
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
					}
					// set the previousNegNode to the current one.
					previousNegNode = negNode;
					/* if there is disjunction in the sentence (and not only conjunction), then we have to first find the context of the
					 * current node-predicate and then find the context of this context.
					 *  For that we need to get the ctxOfDepHead (= the context of the current node-predicate)
					and ctxHead (= the context of the combined node)
					 */
					if (disjunction == true){
						SemanticNode<?> ctxOfDepHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
						ctxHead = graph.getContextGraph().getInNeighbors(ctxOfDepHead).iterator().next();
						/*
						 * if there is also clausal coordination, then the edge linking the top node with the combined node of the predicates
						 * should be removed and a new edge from the top to the negated context should be created.
						 */
						if (clausalCoord == true) {
							SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
							SemanticNode<?> top = graph.getStartNode(orEdge);
							graph.removeContextEdge(orEdge);
							ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
							graph.addContextEdge(label, top, negNode);
						}
						// if there is no disjunction, then we just need to find the context of the current node-predicate. 
					} else {
						// find the context of the current node-predicate
						ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
					}
					/*
					 * if we dont have verbal coordination, but either clausal or noun/adject/etc, or if we have coord generally, 
					 *  then the coordinated nodes have to be added to their parent. 
					 */
				} else if ((coordCtxs.containsKey(depHead) && !coordCtxs.get(depHead).equals("verbal")) || coord == true) {
					/*
					 * If there is dijunction and also clausal coordination, then the edge linking the top node with the current node
					 * should be removed and a new edge from the top to the negated context should be created.
					 */
					if (disjunction == true && coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("clausal")){
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
						ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
						SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
						SemanticNode<?> top = graph.getStartNode(orEdge);
						graph.removeContextEdge(orEdge);
						ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
						graph.addContextEdge(label, top, negNode);
						// if no disjunction, then just create the self neg node and get the parent of the current node	
					} else {
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
						head = depGraph.getInNeighbors(node).iterator().next();
						// create the self node of the head of the negation
						ctxHead = addSelfContextNodeAndEdgeToGraph(head);
					}
				}

				// create and add the edge between the negation and its head 
				ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.NOT, new  RoleEdgeContent());
				graph.addContextEdge(labelEdge, negNode, ctxHead);	
				// put the negated node created and its head to the hash
				negCtxs.put(negNode, head);
				// re-set the context of the head of the negation to the ctx of that head
				if (head instanceof SkolemNode)
					((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());
				//graph.displayContexts();
			}
			/*
			 * second else if deals with the sentences:
			 * No dog is carrying a stick.
			 * The dog is carrying no stick.
			 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a NP
			 */
			else if (((SkolemNodeContent) node.getContent()).getStem().equals("no") ){
				// get the head of the negation
				SemanticNode<?> head = depGraph.getInNeighbors(node).iterator().next();;
				SemanticNode<?> ctxHead = null;
				// get the head of the head in order to hopefully reach the main verb
				SemanticNode<?> headOfHead = depGraph.getInNeighbors(head).iterator().next();
				//If we have verbal coordination, then the combined node of the two predicates has to be negated
				if (coordCtxs.containsKey(headOfHead) && coordCtxs.get(headOfHead).equals("verbal")){
					if (!graph.getContextGraph().containsNode(previousNegNode)){
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
					}
					previousNegNode = negNode;
					/* if there is disjunction in the sentence (and not only conjunction), then we have to first find the context of the
					 * current headOfHead and then find the context of this context.
					 *  For that we need to get the ctxOfDepHead (= the context of the current node-predicate)
					and ctxHead (= the context of the combined node)
					 */
					if (disjunction == true){
						SemanticNode<?> ctxOfDepHead = graph.getContextGraph().getInNeighbors(headOfHead).iterator().next();
						ctxHead = graph.getContextGraph().getInNeighbors(ctxOfDepHead).iterator().next();
						/*
						 * if there is also clausal coordination, then the edge linking the top node with the combined node of the predicates
						 * should be removed and a new edge from the top to the negated context should be created.
						 */
						if (clausalCoord == true) {
							SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
							SemanticNode<?> top = graph.getStartNode(orEdge);
							graph.removeContextEdge(orEdge);
							ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
							graph.addContextEdge(label, top, negNode);
						}
						// if there is no disjunction
					} else {
						// find the context head of the head of the head
						ctxHead = graph.getContextGraph().getInNeighbors(headOfHead).iterator().next();
					}
					// set the context of the children of this context to this context
					for (SemanticNode<?> child : graph.getOutNeighbors(ctxHead)){
						if (child instanceof SkolemNode)
							((SkolemNodeContent) child.getContent()).setContext(ctxHead.getLabel());
					}
					/*
					 * if we dont have verbal coordination, but either clausal or noun/adject/etc, or if we have coord generally, 
					 *  then the coordinated nodes have to be linked to their parent. 
					 */
				} else if ((coordCtxs.containsKey(headOfHead) && !coordCtxs.get(headOfHead).equals("verbal")) || coord == true) {
					/*
					 * If there is dijunction and also clausal coordination, then the edge linking the top node with the current node
					 * should be removed and a new edge from the top to the negated context should be created.
					 */
					if (disjunction == true && coordCtxs.containsKey(headOfHead) && coordCtxs.get(headOfHead).equals("clausal")){
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
						ctxHead = graph.getContextGraph().getInNeighbors(headOfHead).iterator().next();
						SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
						SemanticNode<?> top = graph.getStartNode(orEdge);
						graph.removeContextEdge(orEdge);
						ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
						graph.addContextEdge(label, top, negNode);		
						// if no disjunction, then just create the self neg node and get the parent of the current node	
					} else {
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
						head = depGraph.getInNeighbors(node).iterator().next();
						// create the self node of the head of the negation
						ctxHead = addSelfContextNodeAndEdgeToGraph(headOfHead);
					}					
				}

				// create and add the edge between the negation and the head of its head 
				ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.NOT, new  RoleEdgeContent());
				graph.addContextEdge(labelEdge, negNode, ctxHead);
				// the negated argument must be instantiated within the context of its head; it doesnt belong to top anymore		
				ContextHeadEdge instEdge = new ContextHeadEdge(GraphLabels.INST, new  RoleEdgeContent());
				graph.addContextEdge(instEdge, ctxHead, head);		
				// the existing ctx of the negated argument is set to the next context
				if (head instanceof SkolemNode)
					((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());	
				// put the negated node created and its head to the hash
				negCtxs.put(negNode, headOfHead);
				// re-set the context of the head of the head of the negation to the ctx of that head
				if (headOfHead instanceof SkolemNode)
					((SkolemNodeContent) headOfHead.getContent()).setContext(ctxHead.getLabel());

				/*
				 * if there is coordination (noun, adj, adv), then the children of this coordination should be added as 
				 * instantiable nodes to their mother context since they dont exist in top anymore. Their conext should also
				 * be adjusted.
				 */
				if (coord == true) {
					SemanticNode<?> combHead = graph.getRoleGraph().getInNeighbors(head).iterator().next();
					for (SemanticNode<?> child : graph.getRoleGraph().getOutNeighbors(combHead)){
						if (child instanceof SkolemNode && !child.equals(head)) {
							((SkolemNodeContent) child.getContent()).setContext(ctxHead.getLabel());
							// the negated argument must be instantiated within the context of its head; it doesnt belong to top anymore		
							ContextHeadEdge instEdge2 = new ContextHeadEdge(GraphLabels.INST, new  RoleEdgeContent());
							graph.addContextEdge(instEdge2, ctxHead, child);
						}
					}
				}			
			} 
			/*
			 * third else if deals with the sentences:
			 * Nobody is carrying a stick.
			 * The dog is carrying nothing.
			 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a predicate
			 */
			else if (((SkolemNodeContent) node.getContent()).getStem().equals("nobody") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("nothing") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("noone") ){
				SemanticNode<?> head = null;
				SemanticNode<?> ctxHead = null;
				// get the head of the negation
				SemanticNode<?> depHead = depGraph.getInNeighbors(node).iterator().next();			
				//If we have verbal coordination, then the combined node of the two predicates has to be negated
				if (coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("verbal")){
					// find the role head of this dep head
					head = graph.getRoleGraph().getInNeighbors(depHead).iterator().next();
					if (!graph.getContextGraph().containsNode(previousNegNode)){
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
					}
					previousNegNode = negNode;
					/* if there is disjunction in the sentence (and not only conjunction), then we have to first find the context of the
					 * current node-predicate and then find the context of this context.
					 *  For that we need to get the ctxOfDepHead (= the context of the current node-predicate)
					and ctxHead (= the context of the combined node)
					 */
					if (disjunction == true){
						SemanticNode<?> ctxOfDepHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
						ctxHead = graph.getContextGraph().getInNeighbors(ctxOfDepHead).iterator().next();
						/*
						 * if there is also clausal coordination, then the edge linking the top node with the combined node of the predicates
						 * should be removed and a new edge from the top to the negated context should be created.
						 */
						if (clausalCoord == true) {
							SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
							SemanticNode<?> top = graph.getStartNode(orEdge);
							graph.removeContextEdge(orEdge);
							ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
							graph.addContextEdge(label, top, negNode);
						}
					// if no disjunction, then just create the self neg node and get the parent of the current node	
					} else {
						// find the context head of this role head
						ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
					}
					/*
					 * if we dont have verbal coordination, but either clausal or noun/adject/etc, or if we have coord generally, 
					 *  then the coordinated nodes have to be linked to their parent. 
					 */
				} else if ((coordCtxs.containsKey(depHead) && !coordCtxs.get(depHead).equals("verbal")) || coord == true) {
					/*
					 * If there is dijunction and also clausal coordination, then the edge linking the top node with the current node
					 * should be removed and a new edge from the top to the negated context should be created.
					 */
					if (disjunction == true && coordCtxs.containsKey(depHead) && coordCtxs.get(depHead).equals("clausal")){
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
						ctxHead = graph.getContextGraph().getInNeighbors(depHead).iterator().next();
						SemanticEdge orEdge = graph.getContextGraph().getInEdges(ctxHead).iterator().next();
						SemanticNode<?> top = graph.getStartNode(orEdge);
						graph.removeContextEdge(orEdge);
						ContextHeadEdge label = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
						graph.addContextEdge(label, top, negNode);	
					// if no disjunction, then just create the self neg node and get the parent of the current node	
					} else {
						// create the self node of the negation
						negNode = addSelfContextNodeAndEdgeToGraph(node);
						head = depGraph.getInNeighbors(node).iterator().next();
						// create the self node of the head of the negation
						ctxHead = addSelfContextNodeAndEdgeToGraph(head);
					}			
				}

				// create and add the edge between the negation and the head of its head 
				ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.NOT, new  RoleEdgeContent());
				graph.addContextEdge(labelEdge, negNode, ctxHead);
				//depending on the word, we need a new node to carry the meaning
				SemanticNode<?> finish = null;
				if (((SkolemNodeContent) node.getContent()).getStem().equals("nobody") ){
					// create new skolem node with new content; we need a skolem node and not jsut a context node
					// because we want to save the context info of this node in itself
					SkolemNodeContent content = new SkolemNodeContent();
					content.setSurface("person");
					content.setStem("person");
					content.setPosTag("NN");
					content.setPartOfSpeech("NN");
					// the position is set to be the negative position of the word it is replacing, e.g. nobody
					content.setPosition(((SkolemNodeContent) node.getContent()).getPosition()*-1);
					content.setDerived(false);		
					content.setSkolem("person"+"_"+content.getPosition());
					finish = new SkolemNode(content.getSkolem(), content);
				} else if (((SkolemNodeContent) node.getContent()).getStem().equals("nothing") ) {
					// create new skolem node with new content; we need a skolem node and not jsut a context node
					// because we want to save the context info of this node in itself
					SkolemNodeContent content = new SkolemNodeContent();
					content.setSurface("thing");
					content.setStem("thing");
					content.setPosTag("NN");
					content.setPartOfSpeech("NN");
					// the position is set to be the negative position of the word it is replacing, e.g. nobody
					content.setPosition(((SkolemNodeContent) node.getContent()).getPosition()*-1);
					content.setDerived(false);		
					content.setSkolem("thing"+"_"+content.getPosition());
					finish = new SkolemNode(content.getSkolem(), content);
				}

				// the negated argument must be instantiated within the context of its head; it doesnt belong to top anymore	
				ContextHeadEdge instEdge = new ContextHeadEdge(GraphLabels.INST, new  RoleEdgeContent());
				graph.addContextEdge(instEdge, ctxHead, finish);
				// the existing ctx of the negated argument is set to the next context
				((SkolemNodeContent) finish.getContent()).setContext(ctxHead.getLabel());
				// put the negated node created and its head to the hash
				negCtxs.put(negNode, head);
				// re-set the context of the head of the negation to the ctx of that head
				if (head instanceof SkolemNode)
					((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());
			}	
		}
	}


	/***
	 * Adds a context edge to the big graph. The context edge has the label "head" and starts from the ctx of the word
	 * and finishes at the word itself. 
	 * @param node
	 */
	private SemanticNode<?> addSelfContextNodeAndEdgeToGraph(SemanticNode<?> node){
		ContextHeadEdge cHEdge = new ContextHeadEdge(GraphLabels.CONTEXT_HEAD, new  RoleEdgeContent());
		SemanticNode<?> start = new ContextNode("ctx("+node.getLabel()+")", new ContextNodeContent());
		for (SemanticNode<?> contextN : graph.getContextGraph().getNodes()){
			if (start.getLabel().equals(contextN.getLabel())){
				start = contextN;
			}
		}
		graph.addContextEdge(cHEdge, start, node);
		return start;
	}

	/**
	 * Recursively sets the contexts of the children of the given parent node
	 * @param firstParent: the same as the parent of the 1st iteration. Then, the firstParent remains the same so that the context is always the same. 
	 * @param parent
	 */
	private void setContextsRecursively(SemanticNode<?> firstParent, SemanticNode<?> parent){
		Set<SemanticNode<?>> outNeighbors = graph.getDependencyGraph().getOutNeighbors(parent);
		if (!outNeighbors.isEmpty()){
			for (SemanticNode<?> neighbor: outNeighbors){
				if (!graph.getContextGraph().containsNode(neighbor)) {
					((SkolemNodeContent) neighbor.getContent()).setContext(graph.getContextGraph().getInNeighbors(firstParent).iterator().next().getLabel());
				}
				setContextsRecursively(firstParent,neighbor);
			}
		}
	}

	/***
	 * Integrates all coordinating contexts. 
	 */
	private void integrateCoordinatingContexts() {
		SemGraph depGraph = graph.getDependencyGraph();
		Set<SemanticEdge> edges = depGraph.getEdges();
		for (SemanticEdge edge : edges){
			// if there is disjunction
			if (edge.getLabel().equals("conj:or")){
				disjunction = true;
				// find the edge of the coord and get the start and the end nodes of this edge ( = the two parts of the coord)
				SemanticNode<?> start = depGraph.getStartNode(edge);
				SemanticNode<?> finish = depGraph.getEndNode(edge);
				// create the contexts of the two nodes and get them back
				SemanticNode<?> ctxOfStart = this.addSelfContextNodeAndEdgeToGraph(start);
				SemanticNode<?> ctxOfFinish = this.addSelfContextNodeAndEdgeToGraph(finish);
				/* check if the parents of the start node are not empty. if they are not empty, then there is no predicate coordination.*/
				if (!depGraph.getInNeighbors(start).isEmpty()){
					//get the parent of the coord
					SemanticNode<?> parentOfStart = depGraph.getInNeighbors(start).iterator().next();
					// get the context of the parent
					SemanticNode<?> ctxOfParent = this.addSelfContextNodeAndEdgeToGraph(parentOfStart);
					// create contexts from the parent to the coordinated children
					ContextHeadEdge labelStart = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
					// create the edges between the parent of the coord and the two coordinating nodes
					graph.addContextEdge(labelStart, ctxOfParent, ctxOfStart);
					ContextHeadEdge labelFinish = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
					graph.addContextEdge(labelFinish, ctxOfParent, ctxOfFinish);
					coord = true;
					//put the start and the finish into the hash of coordination (node:type of coordination). The hash will be needed when chcking for negative contexts.
					if (!coordCtxs.containsKey(start))
						coordCtxs.put(start, "other");
					if (!coordCtxs.containsKey(finish))
						coordCtxs.put(finish, "other");
					// if the parent of the start node are empty (= parents), then there is predicate coordination
				} else {
					// if the start and the finish are depending on other nodes in teh role graph, then there is no clausal coord but simple verbal one 
					if (!graph.getRoleGraph().getInNeighbors(start).isEmpty() && !graph.getRoleGraph().getInNeighbors(finish).isEmpty() ){
						// if we have verbal coordination, create a comb node of the two from whcih the two predicates will depend
						SemanticNode<?> combNode = graph.getRoleGraph().getInNeighbors(start).iterator().next();
						ContextNode top = new ContextNode("ctx("+combNode.getLabel()+")", new ContextNodeContent());
						graph.addNode(top);
						ContextHeadEdge labelStart = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
						graph.addContextEdge(labelStart, top, ctxOfStart);
						((SkolemNodeContent) start.getContent()).setContext(ctxOfStart.getLabel());
						ContextHeadEdge labelFinish = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
						graph.addContextEdge(labelFinish, top, ctxOfFinish);
						((SkolemNodeContent) finish.getContent()).setContext(ctxOfFinish.getLabel());
						// again, put everything int he hash
						verbCoord = true;
						if (!coordCtxs.containsKey(start) || (coordCtxs.containsKey(start) && coordCtxs.get(start).equals("clausal")))
							coordCtxs.put(start, "verbal");
						if (!coordCtxs.containsKey(finish))
							coordCtxs.put(finish, "verbal");
					// if they are not depending on any other nodes of the role graph, it means we have clausal coordination
					} else {
						// in this case we create a node top from which the two separate sentences depend
						ContextNode top = new ContextNode("top", new ContextNodeContent());
						graph.addNode(top);
						ContextHeadEdge labelStart = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
						// if there is also verbCoord within one of the sentences, then the combined node of this predicate should be used as the start
						if (verbCoord == true){
							ctxOfStart = graph.getContextGraph().getInNeighbors(ctxOfStart).iterator().next();
						}
						graph.addContextEdge(labelStart, top, ctxOfStart);
						((SkolemNodeContent) start.getContent()).setContext(ctxOfStart.getLabel());
						ContextHeadEdge labelFinish = new ContextHeadEdge(GraphLabels.OR, new  RoleEdgeContent());
						graph.addContextEdge(labelFinish, top, ctxOfFinish);
						((SkolemNodeContent) finish.getContent()).setContext(ctxOfFinish.getLabel());
						clausalCoord = true;
						// again, put everything int he hash
						if (!coordCtxs.containsKey(start))
							coordCtxs.put(start, "clausal");
						if (!coordCtxs.containsKey(finish))
							coordCtxs.put(finish, "clausal");
					}
				}
			// if there is conjunction
			} else if (edge.getLabel().equals("conj:and")){
				// find the edge of the coord and get the start and the end nodes of this edge ( = the two parts of the coord)
				SemanticNode<?> start = depGraph.getStartNode(edge);
				SemanticNode<?> finish = depGraph.getEndNode(edge);
				// if the start and the finish are depending on other nodes in teh role graph, then there is no clausal coord but simple verbal one 
				if (!graph.getRoleGraph().getInNeighbors(start).isEmpty() && !graph.getRoleGraph().getInNeighbors(finish).isEmpty() ){
					/* check if the start node has a parent. If empty, there is predicate coordination.
					 * and we need to separate the two predicates*/
					if  (depGraph.getInNeighbors(start).isEmpty()) {
						SemanticNode<?> combNode = graph.getRoleGraph().getInNeighbors(start).iterator().next(); 
						ContextHeadEdge combEdge1 = new ContextHeadEdge(GraphLabels.CONTEXT_HEAD, new  RoleEdgeContent());
						ContextHeadEdge combEdge2 = new ContextHeadEdge(GraphLabels.CONTEXT_HEAD, new  RoleEdgeContent());
						SemanticNode<?> ctxOfCombNode = new ContextNode("ctx("+combNode.getLabel()+")", new ContextNodeContent());
						graph.addContextEdge(combEdge1, ctxOfCombNode, start);
						graph.addContextEdge(combEdge2, ctxOfCombNode, finish);
						verbCoord = true;
						// again, put everything int he hash. If the "start" verb is already in the hash and has the value "clausal"
						// then add it again with the type "verbal". The type "verbal" is then the main one for this verb.  
						if (!coordCtxs.containsKey(start) || (coordCtxs.containsKey(start) && coordCtxs.get(start).equals("clausal")))
							coordCtxs.put(start, "verbal");
						if (!coordCtxs.containsKey(finish))
							coordCtxs.put(finish, "verbal");
					// if we dont have predicate coordination, we have simple one
					} else {
						coord = true;
						if (!coordCtxs.containsKey(start))
							coordCtxs.put(start, "other");
						if (!coordCtxs.containsKey(finish))
							coordCtxs.put(finish, "other");
					}
				// if they are not depending on any other nodes of the role graph, it means we have clausal coordination	
				} else {
					clausalCoord = true;
					// again, put everything in the hash
					if (!coordCtxs.containsKey(start))
						coordCtxs.put(start, "clausal");
					if (!coordCtxs.containsKey(finish))
						coordCtxs.put(finish, "clausal");
				}
			}
		}
	}

	/**
	 * Integrates contexts as they are appearing from raise and control verbs.
	 */
	private void integrateClausalContexts(){
		SemGraph roleGraph = graph.getRoleGraph();
		Set<SemanticEdge> edges = roleGraph.getEdges();
		for (SemanticEdge edge : edges){
			if (edge.getLabel().equals("sem_comp") || edge.getLabel().equals("sem_xcomp")){
				SemanticNode<?> start = graph.getStartNode(edge);
				SemanticNode<?> finish = graph.getFinishNode(edge);
				SemanticNode<?> ctxOfFinish = this.addSelfContextNodeAndEdgeToGraph(finish);
				String label = ((SkolemNodeContent) start.getContent()).getStem();
				ContextHeadEdge labelEdge = new ContextHeadEdge(label, new  RoleEdgeContent());
				graph.addContextEdge(labelEdge,start, ctxOfFinish);
				if (finish instanceof SkolemNode)
					((SkolemNodeContent) finish.getContent()).setContext(start.getLabel());
			}
		}
	}

	private void integrateImplicativeContexts() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/kkalouli/Documents/libraries/implicatives.txt"), "UTF-8"));
		String strLine;
		HashMap<String,String> mapOfImpl = new HashMap<String,String>();
		while ((strLine = br.readLine()) != null) { 
			String sign = strLine.substring(0,strLine.indexOf(" "));
			String words = strLine.substring(strLine.indexOf("= ")+2);
			for (String word : words.split(" ")){
				word = word.replace(",", "");
				mapOfImpl.put(word,sign);
			}		
		}
		for (SemanticNode<?> node : depGraph.getNodes()){
			if (mapOfImpl.containsKey(((SkolemNodeContent) node.getContent()).getStem())){
				String posCtx = mapOfImpl.get(node).split("_")[0];
				String negCtxOther = mapOfImpl.get(node).split("_")[1];
				if (posCtx.equals("N")){

				}
			}
		}
		br.close();
	}
}
