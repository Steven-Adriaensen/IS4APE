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
 * @param <DesignType> The type of the design (it should properly (re-)define equals/hashcode methods!)
 * @param <ExecutionType> The type of the execution
 */
public class IndependentSampleAveragesModel<DesignType,ExecutionType> implements PerformanceModel<DesignType,ExecutionType>{
	final Function<ExecutionType,Double> f; //The notion of 'desirability of an execution' used
	
	HashMap<DesignType,List<Double>> results; //stores performance observations f(e) for all e \in E'
	int n_exec;
	double sum_f;
	double sum_f2;
	
	/**
	 * Creates an instance of the IS estimator.
	 * @param f: The notion of 'desirability of an execution' to be used
	 * @param pr: The function to be used to compute the likelihood of generating an execution using a given design
	 */
	public IndependentSampleAveragesModel(Function<ExecutionType,Double> f){
		this.f = f;
		results = new HashMap<DesignType,List<Double>>();
	}
	
	private double STD(){
		return Math.sqrt(sum_f2/n_exec - (sum_f*sum_f)/(n_exec*n_exec));
	}

	@Override
	public void update(DesignType pi, ExecutionType exec) {
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
		//compute standard deviation
		n_exec++;
		sum_f += f_exec;
		sum_f2 += f_exec*f_exec;
	}
	
	@Override
	public double o(DesignType pi) {
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
	 * Returns the sample size of the estimate of a given design
	 * i.e. the number of observations (executions) on which it is based.
	 */
	public double n(DesignType pi){
		if(results.containsKey(pi)){
			return results.get(pi).size();
		}else{
			return 0;
		}
	}
	
	public double unc(DesignType pi){
		if(results.containsKey(pi)){
			return STD()/Math.sqrt(n(pi));
		}else{
			return Double.POSITIVE_INFINITY;
		}
	}

	@Override
	public double sim(DesignType pi1, DesignType pi2) {
		return pi1.equals(pi2)? 1.0 : 0.0;
	}

}
