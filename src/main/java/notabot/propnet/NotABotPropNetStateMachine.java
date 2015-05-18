package notabot.propnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

public class NotABotPropNetStateMachine extends StateMachine{
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    // mapping from role to relevant moves
    private Map<Role, Set<Move>> relevantInputMap;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
			propNet = OptimizingPropNetFactory.create(description);
	        roles = propNet.getRoles();
	        ordering = getOrdering();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
        relevantInputMap = findRelevantMoves();
    }

    public Map<Role, Set<Move>> getRelevantInputMap(){
    	return relevantInputMap;
    }

	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		// Done
		setPropNetState(state);
		return propMark(propNet.getTerminalProposition());
	}

	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined.
	 */
	@Override
	public int getGoal(MachineState state, Role role)
	throws GoalDefinitionException {
		// Done
		setPropNetState(state);
		for (Proposition p: propNet.getGoalPropositions().get(role)){
			if (propMark(p)){
				return Integer.parseInt(p.getName().getBody().get(1).toString());
			}
		}
		return 0;
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		// Done
		clearPropNet();
		propNet.getInitProposition().setValue(true);
		return computeNextState();
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
	throws MoveDefinitionException {
		// Done
		setPropNetState(state);
		List<Move> moves = new ArrayList<Move>();

		for (Proposition p: propNet.getLegalPropositions().get(role)){
			if (propMark(p)){
				moves.add(new Move(p.getName().getBody().get(1)));
			}
		}

		return moves;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
	throws TransitionDefinitionException {
		// Done
		setPropNetState(state, moves);
		return computeNextState();
	}

	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 *
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 *
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 *
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering()
	{
	    // List to contain the topological ordering.
	    List<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

	    // TODO: Compute the topological ordering.

		return order;
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 *
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with
	 * setting input values, feel free to change this for a more efficient implementation.
	 *
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}

	private void setPropNetState(MachineState state){
		clearPropNet();
		markBases(state.getContents());
	}

	private void setPropNetState(MachineState state, List<Move> moves){
		setPropNetState(state);
		markActions(moves);
	}

	private void clearPropNet(){
		for (Proposition p: propNet.getBasePropositions().values()){
			p.setValue(false);
		}
		// TODO clearing inputs might not be necessary
		for (Proposition p: propNet.getInputPropositions().values()){p.setValue(false);}
		propNet.getInitProposition().setValue(false);
	}

	private void markBases(Set<GdlSentence> sentences){
		for (GdlSentence sent: sentences){
			propNet.getBasePropositions().get(sent).setValue(true);
		}
	}

	private void markActions(List<Move> moves){

		for (int i=0; i<getRoles().size(); i++){
			Move m = moves.get(i);
			for (Proposition legal: propNet.getLegalPropositions().get(getRoles().get(i))){
				if (legal.getName().get(1).equals(m.getContents())){
					Proposition does = propNet.getLegalInputMap().get(legal);
					propNet.getInputPropositions().get(does.getName()).setValue(true);
				}
			}
		}

	}

	private boolean propMark(Component c){
		if (c instanceof Proposition){
			// view proposition
			if (c.getInputs().size() == 1 && !(c.getSingleInput() instanceof Transition)){
				return propMark(c.getSingleInput());
			}
			// base or input proposition
			else{
				return c.getValue();
			}
		}
		else if (c instanceof And){
			for (Component c2:c.getInputs()){
				if (!propMark(c2)) return false;
			}
			return true;
		}
		else if (c instanceof Or){
			for (Component c2:c.getInputs()){
				if (propMark(c2)) return true;
			}
			return false;
		}
		else if (c instanceof Constant){
			//System.out.println("NotABotPropNetStateMachine found Constant during propMark");
			return c.getValue();
		}
		else if (c instanceof Not){
			return !propMark(c.getSingleInput());
		}
		else if (c instanceof Transition){
			return propMark(c.getSingleInput());
		}

		System.out.println("NotABotPropNetStateMachine found unknown Component during propMark");
		return false;
	}

	private MachineState computeCurrentState(){
		Set<GdlSentence> contents = new HashSet<GdlSentence>();

		for (GdlSentence s: propNet.getBasePropositions().keySet()){
			if (propNet.getBasePropositions().get(s).getValue()){
				contents.add(s);
			}
		}

		return new MachineState(contents);
	}


	private MachineState computeNextState(){
		int n = propNet.getBasePropositions().values().size();
		Proposition[] bases = propNet.getBasePropositions().values().toArray(new Proposition[n]);
		boolean[] vals = new boolean[bases.length];


		for (int i=0; i<bases.length; i++){
			vals[i] = propMark(bases[i].getSingleInput());
		}
		for (int i=0; i<bases.length; i++){
			bases[i].setValue(vals[i]);
		}

		return computeCurrentState();
	}



	private Map<Role, Set<Move>> findRelevantMoves(){
		Set<Proposition> relevantInputs = new HashSet<Proposition>();
		Set<Component> visited = new HashSet<Component>();
		Stack<Component> stack = new Stack<Component>();

		stack.add(propNet.getTerminalProposition());
		visited.add(propNet.getTerminalProposition());

		while (!stack.isEmpty()){
			Component curr = stack.pop();
			for (Component comp: curr.getInputs()){
				// add unvisited components to stack
				if (!visited.contains(comp)){
					stack.add(comp);
					visited.add(comp);

					// add input proposition to set
					if (comp instanceof Proposition && comp.getInputs().size()==0){
						relevantInputs.add((Proposition) comp);
						//System.out.println(comp);
					}
				}
			}
		}

		relevantInputs.remove(propNet.getInitProposition());
		// create mapping from role to set of relevant moves
		Map<Role, Set<Move>> relevantInputMap = new HashMap<Role, Set<Move>>();
		for (Role role: getRoles()){
			relevantInputMap.put(role, new HashSet<Move>());

			for (Proposition p: propNet.getLegalPropositions().get(role)){
				Proposition does = propNet.getLegalInputMap().get(p);
				if (relevantInputs.contains(does)){
					Move m = new Move(p.getName().getBody().get(1));
					//System.out.println(role);
					//System.out.println(m);
					relevantInputMap.get(role).add(m);
				}
			}
		}

		return relevantInputMap;
	}

}
