package notabot.gametree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import notabot.MoveScore;

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
	private static MoveComparator moveComparator;

	MachineState state;// the state this node represents
	int numMoveCombos;// the number of move combinations
	GameTreeNode[] children;// array of child nodes for all move combos
	int terminalGoal;// contains player goal for terminal states
	boolean isTerminal;
	List<Move> playerMoves;
	int numPlayerMoves;

	int lastComboSampled = -1;

	int numSamples[];// number of samples done for each of this player's moves
	long sumSamples[];// running sum of samples for each of this player's moves

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
	public GameTreeNode(MachineState state){
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
				numSamples = new int[numPlayerMoves];
				sumSamples = new long[numPlayerMoves];

				Collections.sort(playerMoves, moveComparator);
			}
			catch (MoveDefinitionException e){
				e.printStackTrace();
				numMoveCombos = 1;
				numSamples = new int[1];
				sumSamples = new long[1];
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

	private class MoveComparator implements Comparator<Move>{
		@Override
		public int compare(Move m0, Move m1) {
			return m0.toString().compareTo(m1.toString());
		}
	}

	/**
	 * Run one sample from this node
	 */
	public int runSample(int expansionDepth){
		if (isTerminal){
			return terminalGoal;
		}

		int combo;
		if (expansionDepth == 0){
			combo = rand.nextInt(numMoveCombos);
		}
		else{
			combo = (lastComboSampled + 1) % numMoveCombos;
			expansionDepth --;
		}

		int playerMoveIndex = combo % numPlayerMoves;

		if (children[combo] == null){
			createChild(combo);
		}

		int goal = children[combo].runSample(expansionDepth);
		sumSamples[playerMoveIndex] += goal;
		numSamples[playerMoveIndex] ++;
		lastComboSampled = combo;

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
			List<Move> playerMoves = stateMachine.getLegalMoves(state, roles.get(playerIndex));
			int divisor = numPlayerMoves;
			int playerMoveIndex = combo%divisor;

			// get each opponent's move for current combination
			for (int r = 0; r < numRoles; r++) {
				if (r!=playerIndex){
					List<Move> currMoves = stateMachine.getLegalMoves(state, roles.get(r));
					moveCombo.add(currMoves.get(combo/divisor % currMoves.size()));
					divisor *= currMoves.size();
				}
				else{
					moveCombo.add(playerMoves.get(playerMoveIndex));
				}
			}

			// create child node
			children[combo] = new GameTreeNode(stateMachine.getNextState(state, moveCombo));
		}
		catch (MoveDefinitionException | TransitionDefinitionException e) {
			e.printStackTrace();
		}
	}

	public boolean isState(MachineState state){
		return this.state.equals(state);
	}

	/**
	 * Compute the best move for the current node
	 * based on information from the samples run so far
	 */
	public MoveScore getBestMove(boolean printout){
		if (printout) System.out.println("SCORE FOR EACH MOVE: ");
		//List<Move> moves = stateMachine.getLegalMoves(state, roles.get(playerIndex));
		Move bestMove = null;
		double bestScore = 0;
		for (int i=0; i<numPlayerMoves; i++){
			if (numSamples[i] > 0){
				double score = ((double) sumSamples[i])/numSamples[i];
				if (score > bestScore){
					bestScore = score;
					bestMove = playerMoves.get(i);
				}
				if (printout) System.out.println("\t" + playerMoves.get(i) + " - " + numSamples[i] + " - " + score);
			}
		}

		return new MoveScore(bestScore);
	}

	public double getScore(){
		int totalNumSamples = 0;
		long totalSumSamples = 0;
		for (int i=0; i<numPlayerMoves; i++){
			totalNumSamples += numSamples[i];
			totalSumSamples += sumSamples[i];
		}
		return ((double) totalSumSamples)/totalNumSamples;
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

			// update best outcome of the worst outcomes
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

		return bestWorst;
	}



}
