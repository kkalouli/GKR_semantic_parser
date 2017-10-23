package semantic.graph.vetypes;

import java.io.Serializable;

/**
 * A {@link SemanticNode} representing a word occurrence
*
 */
public class SkolemNode extends TermNode implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3336060161648715335L;

	public SkolemNode(String label, SkolemNodeContent content) {
		super(label, content);
	}

	/**
	 * Get the word stem
	 * @return
	 */
	public String getStem() {
		return ((SkolemNodeContent) this.content).getStem();
	}

	/**
	 * Get the token position in the sentence of the work/skolem
	 * @return
	 */
	public int getPosition() {
		return ((SkolemNodeContent) this.content).getPosition();
	}

	/**
	 * Get the part of speech of the word
	 * @return
	 */
	public String getPartOfSpeech() {
		return ((SkolemNodeContent) this.content).getPartOfSpeech();
	}

	/**
	 * Get the surface form of the word
	 * @return
	 */
	public String getSurface() {
		return ((SkolemNodeContent) this.content).getSurface();
	}

	/** 
	 * Get the name of the semantic context that introduces the skolem
	 * @return
	 */
	public String getContext() {
		return ((SkolemNodeContent) this.content).getContext();
	}

	/**
	 * Get the skolem / variable name of the node
	 * @return
	 */
	public String getSkolem() {
		return ((SkolemNodeContent) this.content).getSkolem();
	}


}
