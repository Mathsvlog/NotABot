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
	private StateMachine stateMachine;

	public NotABot(){
		stateMachine = getInitialStateMachine();
	}

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

		return getBestMove(getCurrentState());
	}

	private Move getBestMove(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		System.out.println(stateMachine);
		System.out.println(state);
		System.out.println(getRole());
		List<Move> moves = stateMachine.getLegalMoves(state, getRole());
		Move bestMove = null;
		MachineState bestEndState = null;

		for (Move move:moves){
			System.out.println(move);
			List<Move> currMoves = new ArrayList<Move>();
			currMoves.add(move);
			MachineState currEndState = getBestEndState(stateMachine.getNextState(state, currMoves));

			if (bestMove==null || stateMachine.getGoal(currEndState, getRole()) > stateMachine.getGoal(bestEndState, getRole())){
				bestMove = move;
				bestEndState = currEndState;
			}

		}

		return bestMove;
	}

	private MachineState getBestEndState(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if (stateMachine.isTerminal(state)) return state;

		List<Move> moves = stateMachine.getLegalMoves(state, getRole());
		MachineState bestEndState = null;

		for (Move move:moves){
			List<Move> currMoves = new ArrayList<Move>();
			currMoves.add(move);
			MachineState currEndState = getBestEndState(stateMachine.getNextState(state, currMoves));

			if (bestEndState==null || stateMachine.getGoal(currEndState, getRole()) > stateMachine.getGoal(bestEndState, getRole())){
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
