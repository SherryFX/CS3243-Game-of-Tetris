package gameoftetris;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
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

    // Set these values if you want to automatically generate chromosomes
    // According to some research, the optimal setting is 100 chromosomes with
    // 200 generations
    // I suggest you set MAX_ALLOWED_EVOLUTIONS to 1, and run the program
    // periodically rather than
    // setting MAX_ALLOWED_EVOLUTIONS to a large number,
    // since each generation can take a significant amount of time.
    // For the first run, have chromosomes auto generated randomly.
    // The chromosomes will be saved into log.txt
    // For future runs, have chromosomes manually retrieved from log.txt.
    //
    public static final int MAX_ALLOWED_EVOLUTIONS = 10;
    public static final int POPULATION_SIZE = 100;

    public static void main(String[] args) throws Exception {

        // Transfer all readings from log.txt to log_old.txt
        // log_old.txt is like a backup
        FileInputStream srcStream = new FileInputStream("log.txt");
        FileOutputStream destStream = new FileOutputStream("log_old.txt");
        FileChannel src = srcStream.getChannel();
        FileChannel dest = destStream.getChannel();
        dest.transferFrom(src, 0, src.size());
        srcStream.close();
        destStream.close();

        BufferedReader in = new BufferedReader(new FileReader("log.txt"));

        Configuration conf = new DefaultConfiguration();
        FitnessFunction fitnessFunction = new PlayerFitnessFunction();
        conf.setFitnessFunction(fitnessFunction);

        Gene[] genes = new Gene[7];
        genes[0] = new DoubleGene(conf, 0, 2);
        genes[1] = new DoubleGene(conf, 0, 2);
        genes[2] = new DoubleGene(conf, 0, 2);
        genes[3] = new DoubleGene(conf, 0, 2);
        genes[4] = new DoubleGene(conf, 0, 2);
        genes[5] = new DoubleGene(conf, 0, 2);
        genes[6] = new DoubleGene(conf, 0, 2);
        Chromosome chromosome = new Chromosome(conf, genes);
        conf.setSampleChromosome(chromosome);

        // AUTO RANDOM CHROMOSOME GENERATION --------------------------
        // Uncomment this section to auto generate chromosomes
        // Make sure manual generation section is commented out

        conf.setPopulationSize(POPULATION_SIZE);
        Genotype population = Genotype.randomInitialGenotype(conf);

        // ------------------------------------------------------------

        // MANUAL GENERATION OF CHROMOSOMES ---------------------------
        // Uncomment this section to manually generate chromosomes from log.txt
        // Population size will set to number of chromosomes read from log.txt
        /*
         * Population p = new Population(conf); String line = null; int count =
         * 0; while ((line = in.readLine()) != null){ String[] arr =
         * line.split(" "); genes = new Gene[7]; genes[0] = new DoubleGene(conf,
         * 0, 2); genes[0].setAllele(Double.parseDouble(arr[0])); genes[1] = new
         * DoubleGene(conf, 0, 2);
         * genes[1].setAllele(Double.parseDouble(arr[1])); genes[2] = new
         * DoubleGene(conf, 0, 2);
         * genes[2].setAllele(Double.parseDouble(arr[2])); genes[3] = new
         * DoubleGene(conf, 0, 2);
         * genes[3].setAllele(Double.parseDouble(arr[3])); genes[4] = new
         * DoubleGene(conf, 0, 2);
         * genes[4].setAllele(Double.parseDouble(arr[4])); genes[5] = new
         * DoubleGene(conf, 0, 2);
         * genes[5].setAllele(Double.parseDouble(arr[5])); genes[6] = new
         * DoubleGene(conf, 0, 2);
         * genes[6].setAllele(Double.parseDouble(arr[6])); Chromosome c = new
         * Chromosome(conf, genes); p.addChromosome(c); count++; }
         * conf.setPopulationSize(count); Genotype population = new
         * Genotype(conf, p);
         */
        // ------------------------------------------------------------
        in.close();

        for (int i = 0; i < MAX_ALLOWED_EVOLUTIONS; i++) {
            population.evolve();
        }

        // Clear log.txt once data is backed up to log_old.txt
        PrintWriter out = new PrintWriter("log.txt");
        out.print("");
        out.close();

        out = new PrintWriter(new BufferedWriter(
            new FileWriter("log.txt", true)));
        IChromosome[] chromosomes = population.getChromosomes();
        for (int j = 0; j < chromosomes.length; j++) {
            String s = "";
            IChromosome c = chromosomes[j];
            Gene[] gene_array = c.getGenes();
            for (int k = 0; k < gene_array.length; k++) {
                Gene g = gene_array[k];
                s += (double) g.getAllele() + " ";
            }
            s += c.getFitnessValue();
            out.println(s);
        }
        out.close();

        IChromosome bestSolutionSoFar = population.getFittestChromosome();

        Arrays.stream(bestSolutionSoFar.getGenes())
            .mapToDouble(gene -> (double) gene.getAllele())
            .forEach(System.out::println);
    }

}
