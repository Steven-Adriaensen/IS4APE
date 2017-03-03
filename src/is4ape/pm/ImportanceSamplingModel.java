package is4ape.pm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ImportanceSamplingModel<PolicyType,ExecutionType> implements PerformanceModel<PolicyType,ExecutionType>{	
	final BiFunction<PolicyType,ExecutionType,Double> pr;
	final Function<ExecutionType,Double> f;
	
	List<ExecutionType> execs;
	List<Double> gs;
	Map<PolicyType,Integer> pi_used;
	double fmin;
	double fmax;
		
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
			//add pseudo count:
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
	
	protected double n(PolicyType pi) {
		//compute effective sample size
		double n = 0;
		//compute IS estimate
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
		//account for error, norms should be the same as wel
		bc = bc/Math.max(norm1/norm2,norm2/norm1);
		return bc;
	}
	
}
