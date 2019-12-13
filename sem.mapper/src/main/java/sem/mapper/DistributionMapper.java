package sem.mapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jep.Jep;
import jep.JepException;
import sem.graph.SemGraph;
import sem.graph.SemanticEdge;
import sem.graph.SemanticGraph;
import sem.graph.SemanticNode;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.SkolemNodeContent;
import sem.graph.vetypes.TermNode;

public class DistributionMapper {
	private SemGraph roleGraph;
	private SemGraph ctxGraph;
	private SemanticGraph graph;

	
	public DistributionMapper(SemanticGraph graph){
		this.roleGraph = graph.getRoleGraph();
		this.ctxGraph = graph.getContextGraph();
		this.graph = graph;
	}
	
	public void mapCtxsToDistrReps(BufferedWriter writer) throws IOException{
		ArrayList<SemanticNode<?>> headsToRepresent = new ArrayList<SemanticNode<?>>();
		ArrayList<SemanticNode<?>> potentialHeadsToRepresent = new ArrayList<SemanticNode<?>>();
		ArrayList<SkolemNode> childrenSkolems = new ArrayList<SkolemNode>();
		for (SemanticEdge edge : ctxGraph.getEdges()){
			if (edge.getLabel().startsWith("ctx_hd")){
				SemanticNode<?> start = ctxGraph.getStartNode(edge);
				SemanticNode<?> finish = ctxGraph.getEndNode(edge);
				if (!ctxGraph.getInEdges(start).isEmpty()){	
					headsToRepresent.add(finish);
				}
				else {
					potentialHeadsToRepresent.add(finish);
				}
			}
		}
		if (headsToRepresent.isEmpty()){
			headsToRepresent.addAll(potentialHeadsToRepresent);
		}
		for (SemanticNode<?> head : headsToRepresent){
			/*SemanticNode<?> parentOfHead = graph.getInNeighbors(head).iterator().next();
			if (parentOfHead instanceof TermNode){
				for (SemanticNode<?> out : roleGraph.getOutReach(parentOfHead))
				childrenSkolems.add(out);
			}*/
			Set<SemanticNode<?>> childrenNodes = roleGraph.getOutReach(head);
			for (SemanticNode<?> node : childrenNodes){
				if (node instanceof SkolemNode){
					childrenSkolems.add((SkolemNode) node);
				} 
			}
			ArrayList<SkolemNode> sortedSkolems = sortSkolemsWithPositionInSentence(childrenSkolems);
			writer.write(sortedSkolems.toString());	
			writer.flush();
		}
		writer.write("\n");
		
		
	}
	
	
	public ArrayList<SkolemNode> sortSkolemsWithPositionInSentence(ArrayList<SkolemNode> nodesToSort) {
		  java.util.Collections.sort(nodesToSort, new NodePositionComparator());
		  return nodesToSort;
		}
	
	
	class NodePositionComparator implements Comparator<SkolemNode> {
	    @Override
	    public int compare(SkolemNode a, SkolemNode b) {
	    	int aPos = ((SkolemNodeContent) a.getContent()).getPosition();
	    	int bPos = ((SkolemNodeContent) b.getContent()).getPosition();
	        return aPos < bPos ? -1 : aPos == bPos ? 0 : 1;
	    }
	}
	
	/*public static void getInferSentEmbedFromPython() throws JepException{
		try(Jep jep = new Jep(false, "/Users/kkalouli/Documents/libraries/InferSent/InferSent/")) {
			jep.eval("import torch");
			jep.eval("from random import randint");
			jep.eval("from models import InferSent");
			jep.eval("import run_model");
			
			Object result3 = jep.getValue("run_model.getModel()");
			
			jep.set("model_path", "encoder/infersent%s.pkl");
			/*
			jep.set("params",  "{'bsize': 64, 'word_emb_dim': 300, 'enc_lstm_dim': 2048,'pool_type': 'max', 'dpout_model': 0.0, 'version': model_version}");
			jep.set("bsize", 64);
			jep.set("word_emb_dim", 300);
			jep.set("enc_lstm_dim", 2048);
			jep.set("pool_type", "max");
			jep.set("dpout_model", 0.0);
			jep.set("version", 1);
			jep.set("W2V_PATH", "GloVe/glove.840B.300d.txt");*/
			

			/*		
			// using eval(String) to invoke methods
			jep.set("arg", obj);
			jep.eval("x = somePyModule.foo1(arg)");
			Object result1 = jep.getValue("x");

			// using getValue(String) to invoke methods
			Object result2 = jep.getValue("somePyModule.foo2()");

			// using invoke to invoke methods
			jep.eval("foo3 = somePyModule.foo3")
			Object result3 = jep.invoke("foo3", obj);

			// using runScript
			jep.runScript("path/To/Script");
			*/
		//}
	//}
	
	/*public static void main(String args[]) throws IOException {
		try {
			getInferSentEmbedFromPython();
		} catch (JepException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	


}
