package semantic.graph.vetypes;

import java.io.Serializable;

/**
 * A {@link SemanticNode} for contexts
 *
 */
public class ContextNode extends TermNode implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5011300173087204743L;

	public ContextNode(String label, ContextNodeContent content) {
		super(label, content);
	}

}
