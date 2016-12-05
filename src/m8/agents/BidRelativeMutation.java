package m8.agents;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.utility.UtilitySpace;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.MutationPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Malintha on 12/5/2016.
 */
public class BidRelativeMutation implements MutationPolicy {

    private final UtilitySpace utilitySpace;
    private final Bid opponentBid;
    private final double targetUtility;
    private static final double different_threshold = 0.05;

    public BidRelativeMutation(Bid opponentBid, UtilitySpace utilitySpace, double targetUtility) {
        this.utilitySpace = utilitySpace;
        this.opponentBid = opponentBid;
        this.targetUtility = targetUtility;
    }

    @Override
    public Chromosome mutate(Chromosome original) throws MathIllegalArgumentException {
        if (!(original instanceof Bid)) {
            throw new MathIllegalArgumentException(LocalizedFormats.RANDOMKEY_MUTATION_WRONG_CLASS, new Object[]{original.getClass().getSimpleName()});
        } else {
            Bid originalRk = (Bid) original;
            List<Issue> repr = originalRk.getIssues();


            int rInd = GeneticAlgorithm.getRandomGenerator().nextInt(repr.size());
            ArrayList newRepr = new ArrayList(repr);
            newRepr.set(rInd, Double.valueOf(GeneticAlgorithm.getRandomGenerator().nextDouble()));
            return originalRk.newFixedLengthChromosome(newRepr);
        }
    }


}
