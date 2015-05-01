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
	private final int numRoles;

	public GameTree(StateMachine stateMachine, MachineState initialState, Role playerRole){
		this.stateMachine = stateMachine;
		int playerIndex = stateMachine.getRoles().indexOf(playerRole);
		root = new GameTreeNode(initialState, stateMachine, playerIndex);
		numRoles = stateMachine.getRoles().size();
	}

	/**
	 * Perform one sample down the tree
	 */
	public void runSample(){
		GameTreeNode selected = root.selectNode();
		//selected.expandNode((selected==root)?numRoles:numRoles-1);
		selected.expandNode(0);
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
			ms = root.getBestMove(numRoles, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
			return ms.getMove();
		}
		catch (MoveDefinitionException | TransitionDefinitionException | GoalDefinitionException e) {
			e.printStackTrace();
		}
		return null;
	}


}
