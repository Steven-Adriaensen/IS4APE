package is4ape;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import is4ape.bench.loop.Looping;
import is4ape.bench.scheduler_hh.SchedulerHH;
import is4ape.bench.scheduler_hh.SchedulerHH.Configuration;
import is4ape.bench.sort.InputSort;
import is4ape.pm.memoize.MemoizedBiFunction;
import is4ape.poc.PoC;

/**
 * This is the main class for testing our proof of concept (PoC).
 * It illustrates an example setup, for four different scenarios
 * 
 * @author Steven Adriaensen
 *
 */
public class Main {
	//Scenarios on which our PoC can be tested (using this main file)
	enum Scenario{
		LOOP_DISCRETE, 		//looping problem with discretized configuration space
		LOOP_CONTINUOUS, 	//looping problem with continuous configuration space
		INPUTSORT, 			//static sorting ASP
		SCHEDULER 			//metaheuristic scheduler
	}
	
	//performance estimation modes supported by our PoC
	enum Estimation{ 
		SAMPLE_AVERAGE, //using independent sample averages (PoC-SA)
		IMPORTANCE_SAMPLING //using importance sampling estimates (PoC-IS)
	}
	
	/**
	 * A convenience method for running PoC (w/wo IS) using default parameter setting 
	 * on one of the four different scenarios.
	 * 
	 * @param args: This takes 4-5 command-line arguments, in order:
	 *     args[0]: scenario on which to run PoC 
	 *              (0: LOOP_DISCRETE, 1: LOOP_CONTINUOUS, 2: INPUTSORT, 3: SCHEDULER) 
	 *     args[1]: performance estimation mode used 
	 *              (0: SAMPLE_AVERAGE, 1: IMPORTANCE SAMPLING) 
	 *     args[2]: number of candidate evaluations after which to terminate (~ tuning budget, N)
	 *     args[3]: path to file to which information about the incumbent design is written at any time.
	 *     args[4]: OPTIONAL: seed for the random generator
	 */
	public static void main(String[] args) {
		//parse command line arguments
		Scenario scenario = Scenario.values()[Integer.parseInt(args[0])];
		Estimation mode = Estimation.values()[Integer.parseInt(args[1])];
		int N = Integer.parseInt(args[2]);
		File output = new File(args[3]);
		Random rng = args.length > 4? new Random(Long.parseLong(args[4])) : new Random();
		
		//default parameter settings
		int K = 3;
		int L = 10;

		//actual call
		try {
			run(scenario,mode,K,L,N,output,rng);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Runs our PoC.
	 * 
	 * @param scenario: the scenario to which to apply it (~ wb-ACP)
	 * @param mode: the performance estimation mode used (PoC-IS vs PoC-SA).
	 * @param K: a parameter affecting how similarity affects the estimate-quality/reliability tradeoff. (default 3)
	 * @param L: a parameter determining how many candidate designs are explored per evaluation
	 * @param N: number of candidate evaluations after which to terminate (~ tuning budget)
	 * @param output_file: path to file to which information about the incumbent design is written at any time.
	 * @param rng: random generator to use to make random decisions.
	 * @throws Exception
	 */
	public static void run(Scenario scenario, Estimation mode , double K, int L, int N, File output_file, Random rng) throws Exception{
		@SuppressWarnings("rawtypes")
		PoC poc = null;
		if(scenario.equals(Scenario.LOOP_DISCRETE) || scenario.equals(Scenario.LOOP_CONTINUOUS)){
			//<LOOPING PROBLEM>
			//SA or IS
			BiFunction<List<Double>,Looping.ExecutionInfo,Double> pr = null;
			if(mode.equals(Estimation.IMPORTANCE_SAMPLING)){
				pr = Looping::getLikelihood;
			}
			//DISCRETE or CONTINUOUS
			Function<Random,List<Double>> globalPrior;
			BiFunction<List<Double>,Random,List<Double>> localPrior;
			if(scenario.equals(Scenario.LOOP_DISCRETE)){
				globalPrior = Looping::uniformGlobalPriorDiscrete;
				localPrior = Looping::uniformLocalPriorDiscrete;
			}else{
				globalPrior = Looping::uniformGlobalPriorContinuous;
				localPrior = Looping::uniformLocalPriorContinuous;
			}
			//create instance of our PoC
			poc = new PoC<Random,List<Double>,Looping.ExecutionInfo>(
							Looping::run,
							globalPrior,
							localPrior,
							(Random rng2) -> rng2,
							pr,
							Looping.ExecutionInfo::p,
							K,
							L,
							N,
							Looping.generateInitial());
		}else if(scenario.equals(Scenario.INPUTSORT)){
			File data_dir = new File("sort_data");
			if(!data_dir.exists() || data_dir.listFiles().length == 0) {
				throw new Exception("sort_data not found: Please extract the contents of sort_data.zip to the sort_data directory.");
			}
			//SA or IS
			BiFunction<List<Double>,InputSort.ExecutionInfo,Double> pr = null;
			if(mode.equals(Estimation.IMPORTANCE_SAMPLING)){
				pr = InputSort::pr;
			}
			//create instance of our PoC
			poc = new PoC<Integer,List<Double>,InputSort.ExecutionInfo>(
							(Integer seq_id, List<Double> c) -> {return InputSort.run(seq_id,c,rng);},
							InputSort::uniformGlobalPrior,
							InputSort::gaussianLocalPrior,
							InputSort::sample_D_training,
							pr,
							InputSort::p,
							K,
							L,
							N,
							null);
		}else if(scenario.equals(Scenario.SCHEDULER)){
			File data_dir = new File("hh_data");
			if(!data_dir.exists() || data_dir.listFiles().length == 0) {
				throw new Exception("hh data not found: Please extract the contents of hh_data.zip to the hh_data directory.");
			}
			//SA or IS
			BiFunction<Configuration,SchedulerHH.ExecutionInfo,Double> pr = null;
			if(mode.equals(Estimation.IMPORTANCE_SAMPLING)){
				pr = MemoizedBiFunction.from(SchedulerHH::getLikelihood);
			}
			//create instance of our PoC
			poc = new PoC<SchedulerHH.Input,Configuration,SchedulerHH.ExecutionInfo>(
							SchedulerHH::run,
							Configuration::globalPrior,
							Configuration::localPrior,
							SchedulerHH::sample_D_training,
							pr,
							SchedulerHH.ExecutionInfo::p,
							K,
							L,
							N,
							null); //max_evals
		}else {
			throw new Exception("Unknown scenario: "+scenario);
		}
		poc.minimize(rng,output_file);
	}
}
