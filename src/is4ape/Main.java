package is4ape;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import is4ape.bench.Benchmark1;
import is4ape.poc.PoC;

public class Main {
	enum Range{
		DISCRETE,
		CONTINUOUS
	}
	
	enum Estimation{
		SAMPLE_AVERAGE,
		IMPORTANCE_SAMPLING
	}
	
	static final Range range = Range.DISCRETE; //change to Continuous to evaluate continuous setting
	static final Estimation estimation = Estimation.IMPORTANCE_SAMPLING; //change to SAMPLE_AVERAGE to use plain sample estimates in PoC (baseline)

	public static void main(String[] args) {
		//initialise random element
		long start_time = System.nanoTime();
		Random rng = new Random(start_time);
		
		//initial configuration
		List<Double> init = Benchmark1.generateInitial();
		
		//initialize priors
		Function<Random,List<Double>> globalPrior;
		BiFunction<List<Double>,Random,List<Double>> localPrior;
		if(range == Range.DISCRETE){
			//discrete
			globalPrior = Benchmark1::uniformGlobalPriorDiscrete;
			localPrior = Benchmark1::uniformLocalPriorDiscrete;
		}else{
			//continuous
			globalPrior = Benchmark1::uniformGlobalPriorContinuous;
			localPrior = Benchmark1::uniformLocalPriorContinuous;
		}
		
		BiFunction<List<Double>,Benchmark1.ExecutionInfo,Double> logPr = null;
		if(estimation == Estimation.IMPORTANCE_SAMPLING){
			logPr = Benchmark1::getLikelihood;
		}
		
		//create instance of our PoC
		PoC<Random,List<Double>,Benchmark1.ExecutionInfo> poc = 
				new PoC<Random,List<Double>,Benchmark1.ExecutionInfo>(
						rng,
						init,
						globalPrior,
						localPrior,
						Benchmark1::run,
						Benchmark1.ExecutionInfo::f,
						logPr,
						1.96, //Z for changing the incumbent
						10, //pool size
						100, //# proposals/runs
						10000); //max_evals
		
		//start optimization
		poc.maximize(rng,start_time);
	}
	
}
