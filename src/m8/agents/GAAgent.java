package m8.agents;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.Domain;
import negotiator.boaframework.offeringstrategy.anac2010.IAMhaggler2010.TimeConcessionFunction;
import negotiator.issue.Issue;
import negotiator.tournament.Tournament;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.Evaluator;
import org.apache.commons.math.genetics.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by Malintha on 12/4/2016.
 */
public class GAAgent extends CorrelationAgent {

    private static int populationSize = 100;
    private double targetUtility;
    private ArrayList<ChromosomicBid> proposalList;


    private ArrayList<ChromosomicBid> createPopulation() {
        ArrayList<ChromosomicBid> newPopulation = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            newPopulation.add((ChromosomicBid) utilitySpace.getDomain().getRandomBid(new Random()));
        }
        return newPopulation;
    }

    private void setTargetUtility(double startUtility) {
        TimeConcessionFunction tcf = new TimeConcessionFunction(TimeConcessionFunction.Beta.LINEAR, TimeConcessionFunction.BREAKOFF);
        double currentTime = timeline.getTime() * timeline.getTotalTime() * 1000;
        double totalTime = timeline.getTotalTime() * 1000;
        this.targetUtility = tcf.getConcession(startUtility, Math.round(currentTime), totalTime);
    }

    private double getUtilityByIssue(ChromosomicBid bid, Issue issue) {
        AdditiveUtilitySpace additiveUtilitySpace = new AdditiveUtilitySpace(utilitySpace.getDomain());
        double weight = additiveUtilitySpace.getWeight(issue.getNumber());
        Evaluator evaluator = additiveUtilitySpace.getEvaluator(issue.getNumber());
        double score = evaluator.getEvaluation(additiveUtilitySpace, bid, issue.getNumber());
        double issueUtility = score*weight;
        return issueUtility;
    }

    private boolean calculateIsEquallyDistributed(ChromosomicBid bid) {
        List<Issue> issueList = bid.getIssues();
        double bidUtility = utilitySpace.getUtility(bid);
        double threshold = 0.4;
        for(Issue issue : issueList) {
            double issueUtility = getUtilityByIssue(bid, issue);
            double utilityProportion = issueUtility/bidUtility;
            if(utilityProportion > threshold) {
                return false;
            }
        }
        return true;
    }

    private ChromosomicBid getNextBid() {
        if(proposalList.size() != 0) {
            ChromosomicBid b = (ChromosomicBid) proposalList.get(0);
            proposalList.remove(0);
            return b;
        }

        //goto next target utility
        //create population
        //select parents
        //perform genetic operations
        //get resulting children
        //if intargetUtility and similarity then put to proposallist
        //get next bid

        double currentUtility = targetUtility;
        setTargetUtility(currentUtility);
        ArrayList<ChromosomicBid> parentList = selectParents(createPopulation());


        return null;
    }

    private ArrayList<ChromosomicBid> selectParents(ArrayList<ChromosomicBid> population) {
        ArrayList<ChromosomicBid> parentList = new ArrayList<>();
        proposalList = new ArrayList<>();
        for (ChromosomicBid b : population) {
            if (calculateIsEquallyDistributed(b)) {
                parentList.add(b);
            }
            else {
                if (isInTargetUtility(b)) {
                    proposalList.add(b);
                }
            }
        }
        return parentList;
    }

    private boolean isInTargetUtility(Bid bid) {
        double bidUtility = utilitySpace.getUtility(bid);
        if (Math.abs(bidUtility-targetUtility) < 0.001) {
            return true;
        }
        return false;
    }

    private ArrayList<Bid> performGeneticOperations(ArrayList<ChromosomicBid> parentList) {
        GeneticAlgorithm ga = new GeneticAlgorithm(new OnePointCrossover<>(), 1, new RandomKeyMutation(), 0.1, new TournamentSelection(1));

        ArrayList<Chromosome> bidChromosomes = new ArrayList<>();
        for(ChromosomicBid cb : parentList) {
            bidChromosomes.add(cb);
        }

        Population initialPopulation = new ElitisticListPopulation(bidChromosomes, 100, 0.2);
        StoppingCondition stopCond = new FixedGenerationCount(3);
        Population finalPopulation = ga.evolve(initialPopulation, stopCond);
        for(Chromosome cb : finalPopulation) {

        }


        return null;
    }

}


class ChromosomicBid extends Bid {

    double utility;
    double similarity;
    Agent agent;

    public ChromosomicBid(Domain domain) {
        super(domain);
        agent = new GAAgent();
    }

    @Override
    public double fitness() {
        utility = agent.utilitySpace.getUtility(this);
        return utility;
    }

}