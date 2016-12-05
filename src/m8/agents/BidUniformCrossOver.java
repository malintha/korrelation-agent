package m8.agents;

import negotiator.Bid;
import negotiator.Domain;
import negotiator.issue.Issue;
import negotiator.issue.Value;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ChromosomePair;
import org.apache.commons.math3.genetics.CrossoverPolicy;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Malintha on 12/5/2016.
 */
public class BidUniformCrossOver implements CrossoverPolicy {

    private final double ratio;
    private final Domain domain;

    public BidUniformCrossOver(double ratio, Domain domain) throws OutOfRangeException {
        this.domain = domain;
        if (ratio >= 0.0D && ratio <= 1.0D) {
            this.ratio = ratio;
        } else {
            throw new OutOfRangeException(LocalizedFormats.CROSSOVER_RATE, Double.valueOf(ratio), Double.valueOf(0.0D), Double.valueOf(1.0D));
        }
    }

    @Override
    public ChromosomePair crossover(Chromosome first, Chromosome second) throws MathIllegalArgumentException {
        if (first instanceof Bid && second instanceof Bid) {
            return this.mate((Bid) first, (Bid) second);
        } else {
            throw new MathIllegalArgumentException(LocalizedFormats.ARGUMENT_OUTSIDE_DOMAIN, new Object[0]);
        }
    }

    private ChromosomePair mate(Bid first, Bid second) {
        int length = first.getIssues().size();
        if (length != second.getIssues().size()) {
            throw new DimensionMismatchException(second.getIssues().size(), length);
        } else {
            List<Issue> parent1Rep = first.getIssues();
            List<Issue> parent2Rep = second.getIssues();
            HashMap<Integer, Value> child1Rep = new HashMap<>(length);
            HashMap<Integer, Value> child2Rep = new HashMap<>(length);
            RandomGenerator random = GeneticAlgorithm.getRandomGenerator();

            for (int index = 0; index < length; ++index) {
                if (random.nextDouble() < this.ratio) {
                    child1Rep.put(index, second.getValue(index + 1));
                    child2Rep.put(index, first.getValue(index + 1));
                } else {
                    child1Rep.put(index, first.getValue(index + 1));
                    child2Rep.put(index, second.getValue(index + 1));
                }
            }

            Bid b1 = new Bid(domain, child1Rep);
            Bid b2 = new Bid(domain, child2Rep);

            return new ChromosomePair(b1, b2);
        }
    }

}
