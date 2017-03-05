package is4ape.poc;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.math3.distribution.NormalDistribution;

import is4ape.pm.IndependentSampleAveragesModel;
import is4ape.pm.ImportanceSamplingModelMemoized;
import is4ape.pm.PerformanceModel;

/**
 * This class provides a fully generic implementation of our proof of concept.
 * 
 * @author Steven Adriaensen
 *
 * @param <InputType> The type of the input (e.g. problem instances to be solved, budget available for doing so)
 * @param <PolicyType> The type of the design
 * @param <ExecutionType> The type of the execution
 */
public class PoC<InputType,PolicyType,ExecutionType> {
	final InputType input; //single input over which performance is to be maximised
	final PolicyType init_pi; //initial candidate design
	final Function<Random,PolicyType> globalPrior; //global prior used
	final BiFunction<PolicyType,Random,PolicyType> localPrior; //local prior used
	final BiFunction<InputType,PolicyType,ExecutionType> executor; //method for generating samples for (i.e. executing) a given design on a given input
	
	//the functions below define performance (i.e. the objective function to be maximised)
	final Function<ExecutionType,Double> f; //The function describing the relationship between execution and performance space
	final BiFunction<PolicyType,ExecutionType,Double> pr; //The function describing the relationship between design and execution space
	
	final int max_evals; //maximum # evaluations to perform
	final double Z; //z-score weighting uncertainty in our choice of incumbent
	final int psize; //the (maximum) size of the pool
	final double nprop; //the number of proposals to generate
	
	PolicyType incumbent; //the current best design
	PerformanceModel<PolicyType,ExecutionType> pModel; //the current performance estimates
	List<PolicyType> pool; //the pool of candidate designs
	
	/**
	 * Creates an instance of our PoC
	 * 
	 * @param input: single input over which performance is to be maximised
	 * @param init_pi: initial candidate design as starting point for the search 
	 * (if 'null' one will be sampled from global prior)
	 * @param globalPrior: global prior used (i.e. global search operator)
	 * @param localPrior: local prior used (i.e. local search operator)
	 * @param executor: method for generating samples for (i.e. executing) a given design on a given input
	 * @param f: the notion of the desirability of an execution to be used
	 * @param pr: a function computing the likelihood of generating a given execution, using a given design
	 * (if given: IS estimates will be used, otherwise: independent sample averages will be used)
	 * @param Z: z-score weighting uncertainty in our choice of incumbent
	 * @param psize: the (maximum) size of the pool
	 * @param nprop: the number of proposals to generate
	 * @param max_evals: # evaluations after which to return the incumbent
	 */
	public PoC(
			InputType input,
			PolicyType init_pi,
			Function<Random,PolicyType> globalPrior,
			BiFunction<PolicyType,Random,PolicyType> localPrior,
			BiFunction<InputType,PolicyType,ExecutionType> executor,
			Function<ExecutionType,Double> f,
			BiFunction<PolicyType,ExecutionType,Double> pr,
			double Z,
			int psize,
			double nprop,
			int max_evals){
		this.input = input;
		this.init_pi = init_pi;
		this.executor = executor;
		this.f = f;
		this.pr = pr;
		this.max_evals = max_evals;
		this.globalPrior = globalPrior;
		this.localPrior = localPrior;
		this.Z = Z;
		this.psize = psize;
		this.nprop = nprop;
	}
	
	/**
	 * Attempts to find the design maximising performance.
	 * 
	 * @param rng: The source of random numbers
	 * @return the incumbent after max_evals
	 */
	public PolicyType maximize(Random rng, long ID){
		init(rng);
		System.out.println("Initial incumbent: " + incumbent);
		int eval = 0;
		File csv = new File("./csv/PoC_Run"+ID+".csv"); //for logging
		log(csv,"Run, Perf. estimate, Incumbent, Time"); //for logging
		while(eval < max_evals){
			//generate proposals
			System.out.println("generating proposals...");
			List<PolicyType> proposals = new ArrayList<PolicyType>((int)nprop);
			for(int i = 0; i < nprop; i++){
				PolicyType pi = propose_policy(rng);
				proposals.add(pi);
			}
			
			//update incumbent (remark that a proposal might be our new incumbent even though we never executed it)
			update_incumbent(proposals);
			
			//determine expected improvement for all
			List<RecordEI> candidates = new ArrayList<RecordEI>();
			//proposals
			for(PolicyType pi : proposals){
				candidates.add(new RecordEI(pi));
			}
			//pool
			for(PolicyType pi : pool){
				candidates.add(new RecordEI(pi));
			}
			
			Collections.sort(candidates);
			
			//determine new pool
			pool.clear();
			for(int i = 0; i < Math.min(candidates.size(), psize); i++){
				pool.add(candidates.get(i).pi);
			}
			
			//run the one with the greatest EI (may be the incumbent itself)
			PolicyType pi;
			if(candidates.get(0).EI > EI(incumbent)){
				pi = candidates.get(0).pi;
			}else{
				pi = incumbent;
			}
			
			System.out.println("EVALUATION "+(eval+1));
			
			System.out.println("run "+ pi +" on "+ input);
			ExecutionType exec = executor.apply(input, pi);
			double f_exec = f.apply(exec);
			System.out.println("exec: "+ exec);
			System.out.println("cost was "+ f_exec);
			System.out.println();
			
			//update performance model
			pModel.update(pi, exec);
			
			//determine whether the incumbent has changed
			update_incumbent(pool);
			
			print_pool();
			
			eval++;
			
			//output the current incumbent (last at previous evaluation level)
			log(csv,eval+", "+ pModel.mean(incumbent)+ ", " +incumbent+", "+(System.nanoTime()-ID));
		}
		System.out.println(incumbent);
		System.out.println("took: "+(System.nanoTime()-ID));
		return incumbent;
	}
	
