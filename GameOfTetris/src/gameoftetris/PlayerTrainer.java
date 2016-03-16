package gameoftetris;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;

public class PlayerTrainer {
	
	public static final int MAX_ALLOWED_EVOLUTIONS = 10000000;
	
	public static void main(String[] args) throws Exception {

		  Configuration conf = new DefaultConfiguration();

		  FitnessFunction fitnessFunction = new PlayerFitnessFunction();

		  conf.setFitnessFunction(fitnessFunction);

		  Gene[] genes = new Gene[ 10 ];
		  
		  // Each gene corresponds to a considered feature of the game
		  genes[0] = new DoubleGene(conf, -100, 100);  
		  genes[1] = new DoubleGene(conf, -100, 100);  
		  genes[2] = new DoubleGene(conf, -100, 100);  
		  genes[3] = new DoubleGene(conf, -100, 100); 
		  genes[4] = new DoubleGene(conf, -100, 100);  
		  genes[5] = new DoubleGene(conf, -100, 100); 
		  genes[6] = new DoubleGene(conf, -100, 100);  
		  genes[7] = new DoubleGene(conf, -100, 100); 
		  genes[8] = new DoubleGene(conf, -100, 100);  
		  genes[9] = new DoubleGene(conf, -100, 100); 

		  Chromosome chromosome = new Chromosome(conf, genes);

		  conf.setSampleChromosome(chromosome);

		  conf.setPopulationSize(500);
		  
		  Genotype population = Genotype.randomInitialGenotype(conf);
		  population.evolve();
		  
		  for (int i = 0; i < MAX_ALLOWED_EVOLUTIONS; i++){
			  population.evolve();
		  }
		  
		  IChromosome bestSolutionSoFar = population.getFittestChromosome();
	}
	
}
