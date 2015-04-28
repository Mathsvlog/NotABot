package notabot.gametree;

import notabot.MoveScore;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class GameTree {

	private StateMachine stateMachine;
	private GameTreeNode root;
	private static final int EXPANSION_DEPTH = 10;

	public GameTree(StateMachine stateMachine, MachineState initialState, Role playerRole){
		this.stateMachine = stateMachine;
		int playerIndex = stateMachine.getRoles().indexOf(playerRole);
		root = new GameTreeNode(initialState, stateMachine, playerIndex);
	}

	/**
	 * Perform one sample down the tree
	 */
	public void runSample(){
		root.buildTree(EXPANSION_DEPTH);
	}

	/**
	 * Go down the tree by one step
	 */
	public void traverse(MachineState newState){
		if (root.isState(newState)) return;
		root = root.getChildWithState(newState);
	}

	public Move getBestMove(boolean printout){
		MoveScore ms;
		try {
			ms = root.getBestMove(EXPANSION_DEPTH, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
			return ms.getMove();
		}
		catch (MoveDefinitionException | TransitionDefinitionException | GoalDefinitionException e) {
			e.printStackTrace();
		}
		return null;
	}

}