	private void init(Random rng){
		//initialize the performance model		
		if(pr == null){
			//independent sample averages
			pModel = new IndependentSampleAveragesModel<PolicyType,ExecutionType>(f);
		}else{
			//importance sample estimates
			pModel = new ImportanceSamplingModelMemoized<PolicyType,ExecutionType>(f,pr);
		}
		
		//initialize the pool
		pool = new ArrayList<PolicyType>(psize);
		
		//initialize incumbent
		if(init_pi != null){
			//use the one passed, if any
			incumbent = init_pi;
		}else{
			//sample the initial incumbent from the global prior
			incumbent = globalPrior.apply(rng);
		}
	}
	
	/*
	 * Proposes a design for evaluation, drawn from an equal mixture of 
	 * - the global prior
	 * - the local prior (conditioned on the actual incumbent)
	 */
	private PolicyType propose_policy(Random rng){
		PolicyType proposal;
		if(rng.nextBoolean()){
			//use global (50% likelihood)
			proposal = globalPrior.apply(rng);
		}else{
			//use local conditioned on incumbent (50% likelihood)
			proposal = localPrior.apply(incumbent,rng);
		}
		return proposal;
	}
	
	/*
	 * updates the incumbent (i.e. lines 8 and 13)
	 *  
	 * @param pis: A list of potential new incumbents
	 */
	private void update_incumbent(List<PolicyType> pis){
		double lbz_inc = pModel.mean(incumbent)-Z*pModel.uncertainty(incumbent);
		double lbz_max = lbz_inc;
		int best_index = -1;
		for(int i = 0; i < pis.size(); i++){
			PolicyType pi = pis.get(i);
			double lbp_pi = pModel.mean(pi)-Z*pModel.uncertainty(pi);
			if(lbp_pi > lbz_max){
				best_index = i;
				lbz_max = lbp_pi;
			}
		}
		
		if(lbz_max > lbz_inc){
			incumbent = pis.get(best_index);
			System.out.println("NEW INCUMBENT: "+incumbent);
		}
	}
	
	/*
	 * writes (appends) a line to a given file
	 */
	private void log(File f, String line){
		PrintWriter csv_writer;
		try {
			csv_writer = new PrintWriter(new FileOutputStream(f,true));
			csv_writer.println(line);
			csv_writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * prints out some information about the candidates in C_{pool} (and c_inc)
	 */
	private void print_pool(){
		for(int i = 0; i < pool.size(); i++){
			PolicyType pi = pool.get(i);
			double pi_mean = pModel.mean(pi);
			double pi_punc = Z*pModel.uncertainty(pi);
			System.out.println(i+") "+pi+" "+pi_mean+" ["+(pi_mean-pi_punc)+","+(pi_mean+pi_punc)+"] EI: "+EI(pi));
		}
		double inc_mean = pModel.mean(incumbent);
		double inc_punc = Z*pModel.uncertainty(incumbent);
		System.out.println("incumbent: "+incumbent +" "+inc_mean+" ["+(inc_mean-inc_punc)+","+(inc_mean+inc_punc)+"] EI: "+EI(incumbent));
		System.out.println();
	}
	
	//used for computing Expected Improvement
	final static private NormalDistribution gaussian = new NormalDistribution();
	
	/*
	 * computes the expected improvement criterion for a given design
	 */
	private double EI(PolicyType pi){
		double unc = pModel.uncertainty(pi);
		if(unc == Double.POSITIVE_INFINITY){
			return Double.POSITIVE_INFINITY;
		}else{
			double est = pModel.mean(pi);
			double est_inc = pModel.mean(incumbent);
			double better = est-est_inc;
			if(unc == 0){
				return Math.max(0, better);
			}else{
				double EI = better*gaussian.cumulativeProbability(better/unc)+unc*gaussian.density(better/unc);
				if(Double.isNaN(EI)){
					System.out.println();
				}
				return EI;
			}
		}
		
	}
	
	/*
	 * 'Sortable' record storing a candidate alongside its expected improvement
	 */
	class RecordEI implements Comparable<RecordEI>{
		final PolicyType pi;
		double EI;
		
		RecordEI(PolicyType pi){
			this.pi = pi;
			EI = EI(pi);
		}

		@Override
		public int compareTo(RecordEI other) {
			if(this.EI < other.EI){
				return 1;
			}else if(this.EI > other.EI){
				return -1;
			}else{
				return 0;
			}
		}
		
		
	}
}
