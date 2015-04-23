package notabot.gametree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

	MachineState state;// the state this node represents
	int numMoveCombos;// the number of move combinations
	GameTreeNode[] children;// array of child nodes for all move combos
	int terminalGoal;// contains player goal for terminal states
	boolean isTerminal;

	int numSamples[];// number of samples done for each of this player's moves
	int sumSamples[];// running sum of samples for each of this player's moves

	/**
	 * Constructor for the initial root of the tree
	 */
	public GameTreeNode(MachineState state, StateMachine stateMachine, int playerIndex){
		this(state);

		GameTreeNode.roles = stateMachine.getRoles();
		GameTreeNode.numRoles = roles.size();
		GameTreeNode.stateMachine = stateMachine;
		GameTreeNode.playerIndex = playerIndex;
	}

	/**
	 * Constructor for any state except the initial state
	 */
	public GameTreeNode(MachineState state){
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
				int numPlayerMoves = stateMachine.getLegalMoves(state, roles.get(playerIndex)).size();
				numSamples = new int[numPlayerMoves];
				sumSamples = new int[numPlayerMoves];
			}
			catch (MoveDefinitionException e){
				e.printStackTrace();
				numMoveCombos = 1;
				numSamples = new int[1];
				sumSamples = new int[1];
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
	 * Run one sample from this node
	 */
	public int runSample(){
		if (isTerminal){
			return terminalGoal;
		}

		int combo = rand.nextInt(numMoveCombos);
		int playerMoveIndex = combo/numRoles;

		if (children[combo] == null){
			try {
				// compute move combo
				List<Move> moveCombo = new ArrayList<Move>();
				List<Move> playerMoves = stateMachine.getLegalMoves(state, roles.get(playerIndex));
				int divisor = playerMoves.size();

				// get each opponent's move for current combination
				for (int r = 0; r < numRoles; r++) {
					if (r!=playerIndex){
						List<Move> currMoves = stateMachine.getLegalMoves(state, roles.get(r));
						moveCombo.add(currMoves.get(combo/divisor % currMoves.size()));
						divisor *= currMoves.size();
					}
				}
				moveCombo.add(playerIndex, playerMoves.get(playerMoveIndex));

				// create child node
				children[combo] = new GameTreeNode(stateMachine.getNextState(state, moveCombo));
			}
			catch (MoveDefinitionException | TransitionDefinitionException e) {
				e.printStackTrace();
				return 0;
			}

		}

		int goal = children[combo].runSample();
		sumSamples[playerMoveIndex] += goal;
		numSamples[playerMoveIndex] ++;

		return goal;
	}

	/**
	 * Compute the best move for the current node
	 * based on information from the samples run so far
	 */
	public Move getBestMove(){
		try {
			List<Move> moves = stateMachine.getLegalMoves(state, roles.get(playerIndex));
			Move bestMove = null;
			double bestScore = 0;
			for (int i=0; i<numMoveCombos; i++){
				if (children[i] != null){
					double score = ((double) sumSamples[i])/numSamples[i];
					if (score > bestScore){
						bestScore = score;
						bestMove = moves.get(i);
					}
				}
			}
			return bestMove;
		}
		catch (MoveDefinitionException e) {
			e.printStackTrace();
			return null;
		}
	}


}
