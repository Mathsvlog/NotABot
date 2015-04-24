package notabot;

import org.ggp.base.util.statemachine.Move;

public class MoveScore {

	private Move move;
	private final double score;

	public MoveScore(double score){
		move = null;
		this.score = score;
	}

	public void updateMove(Move move){
		this.move = move;
	}

	public Move getMove(){
		return move;
	}

	public double getScore(){
		return score;
	}

}
