package sem.graph.vetypes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GraphLabels implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1328451004558048056L;

	/**
	 * Marks the head word/concept of a context
	 */
	public static final String CONTEXT_HEAD = "ctx_hd";
	
	/**
	 * Marks a disjunctive context
	 */
	public static final String OR = "coord_or";
	
	
	/**
	 * Marks a negative context
	 */
	public static final String NOT = "not";
	
	public static final String VER = "veridical";
	
	public static final String ANTIVER = "antiveridical";
	
	public static final String AVER = "averidical";
	
	public static final String IMPERATIVE = "imperative";

	public static final String INTERROGATIVE = "interrogative";
	
	public static final String MUST = "must";

	public static final String MAY = "may";

	public static final String MIGHT = "might";
	
	public static final String SHOULD = "should";
	
	public static final String OUGHT = "ought";
	
	public static final String NEED = "need";
	
	
	public static final Set<String> contextEdgeLabels = new HashSet<String>(Arrays.asList(new String[] {
			CONTEXT_HEAD, OR, NOT, IMPERATIVE, 	INTERROGATIVE, MUST, MAY, MIGHT, SHOULD, OUGHT, NEED, VER, ANTIVER, AVER
	}));

	public static final String PRONOUN_RESOLUTION = "prounoun_res";
	public static final String NAME_RESOLUTION = "name_res";
	public static final String IDENTICAL_TO = "identical_to";
	public static final String APPOSITIVE_IDENTICAL_TO = "appositive_identical_to";
	
	public static final Set<String> linkEdgeLabels = new HashSet<String>(Arrays.asList(new String[] {
			PRONOUN_RESOLUTION, NAME_RESOLUTION, IDENTICAL_TO, APPOSITIVE_IDENTICAL_TO
	}));
	
	
	public static final String CARDINALITY = "cardinality";
	public static final String SPECIFIER = "specifier";
	public static final String TENSE = "tense";
	public static final String ASPECT = "aspect";
	public static final String PART_OF = "part_of";
	public static final String NTYPE = "name";
	public static final Set<String> propertyEdgeLabels = new HashSet<String>(Arrays.asList(new String[] {
			CARDINALITY, SPECIFIER, TENSE, ASPECT, PART_OF, NTYPE
	}));
	
	
	public static final String LEX = "lex";
	
	public static final String AMOD = "amod";
	public static final String IS_ELEMENT = "is_element";
	public static final String RESTRICTION = "rstr";
	public static final String PMOD = "pmod";
	public static final String SUBJ = "sem_subj";
	public static final String OBJ = "sem_obj";
	public static final String IOBJ = "sem_iobj";
	public static final String COMP = "sem_comp";
	public static final String XCOMP = "sem_xcomp";
	public static final String NMOD = "nmod";
	public static final String MOD = "mod"; // general mod that we dont know what exactly it is: corresponds to the "dep" UD relation
	public static final String COMPOUND = "cmp";
	
	public static final Set<String> roleEdgeLabels = new HashSet<String>(Arrays.asList(new String[] {
			AMOD, IS_ELEMENT, RESTRICTION, PMOD, SUBJ, OBJ, COMP, XCOMP, NMOD
	}));
	
}
