package is4ape.pm;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class IndependentSampleAveragesModel<PolicyType,ExecutionType> implements PerformanceModel<PolicyType,ExecutionType>{
	final Function<ExecutionType,Double> f;
	
	HashMap<PolicyType,List<Double>> results;
	double fmin = Double.POSITIVE_INFINITY;
	double fmax = Double.NEGATIVE_INFINITY;
	
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
	
	public double std(PolicyType pi){ //with pseudo counts
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
	
	public double n(PolicyType pi){
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
