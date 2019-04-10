package sem.graph.vetypes;

import java.io.Serializable;

import sem.graph.SemanticEdge;


/**
 * A {@link SemanticEdge} linking nodes in the lexical graph, or linking {@link SkolemNode}s to
 * their lexical entries.
 *
 */
public class LexEdge extends SemanticEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3222648800899239804L;

	public LexEdge(String label, LexEdgeContent content) {
		super(label, content);
	}

}
