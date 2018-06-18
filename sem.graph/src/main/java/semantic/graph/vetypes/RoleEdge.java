package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.SemanticEdge;

/**
 * A {@link SemanticEdge} representing a role relation betweem {@link SkolemNode}s or {@link ContextNode}s
 *
 */
public class RoleEdge extends SemanticEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8047312226870234895L;

	public RoleEdge(String label, RoleEdgeContent content) {
		super(label, content);
	}

}
