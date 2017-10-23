package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.EdgeContent;
import semantic.graph.SemanticEdge;


/**
 * A {@link SemanticEdge} for the link graph
 *
 */
public class LinkEdge extends SemanticEdge   implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5660993891211959238L;

	public LinkEdge(String label, EdgeContent content) {
		super(label, content);
	}

}
