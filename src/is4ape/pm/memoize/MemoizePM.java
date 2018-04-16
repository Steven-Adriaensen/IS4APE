package is4ape.pm.memoize;

import java.util.HashMap;
import java.util.Map;

import is4ape.pm.PerformanceModel;

/**
 * Memoization decorator for performance models, memoizing calls to 
 * - o
 * - n
 * - unc
 * - sim
 * 
 * @author Steven Adriaensen
 *
 * @param <DesignType> The type of the design
 * @param <ExecutionType> The type of the execution
 */
public class MemoizePM<DesignType,ExecutionType> implements PerformanceModel<DesignType,ExecutionType>{	
	Map<DesignType,Double> o_cache;
	Map<DesignType,Double> n_cache;
	Map<DesignType,Double> unc_cache;
	Map<DesignType,Map<DesignType,Double>> sim_cache;
	PerformanceModel<DesignType,ExecutionType> M;
	
	public MemoizePM(PerformanceModel<DesignType,ExecutionType> M) {
		this.M = M;
		o_cache = new HashMap<DesignType,Double>();
		n_cache = new HashMap<DesignType,Double>();
		unc_cache = new HashMap<DesignType,Double>();
		sim_cache = new HashMap<DesignType,Map<DesignType,Double>>();
	}

	@Override
	public void update(DesignType theta, ExecutionType exec){
		o_cache.clear();
		n_cache.clear();
		unc_cache.clear();
		sim_cache.clear();
		M.update(theta, exec);
	}
	
	@Override
	public double o(DesignType theta){
		if(!o_cache.containsKey(theta)){
			o_cache.put(theta, M.o(theta));
		}
		return o_cache.get(theta);
	}
	
	@Override
	public double unc(DesignType theta){
		if(!unc_cache.containsKey(theta)){
			unc_cache.put(theta, M.unc(theta));
		}
		return unc_cache.get(theta);
	}
	
	@Override
	public double sim(DesignType pi1, DesignType pi2){
		boolean forPi1;
		if(sim_cache.containsKey(pi1) && sim_cache.get(pi1).containsKey(pi2)){
			forPi1 = true;
		}else if(sim_cache.containsKey(pi2) && sim_cache.get(pi2).containsKey(pi1)){
			forPi1 = false;
		}else{
			if(!sim_cache.containsKey(pi1)){
				Map<DesignType,Double> sims = new HashMap<DesignType,Double>();
				sim_cache.put(pi1, sims);
			}
			sim_cache.get(pi1).put(pi2, M.sim(pi1, pi2));
			forPi1 = true;
		}
		if(forPi1){
			return sim_cache.get(pi1).get(pi2);
		}else{
			return sim_cache.get(pi2).get(pi1);
		}
	}

	@Override
	public double n(DesignType theta) {
		if(!n_cache.containsKey(theta)){
			n_cache.put(theta, M.n(theta));
		}
		return n_cache.get(theta);
	}
	
}
