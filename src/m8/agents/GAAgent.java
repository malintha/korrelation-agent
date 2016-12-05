package m8.agents;

import agents.m8.CorrelationAgent;
import negotiator.Bid;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.boaframework.offeringstrategy.anac2010.IAMhaggler2010.TimeConcessionFunction;
import negotiator.issue.Issue;
import negotiator.issue.Objective;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.*;
import org.apache.commons.math3.genetics.*;

import java.util.*;

/**
 * Created by Malintha on 12/4/2016.
 */
public class GAAgent extends CorrelationAgent {

    private static int populationSize = 1000;
    private double targetUtility;
    private double currentUtility;
    private ArrayList<Bid> proposalList;

    @Override
    public Action generateCounterOffer(Offer opponentOffer) {
        return new Offer(getAgentID(), getNextBid());
    }

    private ArrayList<Bid> createPopulation() {
        ArrayList<Bid> newPopulation = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            newPopulation.add(utilitySpace.getDomain().getRandomBid(new Random()));
        }
        return newPopulation;
    }

    private void setTargetUtility(double startUtility) {
        TimeConcessionFunction tcf = new TimeConcessionFunction(TimeConcessionFunction.Beta.LINEAR, TimeConcessionFunction.BREAKOFF);
        double currentTime = timeline.getTime() * timeline.getTotalTime() * 1000;
        double totalTime = timeline.getTotalTime() * 1000;
        this.targetUtility = tcf.getConcession(startUtility, Math.round(currentTime), totalTime);
    }

    private double getUtilityByIssue(Bid bid, Issue issue) {
        AdditiveUtilitySpace additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;
        Iterator<Map.Entry<Objective, Evaluator>> issueE = additiveUtilitySpace.getEvaluators().iterator();
        Set<Map.Entry<Objective, Evaluator>> evaluatorSet = additiveUtilitySpace.getEvaluators();
        Evaluator eval = additiveUtilitySpace.getEvaluator(1);
        double weight1 = eval.getWeight();
        if (eval.getType() == EVALUATORTYPE.DISCRETE) {

        }

        while (issueE.hasNext()) { // every issue
            Map.Entry<Objective, Evaluator> entry = issueE.next();
            Evaluator e = entry.getValue();
            double weight = e.getWeight();
            if (e.getType() == EVALUATORTYPE.DISCRETE) {
                Iterator<ValueDiscrete> v = ((EvaluatorDiscrete) e).getValues()
                        .iterator();
                List<Double> s = new ArrayList<Double>();
                double sumU = 0;
                while (v.hasNext()) {
                    ValueDiscrete vd = v.next();
                    try {
                        double val = ((EvaluatorDiscrete) e).getEvaluation(vd);
                        s.add(val);
                        sumU += val;

                    } catch (Exception e1) {
                        // System.out.println("META-Agent IO exception: " +
                        // e1.toString());
                    }
                }
                int currSize = s.size();

            } else if (e.getType() == EVALUATORTYPE.INTEGER) {
                double tempEU = ((double) (((EvaluatorInteger) e).getUpperBound() + ((EvaluatorInteger) e)
                        .getLowerBound())) / 2;
                double tempStdevU = Math
                        .sqrt((Math.pow(((EvaluatorInteger) e).getUpperBound()
                                - tempEU, 2) + Math.pow(
                                ((EvaluatorInteger) e).getLowerBound() - tempEU,
                                2)) / 2);
            } else if (e.getType() == EVALUATORTYPE.REAL) {
                double tempEU = (((EvaluatorReal) e).getUpperBound() + ((EvaluatorReal) e)
                        .getLowerBound()) / 2;
                double tempStdevU = Math
                        .sqrt((Math.pow(((EvaluatorReal) e).getUpperBound()
                                - tempEU, 2) + Math.pow(
                                ((EvaluatorReal) e).getLowerBound() - tempEU, 2)) / 2);
            } else {
                double tempEU = 0.5;
                double tempStdevU = 0;
            }
        }

        double weight = additiveUtilitySpace.getWeight(issue.getNumber());

        double issueUtility = 1 * weight;
        return issueUtility;
    }

    private boolean calculateIsEquallyDistributed(Bid bid) {
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
        if (proposalList != null) {
            if (proposalList.size() != 0) {
                ChromosomicBid b = (ChromosomicBid) proposalList.get(0);
                proposalList.remove(0);
                return b;
            }
        }

        double currentUtility = utilitySpace.getUtility(this.myLastBid);
        setTargetUtility(currentUtility);
        ArrayList<Bid> population = createPopulation();
        ArrayList<Chromosome> parentList = selectParents(population);
        performGeneticOperations(parentList);

        return null;
    }

    private ArrayList<Chromosome> selectParents(ArrayList<Bid> population) {
        ArrayList<Chromosome> parentList = new ArrayList<>();
        proposalList = new ArrayList<>();
        for (Bid b : population) {
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
        if (Math.abs(bidUtility - targetUtility) < 0.01) {
            return true;
        }
        return false;
    }

    private void performGeneticOperations(ArrayList<Chromosome> parentList) {
        org.apache.commons.math3.genetics.GeneticAlgorithm ga = new org.apache.commons.math3.genetics.GeneticAlgorithm(new BidUniformCrossOver(0.1, utilitySpace.getDomain()), 1, new org.apache.commons.math3.genetics.RandomKeyMutation(), 0.1, new org.apache.commons.math3.genetics.TournamentSelection(3));
//        GeneticAlgorithm ga = new GeneticAlgorithm(new CycleCrossover(), 1, new RandomKeyMutation(), 0.1, new TournamentSelection(1));

        Population initialPopulation = new ElitisticListPopulation(parentList, 1000, 0.2);
        StoppingCondition stopCond = new FixedGenerationCount(3);
        Population finalPopulation = ga.evolve(initialPopulation, stopCond);
        for(Chromosome cb : finalPopulation) {
            proposalList.add((ChromosomicBid) cb);
        }

    }

}
