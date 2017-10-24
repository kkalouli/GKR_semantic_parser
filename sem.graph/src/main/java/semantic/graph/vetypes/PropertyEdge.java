package semantic.graph.vetypes;

import java.io.Serializable;

import semantic.graph.EdgeContent;
import semantic.graph.SemanticEdge;

/**
 * A {@link SemanticEdge} connecting to a {@link ValueNode}.
 * Typcially epresents various morpho-semantic feature, e.g. tense, cardinality.
 *
 */
public class PropertyEdge extends SemanticEdge implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1611142200547781675L;

	public PropertyEdge(String label, EdgeContent content) {
		super(label, content);
	}

}
