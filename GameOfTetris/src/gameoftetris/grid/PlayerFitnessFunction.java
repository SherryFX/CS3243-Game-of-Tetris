package grid;

import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.IChromosome;

public class PlayerFitnessFunction extends FitnessFunction {

    public PlayerFitnessFunction() {

    }

    @Override
    // Evaluation of subject's fitness
        protected
        double evaluate(IChromosome subject) {
        Gene[] geneArray = subject.getGenes();
        double[] weights = new double[geneArray.length];
        int i = 0;
        for (Gene g : geneArray) {
            weights[i] = (double) g.getAllele();
            i++;
        }
        return new PlayerSkeleton(weights).run();
    }
}
