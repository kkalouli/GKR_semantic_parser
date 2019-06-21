package sem.graph;


import com.mxgraph.layout.*;
import com.mxgraph.swing.*;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import org.jgrapht.*;
import org.jgrapht.ext.*;
import org.jgrapht.graph.*;

import javax.swing.*;
import java.awt.*;

public class SemGraphDisplayer extends JApplet {


	private static final long serialVersionUID = 2202072534703043194L;
	private JGraphXAdapter<SemanticNode<?>, SemanticEdge> jgxAdapter;


	public void display(Graph<SemanticNode<?>, SemanticEdge> graph)
	{
		SemGraphDisplayer applet = new SemGraphDisplayer();
		applet.init(graph);

		/*JFrame frame = new JFrame();
		frame.getContentPane().add(applet);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);*/
	}

	
	public void init(Graph<SemanticNode<?>, SemanticEdge> graph)
	{
	
		jgxAdapter = new JGraphXAdapter<SemanticNode<?>, SemanticEdge>(graph);
		mxGraphComponent component = new mxGraphComponent(jgxAdapter);
		component.setConnectable(false);
		component.getGraph().setAllowDanglingEdges(false);
		//getContentPane().add(component);
		//setPreferredSize(new Dimension(300, 300));
		
		mxCircleLayout layout = new mxCircleLayout(jgxAdapter);
		layout.execute(jgxAdapter.getDefaultParent());
		
		Object mxDefaultParent = component.getGraph().getDefaultParent();

		component.getGraph().setCellStyles(mxConstants.STYLE_FILLCOLOR, "green", component.getGraph().getChildCells(mxDefaultParent)); //changes the color to red
		component.refresh();
		
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		
		frame.getContentPane().add(component);
		frame.setVisible(true);

		
		/*jgxAdapter = new JGraphXAdapter<SemanticNode<?>, SemanticEdge>(graph);

		mxGraphComponent component = new mxGraphComponent(jgxAdapter);
		component.setConnectable(false);
		component.getGraph().setAllowDanglingEdges(false);
		getContentPane().add(component);

		// positioning via jgraphx layouts
		mxCircleLayout layout = new mxCircleLayout(jgxAdapter);
		layout.execute(jgxAdapter.getDefaultParent());
		*/
	}
}

