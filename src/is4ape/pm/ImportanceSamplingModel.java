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
 * @param <DesignType> The type of the design
 * @param <ExecutionType> The type of the execution
 */
public class ImportanceSamplingModel<DesignType,ExecutionType> implements PerformanceModel<DesignType,ExecutionType>{
	final BiFunction<DesignType,ExecutionType,Double> pr; //The function describing the relationship between design and execution space
	final Function<ExecutionType,Double> p; //The notion of 'desirability of an execution' used
	
	List<ExecutionType> execs; //E': list of executions generated
	List<Double> qs; //Q'(e) for all e in E' (to avoid re-computing these)
	Map<DesignType,Integer> Theta_used; //\Theta': the mixture of configurations used to generate E'
	
	//used to compute variability p
	double sum_p;
	double sum_p2;
		
	/**
	 * Creates an instance of the IS estimator.
	 * @param f: The notion of 'desirability of an execution' to be used
	 * @param pr The function to be used to compute the likelihood of generating an execution using a given design
	 */
	public ImportanceSamplingModel(Function<ExecutionType,Double> p,BiFunction<DesignType,ExecutionType,Double> pr){
		this.p = p;
		this.pr = pr;
		
		execs = new ArrayList<ExecutionType>();
		Theta_used = new HashMap<DesignType,Integer>();
		qs = new ArrayList<Double>();
		sum_p = 0;
		sum_p2 = 0;
	}
	
	public void update(DesignType theta, ExecutionType exec){
		double p_exec = p.apply(exec);
		
		//update for standard deviation
		sum_p += p_exec;
		sum_p2 += p_exec*p_exec;
		
		//update g-values:
		//for existing executions O(E')
		for(int i = 0; i < execs.size(); i++){
			qs.set(i,qs.get(i)+pr.apply(theta, execs.get(i)));
		}
		execs.add(exec);
		if(Theta_used.containsKey(theta)){
			Theta_used.put(theta,Theta_used.get(theta) + 1);
		}else{
			Theta_used.put(theta,1);
		}
		//for new execution O(Pi')
		double qNew = 0;
		Set<DesignType> keyset = Theta_used.keySet();
		for(DesignType used_pi : keyset){
			qNew += Theta_used.get(used_pi)*pr.apply(used_pi,exec);
		}
		qs.add(qNew);
	}
	
	private double STD(){
		return Math.sqrt(sum_p2/execs.size() - (sum_p*sum_p)/(execs.size()*execs.size()));
	}
	
	public double o(DesignType theta) {
		double mean = 0;
		//compute IS estimate
		double norm = 0;
		//loop over all prior executions, adding weighted observations
		for(int i = 0; i < execs.size(); i++){
			ExecutionType exec = execs.get(i);
			double w = pr.apply(theta,exec)/qs.get(i);
			norm += w;
			mean += w*p.apply(exec);
		}
		//normalise
		return norm == 0? mean : mean/norm;
	}
	
	@Override
	public double unc(DesignType theta){
		//if n = 0
		double n = n(theta);
		if(n == 0){
			return Double.POSITIVE_INFINITY;
		}else{
			return STD()/Math.sqrt(n);
		}
	}
	
	
	public double n(DesignType theta) {
		double n = 0;
		double norm = 0; //sum of weights
		double norm2 = 0; //sum of squared weights
		
		//loop over all prior executions
		for(int i = 0; i < execs.size(); i++){
			double w = pr.apply(theta,execs.get(i))/qs.get(i);
			norm += w;
			norm2 += w*w;
		}

		if(norm == 0){
			//no relevant executions
			n = 0;
		}else{
			double neff = (norm*norm)/norm2;
			n = neff*Math.min(norm, 1.0/norm);
		}
		return n;
	}
	
	public double sim(DesignType theta1, DesignType theta2) {	
		double n1 = n(theta1);
		if(n1 == 0){
			return 0;
		}
		double n2 = n(theta2);
		if(n2 == 0){
			return 0;
		}
		double sc = 0;
		double norm1 = 0;
		double norm2 = 0;
		for(int i = 0; i < qs.size(); i++){
			ExecutionType exec = execs.get(i);
			double G = qs.get(i);
			double w1 = pr.apply(theta1,exec)/G;
			double w2 = pr.apply(theta2,exec)/G;
			norm1 += w1;
			norm2 += w2;
			sc += Math.min(w1,w2);
		}
		//normalise
		sc = sc/Math.max(norm1, norm2);
		return sc;
	}

}
