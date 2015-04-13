package notabot;

import java.util.Stack;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class NotABot extends StateMachineGamer{

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// TODO Auto-generated method stub

	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		Pair<Stack<Move>, Integer> moveState = getBestMovePath(getCurrentState());

		return moveState.getFirst().pop();
	}

	private Pair<Stack<Move>, Integer> getBestMovePath(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if (getStateMachine().isTerminal(state)){
			return new Pair<Stack<Move>, Integer>(new Stack<Move>(), getStateMachine().getGoal(state, getRole()));
		}

		Pair<Stack<Move>, Integer> bestMoveGoal = null;
		Move bestMove = null;

		for (Move move: getStateMachine().getLegalMoves(state, getRole())){
			Stack<Move> currRoleMoves = new Stack<Move>();
			currRoleMoves.add(move);// TODO add moves for all roles

			Pair<Stack<Move>, Integer> moveGoal = getBestMovePath(getStateMachine().getNextState(state, currRoleMoves));

			if (bestMoveGoal==null ||  moveGoal.getSecond() > bestMoveGoal.getSecond()){
				bestMoveGoal = moveGoal;
				bestMove = move;
				if (bestMoveGoal.getSecond() == 100){
					bestMoveGoal.getFirst().add(bestMove);
					return bestMoveGoal;
				}
			}


		}

		bestMoveGoal.getFirst().add(bestMove);

		return bestMoveGoal;
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return "NotABot";
	}

}
