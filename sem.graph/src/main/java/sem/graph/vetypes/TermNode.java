package sem.graph.vetypes;

import java.io.Serializable;

import sem.graph.SemanticNode;


/**
 * A {@link SemanticNode} covering {@link SkolemNode}s and {@link ContextNode}s
 *
 */
public class TermNode extends SemanticNode<TermNodeContent> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8822834881686718756L;

	public TermNode(String label, TermNodeContent content) {
		super(label, content);
	}

}
