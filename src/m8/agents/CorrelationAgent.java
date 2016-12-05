package m8.agents;

import agents.similarity.Similarity;
import negotiator.*;
import negotiator.actions.*;

import java.util.*;
import java.util.logging.Logger;

public class CorrelationAgent extends Agent {

    public Action actionOfPartner = null;
    public static Logger log = Logger.getLogger(CorrelationAgent.class.getName());
    public Action myLastAction;
    public Bid myLastBid;
    public AgentID agentID;
    public int fSmartSteps;
    public ArrayList<Bid> opponentsOffers;
    public int round = 0;
    public int nOfIssues = 0;
    public Similarity fSimilarity;
    public enum ActionType {
        START, OFFER, ACCEPT, BREAKOFF
    }
    public static final int NUMBER_OF_SMART_STEPS = 0;
    public static final double ALLOWED_UTILITY_DEVIATION = 0.01;
    public static final double CONCESSION_FACTOR = 0.035;
    public HashMap<Bid, Double> utilityCash;
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

    public Action generateCounterOffer(Offer opponentOffer) {
        Bid pOpponentBid = opponentOffer.getBid();
        Bid myBid;
        log.info("round = "+round+" issues = "+nOfIssues);
        if(useCorrelation()) {
            log.info("Using correlation strategy");
            myBid = getNextBidSmart(pOpponentBid);
        }
        else {
            log.info("Using similarity strategy");
            myBid = getNextBidSmart(pOpponentBid);
        }
        return new Offer(getAgentID(), myBid);
    }

    public Bid getNextBidCorrelated(Bid pOppntBid) {
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
        return round > nOfIssues * 2;
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

///*
//    public double[] getPartialCorrelations(int in, int nvars) {
//
//        final double[] output = new double[(nvars - in + 1) * (nvars - in) / 2];
//        double[] d = new double[nvars];
//        double[] rhs = new double[nvars];
//        double[] r = new double[nvars * (nvars - 1) / 2];
//        double sserr = 0.0;
//        int pos;
//        int pos1;
//        int pos2;
//        final int rms_off = -in;
//        final int wrk_off = -(in + 1);
//        final double[] rms = new double[nvars - in];
//        final double[] work = new double[nvars - in - 1];
//        double sumxx;
//        double sumxy;
//        double sumyy;
//        final int offXX = (nvars - in) * (nvars - in - 1) / 2;
//        if (in < -1 || in >= nvars) {
//            return null;
//        }
//        final int nvm = nvars - 1;
//        final int base_pos = r.length - (nvm - in) * (nvm - in + 1) / 2;
//        if (d[in] > 0.0) {
//            rms[in + rms_off] = 1.0 / Math.sqrt(d[in]);
//        }
//        for (int col = in + 1; col < nvars; col++) {
//            pos = base_pos + col - 1 - in;
//            sumxx = d[col];
//            for (int row = in; row < col; row++) {
//                sumxx += d[row] * r[pos] * r[pos];
//                pos += nvars - row - 2;
//            }
//            if (sumxx > 0.0) {
//                rms[col + rms_off] = 1.0 / Math.sqrt(sumxx);
//            } else {
//                rms[col + rms_off] = 0.0;
//            }
//        }
//        sumyy = sserr;
//        for (int row = in; row < nvars; row++) {
//            sumyy += d[row] * rhs[row] * rhs[row];
//        }
//        if (sumyy > 0.0) {
//            sumyy = 1.0 / Math.sqrt(sumyy);
//        }
//        pos = 0;
//        for (int col1 = in; col1 < nvars; col1++) {
//            sumxy = 0.0;
//            Arrays.fill(work, 0.0);
//            pos1 = base_pos + col1 - in - 1;
//            for (int row = in; row < col1; row++) {
//                pos2 = pos1 + 1;
//                for (int col2 = col1 + 1; col2 < nvars; col2++) {
//                    work[col2 + wrk_off] += d[row] * r[pos1] * r[pos2];
//                    pos2++;
//                }
//                sumxy += d[row] * r[pos1] * rhs[row];
//                pos1 += nvars - row - 2;
//            }
//            pos2 = pos1 + 1;
//            for (int col2 = col1 + 1; col2 < nvars; col2++) {
//                work[col2 + wrk_off] += d[col1] * r[pos2];
//                ++pos2;
//                output[ (col2 - 1 - in) * (col2 - in) / 2 + col1 - in] =
//                        work[col2 + wrk_off] * rms[col1 + rms_off] * rms[col2 + rms_off];
//                ++pos;
//            }
//            sumxy += d[col1] * rhs[col1];
//            output[col1 + rms_off + offXX] = sumxy * rms[col1 + rms_off] * sumyy;
//        }
//
//        return output;
//    }*/




}

