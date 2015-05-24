package notabot.gametree;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import notabot.MoveScore;
import notabot.NotABot;
import notabot.visualizer.NotABotTreeVisualizer;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class GameTree {

	protected volatile StateMachine stateMachine;
	protected GameTreeNode root;
	protected final int numRoles;
	protected Move lastMove;

	public static final boolean SHOW_VISUALIZER = false;
	private final String VIS_FRAME_TITLE = "NotABot Game Tree Visualizer";
	private final NotABotTreeVisualizer vis;
	private final JFrame frame;

	// info for GameTreeNodes to access
	final Random rand = new Random();
	final List<Role> roles;
	final int playerIndex;// this player's role index
	final MoveComparator moveComparator;// used to sort move lists
	int numDepthCharges = 0;// used to count number of paths sampled during turn
	double selectTemperature;

	public GameTree(StateMachine stateMachine, MachineState initialState, Role playerRole){
		this.stateMachine = stateMachine;
		playerIndex = stateMachine.getRoles().indexOf(playerRole);
		roles = stateMachine.getRoles();
		numRoles = roles.size();
		moveComparator = new MoveComparator();

		root = new GameTreeNode(initialState, this);

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
			frame = null;
		}
	}

	private class MoveComparator implements Comparator<Move>{
		@Override
		public int compare(Move m0, Move m1) {
			if (m0==null) return 1;
			if (m1==null) return -1;
			return m0.toString().compareTo(m1.toString());
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
	public boolean traverse(MachineState newState){
		if (root.isState(newState)) return true;
		root = root.getChildWithState(newState);
		if (root == null) return false;
		if (SHOW_VISUALIZER){
			vis.setRoot(root);
			if (lastMove != null) frame.setTitle(VIS_FRAME_TITLE + " - Last Move: " +lastMove);
		}
		return true;
	}

	/**
	 * Computes best move from root using MiniMax with Alpha-Beta pruning
	 */
	public Move getBestMove(boolean printout){
		return getBestMoveScore(printout).getMove();
	}

	/**
	 * Computes best move from root using MiniMax with Alpha-Beta pruning
	 */
	public MoveScore getBestMoveScore(boolean printout){
		MoveScore ms;
		try {
			ms = root.getBestMove(numRoles, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, printout);
			return ms;
		}
		catch (MoveDefinitionException | TransitionDefinitionException | GoalDefinitionException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * Resets the depth charge counter to 0
	 */
	public void resetDepthChargeCounter(){
		numDepthCharges = 0;
	}

	/**
	 * Returns number of depth charges
	 */
	public int getNumDepthCharges(){
		return numDepthCharges;
	}

	/**
	 * Updates the select phase temperature based on the remaining time
	 */
	public void updateSelectTemperature(){
		selectTemperature = NotABot.timeLeft()/100;
	}
}
