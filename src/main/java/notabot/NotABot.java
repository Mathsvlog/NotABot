package notabot;

import java.util.ArrayList;
import java.util.List;

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

		Move move = getBestMove(getCurrentState());
		System.out.println(move);
		return move;
	}

	private Move getBestMove(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		Move bestMove = null;
		MachineState bestEndState = null;

		for (Move move:moves){
			List<Move> currMoves = new ArrayList<Move>();
			currMoves.add(move);
			MachineState currEndState = getBestEndState(getStateMachine().getNextState(state, currMoves));

			if (bestMove==null || getStateMachine().getGoal(currEndState, getRole()) > getStateMachine().getGoal(bestEndState, getRole())){
				bestMove = move;
				bestEndState = currEndState;
			}

		}

		return bestMove;
	}

	private MachineState getBestEndState(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if (getStateMachine().isTerminal(state)) return state;

		List<Move> moves = getStateMachine().getLegalMoves(state, getRole());
		MachineState bestEndState = null;

		for (Move move:moves){
			List<Move> currMoves = new ArrayList<Move>();
			currMoves.add(move);
			MachineState currEndState = getBestEndState(getStateMachine().getNextState(state, currMoves));

			if (bestEndState==null || getStateMachine().getGoal(currEndState, getRole()) > getStateMachine().getGoal(bestEndState, getRole())){
				bestEndState = currEndState;
			}

		}

		return bestEndState;
	}


	private List<Move> getMoves(MachineState state) throws MoveDefinitionException{
		return getStateMachine().getLegalMoves(state, getRole());
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
