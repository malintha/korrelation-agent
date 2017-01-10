package m8.agents;

import agents.similarity.Similarity;
import negotiator.*;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;
import negotiator.issue.*;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

public class CorrelationAgent extends Agent {

    public static final int NUMBER_OF_SMART_STEPS = 0;
    public static final double ALLOWED_UTILITY_DEVIATION = 0.01;
    public static final double CONCESSION_FACTOR = 0.035;
    public static Logger log = Logger.getLogger(agents.m8.CorrelationAgent.class.getName());
    static AdditiveUtilitySpace additiveUtilitySpace;
    public Action actionOfPartner = null;
    public Action myLastAction;
    public Bid myLastBid;
    public AgentID agentID;
    public int fSmartSteps;
    public ArrayList<Bid> opponentsOffers;
    public int round = 0;
    public int nOfIssues = 0;
    public Similarity fSimilarity;
    public HashMap<Bid, Double> utilityCash;
    int numOfDiscreteIssues = 0;
    HashMap<Integer, HashMap<ValueDiscrete, Double>> valuesMap;

    List<ArrayBlockingQueue<Double>> bidsList;

    public ActionType getActionType(Action lAction) {
        ActionType lActionType = ActionType.START;
        if (lAction instanceof Offer)
            lActionType = ActionType.OFFER;
        else if (lAction instanceof Accept)
            lActionType = ActionType.ACCEPT;
        else if (lAction instanceof EndNegotiation)
            lActionType = ActionType.BREAKOFF;
        return lActionType;
    }

    /**
     * init is called when a next session starts with the same opponent.
     */

