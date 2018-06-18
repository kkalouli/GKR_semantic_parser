package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.EdgeContent;
import semantic.graph.SemanticEdge;


/**
 * A {@link SemanticEdge} linking a {@link ContextNode} to a {@link SkolemNode}
 *
 */
public class ContextHeadEdge extends SemanticEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5669553573045050098L;

	public ContextHeadEdge(String label, EdgeContent content) {
		super(label, content);
	}

}
