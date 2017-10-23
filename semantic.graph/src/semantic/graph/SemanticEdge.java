package semantic.graph;

import java.io.Serializable;

/**
 * A general class for representing edges connecting {@link SemanticNode}s.
 * Records the ids of the source and destination nodes. 
 * See {@link SemanticGraph#getStartNode(SemanticEdge)} and 
 * {@link SemanticGraph#getFinishNode(SemanticEdge)}for retrieving the actual nodes
 *
 */
public class SemanticEdge implements Serializable, Comparable<SemanticEdge> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5871682058629224402L;
	//protected int id;
	protected String sourceVertexId;
	protected String destVertexId;
	protected String label;
	protected EdgeContent content;
	
	public SemanticEdge(/*int id,*/ String label, EdgeContent content) {
		//super();
		//this.id = id;
		this.label = label;
		this.content = content;
	}
	
	SemanticEdge() {
		this.label = null;
		this.content = null;
	}
	//public SemanticEdge(String label, EdgeContent content) {
	//	this(-1, label, content);
	//}
	
	/**
	 * Get the (unique) identifier of the edge
	 * @return
	 */
	public String getId() {
		return sourceVertexId + "-" +  label + "-" + destVertexId;
	}
	
	//protected void setId(int id) {
	//	this.id = id;
	//}
	
	/**
	 * Get the label of the edge (need not be unique)
	 * @return
	 */
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * Get the edge content
	 * @return
	 */
	public EdgeContent getContent() {
		return content;
	}
	
	public void setContent(EdgeContent content) {
		this.content = content;
	}

	/** 
	 * Get the identifier of the source vertex.
	 * See {@link SemanticGraph#getStartNode(SemanticEdge)} for getting the actual edge
	 * @return
	 */
	public String getSourceVertexId() {
		return sourceVertexId;
	}

	/**
	 * Get the identifier of the destination vertex.
	 * See {@link SemanticGraph#getFinishNode(SemanticEdge)} for getting the actual edge
	 * @return
	 */
	public String getDestVertexId() {
		return destVertexId;
	}
	
	/**
	 * Use label for mnemonic printing of edges (in graph display)
	 */
	@Override
	public String toString() {
		return label;
	}

	@Override
	public int compareTo(SemanticEdge o) {
		int retval = label.compareTo(o.label);
		if (retval == 0) {
			retval = sourceVertexId.compareTo(o.sourceVertexId);
		}
		if (retval == 0) {
			retval = destVertexId.compareTo(o.destVertexId);
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
		SemanticEdge other = (SemanticEdge) obj;
		if (id != other.id)
			return false;
		return true;
	}
	*/
}