    public void init() {
        this.myLastBid = null;
        this.myLastAction = null;
        this.fSmartSteps = 0;
        this.opponentsOffers = new ArrayList<>();
        this.nOfIssues = utilitySpace.getDomain().getIssues().size();
        this.agentID = this.getAgentID();
        this.fSimilarity = new Similarity(utilitySpace.getDomain());
        this.bidsList = new ArrayList<>(utilitySpace.getDomain().getIssues().size());
        additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

        for(int i =0;i < this.nOfIssues; i++) {
            bidsList.add(new ArrayBlockingQueue<Double>(15));
        }

        this.utilityCash = new HashMap<>();
        BidIterator lIter = new BidIterator(utilitySpace.getDomain());
        try {
            while (lIter.hasNext()) {
                Bid tmpBid = lIter.next();
                utilityCash.put(tmpBid,
                        new Double(utilitySpace.getUtility(tmpBid)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        valuesMap = new HashMap<>();

        for (int i = 0; i < this.nOfIssues; i++) {
            Issue issue = utilitySpace.getDomain().getIssues().get(i);
            if (issue.getType() == ISSUETYPE.DISCRETE) {
                IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
                List<ValueDiscrete> valueDiscretes = issueDiscrete.getValues();
                HashMap<ValueDiscrete, Double> h = new HashMap<>();
                for (int j = 0; j < valueDiscretes.size(); j++) {
                    double eval = ((EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issue.getNumber())).getValue(valueDiscretes.get(j));
                    h.put(valueDiscretes.get(j), eval);
                }
                valuesMap.put(issue.getNumber(), h);
            }
        }

        log.info("Agent "+agentID+" is initialized");
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    public void ReceiveMessage(Action opponentAction) {
        actionOfPartner = opponentAction;
    }

    @Override
    public Action chooseAction() {
        round++;
        ActionType lActionType = getActionType(actionOfPartner);
        Action lMyAction = null;
        try {
            switch (lActionType) {
                case OFFER:
                    Offer loffer = ((Offer) actionOfPartner);
                    System.out.println("###opponent last bid = "+loffer.getBid().toString());
                    opponentsOffers.add(loffer.getBid());
                    if (myLastAction == null) { //this is opponent's initial offer (y0)
                        if (utilitySpace.getUtility(loffer.getBid()) == 1) {
                            lMyAction = new Accept(getAgentID(), loffer.getBid());
                        } else { //i generate my initial offer
                            lMyAction = generateInitialOffer(loffer);
                        }
                    } else if (utilitySpace.getUtility(loffer.getBid()) >= utilitySpace.getUtility(((Offer)myLastAction).getBid())) {
                        lMyAction = new Accept(getAgentID(), loffer.getBid());
                    } else { //generate counter offer
                        lMyAction = generateCounterOffer(loffer);
                    }
                    break;
                case ACCEPT:
                    break;
                case BREAKOFF:
                    break;
                default: //I am starting
                    if (myLastAction == null) {
                        log.info("###default###" + lActionType.name());
                        lMyAction = generateInitialOffer(null);
                    } else { // simply repeat last action
                        lMyAction = myLastAction;
                        myLastBid = ((Offer)myLastAction).getBid();
                    }
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        myLastAction = lMyAction;
        return lMyAction;
    }

    public Action generateInitialOffer(Offer opponentOffer) throws Exception {
        Bid myInitialBid = null;

        if (opponentOffer != null) {
            myInitialBid = getBidRandomWalk(0.8, 1.0);
        }
        else {
            Domain domain = utilitySpace.getDomain();
            myInitialBid = getBidRandomWalk(0.8, 1.0);
        }
        this.myLastBid = myInitialBid;
        return new Offer(getAgentID(), myInitialBid);
    }

    public Action generateCounterOffer(Offer opponentOffer) throws InterruptedException {
        Bid pOpponentBid = opponentOffer.getBid();

        for(int i =0; i<this.nOfIssues ; i++) {
            ArrayBlockingQueue<Double> issueValues = bidsList.get(i);
            if(issueValues.remainingCapacity() == 0) {
                issueValues.take();
            }
            Issue issue = utilitySpace.getDomain().getIssues().get(i);
            double key;
            if (issue.getType() == ISSUETYPE.DISCRETE) {
                key = valuesMap.get(issue.getNumber()).get(opponentOffer.getBid().getValue(issue.getNumber()));
            } else {
                key = Double.valueOf(opponentOffer.getBid().getValue(issue.getNumber()).toString());
            }
//            double evaluation = additiveUtilitySpace.getEvaluator(i+1).getEvaluation(additiveUtilitySpace, pOpponentBid, i+1);
            issueValues.offer(key);
        }

        Bid myBid = null;
        if(useCorrelation()) {
            getNextBidCorrelated(pOpponentBid);
            myBid = getNextBidSmart(pOpponentBid);

        }
        else {
            try {
                myBid = getBidRandomWalk(0.9, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new Offer(getAgentID(), myBid);
    }

    public Bid getNextBidCorrelated(Bid pOppntBid) {
        double Ei = 0;
        Bid myNextBid = myLastBid;
        HashMap<Integer, Value> myValues = myLastBid.getValues();
        HashMap<Integer, Value> newValues = new HashMap<Integer, Value>();
        double utilityExcess = 0;
        Evaluator evaluator;
        for (int i = 0; i < nOfIssues; i++) {
            Issue issueI = utilitySpace.getDomain().getIssues().get(i);
            ArrayBlockingQueue<Double> issueIValues = bidsList.get(i);
            Double[] issueIArray = issueIValues.toArray(new Double[issueIValues.size()]);

            double Yi = ((EvaluatorDiscrete)additiveUtilitySpace.getEvaluator(i)).getValue((ValueDiscrete) pOppntBid.getValue(i));
            double Xi = ((EvaluatorDiscrete)additiveUtilitySpace.getEvaluator(i)).getValue((ValueDiscrete) myLastBid.getValue(i));
//            double Yi = additiveUtilitySpace.getEvaluation(issueI.getNumber(), pOppntBid);
//            double Xi = additiveUtilitySpace.getEvaluation(issueI.getNumber(), myLastBid);
            evaluator = additiveUtilitySpace.getEvaluator(i);
            IssueDiscrete thisIssueDiscrete = (IssueDiscrete) myLastBid.getIssues().get(i);
            Ei = Yi - Xi;
            if(Ei > 0) {
                //opponent's recommendation is more preferred by the both. hence use it.
                newValues.put(thisIssueDiscrete.getNumber(), pOppntBid.getValue(thisIssueDiscrete.getNumber()));
            }
            if (Ei < 0) {
                // I have suggested a value more preferred to me, not by the opponent. Hence use opponent's suggestion,
                // but keep the utility im losing to balance it out by increasing on another issue.

                utilityExcess += /*evaluator.getWeight()**/(Xi - Yi);
                newValues.put(thisIssueDiscrete.getNumber(), pOppntBid.getValue(thisIssueDiscrete.getNumber()));
                double delta_x = Xi - Yi;
                for (int j = 0; j < nOfIssues; j++) {
                    if (j != i) {
                        Issue issueJ = utilitySpace.getDomain().getIssues().get(j);
                        ArrayBlockingQueue<Double> issueJValues = bidsList.get(j);
                        Double[] issueJArray = issueJValues.toArray(new Double[issueJValues.size()]);
                        double r = getCorrelationXZ(issueIArray, issueJArray);
                        System.out.println(r);
                    }
                }
            }
        }

        return null;
    }

    public Value getDiscreteFloorX(double aspirationalValue) {
    return null;
    }

    public Bid getNextBidSmart(Bid pOppntBid) {
        double lMyUtility = 0, lOppntUtility = 0, lTargetUtility;
        try {
            lMyUtility = utilitySpace.getUtility(myLastBid);
            lOppntUtility = utilitySpace.getUtility(pOppntBid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (fSmartSteps >= NUMBER_OF_SMART_STEPS) {
            lTargetUtility = getTargetUtility(lMyUtility);
            fSmartSteps = 0;
        } else {
            lTargetUtility = lMyUtility;
            fSmartSteps++;
        }
        Bid lMyLastBid = myLastBid;
        Bid lBid = getTradeOffExhaustive(lTargetUtility, pOppntBid);
        if (Math.abs(fSimilarity.getSimilarity(lMyLastBid, lBid)) > 0.993) {
            lTargetUtility = getTargetUtility(lMyUtility);
            fSmartSteps = 0;
            lBid = getTradeOffExhaustive(lTargetUtility, pOppntBid);
        }
        return lBid;
    }

    public Bid getTradeOffExhaustive(double pUtility, Bid pOppntBid) {
        Bid lBid = null;
        double lSim = -1;
        for (Map.Entry<Bid, Double> entry : utilityCash.entrySet()) {
            Bid tmpBid = entry.getKey();
            double lUtil = entry.getValue();
            if (Math.abs(lUtil - pUtility) < ALLOWED_UTILITY_DEVIATION) {
                double lTmpSim = fSimilarity.getSimilarity(tmpBid, pOppntBid);
                if (lTmpSim > lSim) {
                    lSim = lTmpSim;
                    lBid = tmpBid;
                }
            }
        }
        return lBid;
    }

    public boolean useCorrelation() {
        return round > 3;
    }

    public double getTargetUtility(double myUtility) {
        return myUtility - getConcessionFactor();
    }

    public double getConcessionFactor() {
        return CONCESSION_FACTOR;
    }

    public Bid getBidRandomWalk(double lowerBound, double upperBoud)
            throws Exception {
        // find all suitable bids
        ArrayList<Bid> lBidsRange = new ArrayList<Bid>();
        BidIterator lIter = new BidIterator(utilitySpace.getDomain());
        while (lIter.hasNext()) {
            Bid tmpBid = lIter.next();
            double lUtil = 0;
            try {
                lUtil = utilitySpace.getUtility(tmpBid);
                if (lUtil >= lowerBound && lUtil <= upperBoud)
                    lBidsRange.add(tmpBid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (lBidsRange.size() < 1) {
            return null;
        }
        if (lBidsRange.size() < 2) {
            return lBidsRange.get(0);
        } else {
            int lIndex = (new Random()).nextInt(lBidsRange.size() - 1);
            return lBidsRange.get(lIndex);
        }
    }

    public double getPartialCorrelationControlledZ(double rxy, double rxz, double ryz) {
        double rxy_z = 0;
        rxy_z = (rxy - rxz * ryz) / (Math.sqrt(1 - rxz * rxz) * Math.sqrt(1 - ryz * ryz));
        return rxy_z;
    }

    public double getCorrelationXZ(Double[] x, Double[] y) {
        double sumx = 0.0, sumy = 0.0;
        int n = x.length;
        for (int i = 0; i < x.length; i++) {
            sumx += x[i];
            sumy += y[i];
        }

        double xbar = sumx / n;
        double ybar = sumy / n;

        double[] xi_xbar = new double[n];
        double[] yi_ybar = new double[n];
        double[] xi_xbar_yi_ybar = new double[n];
        double[] xi_xbar_sq = new double[n];
        double[] yi_ybar_sq = new double[n];

        for (int i = 0; i < n; i++) {
            xi_xbar[i] = x[i] - xbar;
            yi_ybar[i] = y[i] - ybar;
            xi_xbar_yi_ybar[i] = xi_xbar[i] * yi_ybar[i];
            xi_xbar_sq[i] = xi_xbar[i] * xi_xbar[i];
            yi_ybar_sq[i] = yi_ybar[i] * yi_ybar[i];
        }

        double sig_xi_xbar_sq = 0, sig_yi_ybar_sq = 0, sig_xi_xbar_yi_ybar = 0;

        for (int i = 0; i < n; i++) {
            sig_xi_xbar_sq += xi_xbar_sq[i];
            sig_yi_ybar_sq += yi_ybar_sq[i];
            sig_xi_xbar_yi_ybar += xi_xbar_yi_ybar[i];
        }

        double rxy = sig_xi_xbar_yi_ybar / (Math.sqrt(sig_xi_xbar_sq * sig_yi_ybar_sq));
        return rxy;
    }

    public enum ActionType {
        START, OFFER, ACCEPT, BREAKOFF
    }

}

