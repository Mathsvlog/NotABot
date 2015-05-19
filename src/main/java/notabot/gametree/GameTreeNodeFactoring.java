package notabot.gametree;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class GameTreeNodeFactoring extends GameTreeNode{

	/**
	 * Constructor for any state except the initial state
	 */
	public GameTreeNodeFactoring(MachineState state, GameTreeFactoring gameTree){
		super(state, gameTree, gameTree.VALID_MOVES);
	}

	@Override
	public void createChild(int combo){
		try {
			// compute move combo
			List<Move> moveCombo = new ArrayList<Move>();
			int divisor = numPlayerMoves;
			int playerMoveIndex = combo%numPlayerMoves;

			// get each opponent's move for current combination
			for (int r = 0; r < TREE.numRoles; r++) {
				if (r != TREE.playerIndex){
					List<Move> currMoves = TREE.stateMachine.getLegalMoves(state, TREE.roles.get(r));
					moveCombo.add(currMoves.get((combo/divisor) % currMoves.size()));
					divisor *= currMoves.size();
				}
				else{
					moveCombo.add(playerMoves.get(playerMoveIndex));
				}
			}

			// create child node
			children[combo] = new GameTreeNodeFactoring(TREE.stateMachine.getNextState(state, moveCombo), (GameTreeFactoring) TREE);
		}
		catch (MoveDefinitionException | TransitionDefinitionException e) {
			e.printStackTrace();
		}
	}

}
