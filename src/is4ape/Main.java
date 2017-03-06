package is4ape;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import is4ape.bench.Benchmark1;
import is4ape.poc.PoC;

/**
 * This is the main class for testing our proof of concept.
 * It illustrates an example setup, using benchmark 1.
 * 
 * The range field determines whether the discrete or continuous setup is used.
 * (note that only the prior distributions differ)
 * 
 * The 'estimation' field determines whether independent sample or importance sample estimates are used.
 * (in the former case, no likelihood function is passed to the PoC and expect poor performance unless |C| << max_evals)
 * 
 * Information about the incumbent after every evaluation is written to a csv file in the csv directory.
 * 
 * @author Steven Adriaensen
 *
 */
public class Main {
	enum Range{ //parameter range setup
		DISCRETE, //discrete setup
		CONTINUOUS //continuous setup
	}
	
	enum Estimation{ //performance estimation mode
		SAMPLE_AVERAGE, //using independent sample averages
		IMPORTANCE_SAMPLING //using IS estimates
	}
	
	static final Range range = Range.DISCRETE; //change to CONTINUOUS to evaluate PoC in the continuous setup
	static final Estimation estimation = Estimation.IMPORTANCE_SAMPLING; //change to SAMPLE_AVERAGE to use plain sample estimates in PoC

	public static void main(String[] args) {
		long start_time = System.currentTimeMillis();
		
		//initialise random element (if a fixed seed <=> start_time is used our PoC behaves deterministic)
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
