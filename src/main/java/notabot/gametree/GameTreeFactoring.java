package notabot.gametree;

import java.util.Set;

import notabot.propnet.NotABotPropNetStateMachine;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;

public class GameTreeFactoring extends GameTree {

	final Set<Move> VALID_MOVES;

	public GameTreeFactoring(NotABotPropNetStateMachine propNet, MachineState initialState,
			Role playerRole, int subgameIndex) {
		super(propNet, initialState, playerRole);

		/*
 		this.stateMachine = stateMachine;
		int playerIndex = stateMachine.getRoles().indexOf(playerRole);
		root = new GameTreeNode(initialState, stateMachine, playerIndex);
		numRoles = stateMachine.getRoles().size();
		 */

		int playerIndex = stateMachine.getRoles().indexOf(playerRole);
		VALID_MOVES = propNet.getRelevantSubgameMoves(subgameIndex, playerRole);
		root = new GameTreeNodeFactoring(initialState, this);

	}



}
