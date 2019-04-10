package sem.graph.vetypes;

import java.io.Serializable;

import sem.graph.EdgeContent;
import sem.graph.SemanticEdge;

/**
 * A {@link SemanticEdge} linking two {@link ContextNode}s 
 *
 */
public class ContextEdge extends SemanticEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8887294916198938199L;

	public ContextEdge(String label, EdgeContent content) {
		super(label, content);
	}

}
