package sem.graph.vetypes;

import java.io.Serializable;

import sem.graph.SemanticNode;


/**
 * A {@link SemanticNode} representing the value of a distributional feature. Not used for now.
  *
 */
public class DistributionalNode extends SemanticNode<DistributionalNodeContent> implements Serializable {


	/**
	 * 
	 */
	private static final long serialVersionUID = -4813401309762528665L;

	public DistributionalNode(String vector, DistributionalNodeContent content) {
		super(vector, content);
	}

	protected String vector;
	
	public float getFloatFromString(){
		float vecFloat = Float.parseFloat(vector);
		return vecFloat;
	}

}
