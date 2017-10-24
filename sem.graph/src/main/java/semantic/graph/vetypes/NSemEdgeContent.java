package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.EdgeContent;


/**
 * The {@link EdgeContent} of {@link NSemEdge}s
 *
 */
public class NSemEdgeContent implements EdgeContent, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7199690352398974470L;
	private boolean inherent;
	private String relation;
	private boolean positive;
	
	/**
	 * Is the Naive Semantics inherent or typical?
	 * @return {@code true} if inherent
	 */
	public boolean isInherent() {
		return inherent;
	}
	
	/**
	 * Mark the naive semantics as inherent or typical
	 * @param inherent
	 *  If {@code}, marks it as inherent, else typical
	 */
	public void setInherent(boolean inherent) {
		this.inherent = inherent;
	}
	
	/**
	 * The general NS relation type of the formula encoded on daughter nodes.
	 * E.g. "cons_of_event"
	 * @return
	 *  A general NS relation name
	 */
	public String getRelation() {
		return relation;
	}
	
	/**
	 * Set he general NS relation type of the formula encoded on daughter nodes.
	 * @param relation
	 */
	public void setRelation(String relation) {
		this.relation = relation;
	}
	
	/**
	 * Is the formula encoded on the daughter node in a positive or negative context
	 * @return
	 * {@code true} if positive
	 */
	public boolean isPositive() {
		return positive;
	}
	
	/**
	 * Set the polarity on the formula encoded on the daughter node
	 */	
	public void setPositive(boolean positive) {
		this.positive = positive;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(relation);
		sb.append("(").append(inherent).append(",").append(positive).append(")");
		return sb.toString();
	}

}
