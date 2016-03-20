import java.util.Arrays;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;

public class PlayerTrainer {

    public static final int MAX_ALLOWED_EVOLUTIONS = 100;

    public static void main(String[] args) throws Exception {

        Configuration conf = new DefaultConfiguration();

        FitnessFunction fitnessFunction = new PlayerFitnessFunction();

        conf.setFitnessFunction(fitnessFunction);

        Gene[] genes = new Gene[6];

        // Each gene corresponds to a considered feature of the game
        genes[0] = new DoubleGene(conf, 0, 2);
        genes[1] = new DoubleGene(conf, 0, 2);
        genes[2] = new DoubleGene(conf, 0, 2);
        genes[3] = new DoubleGene(conf, 0, 2);
        genes[4] = new DoubleGene(conf, 0, 2);
        genes[5] = new DoubleGene(conf, 0, 2);
        Chromosome chromosome = new Chromosome(conf, genes);

        conf.setSampleChromosome(chromosome);

        conf.setPopulationSize(10);

        Genotype population = Genotype.randomInitialGenotype(conf);
        population.evolve();

        for (int i = 0; i < MAX_ALLOWED_EVOLUTIONS; i++) {
            population.evolve();
        }

        IChromosome bestSolutionSoFar = population.getFittestChromosome();

        Arrays.stream(bestSolutionSoFar.getGenes())
            .mapToDouble(gene -> (double) gene.getAllele())
            .forEach(System.out::println);
        ;
    }

}
