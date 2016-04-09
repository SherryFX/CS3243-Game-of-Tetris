import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.Population;
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
    public static final int NUM_THREADS = 4;
    public static final int MAX_ALLOWED_EVOLUTIONS = 2;
    public static final int POPULATION_SIZE = 100;

    public static void main(String[] args) throws Exception {

        // Transfer all readings from log.txt to log_old.txt
        // log_old.txt is like a backup
        backupLog();

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

//        conf.setPopulationSize(POPULATION_SIZE);
//        Genotype population = Genotype.randomInitialGenotype(conf);
        //
        // ------------------------------------------------------------

        // MANUAL GENERATION OF CHROMOSOMES ---------------------------
        // Uncomment this section to manually generate chromosomes from log.txt
        // Population size will set to number of chromosomes read from log.txt
         BufferedReader in = new BufferedReader(new FileReader("log.txt"));
         Population p = new Population(conf);
         String line = null;
         int count = 0;
         while ((line = in.readLine()) != null){
         String[] arr = line.split(" "); genes = new Gene[7]; genes[0] = new
         DoubleGene(conf,0, 2);
         genes[0].setAllele(Double.parseDouble(arr[0]));
         genes[1] = new DoubleGene(conf, 0, 2);
         genes[1].setAllele(Double.parseDouble(arr[1]));
         genes[2] = new DoubleGene(conf, 0, 2);
         genes[2].setAllele(Double.parseDouble(arr[2]));
         genes[3] = new DoubleGene(conf, 0, 2);
         genes[3].setAllele(Double.parseDouble(arr[3]));
         genes[4] = new DoubleGene(conf, 0, 2);
         genes[4].setAllele(Double.parseDouble(arr[4]));
         genes[5] = new DoubleGene(conf, 0, 2);
         genes[5].setAllele(Double.parseDouble(arr[5]));
         genes[6] = new DoubleGene(conf, 0, 2);
         genes[6].setAllele(Double.parseDouble(arr[6]));
         Chromosome c = new Chromosome(conf, genes);
         p.addChromosome(c);
         count++;
         }
         in.close();
         conf.setPopulationSize(count);
         Genotype population = new Genotype(conf, p);

        // ------------------------------------------------------------

     // WRITE RANDOM POPULATION TO LOG.TXT
        // WARNING: This will delete your current log.txt contents
//        IChromosome[] chromos = population.getPopulation().toChromosomes();
//        updateLog(chromos);
        
        for (int i = 0; i < MAX_ALLOWED_EVOLUTIONS; i++) {
            System.out.println("EVOLUTION CYCLE NO. " + i);
            population.evolve();
            IChromosome[] chromosomes = population.getPopulation().toChromosomes();
            IChromosome fittest = population.getFittestChromosome();
            Gene[] gene_array = fittest.getGenes();
            String s = "Fittest chromosome weights and value: ";
            for (int g = 0; g < gene_array.length; g++){
             s += gene_array[g].getAllele() + " ";
            }
            s += fittest.getFitnessValue() + " ";
            System.out.println(s);
            updateLog(chromosomes, i);
        }

        IChromosome bestSolutionSoFar = population.getFittestChromosome();

//        Arrays.stream(bestSolutionSoFar.getGenes()).mapToDouble(gene -> (double) gene.getAllele())
//            .forEach(System.out::println);
    }

    public static void updateLog(IChromosome[] chromosomes, int index) throws IOException {
    //    backupLog();
    //    clearLog();
    	String fileName = "log" + index + ".txt";
        PrintWriter weightsOut = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
        double r = 0;
        for (int j = 0; j < chromosomes.length; j++) {
            String s = "";
            IChromosome c = chromosomes[j];
            Gene[] gene_array = c.getGenes();
            for (int k = 0; k < gene_array.length; k++) {
                Gene g = gene_array[k];
                s += (double) g.getAllele() + " ";
            }
            r += c.getFitnessValue();
            weightsOut.println(s);
        }
        weightsOut.close();
        double avg = r/chromosomes.length;
        System.out.println("Average rows is: " + avg);
    }

    public static void clearLog() throws FileNotFoundException {
        PrintWriter out = new PrintWriter("log.txt");
        out.print("");
        out.close();
    }

    public static void backupLog() throws IOException {
        FileInputStream srcStream = new FileInputStream("log.txt");
        FileOutputStream destStream = new FileOutputStream("log_old.txt");
        FileChannel src = srcStream.getChannel();
        FileChannel dest = destStream.getChannel();
        dest.transferFrom(src, 0, src.size());
        srcStream.close();
        destStream.close();
    }

}
