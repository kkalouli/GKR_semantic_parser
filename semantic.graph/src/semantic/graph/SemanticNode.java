package semantic.graph;

import java.io.Serializable;


/**
 * General class for SemanticNodes: subtyped through type parameter.
 * Node label is treated as Id and should be unique
 * @author richard_crouch
 *
 * @param <T>
 */
public class SemanticNode<T extends NodeContent> implements Serializable, Comparable<SemanticNode<?>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5358870829854126063L;
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

	/*
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SemanticNode other = (SemanticNode) obj;
		if (id != other.id)
			return false;
		return true;
	}
	*/
}
