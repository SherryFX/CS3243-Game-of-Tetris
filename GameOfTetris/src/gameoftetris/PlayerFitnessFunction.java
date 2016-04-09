
import java.util.Arrays;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

public class PlayerFitnessFunction extends FitnessFunction {

    public PlayerFitnessFunction() {

    }

    @Override
    // Evaluation of subject's fitness
    protected double evaluate(IChromosome subject) {
        double[] weights = Arrays.stream(subject.getGenes()).mapToDouble(gene -> (double) gene.getAllele()).toArray();
        return new PlayerSkeletonOneLayer(weights).run();
    }
}
