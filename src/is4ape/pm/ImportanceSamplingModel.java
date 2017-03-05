package is4ape.pm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class implements all importance sampling estimators.
 * Its implementation is fully generic w.r.t. the type (i.e. representation) of designs and executions.
 * 
 * @author Steven Adriaensen
 *
 * @param <PolicyType> The type of the design
 * @param <ExecutionType> The type of the execution
 */
public class ImportanceSamplingModel<PolicyType,ExecutionType> implements PerformanceModel<PolicyType,ExecutionType>{	
	final BiFunction<PolicyType,ExecutionType,Double> pr; //The function describing the relationship between design and execution space
	final Function<ExecutionType,Double> f; //The notion of 'desirability of an execution' used
	
	List<ExecutionType> execs; //E': list of executions generated
	List<Double> gs; //G(e) for all e in E' (to avoid re-computing these)
	Map<PolicyType,Integer> pi_used; //C': the mixture of configurations used to generate E'
	double fmin; //desirability of the worst execution
	double fmax; //desirability of the best execution
		
	/**
	 * Creates an instance of the IS estimator.
	 * @param f: The notion of 'desirability of an execution' to be used
	 * @param pr The function to be used to compute the likelihood of generating an execution using a given design
	 */
	public ImportanceSamplingModel(Function<ExecutionType,Double> f,BiFunction<PolicyType,ExecutionType,Double> pr){
		this.f = f;
		this.pr = pr;
		
		execs = new ArrayList<ExecutionType>();
		pi_used = new HashMap<PolicyType,Integer>();
		gs = new ArrayList<Double>();
		fmin = Double.POSITIVE_INFINITY;
		fmax = Double.NEGATIVE_INFINITY;
	}
	
	public void update(PolicyType pi, ExecutionType exec){
		double f_exec = f.apply(exec);
		fmax = Math.max(f_exec, fmax);
		fmin = Math.min(f_exec, fmin);
		
		//update g-values:
		//for existing executions O(E')
		for(int i = 0; i < execs.size(); i++){
			gs.set(i,gs.get(i)+pr.apply(pi, execs.get(i)));
		}
		execs.add(exec);
		if(pi_used.containsKey(pi)){
			pi_used.put(pi,pi_used.get(pi) + 1);
		}else{
			pi_used.put(pi,1);
		}
		//for new execution O(Pi')
		double gNew = 0;
		Set<PolicyType> keyset = pi_used.keySet();
		for(PolicyType used_pi : keyset){
			gNew += pi_used.get(used_pi)*pr.apply(used_pi,exec);
		}
		gs.add(gNew);
	}
	
	public double mean(PolicyType pi) {
		double mean = 0;
		//compute IS estimate
		double norm = 0;
		//loop over all prior executions, adding weighted observations
		for(int i = 0; i < execs.size(); i++){
			ExecutionType exec = execs.get(i);
			double w = pr.apply(pi,exec)/gs.get(i);
			norm += w;
			mean += w*f.apply(exec);
		}
		//normalise
		return mean/norm;
	}
	
	/*
	 * Computes an estimate of the standard deviation of the performance of a given design
	 */
	protected double std(PolicyType pi){
		double var = 0;
		//estimate the variance of weight distribution;
		double mean = mean(pi);
		double norm = 0;
		//loop over all prior executions, adding weighted deviations
		for(int i = 0; i < execs.size(); i++){
			ExecutionType exec = execs.get(i);
			double w = pr.apply(pi,exec)/gs.get(i);
			norm += w;
			double delta = f.apply(exec)-mean;
			var += w*delta*delta;
		}
		if(norm == 0){
			return Double.POSITIVE_INFINITY;
		}else{
			//normalise
			var = var/norm;
			//add pseudo count (avoids underestimating std early on)
			double maxdiff = fmax-fmin;
			var = var + (maxdiff*maxdiff)/n(pi);
			return Math.sqrt(var);
		}
	}
	
	@Override
	public double uncertainty(PolicyType pi){
		//if n = 0
		double n = n(pi);
		if(n == 0){
			return Double.POSITIVE_INFINITY;
		}else{
			return std(pi)/Math.sqrt(n);
		}
	}
	
	/*
	 * Computes the 'effective' sample size of a given design
	 */
	protected double n(PolicyType pi) {
		double n = 0;
		double norm = 0; //sum of weights
		double norm2 = 0; //sum of squared weights
		//loop over all prior executions
		for(int i = 0; i < execs.size(); i++){
			double w = pr.apply(pi,execs.get(i))/gs.get(i);
			norm += w;
			norm2 += w*w;
		}

		if(norm == 0){
			//no relevant executions
			n = 0;
		}else{
			double effective_sample_size = (norm*norm)/norm2;
			n = effective_sample_size/(1+Math.abs(Math.log10(norm)));
		}
		return n;
	}
	
	public double similarity(PolicyType pi1, PolicyType pi2) {	
		double n1 = n(pi1);
		if(n1 == 0){
			return 0;
		}
		double n2 = n(pi2);
		if(n2 == 0){
			return 0;
		}
		//Bhattacharyya on weight distributions
		double bc = 0;
		double norm1 = 0;
		double norm2 = 0;
		for(int i = 0; i < gs.size(); i++){
			ExecutionType exec = execs.get(i);
			double w1 = pr.apply(pi1,exec)/gs.get(i);
			double w2 = pr.apply(pi2,exec)/gs.get(i);
			norm1 += w1;
			norm2 += w2;
			bc += Math.sqrt(w1*w2);
		}
		//normalise
		bc = bc/Math.sqrt(norm1*norm2);
		//account for error, norms should be the same as well
		bc = bc/Math.max(norm1/norm2,norm2/norm1);
		return bc;
	}
	
}
