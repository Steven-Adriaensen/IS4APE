package is4ape.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class provides all information about benchmark 1.
 * 
 * @author Steven Adriaensen
 *
 */
public class Benchmark1{
	private static final int N = 20; //the number of iterations performed
	
	/*
	 * This class provides all information about an execution of benchmark 1
	 */
	public static class ExecutionInfo{
		final int num_it; //# iterations performed
		final double sum_r; //sum of reward received
		
		public ExecutionInfo(int num_it, double reward){
			this.num_it = num_it;
			this.sum_r = reward;
		}
		
		/*
		 * Method returning the desirability of a given execution.
		 * For benchmark 1 specifically: the sum of rewards received
		 */
		public static double f(ExecutionInfo exec){
			return exec.sum_r;
		}
		
		public String toString(){
			return "Benchmark1.Execution(it:"+num_it+")";
		}
	}
	
	/**
	 * Method executing benchmark 1 using a given configuration and random number generator.
	 * Remark that the random number generator here corresponds to the input (problem instance) to be solved.
	 * 
	 * @param rng: random number generator used a 'input' 
	 * @param c: configuration used (list of length N)
	 * @return An instance of ExecutionInfo, providing information about the execution.
	 */
	public static ExecutionInfo run(Random rng, List<Double> c){
		double sum_r = 0;
		int i = 0;
		while(i < N){
			if(rng.nextDouble() < c.get(i)){
				double reward_i = 1+2*rng.nextGaussian();
				sum_r += reward_i;
				i++;
			}else{
				break;
			}
		}
		return new ExecutionInfo(i,sum_r);
	}
	
	/**
	 * Method determining the likelihood that a given configuration generates a given execution.
	 * (computation ignores the likelihood of stochastic events during the execution)
	 * 
	 * @param c: configuration
	 * @param exec: execution
	 * @return likelihood in [0,1]
	 */
	public static double getLikelihood(List<Double> c, ExecutionInfo exec){
		double l = 1;
		for(int i = 0; i < exec.num_it; i++){
			l *= c.get(i);
		}
		if(exec.num_it == c.size()){
			//#it = 20
			return l;
		}else{
			//#it < 20
			return l*(1-c.get(exec.num_it));
		}
	}
	
	/**
	 * @return The initial configuration used during our experiments, i.e. c_i = 0.5 \forall i
	 */
	public static List<Double> generateInitial(){
		List<Double> design = new ArrayList<Double>(N);
		for(int i = 0; i < N; i++){
			design.add(0.5);
		}
		return design;
	}
	
	/**
	 * The global prior used in a continuous setting.
	 * Selects a configuration uniformly at random from the whole (undiscretized) 
	 * configuration space.
	 * 
	 * @param rng: random number generator to be used.
	 * @return a random configuration.
	 */
	public static List<Double> uniformGlobalPriorContinuous(Random rng) {
		List<Double> design = new ArrayList<Double>(N);
		for(int i = 0; i < N; i++){
			design.add(rng.nextDouble());
		}
		return design;
	}

	/**
	 * The local prior used in a continuous setting.
	 * Modifies a given configuration, by randomly changing a single of its parameters.
	 * (i.e. one-exchange)
	 * 
	 * @param current: the configuration on which this prior is conditioned.
	 * @param rng: random number generator to be used.
	 * @return a configuration differing from current in 1 parameter value.
	 */
	public static List<Double> uniformLocalPriorContinuous(List<Double> current, Random rng){
		List<Double> nh = new ArrayList<Double>(current);
		int mutation_index = rng.nextInt(N);
		nh.set(mutation_index, rng.nextDouble());
		return nh;
	}
	
	/**
	 * The global prior used in a discretized setting.
	 * Selects a configuration uniformly at random from discretized
	 * configuration space.
	 * 
	 * @param rng: Random number generator to be used.
	 * @return a random configuration.
	 */
	public static List<Double> uniformGlobalPriorDiscrete(Random rng) {
		List<Double> design = new ArrayList<Double>(N);
		for(int i = 0; i < N; i++){
			design.add(0.1*rng.nextInt(11));
		}
		return design;
	}

	/**
	 * The local prior used in a discretized setting.
	 * Modifies a given configuration, by randomly changing a single of its parameters.
	 * (i.e. one-exchange)
	 * 
	 * @param current: the configuration on which this prior is conditioned.
	 * @param rng: random number generator to be used.
	 * @return a configuration differing from current in at most 1 parameter value.
	 */
	public static List<Double> uniformLocalPriorDiscrete(List<Double> current, Random rng){
		List<Double> nh = new ArrayList<Double>(current);
		int mutation_index = rng.nextInt(N);
		nh.set(mutation_index, 0.1*rng.nextInt(11));
		return nh;
	}

}
