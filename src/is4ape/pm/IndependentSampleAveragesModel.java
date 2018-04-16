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
	final Function<ExecutionType,Double> p; //The notion of 'desirability of an execution' used
	
	HashMap<DesignType,List<Double>> results; //stores performance observations f(e) for all e \in E'
	int n_exec;
	double sum_p;
	double sum_p2;
	
	/**
	 * Creates an instance of the IS estimator.
	 * @param f: The notion of 'desirability of an execution' to be used
	 * @param pr: The function to be used to compute the likelihood of generating an execution using a given design
	 */
	public IndependentSampleAveragesModel(Function<ExecutionType,Double> p){
		this.p = p;
		results = new HashMap<DesignType,List<Double>>();
	}
	
	private double STD(){
		return Math.sqrt(sum_p2/n_exec - (sum_p*sum_p)/(n_exec*n_exec));
	}

	@Override
	public void update(DesignType theta, ExecutionType exec) {
		List<Double> results_theta;
		//if first result for policy
		if(results.containsKey(theta)){
			results_theta = results.get(theta);
		}else{
			results_theta = new LinkedList<Double>();
			results.put(theta, results_theta);
		}
		double f_exec = p.apply(exec);
		results_theta.add(f_exec);
		//compute standard deviation
		n_exec++;
		sum_p += f_exec;
		sum_p2 += f_exec*f_exec;
	}
	
	@Override
	public double o(DesignType theta) {
		double avg = 0;
		if(results.containsKey(theta)){
			List<Double> res = results.get(theta);
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
	public double n(DesignType theta){
		if(results.containsKey(theta)){
			return results.get(theta).size();
		}else{
			return 0;
		}
	}
	
	public double unc(DesignType theta){
		if(results.containsKey(theta)){
			return STD()/Math.sqrt(n(theta));
		}else{
			return Double.POSITIVE_INFINITY;
		}
	}

	@Override
	public double sim(DesignType theta1, DesignType theta2) {
		return theta1.equals(theta2)? 1.0 : 0.0;
	}

}
