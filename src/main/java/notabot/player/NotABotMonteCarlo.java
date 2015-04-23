package notabot.player;

import notabot.NotABot;
import notabot.gametree.GameTree;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NotABotMonteCarlo extends NotABot {

	GameTree tree;

	@Override
	protected void runMetaGame() {
		tree = new GameTree(getStateMachine(), getCurrentState(), getRole());
		// run samples
		while (!hasTimedOut()){
			tree.runSample();
		}
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		// TODO update tree with moves from last turn

		while (!hasTimedOut()){
			tree.runSample();
		}
		Move move = tree.getBestMove();
		if (move == null){
			System.out.println("TREE RETURNED NULL MOVE");
			move = getStateMachine().getLegalMoves(getCurrentState(), getRole()).get(0);
		}
		return move;
	}

}
