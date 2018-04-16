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
	static public int Neff = 0;
	final BiFunction<DesignType,ExecutionType,Double> pr; //The function describing the relationship between design and execution space
	final Function<ExecutionType,Double> f; //The notion of 'desirability of an execution' used
	
	List<ExecutionType> execs; //E': list of executions generated
	List<Double> gs; //G(e) for all e in E' (to avoid re-computing these)
	Map<DesignType,Integer> pi_used; //C': the mixture of configurations used to generate E'
	double sum_f;
	double sum_f2;
		
	/**
	 * Creates an instance of the IS estimator.
	 * @param f: The notion of 'desirability of an execution' to be used
	 * @param pr The function to be used to compute the likelihood of generating an execution using a given design
	 */
	public ImportanceSamplingModel(Function<ExecutionType,Double> f,BiFunction<DesignType,ExecutionType,Double> pr){
		this.f = f;
		this.pr = pr;
		
		execs = new ArrayList<ExecutionType>();
		pi_used = new HashMap<DesignType,Integer>();
		gs = new ArrayList<Double>();
		sum_f = 0;
		sum_f2 = 0;
	}
	
	public void update(DesignType pi, ExecutionType exec){
		double f_exec = f.apply(exec);
		
		//update for standard deviation
		sum_f += f_exec;
		sum_f2 += f_exec*f_exec;
		
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
		Set<DesignType> keyset = pi_used.keySet();
		for(DesignType used_pi : keyset){
			gNew += pi_used.get(used_pi)*pr.apply(used_pi,exec);
		}
		gs.add(gNew);
	}
	
	private double STD(){
		return Math.sqrt(sum_f2/execs.size() - (sum_f*sum_f)/(execs.size()*execs.size()));
	}
	
	public double o(DesignType pi) {
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
		return norm == 0? mean : mean/norm;
	}
	
	@Override
	public double unc(DesignType pi){
		//if n = 0
		double n = n(pi);
		if(n == 0){
			return Double.POSITIVE_INFINITY;
		}else{
			return STD()/Math.sqrt(n);
		}
	}
	
	
	public double n(DesignType pi) {
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
			double neff = (norm*norm)/norm2;
			n = neff*Math.min(norm, 1.0/norm);
		}
		return n;
	}
	
	public double sim(DesignType pi1, DesignType pi2) {	
		double n1 = n(pi1);
		if(n1 == 0){
			return 0;
		}
		double n2 = n(pi2);
		if(n2 == 0){
			return 0;
		}
		double sc = 0;
		double norm1 = 0;
		double norm2 = 0;
		for(int i = 0; i < gs.size(); i++){
			ExecutionType exec = execs.get(i);
			double G = gs.get(i);
			double w1 = pr.apply(pi1,exec)/G;
			double w2 = pr.apply(pi2,exec)/G;
			norm1 += w1;
			norm2 += w2;
			sc += Math.min(w1,w2);
		}
		//normalise
		sc = sc/Math.max(norm1, norm2);
		return sc;
	}

}
