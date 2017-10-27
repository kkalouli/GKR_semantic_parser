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
	
	public ContextMapper(semantic.graph.SemanticGraph graph){
		this.graph = graph;
		this.depGraph = this.graph.getDependencyGraph();
		this.negCtxs = new HashMap<SemanticNode<?>,SemanticNode<?>>();
	}
	
	public void integrateAllContexts(){
		integrateClausalContexts();
		integrateCoordinatingContexts();
		integrateNegativeContexts();
		integrateModalContexts();		
		SemGraph conGraph = graph.getContextGraph();
		// if there have been no special contexts until now, then create simple context graph containing only the root node
		if (conGraph.getNodes().isEmpty()){
			addSelfContextNodeAndEdgeToGraph(graph.getRootNode());
		}
		for (SemanticNode<?> cNode: conGraph.getNodes()){
			// if no incoming edges, it is the top context
			if (conGraph.getInEdges(cNode).isEmpty()){
				cNode.setLabel("top");
				// for all other nodes
			} 
		}
	}

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
	

	private void integrateNegativeContexts(){
		SemanticNode<?> negNode = null;
		Set<SemanticNode<?>> nodes = depGraph.getNodes();
		for (SemanticNode<?> node : nodes){
			/* First if clause deals with the following negations:
			 * The dog is not carrying the stick.
			 * --> the head of the negation is a predicate and all arguments exist in top
			*/
			if (((SkolemNodeContent) node.getContent()).getStem().equals("not") 
					|| ((SkolemNodeContent) node.getContent()).getStem().equals("n't") ){
				
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				// get the head of the negation
				SemanticNode<?> head = depGraph.getInNeighbors(node).iterator().next();
				// create the self node of the head of the negation
				SemanticNode<?> ctxHead = addSelfContextNodeAndEdgeToGraph(head);
				// create and add the edge between the negation and its head 
				ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.NOT, new  RoleEdgeContent());
				graph.addContextEdge(labelEdge, negNode, ctxHead);	
				// put the negated node created and its head to the hash
				negCtxs.put(negNode, head);
				// re-set the context of the head of the negation to the ctx of that head
				((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());
			}/*
				 * second else if deals with the sentences:
				 * No dog is carrying a stick.
				 * The dog is carrying no stick.
				 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a NP
				 */
			 else if (((SkolemNodeContent) node.getContent()).getStem().equals("no") ){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				// get the head of the negation
				SemanticNode<?> head = depGraph.getInNeighbors(node).iterator().next();
				// get the head of the head in order to hopefully reach the main verb
				SemanticNode<?> headOfHead = depGraph.getInNeighbors(head).iterator().next();
				// create the self node of the head of the head of the negation
				SemanticNode<?> ctxHead = addSelfContextNodeAndEdgeToGraph(headOfHead);
				// create and add the edge between the negation and the head of its head 
				ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.NOT, new  RoleEdgeContent());
				graph.addContextEdge(labelEdge, negNode, ctxHead);
				// the negated argument must be instantiated within the context of its head; it doesnt belong to top anymore		
				ContextHeadEdge instEdge = new ContextHeadEdge(GraphLabels.INST, new  RoleEdgeContent());
				graph.addContextEdge(instEdge, ctxHead, head);
				// the existing ctx of the negated argument is set to the next context
				((SkolemNodeContent) head.getContent()).setContext(ctxHead.getLabel());	
				// put the negated node created and its head to the hash
				negCtxs.put(negNode, headOfHead);
				// re-set the context of the head of the head of the negation to the ctx of that head
				((SkolemNodeContent) headOfHead.getContent()).setContext(ctxHead.getLabel());
				
			} /*
			 * third else if deals with the sentences:
			 * Nobody is carrying a stick.
			 * The dog is carrying nothing.
			 * --> the dog and the stick exist only in the ctx of the carry and the head of the negation is a predicate
			 */
			else if (((SkolemNodeContent) node.getContent()).getStem().equals("nobody") 
			|| ((SkolemNodeContent) node.getContent()).getStem().equals("nothing") 
			|| ((SkolemNodeContent) node.getContent()).getStem().equals("noone") ){
				// create the self node of the negation
				negNode = addSelfContextNodeAndEdgeToGraph(node);
				// get the head of the negation
				SemanticNode<?> head = depGraph.getInNeighbors(node).iterator().next();
				// create the self node of the head of the negation
				SemanticNode<?> ctxHead = addSelfContextNodeAndEdgeToGraph(head);
				// create and add the edge between the negation and the head of its head 
				ContextHeadEdge labelEdge = new ContextHeadEdge(GraphLabels.NOT, new  RoleEdgeContent());
				graph.addContextEdge(labelEdge, negNode, ctxHead);
				//depending on the word, we need a cnew node to carry the meaning
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
	 * Deal with coordination
	 */
	private void integrateCoordinatingContexts() {
		SemGraph depGraph = graph.getDependencyGraph();
		Set<SemanticEdge> edges = depGraph.getEdges();
		for (SemanticEdge edge : edges){
			if (edge.getLabel().equals("conj:or")){
				// find the edge of the coord and get the start and the end nodes of this edge ( = the two parts of the coord)
				SemanticNode<?> start = depGraph.getStartNode(edge);
				SemanticNode<?> finish = depGraph.getEndNode(edge);
				// create the contexts of the two nodes and get them back
				SemanticNode<?> ctxOfStart = this.addSelfContextNodeAndEdgeToGraph(start);
				SemanticNode<?> ctxOfFinish = this.addSelfContextNodeAndEdgeToGraph(finish);
				/* check if the start node has a parent. If yes, it means that there is no predicate
				 coordination but arguments coordination. If no, then we have predicate coordination.*/
				if (!depGraph.getInNeighbors(start).isEmpty()){
					//get the parent
					SemanticNode<?> parentOfStart = depGraph.getInNeighbors(start).iterator().next();
					// if there is a parent of the first component, i.e. subj or obj coordination, no predicate coordination
					SemanticNode<?> ctxOfParent = this.addSelfContextNodeAndEdgeToGraph(parentOfStart);
					ContextHeadEdge labelStart = new ContextHeadEdge("or", new  RoleEdgeContent());
					// create the edges between the parent of the coord and the two coordinating nodes
					graph.addContextEdge(labelStart, ctxOfParent, ctxOfStart);
					ContextHeadEdge labelFinish = new ContextHeadEdge("or", new  RoleEdgeContent());
					graph.addContextEdge(labelFinish, ctxOfParent, ctxOfFinish);
				} else {
					// if we have predicate coordination, create a top node from whcih the two predicates will depend
					ContextNode top = new ContextNode("top", new ContextNodeContent());
					graph.addNode(top);
					ContextHeadEdge labelStart = new ContextHeadEdge("or", new  RoleEdgeContent());
					graph.addContextEdge(labelStart, top, ctxOfStart);
					((SkolemNodeContent) start.getContent()).setContext(ctxOfStart.getLabel());
					ContextHeadEdge labelFinish = new ContextHeadEdge("or", new  RoleEdgeContent());
					graph.addContextEdge(labelFinish, top, ctxOfFinish);
					((SkolemNodeContent) finish.getContent()).setContext(ctxOfFinish.getLabel());
				}			
			} else if (edge.getLabel().equals("conj:and")){
				// find the edge of the coord and get the start and the end nodes of this edge ( = the two parts of the coord)
				SemanticNode<?> start = depGraph.getStartNode(edge);
				SemanticNode<?> finish = depGraph.getEndNode(edge);
				/* check if the start node has a parent. If no, then we have predicate coordination.
				 * and we need to separate the twpo predicates*/
				if  (depGraph.getInNeighbors(start).isEmpty()) {
					// in the predicate coordination we need the ctx of start because this will be the main one from which
					// the two main coordinated verbs will depend. 
					SemanticNode<?> ctxOfStart = this.addSelfContextNodeAndEdgeToGraph(start);
					ContextHeadEdge labelFinish = new ContextHeadEdge(GraphLabels.CONTEXT_HEAD, new  RoleEdgeContent());
					graph.addContextEdge(labelFinish, ctxOfStart, finish);	
				}			
			}
		}
	}
	
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
	
	/*public static void main(String args[]) throws IOException {
		integrateImplicativeContexts();
	}*/
	
}
