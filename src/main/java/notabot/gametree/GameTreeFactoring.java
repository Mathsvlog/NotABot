package notabot.gametree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import notabot.MoveScore;
import notabot.propnet.NotABotPropNetStateMachine;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;

public class GameTreeFactoring extends GameTree {

	//final Set<Move> VALID_MOVES;
	final Map<Role, Set<Move>> VALID_MOVE_SETS;
	final Map<Role, List<Move>> VALID_MOVES;
	final boolean SUBTRACT_NULL_SCORE = true;

	public GameTreeFactoring(NotABotPropNetStateMachine propNet, MachineState initialState,
			Role playerRole, int subgameIndex) {
		super(propNet, initialState, playerRole);

		VALID_MOVE_SETS = propNet.getRelevantSubgameMoves(subgameIndex);
		VALID_MOVES = computeValidMoves();

		root = new GameTreeNodeFactoring(initialState, this);

	}

	private Map<Role, List<Move>> computeValidMoves(){
		Map<Role, List<Move>> moveMap = new HashMap<Role, List<Move>>();
		for (Role role: roles){
			List<Move> currMoves = new ArrayList<Move>();
			for (Move move: VALID_MOVE_SETS.get(role)){
				currMoves.add(move);
			}
			moveMap.put(role, currMoves);
		}
		return moveMap;
	}

	private int getRootChildCombo(List<Move> moves){
		// initialize combo as player index role
		Move currMove = moves.get(playerIndex);
		if (!root.allMoves.get(playerIndex).contains(currMove)) currMove = null;
		int combo = root.allMoves.get(playerIndex).indexOf(currMove);
		int factor = root.allMoves.get(playerIndex).size();
		// compute root child index
		for (int r = 0; r < numRoles; r++) {
			if (r != playerIndex){
				currMove = moves.get(r);
				if (!root.allMoves.get(r).contains(currMove)) currMove = null;
				combo += factor * root.allMoves.get(r).indexOf(currMove);
				factor *= root.allMoves.get(r).size();
			}
		}
		return combo;
	}

	public void traverse(List<Move> moves){

		int combo = getRootChildCombo(moves);
		if (root.children[combo] == null) root.createChild(combo);
		root = root.children[combo];
	}

	/**
	 * TODO NOT USED
	 */
	public MoveScore getNullMoveScore(){
		double worst = Double.POSITIVE_INFINITY;
		int nullIndex = root.playerMoves.indexOf(null);
		int numMoves = root.playerMoves.size();
		int numOppMoves = root.children.length / root.playerMoves.size();

		for (int o=0; o<numOppMoves; o++){
			int c = numMoves*o + nullIndex;
			if (root.children[c] != null){
				double score = root.children[c].getScore();
				if (worst > score){
					worst = score;
				}
			}
		}
		return new MoveScore(worst, null);
	}

	/**
	 * Simulates two levels of minimax
	 */
	@Override
	public MoveScore getBestMoveScore(boolean printout){
		MoveScore best = null;
		MoveScore nullScore = null;

		System.out.println("NUM PLAYER MOVES: " + root.playerMoves.size());
		int numMoves = root.playerMoves.size();
		int numOppMoves = root.children.length / root.playerMoves.size();
		// for each player move
		for (int p=0; p<numMoves; p++){

			// determine worst outcome for this move
			double worst = Double.POSITIVE_INFINITY;

			for (int o=0; o<numOppMoves; o++){
				int c = numMoves*o + p;
				if (root.children[c] != null){
					double score = root.children[c].getScore();
					if (worst > score){
						worst = score;
					}
				}
			}

			// keep track of best move
			if (root.playerMoves.get(p) != null){
				if (best==null || best.getScore() < worst){
					best = new MoveScore(worst, root.playerMoves.get(p));
				}
			}
			else{
				nullScore = new MoveScore(worst, null);
			}

			if (printout) System.out.println("\t"+root.playerMoves.get(p) + " : " + worst);

		}

		if (best==null){
			if (SUBTRACT_NULL_SCORE) best = new MoveScore(0, null);
			else best = nullScore;
		}

		else if (SUBTRACT_NULL_SCORE){
			best = new MoveScore(best.getScore()-nullScore.getScore(), best.getMove());
		}

		return best;
	}

	public int getNumVisits(){
		return root.numVisits;
	}

	public double selectFunction(int totalVisits){
		return root.selectFunction(totalVisits);
	}

}
