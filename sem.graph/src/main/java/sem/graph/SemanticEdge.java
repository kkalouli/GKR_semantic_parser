package sem.graph;

import java.io.Serializable;

/**
 * A general class for representing edges connecting {@link SemanticNode}s.
 * Records the ids of the source and destination nodes. 
 * See {@link SemanticGraph#getStartNode(SemanticEdge)} and 
 * {@link SemanticGraph#getFinishNode(SemanticEdge)}for retrieving the actual nodes
 *
 */
public class SemanticEdge implements Comparable<SemanticEdge>,Serializable {
	private static final long serialVersionUID = -4736534192867326177L;
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
	
	/**
	 * Get the (unique) identifier of the edge
	 * @return
	 */
	public String getId() {
		return sourceVertexId + "-" +  label + "-" + destVertexId;
	}
	
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
	 * See {@link SemanticGraph#getStartNode(SemanticEdge)} for getting the actual edge.
	 * @return
	 */
	public String getSourceVertexId() {
		return sourceVertexId;
	}

	/**
	 * Get the identifier of the destination vertex.
	 * See {@link SemanticGraph#getFinishNode(SemanticEdge)} for getting the actual edge.
	 * @return
	 */
	public String getDestVertexId() {
		return destVertexId;
	}
	
	/**
	 * Use label for mnemonic printing of edges (in graph display).
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


}
