package m8.agents;

import agents.m8.GAAgent;
import negotiator.Agent;
import negotiator.Bid;
import negotiator.Domain;

/**
 * Created by Malintha on 12/6/2016.
 */

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
        System.out.println("Fitness of this proposal : " + utility);
        return utility;
    }

}