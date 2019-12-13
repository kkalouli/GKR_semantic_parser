package sem.graph;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.jgrapht.Graph;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Interface for basic graph structure for both JGraphT and Grph implementations
 *
 */
public interface SemGraph  {
	
	public void addNode(SemanticNode<?> node);
	
	public void addEdge(SemanticNode<?> start, SemanticNode<?> end, SemanticEdge edge);
	
	public void addUndirectedEdge(SemanticNode<?> node1, SemanticNode<?> node2, SemanticEdge edge);
	
	public void removeEdge(SemanticEdge edge);
	
	public void removeNode(SemanticNode <?> node);

	public boolean containsNode(SemanticNode<?> node);
	
	public boolean containsEdge(SemanticEdge edge);
	
	public Set<SemanticEdge> getEdges();
	
	public Set<SemanticEdge> getEdges(SemanticNode<?> node);
		
	public Set<SemanticEdge> getEdges(SemanticNode<?> start, SemanticNode<?> end);
	
	public Set<SemanticEdge> getInEdges(SemanticNode<?> node);
	
	public Set<SemanticEdge> getOutEdges(SemanticNode<?> node);
	
	public Set<SemanticNode<?>> getNodes();
	
	public Set<SemanticNode<?>> getNeighbors(SemanticNode<?> node);
	
	public Set<SemanticNode<?>> getInNeighbors(SemanticNode<?> node);
	
	public Set<SemanticNode<?>> getOutNeighbors(SemanticNode<?> node);
	
	public Set<SemanticNode<?>> getOutReach(SemanticNode<?> node);
	
	public List<SemanticNode<?>> breadthFirstTraversal(Graph<SemanticNode<?>, SemanticEdge> graph, SemanticNode<?> node);
	
	public Set<SemanticNode<?>> getInReach(SemanticNode<?> node);
	
	public SemanticNode<?> getStartNode(SemanticEdge edge);
	
	public SemanticNode<?> getEndNode(SemanticEdge edge);
	
	public void merge(SemGraph graph);
	
	public List<SemanticEdge> getShortestPath(SemanticNode<?> start, SemanticNode<?> end);
	
	public List<SemanticEdge> getShortestUndirectedPath(SemanticNode<?> start, SemanticNode<?> end);
	
	public SemGraph getSubGraph(Set<SemanticNode<?>> nodes, Set<SemanticEdge> edges);
	
	public JFrame display();
	
	public void exportGraphAsJson();
	
	public BufferedImage saveGraphAsImage();
	
	public String getMxGraph();
	
	//public BufferedImage saveGraphAsImage(Map<Color, List<SemanticNode<?>>> nodeProperties, Map<Color, List<SemanticEdge>> edgeProperties);
	

}
