package notabot.gametree;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public class GameTree {

	private StateMachine stateMachine;
	private GameTreeNode root;

	public GameTree(StateMachine stateMachine, MachineState initialState, Role playerRole){
		this.stateMachine = stateMachine;
		int playerIndex = stateMachine.getRoles().indexOf(playerRole);
		root = new GameTreeNode(initialState, stateMachine, playerIndex);
	}

	/**
	 * Perform one sample down the tree
	 */
	public void runSample(){
		root.runSample();
	}

	/**
	 * Go down the tree by one step
	 */
	public void traverse(){
		// TODO
	}

	public Move getBestMove(){
		return root.getBestMove();
	}

}
