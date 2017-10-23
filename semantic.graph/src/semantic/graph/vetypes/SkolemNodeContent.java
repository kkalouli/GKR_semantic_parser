package semantic.graph.vetypes;

import java.io.Serializable;

/**
 * The {@link NodeContent} for a {@link SkolemNode}.
 * Contains non-trivial amounts of information
 * @author richard_crouch
 *
 */
public class SkolemNodeContent extends TermNodeContent implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 504525117235309018L;
	private String skolem;
	private String surface;
	private String stem;
	private String partOfSpeech;
	private String subPartOfSpeech;
	private String posTag;
	private String context;
	private int position;
	private boolean derived;
	
	public SkolemNodeContent() {
		skolem = "";
		surface = "";
		stem = "";
		partOfSpeech = "";
		subPartOfSpeech = "";
		posTag = "";
		context = "";
		position = 0;
		derived = false;		
	}
	
	/**
	 * Get the name / id of the skolem
	 * @return
	 */
	public String getSkolem() {
		return skolem;
	}
	
	public void setSkolem(String skolem) {
		this.skolem = skolem;
	}
	
	/** 
	 * Get the surface form of the skolem
	 * @return
	 */
	public String getSurface() {
		return surface;
	}
	
	public void setSurface(String surface) {
		this.surface = surface;
	}
	
	/**
	 * Get the stem of the skolem
	 * @return
	 */
	public String getStem() {
		return stem;
	}
	
	public void setStem(String stem) {
		this.stem = stem;
	}
	
	/** 
	 * Get the main part of speech of the skolem (noun, verb, adj, etc)
	 * @return
	 */
	public String getPartOfSpeech() {
		return partOfSpeech;
	}

	public void setPartOfSpeech(String partOfSpeech) {
		this.partOfSpeech = partOfSpeech;
	}

	/**
	 * Get the sub part of speech of the skolem (name, noun, modal, aux, etc)
	 * @return
	 */
	public String getSubPartOfSpeech() {
		return subPartOfSpeech;
	}


	public void setSubPartOfSpeech(String subPartOfSpeech) {
		this.subPartOfSpeech = subPartOfSpeech;
	}


	/** 
	 * Get the part of speech tag of the skolem e.g. (NN, NNS, NNP, VBZ)
	 * @return
	 */
	public String getPosTag() {
		return posTag;
	}


	public void setPosTag(String posTag) {
		this.posTag = posTag;
	}


	/**
	 * Get the name of the semantic context in which the skolem is introduced
	 * @return
	 */
	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	/**
	 * Get the token position of the word introducing the skolem
	 * @return
	 */
	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	/** 
	 * Does the skolem occur explicitly in the sentence, or 
	 * is it introduced/derived from naive semantics
	 * @return
	 */
	public boolean isDerived() {
		return derived;
	}
	
	public void setDerived(boolean derived) {
		this.derived = derived;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(skolem);
		sb.append("(").append(stem).append(',').append(surface).append(',');
		sb.append(partOfSpeech).append(',').append(subPartOfSpeech).append(',').append(posTag).append(',');
		sb.append(context).append(',').append(position).append(',').append(derived).append(')');
		return sb.toString();
	}
}
