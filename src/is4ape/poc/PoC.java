package is4ape.poc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

import is4ape.pm.ImportanceSamplingModel;
import is4ape.pm.IndependentSampleAveragesModel;
import is4ape.pm.PerformanceModel;
import is4ape.pm.memoize.MemoizePM;

/**
 * This class provides a fully generic implementation of our proof of concept.
 * 
 * Information about the incumbent at every iteration is written to the output file in csv format
 * 
 * @author Steven Adriaensen
 *
 * @param <InputType> The type of the input (e.g. problem instances to be solved, budget available for doing so)
 * @param <DesignType> The type of the design
 * @param <ExecutionType> The type of the execution
 */
public class PoC<InputType,DesignType,ExecutionType> {
	//wb-ACP instance being solved <a,Theta,D,pr,p>
	final BiFunction<InputType,DesignType,ExecutionType> a; 	//target algorithm
	final Function<Random,DesignType> globalPrior; 				//Theta (indirectly)
	final BiFunction<DesignType,Random,DesignType> localPrior; 	//Theta (indirectly)
	final Function<Random,InputType> D; 						//input distribution
	final BiFunction<DesignType,ExecutionType,Double> pr; 		//pr'
	final Function<ExecutionType,Double> p; 					//p
	//parameters
	final double K;
	final int L;
	//budget
	final int N;
	//initial configuration (if any)
	final DesignType theta_init;
	
	//variables for logging purposes
	long start_time;
	File trajFile;
	
	PerformanceModel<DesignType,ExecutionType> M; //the current performance 'model'
	DesignType theta_inc; //the current best design
	//counters
	int num_eval;
	int num_it; 
	int num_prop;
	
	
	/**
	 * Creates an instance of our PoC
	 */
	public PoC(
			BiFunction<InputType,DesignType,ExecutionType> a,
			Function<Random,DesignType> globalPrior,
			BiFunction<DesignType,Random,DesignType> localPrior,
			Function<Random,InputType> D,
			BiFunction<DesignType,ExecutionType,Double> pr,
			Function<ExecutionType,Double> p,
			double K,
			int L,
			int N,
			DesignType theta_init
			){
		this.a = a;
		this.globalPrior = globalPrior;
		this.localPrior = localPrior;
		this.D = D;
		this.pr = pr;
		this.p = p;
		this.K = K;
		this.L = L;
		this.N = N;
		this.theta_init = theta_init;
	}
	
	public DesignType minimize(Random rng, File output_file){
		/* initialization */
		init(rng,output_file);
		
		//a single proposal in the first iteration
		int m = 1;
		
		while(num_eval < N){
			//generate proposals & update the incumbent
			List<DesignType> Theta_prop = explore(m,rng);
			//select contender
			DesignType theta_prop = select(Theta_prop,rng);
			//race
			race(theta_prop,rng);
			//update counters and compute m for next iteration
			num_it++;
			m = (int) Math.min((double)num_eval/num_it*(L-(double)num_prop/N)/(1-(double)num_eval/N), 
					(num_eval+Math.min(2,N-num_eval))*L - num_prop);
		}
		explore(L*N-num_prop,rng); //final attempt to find new incumbents
		logCurrentIncumbent();
		return theta_inc;
	}
	
	private void init(Random rng, File output_file){
		//some initialization for logging purposes
		start_time = System.currentTimeMillis();
		trajFile = output_file;
		log(trajFile,"Run, Perf. estimate, Incumbent, Time");
		
		//initialize the incumbent
		theta_inc = theta_init != null? theta_init : globalPrior.apply(rng);
		
		//initialize performance model
		if(pr == null){
			//independent sample averages
			M = new IndependentSampleAveragesModel<DesignType,ExecutionType>(p);
		}else{
			//importance sample estimates
			M = new ImportanceSamplingModel<DesignType,ExecutionType>(p,pr);
		}
		M = new MemoizePM<DesignType,ExecutionType>(M);
		
		//initialize counters
		num_eval = 0;
		num_it = 0;
		num_prop = 0;
	}
	
	private List<DesignType> explore(int m, Random rng){
		//generate m proposals
		List<DesignType> Theta_prop = new ArrayList<DesignType>(m);
		for(int i = 0; i < m; i++){
			num_prop++;
			DesignType theta_i;
			if(rng.nextBoolean()){
				//use global (50% likelihood)
				theta_i = globalPrior.apply(rng);
			}else{
				//use local conditioned on incumbent (50% likelihood)
				theta_i = localPrior.apply(theta_inc,rng);
			}
			Theta_prop.add(theta_i);
			updateIncumbent(theta_i);
		}
		return Theta_prop;
	}
	
	private DesignType select(List<DesignType> Theta_prop, Random rng){
		double max_val = Double.NEGATIVE_INFINITY;
		DesignType max_arg = null;
		//System.out.println("<o: "+M.o(theta_inc)+",unc: "+M.unc(theta_inc)+">");
		for(DesignType theta : Theta_prop){
			double term1 = M.o(theta_inc) == M.o(theta)? 0 : (M.o(theta_inc) - M.o(theta))/(M.unc(theta_inc) + M.unc(theta));
			double val = term1 - Math.pow(M.sim(theta, theta_inc),K)/(1-Math.pow(M.sim(theta, theta_inc),K));
			//System.out.println("<o: "+M.o(theta)+",unc: "+M.unc(theta)+",sim: "+M.sim(theta,theta_inc)+"> "+val);
			if(val > max_val){
				max_val = val;
				max_arg = theta;
			}
		}
		return max_arg == null? Theta_prop.get(0) : max_arg;
	}
	
	private void race(DesignType theta_prop, Random rng){
		//run incumbent
		test(theta_inc,rng); 
		//run contender until either incumbent or worse estimate
		do{
			test(theta_prop,rng);
			updateIncumbent(theta_prop);
		}while(num_eval < N && theta_inc != theta_prop && M.o(theta_prop) < M.o(theta_inc));
	}
	
	private void test(DesignType theta, Random rng){
		if(num_eval < N){
			//log incumbent
			logCurrentIncumbent();
			//run theta on x ~ D
			InputType x = D.apply(rng);
			ExecutionType exec = a.apply(x, theta);
			//update \hat{M}
			M.update(theta, exec);
			num_eval++;
		}
	}

	private void updateIncumbent(DesignType theta){
		if(theta != theta_inc && M.o(theta_inc) >= M.o(theta)){
			double sK = Math.pow(M.sim(theta_inc, theta),K);
			if(sK*(M.o(theta_inc)-M.o(theta)) >= (1-sK)*(M.unc(theta)-M.unc(theta_inc))){
				//System.out.println("<o: "+M.o(theta_inc)+",unc: "+M.unc(theta_inc)+">");
				//System.out.println("<o: "+M.o(theta)+",unc: "+M.unc(theta)+",sim: "+M.sim(theta,theta_inc)+">");
				theta_inc = theta;
			}
		}
	}
	
	private void logCurrentIncumbent(){
		log(trajFile,num_eval+", "+ M.o(theta_inc)+ ", " +theta_inc+", "+(System.currentTimeMillis()-start_time));
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

}
