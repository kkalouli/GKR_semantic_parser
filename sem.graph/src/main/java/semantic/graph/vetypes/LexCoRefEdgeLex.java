package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.EdgeContent;

/**
 * A {@link SemanticEdge} connecting {@link SkolemNodes} is the naive semantics
 * components of lexical graphs back to their surface skolems (if any)
 *
 */
public class LexCoRefEdgeLex extends LinkEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 968773350544017749L;

	public LexCoRefEdgeLex(String label, EdgeContent content) {
		super(label, content);
	}

}
