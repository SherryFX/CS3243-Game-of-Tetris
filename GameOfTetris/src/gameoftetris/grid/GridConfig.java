package grid;

import org.homedns.dade.jcgrid.client.GridNodeClientConfig;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.distr.grid.GridConfigurationBase;
import org.jgap.event.EventManager;
import org.jgap.impl.DefaultConfiguration;
import org.jgap.impl.DoubleGene;

/**
 * Main configuration for defining the problem and the way it is solved in the
 * grid. Thus, the most important class in a JGAP Grid!
 *
 * @author Klaus Meffert
 * @since 3.2
 */
public class GridConfig extends GridConfigurationBase {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void initialize(GridNodeClientConfig gridconfig) throws Exception {
        // Create the problem to be solved.
        // --------------------------------
        gridconfig.setSessionName("JGAP_fitness_distributed");
        Configuration conf = new DefaultConfiguration();
        conf.setEventManager(new EventManager());
        conf.setPopulationSize(100);
        conf.setKeepPopulationSizeConstant(true);
        conf.setFitnessFunction(new PlayerFitnessFunction());
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
        // Setup parameters.
        // -----------------
        setWorkerReturnStrategy(new PlayerWorkerReturnStrategy());
        // No initialization of Genotype on behalf of workers.
        // ---------------------------------------------------
        setGenotypeInitializer(null);
        // Evolution takes place on client only!
        // -------------------------------------
        setWorkerEvolveStrategy(null);
        // Setup the client to produce a work request for each chromosome
        // to get its fitness value computed by a single worker.
        // --------------------------------------------------------------
        setRequestSplitStrategy(new PlayerRequestSplitStrategy(conf));
        setConfiguration(conf);
        // Evolution takes place on client.
        // --------------------------------
        setClientEvolveStrategy(new PlayerClientEvolveStrategy());
        // Register client feedback listener.
        // ----------------------------------
        setClientFeedback(new MyClientFeedback());
    }

    @Override
    public void validate() throws Exception {
        if (getRequestSplitStrategy() == null) {
            throw new RuntimeException("Please set the request split strategy first!");
        }
        if (getConfiguration() == null) {
            throw new RuntimeException("Please set the configuration first!");
        }
    }

}