package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.SemanticNode;


/**
 * A {@link SemanticNode} representing the value of a morpho-semantic feature
  *
 */
public class ValueNode extends SemanticNode<ValueNodeContent> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7512406180628358776L;

	public ValueNode(String label, ValueNodeContent content) {
		super(label, content);
	}

	protected String value;

}
