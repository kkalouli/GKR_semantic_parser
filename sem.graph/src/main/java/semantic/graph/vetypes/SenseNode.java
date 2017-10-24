package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.SemanticNode;


/**
 * A {@link SemanticNode} representing word sense information for a {@link SkolemNode}
 *
 */
public class SenseNode extends SemanticNode<SenseNodeContent> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4932976786214396376L;

	public SenseNode(String label, SenseNodeContent content) {
		super(label, content);
		// TODO Auto-generated constructor stub
	}

}
