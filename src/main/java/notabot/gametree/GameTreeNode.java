package notabot.gametree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import notabot.MoveScore;
import notabot.NotABot;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class GameTreeNode {

	private static Random rand = new Random();
	private static int numRoles;// number of roles for the game
	private static List<Role> roles;
	private static int playerIndex;// this player's role index
	private static StateMachine stateMachine;// state machine for the game
	private static int numNodesCreated=0;
	private static MoveComparator moveComparator;// used to sort move lists
	private static int numDepthCharges = 0;// used to count number of paths sampled during turn
	private static double selectTemperature;

	MachineState state;// the state this node represents
	int numMoveCombos;// the number of move combinations
	GameTreeNode[] children;// array of child nodes for all move combos
	int terminalGoal = 0;// contains player goal for terminal states
	boolean isTerminal;
	List<Move> playerMoves;
	int numPlayerMoves;

	int numVisits = 0;
	long sumSamples = 0;

	/**
	 * Constructor for the initial root of the tree
	 */
	public GameTreeNode(MachineState state, StateMachine stateMachine, int playerIndex){

		GameTreeNode.roles = stateMachine.getRoles();
		GameTreeNode.numRoles = roles.size();
		GameTreeNode.stateMachine = stateMachine;
		GameTreeNode.playerIndex = playerIndex;
		GameTreeNode.moveComparator = new MoveComparator();
		constructGameTreeNode(state);
	}

	/**
	 * Constructor for any state except the initial state
	 */
	public GameTreeNode(MachineState state, GameTreeNode parent){
		constructGameTreeNode(state);
	}

	/**
	 * Constructor helper for GameTreeNode
	 */
	private void constructGameTreeNode(MachineState state){
		numNodesCreated++;
		this.state = state;
		isTerminal = stateMachine.isTerminal(state);

		// If not terminal
		if (!isTerminal){
			// compute the number of move combinations possible
			numMoveCombos = 1;
			try{
				for (Role role: stateMachine.getRoles()){
					List<Move> currMoves = stateMachine.getLegalMoves(state, role);
					numMoveCombos *= currMoves.size();
				}
				// initialze array of child nodes
				children = new GameTreeNode[numMoveCombos];
				playerMoves = new ArrayList<Move>(stateMachine.getLegalMoves(state, roles.get(playerIndex)));
				numPlayerMoves = playerMoves.size();

				Collections.sort(playerMoves, moveComparator);
			}
			catch (MoveDefinitionException e){
				e.printStackTrace();
				numMoveCombos = 1;
			}

		}
		else{
			// compute goal for terminal nodes
			try {
				terminalGoal = stateMachine.getGoal(state, roles.get(playerIndex));
			}
			catch (GoalDefinitionException e) {
				e.printStackTrace();
				terminalGoal = 0;
			}
		}
	}


	/**
	 * Selection phase of MCTS
	 */
	GameTreeNode selectNode(){
		// if node was just created
		if (numVisits == 0 || isTerminal){
			return this;
		}

		// select first unexpanded child
		for (int i=0; i<numMoveCombos; i++){
			if (children[i] == null){
				createChild(i);
				return children[i];
			}
		}

		// pick child with best heuristic score
		double bestScore = 0;
		GameTreeNode bestNode = this;
		Move m = null;
		for (int i=0; i<numMoveCombos; i++){
			double currScore = children[i].selectFunction(numVisits);
			if (currScore > bestScore){
				bestScore = currScore;
				bestNode = children[i];
				m = playerMoves.get(i%numPlayerMoves);
			}
		}

		if (bestNode==null){
			System.out.println("SELECT PHASE RETURNED NULL");
		}
		//System.out.println(m.toString() + " : " + (int)bestScore + ", " + (int)bestNode.getScore());

		return bestNode;
	}

	/**
	 * Heuristic used during selection phase of MCTS
	 */
	double selectFunction(int parentNumVisits){
		return getScore() + GameTreeNode.selectTemperature*Math.sqrt(Math.log(parentNumVisits)/numVisits);
	}

	/**
	 * Expansion phase of MCTS
	 */
	void expandNode(int level){
		if (isTerminal){
			return;
		}

		for (int i=0; i<numMoveCombos; i++){
			if (NotABot.hasTimedOut()) break;

			if (children[i]==null) createChild(i);

			if (level > 0){
				children[i].expandNode(level-1);
			}
			else{
				int goal = children[i].runSample();
				sumSamples += goal;
				numVisits ++;
			}
		}
	}


	private class MoveComparator implements Comparator<Move>{
		@Override
		public int compare(Move m0, Move m1) {
			return m0.toString().compareTo(m1.toString());
		}
	}

	public void buildTree(int expansionDepth){
		if (isTerminal) return;

		if (expansionDepth==0){
			runSample();
			return;
		}

		for (int i=0; i<numMoveCombos; i++){
			if (children[i] == null){
				createChild(i);
			}

			children[i].buildTree(expansionDepth-1);
		}
	}

	/**
	 * Run one sample from this node
	 * Simulation and backprop phases of MCTS
	 */
	public int runSample(){
		if (isTerminal){
			GameTreeNode.numDepthCharges++;
			return terminalGoal;
		}

		int combo = rand.nextInt(numMoveCombos);

		if (children[combo] == null){
			createChild(combo);
		}

		int goal = children[combo].runSample();
		sumSamples += goal;
		numVisits ++;

		return goal;
	}

	public GameTreeNode getChildWithState(MachineState state){

		for (int i=0; i<numMoveCombos; i++){
			if (children[i] != null && children[i].isState(state)){
				System.out.println("FOUND CHILD NODE IN TRAVERSE");
				return children[i];
			}

		}
		for (int i=0; i<numMoveCombos; i++){
			if (children[i] == null){
				createChild(i);
				if (children[i].isState(state)){
					System.out.println("CREATED CHILD IN TRAVERSE");
					return children[i];
				}
			}
		}
		System.out.println("COULD NOT FIND CHILD");

		return null;
	}

	public void createChild(int combo){
		try {
			// compute move combo
			List<Move> moveCombo = new ArrayList<Move>();
			int divisor = numPlayerMoves;
			int playerMoveIndex = combo%numPlayerMoves;

			// get each opponent's move for current combination
			for (int r = 0; r < numRoles; r++) {
				if (r!=playerIndex){
					List<Move> currMoves = stateMachine.getLegalMoves(state, roles.get(r));
					moveCombo.add(currMoves.get((combo/divisor) % currMoves.size()));
					divisor *= currMoves.size();
				}
				else{
					moveCombo.add(playerMoves.get(playerMoveIndex));
				}
			}

			// create child node
			children[combo] = new GameTreeNode(stateMachine.getNextState(state, moveCombo), this);
		}
		catch (MoveDefinitionException | TransitionDefinitionException e) {
			e.printStackTrace();
		}
	}

	public boolean isState(MachineState state){
		return this.state.equals(state);
	}

	public double getScore(){
		return ((double) sumSamples)/numVisits;
	}

	public MoveScore getBestMove(int level, double alpha, double beta, boolean isFirst) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		// if state is terminal, return goal value of state
		if (isTerminal){
			return new MoveScore(terminalGoal);
		}

		// stop when reached max level
		if (level == 0){
			return new MoveScore(getScore());
		}


		// keeps track of best move, assuming opponents will do their best move
		MoveScore bestWorst = null;

		// for each of our possible moves
		//for (Move move: getMoves(state)){
		for (int i=0; i<numPlayerMoves; i++){

			// keeps track of worst outcome with our current move
			MoveScore worstBest = null;
			Move move = playerMoves.get(i);

			double minNodeBeta = beta;//
			int combo = i;

			// compute every possible combination of opponent moves
			//for (int i = 0; i < numCombinations; i++) {
			for (int j=0; j<numMoveCombos/numPlayerMoves; j++){

				// i is playerMoveIndex
				GameTreeNode child = children[combo];
				combo += numPlayerMoves;

				if (child!=null){
					// Find the worst case with this combination]
					MoveScore currMoveScore = child.getBestMove(level-1, alpha, minNodeBeta, false);
					currMoveScore.updateMove(move);

					// update worst outcome
					if (worstBest == null || worstBest.getScore() > currMoveScore.getScore()){
						worstBest = currMoveScore;
					}

					// min node
					minNodeBeta = Math.min(minNodeBeta, currMoveScore.getScore());
					if (alpha >= minNodeBeta){
						//System.out.println("MIN NODE BREAK");
						break;
					}
				}
				/*
				else{
					System.out.println("NULL CHILD DURING MINIMAX");
				}
				*/

			}

			if (isFirst){
				System.out.println(move + " : " + ((worstBest==null)?"?":worstBest.getScore()));
			}

			// update best outcome of the worst outcomes
			if (worstBest != null){
				if (bestWorst == null || bestWorst.getScore() < worstBest.getScore()){
					bestWorst = worstBest;
					bestWorst.updateMove(move);
				}

				// max node
				alpha = Math.max(alpha, bestWorst.getScore());
				if (alpha >= beta){
					//System.out.println("MAX NODE BREAK");
					break;
				}
			}
		}

		if (bestWorst == null){
			System.out.println("MINIMAX FOUND NODE WITH ALL NULL CHILDREN");
			bestWorst = new MoveScore(0);
			bestWorst.updateMove(playerMoves.get(rand.nextInt(playerMoves.size())));
		}

		return bestWorst;
	}


	public int getNumVisits(){
		return numVisits;
	}


	public static void resetDepthChargeCounter(){
		GameTreeNode.numDepthCharges = 0;
	}

	public static int getNumDepthCharges(){
		return GameTreeNode.numDepthCharges;
	}

	public static void updateSelectTemperature(){
		GameTreeNode.selectTemperature = NotABot.timeLeft()/100;
	}


}
