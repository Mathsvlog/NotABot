package notabot;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
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

		System.out.println("________");
		System.out.println(getStateMachine().getLegalMoves(getCurrentState(), getRole()));

		MovePath moveState = getBestMovePath(getCurrentState(), true);

		return moveState.popMove();
	}

	private MovePath getBestMovePath(MachineState state, boolean isFirst) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if (getStateMachine().isTerminal(state)){
			return new MovePath(getStateMachine().getGoal(state, getRole()));
		}

		MovePath bestWorst = null;

		// get moveset for each opponent; compute number of opponent combinations
		List<List<Move>> oppMoves = new ArrayList<List<Move>>();
		int numCombinations = 1;
		for (Role role: getStateMachine().getRoles()){
			if (!role.equals(getRole())){
				List<Move> currMoves = getStateMachine().getLegalMoves(state, role);
				oppMoves.add(currMoves);
				numCombinations *= currMoves.size();
			}
		}

		// for each of our possible moves
		for (Move move: getStateMachine().getLegalMoves(state, getRole())){

			MovePath worstBest = null;

			// compute every possible combination of opponent moves
			for (int i = 0; i < numCombinations; i++) {

				List<Move> moveCombo = new ArrayList<Move>();
				int divisor = 1;

				// get each opponent's move for current combination
				for (int opp = 0; opp < oppMoves.size(); opp++) {
					List<Move> currOppMoves = oppMoves.get(opp);
					moveCombo.add(currOppMoves.get(i/divisor % currOppMoves.size()));
					divisor *= currOppMoves.size();
				}

				// Find the worst case with this combination
				moveCombo.add(move);
				MovePath currComboPath = getBestMovePath(getStateMachine().getNextState(state, moveCombo), false);

				if (worstBest == null || worstBest.getEndStateGoal() > currComboPath.getEndStateGoal()){
					worstBest = currComboPath;
				}

			}

			if (isFirst) System.out.println(move + ": " + worstBest.getEndStateGoal());

			if (bestWorst == null || bestWorst.getEndStateGoal() < worstBest.getEndStateGoal()){
				bestWorst = worstBest;
				bestWorst.pushMove(move);
			}

		}

		if (isFirst) System.out.println();

		return bestWorst;
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
