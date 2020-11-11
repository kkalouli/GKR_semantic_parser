package sem.graph;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;

import java.util.Set;



import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.ListenableGraph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.JSONExporter;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.w3c.dom.Node;

import com.mxgraph.io.mxCodec;
import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxOrganicLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;

import sem.graph.vetypes.ContextEdge;
import sem.graph.vetypes.ContextNode;
import sem.graph.vetypes.LexEdge;
import sem.graph.vetypes.LinkEdge;
import sem.graph.vetypes.RoleEdge;
import sem.graph.vetypes.SenseNode;
import sem.graph.vetypes.SkolemNode;
import sem.graph.vetypes.TermNode;
import sem.graph.vetypes.ValueNode;



/**
 * Implementation of SemGraph via JGraphT
 *
 */
public class SemJGraphT implements  SemGraph, Serializable{

	private static final long serialVersionUID = 4437969385952923418L;
	private Graph<SemanticNode<?>, SemanticEdge> graph;

	public SemJGraphT() {
		this.graph = new DirectedMultigraph<SemanticNode<?>, SemanticEdge>(SemanticEdge.class);
	}

	public SemJGraphT(Graph<SemanticNode<?>, SemanticEdge> graph) {
			this.graph = graph;
	}
	
	
	@Override
	public void addNode(SemanticNode<?> node) {
		graph.addVertex(node);	
	}

	@Override
	public void addEdge(SemanticNode<?> start, SemanticNode<?> end,
			SemanticEdge edge) {
		if (!graph.containsVertex(start)) {
			addNode(start);
		}
		if (!graph.containsVertex(end)) {
			addNode(end);
		}
		graph.addEdge(start, end, edge);			
		edge.sourceVertexId = start.label;
		edge.destVertexId = end.label;
	}

	@Override
	public void addUndirectedEdge(SemanticNode<?> node1, SemanticNode<?> node2,
			SemanticEdge edge) {
		System.err.println("SemJGraphT.addUndirectedEdge :: Not Implemented");	
	}

	@Override
	public void removeEdge(SemanticEdge edge) {
		graph.removeEdge(edge);	
	}
	
	@Override
	public void removeNode(SemanticNode<?> node) {
		graph.removeVertex(node);	
	}

	@Override
	public boolean containsNode(SemanticNode<?> node) {
		return graph.containsVertex(node);
	}

	@Override
	public boolean containsEdge(SemanticEdge edge) {
		return graph.containsEdge(edge);
	}

	@Override
	public Set<SemanticEdge> getEdges() {
		Set<SemanticEdge> retval = graph.edgeSet();
		if (retval == null) {
			return new HashSet<SemanticEdge>(0);
		} else {
			return retval;
		}
	}

