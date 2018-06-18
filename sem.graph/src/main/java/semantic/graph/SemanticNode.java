package semantic.graph;


import java.io.Serializable;


/**
 * General class for SemanticNodes: subtyped through type parameter.
 * Node label is treated as Id and should be unique
 *
 * @param <T>
 */
public class SemanticNode<T extends NodeContent> implements Comparable<SemanticNode<?>> {

	/**
	 * 
	 */
	protected String label;
	protected T content;
	
	protected SemanticNode(String label, T content) {
		this.label = label;
		this.content = content;
	}
	
	SemanticNode() {
		this.label = null;
		this.content = null;
	}


	public String getId() {
		return label;
	}


	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public T getContent() {
		return content;
	}

	public void setContent(T content) {
		this.content = content;
	}
	
	@Override
	public String toString() {
		return label;
	}

	@Override
	public int compareTo(SemanticNode<?> o) {
		int retval = this.getLabel().compareTo(o.getLabel());
		if (retval == 0) {
			retval = this.getContent().toString().compareTo(o.getContent().toString());
		}
		return retval;
	}
}
