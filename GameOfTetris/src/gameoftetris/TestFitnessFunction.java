import java.util.Arrays;
import java.util.Random;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;

public class TestFitnessFunction extends FitnessFunction {

    public TestFitnessFunction() {

    }

    @Override
    // Evaluation of subject's fitness
    protected double evaluate(IChromosome subject) {
    	Random rand = new Random();
    	int n = rand.nextInt(50) + 1;
    	System.out.println("LOL" + n);
    	return n;
    }
}