	@Override
	public Set<SemanticEdge> getEdges(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			return graph.edgesOf(node);
		} else {
			return new HashSet<SemanticEdge>(0);
		}
	}

	@Override
	public Set<SemanticEdge> getEdges(SemanticNode<?> start,
			SemanticNode<?> end) {
		Set<SemanticEdge> retval =  graph.getAllEdges(start, end);
		if (retval == null) {
			return new HashSet<SemanticEdge>(0);
		} else {
			return retval;
		}
	}

	@Override
	public Set<SemanticEdge> getInEdges(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			return graph.incomingEdgesOf(node);
		} else { 
			return new HashSet<SemanticEdge>(0);
		}
	}

	@Override
	public Set<SemanticEdge> getOutEdges(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			return graph.outgoingEdgesOf(node);
		} else { 
			return new HashSet<SemanticEdge>(0);
		}
	}

	@Override
	public Set<SemanticNode<?>> getNodes() {
		Set<SemanticNode<?>> retval =  graph.vertexSet();
		if (retval == null) {
			return new HashSet<SemanticNode<?>>();
		} else {
			return retval;
		}
	}

	@Override
	public Set<SemanticNode<?>> getNeighbors(SemanticNode<?> node) {
		if (graph.containsVertex(node)) {
			Set<SemanticNode<?>> retval = getInNeighbors(node);
			retval.addAll(getOutNeighbors(node));
			return retval;
		} else {
			return new HashSet<SemanticNode<?>>(0);
		}
	}

	@Override
	public Set<SemanticNode<?>> getInNeighbors(SemanticNode<?> node) {
		Set<SemanticNode<?>> retval = new HashSet<SemanticNode<?>>();
		if (graph.containsVertex(node)) {
			Set<SemanticEdge> in = graph.incomingEdgesOf(node);
			if (in != null) {
				for (SemanticEdge e : graph.incomingEdgesOf(node)) {
					retval.add(graph.getEdgeSource(e));
				}
			}
		}
		return retval;
	}

	@Override
	public Set<SemanticNode<?>> getOutNeighbors(SemanticNode<?> node) {
		Set<SemanticNode<?>> retval = new HashSet<SemanticNode<?>>();
		if (graph.containsVertex(node)) {
			Set<SemanticEdge> out = graph.outgoingEdgesOf(node);
			if (out != null) {
				for (SemanticEdge e : graph.outgoingEdgesOf(node)) {
					retval.add(graph.getEdgeTarget(e));
				}
			}
		}
		return retval;
	}

	@Override
	public Set<SemanticNode<?>> getOutReach(SemanticNode<?> node) {
		return new HashSet<SemanticNode<?>>(breadthFirstTraversal(this.graph, node));
	}

	
	@Override
	public List<SemanticNode<?>> breadthFirstTraversal(Graph<SemanticNode<?>, SemanticEdge> graph, SemanticNode<?> node) {
		List<SemanticNode<?>> retval = new ArrayList<SemanticNode<?>>();
		if (graph.containsVertex(node)) {
			BreadthFirstIterator<SemanticNode<?>, SemanticEdge> bfi 
				= new BreadthFirstIterator<SemanticNode<?>, SemanticEdge>(graph, node);
			while (bfi.hasNext()) {
				SemanticNode<?> rnode = bfi.next();
				retval.add(rnode);
			}
		}
		return retval;
	}
	
	@Override
	public Set<SemanticEdge> getOutReachEdges(SemanticNode<?> node) {
		return new HashSet<SemanticEdge>(breadthFirstTraversalEdges(this.graph, node));
	}
		
	public List<SemanticEdge> breadthFirstTraversalEdges(Graph<SemanticNode<?>, SemanticEdge> graph, SemanticNode<?> node) {
		List<SemanticEdge> retval = new ArrayList<SemanticEdge>();
		if (graph.containsVertex(node)) {
			BreadthFirstIterator<SemanticNode<?>, SemanticEdge> bfi 
				= new BreadthFirstIterator<SemanticNode<?>, SemanticEdge>(graph, node);
			while (bfi.hasNext()) {
				SemanticEdge rnode = bfi.getSpanningTreeEdge(bfi.next());
				retval.add(rnode);
			}
		}
		return retval;
	}

	@Override
	public Set<SemanticNode<?>> getInReach(SemanticNode<?> node) {
		//protected static List getAllParents(DefaultDirectedWeightedGraph<Vertex, IACEdge> graph, Vertex vertex) {
		//List parents = new ArrayList<>();
		EdgeReversedGraph<SemanticNode<?>, SemanticEdge> reversedGraph = new EdgeReversedGraph<>(graph);
		return new HashSet<SemanticNode<?>>(breadthFirstTraversal(reversedGraph, node));
	}

	
	
	@Override
	public SemanticNode<?> getStartNode(SemanticEdge edge) {
		if (graph.containsEdge(edge)) {
			return graph.getEdgeSource(edge);
		} else {
			return null;
		}
	}

	@Override
	public SemanticNode<?> getEndNode(SemanticEdge edge) {
		if (graph.containsEdge(edge)) {
			return graph.getEdgeTarget(edge);
		} else {
			return null;
		}
	}

	@Override
	public void merge(SemGraph other) {
		// This graph is assumed to be the main one.
		// Other is a pre-existing graph that needs to be made a sub-graph of this.
		// Note: It is down to subsequent code to ensure that any additions
		// to the other graph are also made to this one
		for (SemanticNode<?> node : other.getNodes()) {
			if (!this.graph.containsVertex(node)) {
				this.addNode(node);
			}
		}
		for (SemanticEdge edge : other.getEdges()) {
			if (!this.graph.containsEdge(edge)) {
				this.addEdge(other.getStartNode(edge), other.getEndNode(edge), edge);
			}
		}			
	}

	@Override
	public List<SemanticEdge> getShortestPath(SemanticNode<?> start,SemanticNode<?> end) {
		DijkstraShortestPath<SemanticNode<?>, SemanticEdge> dsp = new DijkstraShortestPath<SemanticNode<?>, SemanticEdge>(this.graph);
		GraphPath<SemanticNode<?>, SemanticEdge> path = dsp.getPath(start, end);
		if (path != null)
			return dsp.getPath(start,end).getEdgeList(); 
		else
			return new ArrayList<SemanticEdge>();
		
	}

	@Override
	public List<SemanticEdge> getShortestUndirectedPath(SemanticNode<?> start, SemanticNode<?> end) {
		if (!graph.containsVertex(start) || !graph.containsVertex(end)) {
			return new ArrayList<SemanticEdge>(0);
		}
		AsUndirectedGraph<SemanticNode<?>, SemanticEdge> ugraph = new AsUndirectedGraph<SemanticNode<?>, SemanticEdge>(this.graph);
		DijkstraShortestPath<SemanticNode<?>, SemanticEdge> dsp = new DijkstraShortestPath<SemanticNode<?>, SemanticEdge>(ugraph);
		GraphPath<SemanticNode<?>, SemanticEdge> path = dsp.getPath(start, end);
		if (path != null)
			return dsp.getPath(start,end).getEdgeList(); 
		else
			return new ArrayList<SemanticEdge>(0);
	}
	
	public String getMxGraph(){	
		mxGraph mx = new mxGraph();
		mx.getModel().beginUpdate();
		Object parent = mx.getDefaultParent();
		HashMap<String, Object> traversedNodes = new HashMap<String, Object>();
		try
		{
			for (SemanticEdge edge : graph.edgeSet()){
				Object v1 = null;
				Object v2 = null;
				String sourceId = edge.getSourceVertexId();
				String targetId = edge.getDestVertexId();
				String sourceColor = getColorForVertex(sourceId);
				String targetColor = getColorForVertex(targetId);
				if (sourceColor.equals("top")){
					sourceColor = "#b3b5b8";
					sourceId = "top";
				}
				if (targetColor.equals("top")){
					targetColor = "#b3b5b8";
					targetId = "top";
				}
				if (!traversedNodes.containsKey(sourceId)){
					v1 = mx.insertVertex(parent, null, sourceId, 20, 20, 80, 30,"defaultVertex;fillColor="+sourceColor);
					traversedNodes.put(sourceId, v1);
				} else {
					v1 = traversedNodes.get(sourceId);
				}
				if (!traversedNodes.containsKey(targetId)){ // 200, 150
					v2 = mx.insertVertex(parent, null, targetId, 20, 20, 80, 30, "defaultVertex;fillColor="+targetColor);
					traversedNodes.put(targetId, v2);
				} else {
					v2 = traversedNodes.get(targetId);
				}
				Object e1 = mx.insertEdge(parent, null, edge.getLabel(), v1, v2);
			}
		   
		}
		finally
		{
		   // Updates the display
			mx.getModel().endUpdate();
		}
		
		mxCodec encoder = new mxCodec();
		Node result = encoder.encode(mx.getModel());	
		String xml = mxUtils.getPrettyXml(result);	
		return xml;
	}
	
	private String getColorForVertex(String nodeLabel){
		String color = "";
		SemanticNode<?> nodeToGet = null;
		for (SemanticNode<?> node : graph.vertexSet()){
			if (node.getLabel().equals(nodeLabel)){
				nodeToGet = node;
			}
		}
		if (nodeToGet instanceof SkolemNode){
			color = "#bce7fd";
		} else if (nodeToGet instanceof SenseNode){
			color = "#FF4C4C";
		} else if (nodeToGet instanceof ContextNode){
			color = "#b3b5b8";
		} else if (nodeToGet instanceof ValueNode){
			color = "#B2B2FF";
		} else if (nodeToGet instanceof TermNode){
			color = "#FFA500";
		} else if (nodeToGet == null){
			color = "top";
		} else {
			color = "#9CC1A5";
		}
		return color;
	}
	
	public JFrame display(String nameOfGraph){
		JGraphXAdapter<SemanticNode<?>, SemanticEdge> jgxAdapter = new JGraphXAdapter<SemanticNode<?>, SemanticEdge>(graph);
		mxGraphComponent component = new mxGraphComponent(jgxAdapter);
		component.setConnectable(false);
		component.getGraph().setAllowDanglingEdges(false);
		
		mxHierarchicalLayout layout = new mxHierarchicalLayout(jgxAdapter);
		layout.setIntraCellSpacing(80);
		//layout.setFineTuning(true);
		layout.execute(jgxAdapter.getDefaultParent());
		// get the parent of the mxgraph
		Object mxDefaultParent = component.getGraph().getDefaultParent();
		
		// modify colors of nodes and edges based on the type of node/edge
		for (Object node: component.getGraph().getChildCells(mxDefaultParent)){
			if (((mxCell) node).getValue() instanceof SkolemNode){
				component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "#bce7fd", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, "#bce7fd", new Object[]{node});
			} else if (((mxCell) node).getValue() instanceof SenseNode){
				component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FF4C4C", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FF4C4C", new Object[]{node});
			} else if (((mxCell) node).getValue() instanceof ContextNode){
				component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "#B2B2B2", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, "#B2B2B2", new Object[]{node});
			} else if (((mxCell) node).getValue() instanceof ValueNode){
				component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "#32CD32", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, "#32CD32", new Object[]{node}); // B2B2FF
			} else if (((mxCell) node).getValue() instanceof TermNode){
				component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FFA500", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FFA500", new Object[]{node});		
			} else if (((mxCell) node).getValue() instanceof LinkEdge) {
				component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FF7F50", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_FONTCOLOR, "#FF7F50", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, "#FF7F50", new Object[]{node});
			}	
			else {
				component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "#9CC1A5", new Object[]{node});
				component.getGraph().setCellStyles(mxConstants.STYLE_STROKECOLOR, "#9CC1A5", new Object[]{node});
			}
		}
		component.refresh();
			
		JFrame frame = new JFrame(nameOfGraph);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		frame.getContentPane().add(component);
		//frame.setUndecorated(true);
		frame.pack();
		frame.setVisible(true);
		return frame;
	}

	
	 /***
	  * Create a static image out of the graph.
	  */
	public BufferedImage saveGraphAsImage(){
		// following code if the graph is empty: just show empty graph
		BufferedImage image = new BufferedImage(30, 10, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 30, 10);
        graphics.setColor(Color.BLACK);
        //graphics.setFont(new Font("Arial Black", Font.PLAIN, 8));
        String str = "not available";
        //graphics.drawString(str, 2, 5);
		if (this.graph.vertexSet().isEmpty())
			return image;
		// else if graph is full:		
		JFrame frame = this.display("");
		//frame.setSize(frame.getComponent(0).getSize());
		// create buffered image and project jframe on it
		try
		{	
			//Image img = mxCellRenderer.createBufferedImage(frame.getComponent(0).getGraph(), null, 1, Color.WHITE, false, null); 		
			//frame.setUndecorated(true);
			int compWidth = frame.getComponent(0).getWidth();
			int compHeight = frame.getComponent(0).getHeight();
			int width = frame.getContentPane().getWidth();
			int height = frame.getContentPane().getHeight();
			if (compWidth > width || compHeight > height ){
				frame.getComponent(0).setSize(width, height);
				width = frame.getComponent(0).getWidth();
				height = frame.getComponent(0).getHeight();
			}
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);		
			Graphics2D graphics2D = image.createGraphics();
			graphics2D.setBackground(Color.WHITE);
			//graphics2D.fillRect(0, 0, frame.getWidth(), frame.getHeight());
			//graphics2D.setStroke(new BasicStroke(0));
			frame.getContentPane().paint(graphics2D);	
			graphics2D.dispose();
			frame.dispose();
		}
		catch(Exception exception)
		{
			//code
		}
		// set frame to invisible so that the java swing window disappears
		frame.setVisible(false);
		return image;
	}
	
	
	/**
	 * Create a static image out of the graph with the given colormap for the nodes and the edges.
	 */
	/*@SuppressWarnings({ "rawtypes", "unchecked" })
	public BufferedImage saveGraphAsImage(Map<Color, List<SemanticNode<?>>> nodeProperties,
			Map<Color, List<SemanticEdge>> edgeProperties){
		// following code if the graph is empty: just show empty graph
		BufferedImage image = new BufferedImage(200, 40, BufferedImage.TYPE_INT_RGB);
		Graphics graphics = image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 200, 40);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("Arial Black", Font.PLAIN, 18));
        graphics.drawString("graph not available", 10, 20);
		if (this.graph.vertexSet().isEmpty())
			return image;		
		// else if graph is full:
		JGraphModelAdapter jgAdapter = new JGraphModelAdapter<SemanticNode<?>, SemanticEdge>(this.graph);
		JGraph jgraph = new JGraph(jgAdapter); 
		JGraphFacade facade = new JGraphFacade(jgraph);
		JGraphLayout layout = new JGraphHierarchicalLayout();
		layout.run(facade);	
		// rescale whole facade so that the nodes are further apart and thus the edge labels dont overlap
		//facade.scale(facade.getVertices(), 1.2, 1.0, 0, 0);
		// rescale cells that are bigger than the default to fit their content size (get size of font)
		for (Object vert : facade.getVertices()) {
			//Rectangle2D rect = facade.getBounds(vert);
			//rect.setRect(rect.getX()-50, rect.getY(), rect.getWidth(), rect.getHeight());
			//facade.setBounds(vert, rect);
			int width = graphics.getFontMetrics().stringWidth(vert.toString());
			//90 is the default size
			if (width > 90) {
				facade.setSize(vert, width, 30);
			}
		}
		Map nested = facade.createNestedMap(true, true);
		jgraph.getGraphLayoutCache().getModel().beginUpdate();
		jgraph.getGraphLayoutCache().edit(nested);	
		
		// Add additional node and edge properties
		// Currently, only colours
		Map nested1 = new HashMap();
		for (Entry<Color, List<SemanticNode<?>>> kv : nodeProperties.entrySet()) {
			Map nodeColor = new HashMap();
			GraphConstants.setBackground(nodeColor, kv.getKey());
			for (SemanticNode<?> n : kv.getValue()) {
				DefaultGraphCell cell = jgAdapter.getVertexCell(n);
				nested1.put(cell, nodeColor);
			}
		}
		for (Entry<Color, List<SemanticEdge>> kv : edgeProperties.entrySet()) {
			Map edgeColor = new HashMap();
			GraphConstants.setLineColor(edgeColor, kv.getKey());
			for (SemanticEdge e : kv.getValue()) {
				DefaultGraphCell cell = jgAdapter.getEdgeCell(e);
				nested1.put(cell, edgeColor);
			}
		}
		jgraph.getGraphLayoutCache().getModel().beginUpdate();
		jgraph.getGraphLayoutCache().edit(nested1);
		jgraph.getGraphLayoutCache().getModel().endUpdate();
		jgraph.getGraphLayoutCache().getModel().endUpdate();

		// Show in Frame
		JScrollPane component = new JScrollPane(jgraph);
		JFrame frame = new JFrame();
		frame.setBackground(Color.WHITE);
		frame.setUndecorated(true);
		frame.getContentPane().add(component);
		frame.pack();
		frame.setLocation(-2000, -2000);
		frame.setVisible(true);	
		try
		{
			image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);		
			Graphics2D graphics2D = image.createGraphics();
			frame.paint(graphics2D);	
			component.print(graphics2D);
			graphics2D.dispose();
			frame.dispose();
			//ImageIO.write(image,"png", new File(imagePath));
		}
		catch(Exception exception)
		{
			//code
		}
		frame.setVisible(false);
		jgraph.setVisible(false);
		return image;
	}
	
	*/
	
	public void exportGraphAsJson(){
		JSONExporter<SemanticNode<?>, SemanticEdge> exporter = new JSONExporter<SemanticNode<?>, SemanticEdge>();
		Writer writer;
		try {
			writer = new FileWriter("semGraph_to_json.json");
			exporter.exportGraph(graph, writer);
		} catch (IOException | ExportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	@Override
	public SemGraph getSubGraph(Set<SemanticNode<?>> nodes,
			Set<SemanticEdge> edges) {
		AsSubgraph<SemanticNode<?>, SemanticEdge> subGraph = new AsSubgraph<SemanticNode<?>, SemanticEdge>(this.graph, nodes, edges);
		return new SemJGraphT(subGraph);
	}



}
