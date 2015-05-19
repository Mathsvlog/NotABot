package notabot.player;

import notabot.MoveScore;
import notabot.NotABot;
import notabot.gametree.GameTreeFactoring;
import notabot.propnet.NotABotPropNetStateMachine;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NotABotMonteCarloFactoring extends NotABot {

	GameTreeFactoring[] trees;
	NotABotPropNetStateMachine propNet;
	int numSubgames;

	@Override
	protected void runMetaGame() {
		System.out.println("RUNNING METAGAME");

		// ASSUMES USING CachedStateMachne CONTAINING NotABotPropNetStateMachine
		propNet = (NotABotPropNetStateMachine) ((CachedStateMachine) getStateMachine()).getBackingStateMachine();

		// build game tree for each subgame
		numSubgames = propNet.getNumSubgames();
		trees = new GameTreeFactoring[numSubgames];
		for (int i=0; i<numSubgames; i++){
			trees[i] = new GameTreeFactoring(propNet, getCurrentState(), getRole(), i);
		}

		sampleUntilTimeout();
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub

		for (int i=0; i<numSubgames; i++) trees[i].traverse(getCurrentState());

		sampleUntilTimeout();

		MoveScore bestMoveScore = null;

		for (int i=0; i<numSubgames; i++) {
			MoveScore moveScore = trees[0].getBestMoveScore(false);
			if (bestMoveScore == null || moveScore.getScore() > bestMoveScore.getScore()){
				bestMoveScore = moveScore;
			}
			System.out.println("BEST MOVE FOR SUBGAME ("+i+"): "+moveScore.getMove() + " : " + moveScore.getScore());
		}
		/*
		Move move = trees[0].getBestMove(true);
		if (move == null){
			System.out.println("TREE RETURNED NULL MOVE");
			move = getStateMachine().getLegalMoves(getCurrentState(), getRole()).get(0);
		}
		*/
		return bestMoveScore.getMove();
	}

	private void sampleUntilTimeout(){

		Thread[] threads = new Thread[numSubgames];

		for (int i=0; i<numSubgames; i++){
			final int j = i;
			threads[i] = new Thread(){
				@Override
				public void run(){
					int numSamples = 0;
					trees[j].resetDepthChargeCounter();
					while (!NotABot.hasTimedOut()){
						trees[j].updateSelectTemperature();
						trees[j].runSample();
						numSamples++;
					}
					System.out.println("SUBGAME ("+j+") - SAMPLES ("+numSamples+") - CHARGES ("+trees[j].getNumDepthCharges()+")");
					//System.out.println("NUM SAMPLES RAN ("+j+"): " + numSamples);
					//System.out.println("NUM DEPTH CHARGES ("+j+"): " + trees[j].getNumDepthCharges());
				}
			};
		}

		for (int i=0; i<numSubgames; i++){
			threads[i].start();
		}


		for (int i=0; i<numSubgames; i++){
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println();

	}

}
