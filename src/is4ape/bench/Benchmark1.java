package is4ape.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Benchmark1{
	private static final int N = 20; //the number of iterations performed
	
	public static class ExecutionInfo{
		final int num_it;
		final double sum_r;
		
		public ExecutionInfo(int num_it, double reward){
			this.num_it = num_it;
			this.sum_r = reward;
		}
		
		public static double f(ExecutionInfo exec){
			return exec.sum_r;
		}
		
		public String toString(){
			return "Benchmark1.Execution(it:"+num_it+")";
		}
	}
	
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
	
	public static double getLikelihood(List<Double> pi, ExecutionInfo exec){
		double l = 1;
		for(int i = 0; i < exec.num_it; i++){
			l *= pi.get(i);
		}
		if(exec.num_it == pi.size()){
			return l;
		}else{
			return l*(1-pi.get(exec.num_it));
		}
	}
	
	public static List<Double> generateInitial(){
		List<Double> design = new ArrayList<Double>(N);
		for(int i = 0; i < N; i++){
			design.add(0.5);
		}
		return design;
	}
	
	public static List<Double> uniformGlobalPriorContinuous(Random rng) {
		List<Double> design = new ArrayList<Double>(N);
		for(int i = 0; i < N; i++){
			design.add(rng.nextDouble());
		}
		return design;
	}

	public static List<Double> uniformLocalPriorContinuous(List<Double> current, Random rng){
		List<Double> nh = new ArrayList<Double>(current);
		int mutation_index = rng.nextInt(N);
		nh.set(mutation_index, rng.nextDouble());
		return nh;
	}
	
	public static List<Double> uniformGlobalPriorDiscrete(Random rng) {
		List<Double> design = new ArrayList<Double>(N);
		for(int i = 0; i < N; i++){
			design.add(0.1*rng.nextInt(11));
		}
		return design;
	}

	public static List<Double> uniformLocalPriorDiscrete(List<Double> current, Random rng){
		List<Double> nh = new ArrayList<Double>(current);
		int mutation_index = rng.nextInt(N);
		nh.set(mutation_index, 0.1*rng.nextInt(11));
		return nh;
	}

}
