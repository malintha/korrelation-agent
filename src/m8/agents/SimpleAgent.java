package m8.agents;

import agents.similarity.Similarity;
import negotiator.*;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;

import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;

public class SimpleAgent extends Agent {

    public Action actionOfPartner = null;
    public static Logger log = Logger.getLogger(SimpleAgent.class.getName());
    public Action myLastAction;
    public Bid myLastBid;
    public AgentID agentID;
    public int nOfIssues = 0;
    public Similarity fSimilarity;
    public HashMap<Bid, Double> utilityCash;

    public enum ActionType {
        START, OFFER, ACCEPT, BREAKOFF
    }

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

    public void init() {
        this.nOfIssues = utilitySpace.getDomain().getIssues().size();
        this.agentID = this.getAgentID();
        this.fSimilarity = new Similarity(utilitySpace.getDomain());
        this.utilityCash = new HashMap<>();
        BidIterator lIter = new BidIterator(utilitySpace.getDomain());
        while (lIter.hasNext()) {
            Bid tmpBid = lIter.next();
            utilityCash.put(tmpBid, new Double(utilitySpace.getUtility(tmpBid)));
        }
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
        ActionType lActionType = getActionType(actionOfPartner);
        Action lMyAction = null;
        try {
            switch (lActionType) {
                case OFFER:
                    Offer loffer = ((Offer) actionOfPartner);
                    System.out.println("Opponent's last bid = " + loffer.getBid().toString());
                    if (myLastAction == null) { //this is opponent's initial offer (y0)
                        if (utilitySpace.getUtility(loffer.getBid()) == 1) {
                            lMyAction = new Accept(getAgentID(), loffer.getBid());
                            //i generate my initial offer
                        } else {
                            lMyAction = generateInitialOffer();
                        }
                    } else if (utilitySpace.getUtility(loffer.getBid()) >= utilitySpace.getUtility(((Offer) myLastAction).getBid())) {
                        lMyAction = new Accept(getAgentID(), loffer.getBid());
                        //generate counter offer
                    } else {
                        lMyAction = generateCounterOffer();
                    }
                    break;
                case ACCEPT:
                    break;
                case BREAKOFF:
                    break;
                //I am starting
                default:
                    if (myLastAction == null) {
                        log.info("###default###" + lActionType.name());
                        lMyAction = generateInitialOffer();
                    } else {
                        // simply repeat last action
                        lMyAction = myLastAction;
                        myLastBid = ((Offer) myLastAction).getBid();
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        myLastAction = lMyAction;
        return lMyAction;
    }

    public Action generateInitialOffer() throws Exception {
        return new Offer(getAgentID(), getBidRandomWalk());
    }

    public Action generateCounterOffer() throws Exception {
        return new Offer(getAgentID(), getBidRandomWalk());
    }

    public Bid getBidRandomWalk() {
        return utilitySpace.getDomain().getRandomBid(new Random());

    }
}
