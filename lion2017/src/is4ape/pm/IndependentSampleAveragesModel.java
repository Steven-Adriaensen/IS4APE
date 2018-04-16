package is4ape.pm;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * An implementation of the sample-based estimators.
 * 
 * @author Steven Adriaensen
 *
 * @param <PolicyType> The type of the design (it should properly (re-)define equals/hashcode methods!)
 * @param <ExecutionType> The type of the execution
 */
public class IndependentSampleAveragesModel<PolicyType,ExecutionType> implements PerformanceModel<PolicyType,ExecutionType>{
	final Function<ExecutionType,Double> f; //The notion of 'desirability of an execution' used
	
	HashMap<PolicyType,List<Double>> results; //stores performance observations f(e) for all e \in E'
	double fmin = Double.POSITIVE_INFINITY; //desirability of the worst execution
	double fmax = Double.NEGATIVE_INFINITY; //desirability of the best execution
	
	/**
	 * Creates an instance of the IS estimator.
	 * @param f: The notion of 'desirability of an execution' to be used
	 * @param pr: The function to be used to compute the likelihood of generating an execution using a given design
	 */
	public IndependentSampleAveragesModel(Function<ExecutionType,Double> f){
		this.f = f;
		results = new HashMap<PolicyType,List<Double>>();
	}

	@Override
	public void update(PolicyType pi, ExecutionType exec) {
		List<Double> results_pi;
		//if first result for policy
		if(results.containsKey(pi)){
			results_pi = results.get(pi);
		}else{
			results_pi = new LinkedList<Double>();
			results.put(pi, results_pi);
		}
		double f_exec = f.apply(exec);
		results_pi.add(f_exec);
		fmax = Math.max(f_exec, fmax);
		fmin = Math.min(f_exec, fmin);
	}
	
	@Override
	public double mean(PolicyType pi) {
		double avg = 0;
		if(results.containsKey(pi)){
			List<Double> res = results.get(pi);
			int i = 1;
			for(Double f : res){
				avg += (f-avg)/i;
				i++;
			}
		}
		return avg;
	}
	
	/*
	 * Computes an estimate of the standard deviation of the performance of a given design
	 */
	protected double std(PolicyType pi){
		double avg = mean(pi);
		//add pseudo-count
		double maxdiff = fmax-fmin;
		double var = maxdiff*maxdiff;
		//compute variation
		if(results.containsKey(pi)){
			List<Double> res = results.get(pi);
			int i = 2;
			for(Double f : res){
				double diff = f-avg;
				var += (diff*diff-var)/i;
				i++;
			}
		}
		return Math.sqrt(var);
	}
	
	/*
	 * Returns the sample size of the estimate of a given design
	 * i.e. the number of observations (executions) on which it is based.
	 */
	protected double n(PolicyType pi){
		if(results.containsKey(pi)){
			return results.get(pi).size();
		}else{
			return 0;
		}
	}
	
	public double uncertainty(PolicyType pi){
		if(results.containsKey(pi)){
			return std(pi)/Math.sqrt(n(pi));
		}else{
			return Double.POSITIVE_INFINITY;
		}
	}

	@Override
	public double similarity(PolicyType pi1, PolicyType pi2) {
		return pi1.equals(pi2)? 1.0 : 0.0;
	}

}
