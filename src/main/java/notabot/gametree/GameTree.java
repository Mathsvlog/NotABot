package notabot.gametree;

import javax.swing.JFrame;

import notabot.MoveScore;
import notabot.visualizer.NotABotTreeVisualizer;

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
	private Move lastMove;

	public static final boolean SHOW_VISUALIZER = true;
	private final String VIS_FRAME_TITLE = "NotABot Game Tree Visualizer";
	private final NotABotTreeVisualizer vis;
	private final JFrame frame;

	public GameTree(StateMachine stateMachine, MachineState initialState, Role playerRole){
		this.stateMachine = stateMachine;
		int playerIndex = stateMachine.getRoles().indexOf(playerRole);
		root = new GameTreeNode(initialState, stateMachine, playerIndex);
		numRoles = stateMachine.getRoles().size();

		if (SHOW_VISUALIZER){
			System.out.println("BUILD VIS");
			vis = new NotABotTreeVisualizer();
			vis.setRoot(root);
			frame = new JFrame(VIS_FRAME_TITLE);
			//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(vis);
			vis.init();
			frame.setVisible(true);
			frame.setSize(NotABotTreeVisualizer.VIS_WIDTH, NotABotTreeVisualizer.VIS_HEIGHT);
		}
		else{
			vis = null;
		}
	}

	/**
	 * Perform one sample down the tree
	 */
	public void runSample(){
		GameTreeNode selected = root.selectNode();
		//selected.expandNode((selected==root)?numRoles:numRoles-1);
		selected.expandNode(0);

		if (SHOW_VISUALIZER){
			vis.sample(root.getLastSelectMoveIndex());
		}
	}

	/**
	 * Go down the tree by one step
	 */
	public void traverse(MachineState newState){
		if (root.isState(newState)) return;
		root = root.getChildWithState(newState);
		if (SHOW_VISUALIZER){
			vis.setRoot(root);
			if (lastMove != null) frame.setTitle(VIS_FRAME_TITLE + " - Last Move: " +lastMove);
		}
	}

	/**
	 * Computes best move from root using MiniMax with Alpha-Beta pruning
	 */
	public Move getBestMove(boolean printout){
		MoveScore ms;
		try {
			ms = root.getBestMove(numRoles, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
			lastMove = ms.getMove();
			return ms.getMove();
		}
		catch (MoveDefinitionException | TransitionDefinitionException | GoalDefinitionException e) {
			e.printStackTrace();
		}
		return null;
	}

}
