package is4ape.bench.scheduler_hh;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class contains all logic for the dynamic metaheuristic scheduler scenario
 * 
 * @author Steven Adriaensen
 *
 */
public class SchedulerHH {
	final static long budget = 600000;
	final static int nSlots = 100;
	final static long time_per_slot = budget/nSlots;
	
	public static ExecutionInfo run(Input x, Configuration c){
		Policy pi = c.pi;
		Random rng = new Random();
		ExecutionInfo e = new ExecutionInfo();
		e.x = x;
		e.nSlots1 = 0;
		e.nSlots2 = 0;
		for(int i = 0; i < nSlots; i++){
			int sel = pi.selectNext(
					x,
					e.nSlots1,
					e.nSlots2,
					rng
				);
			if(sel == 1){
				e.nSlots1++;
			}else if(sel == 2){
				e.nSlots2++;
			}else{
				System.err.println("Invalid choice policy: "+sel);
			}
		}
		e.r = e.x.getDesirability(e.nSlots1, e.nSlots2);
		return e;
	}
	
	public static Input sample_D_training(Random rng){
		return inputForID(rng.nextInt(2744),rng);
	}
	
	final static double[] workspace = new double[((nSlots+1)*(nSlots+2))/2];
	
	public static double getLikelihood(Configuration c, ExecutionInfo e){
		return getLikelihood(c.toPolicy(),e);
	}
	
	public static double getLikelihood(Policy pi, ExecutionInfo e){
		computeLikelihoods(pi,e.x,e.nSlots1,e.nSlots2);
		int v = ((nSlots)*(nSlots+1))/2+e.nSlots1;
		return workspace[v];
	}

	public static void computeLikelihoods(Policy pi, Input x, int nMax1, int nMax2){
		//clear workspace
		int offset = ((nSlots+1)*(nSlots))/2;
		for(int i = 0; i <= nSlots; i++){
			workspace[offset+i] = 0;
		}
		//key = nSlots1*nSlots + nSlots2;
		//start with (0,0)-1 in the list
		workspace[0] = 1;
		//for each slot:
		int v = 0;
		for(int i = 0; i < nSlots; i++){
			for(int j = 0; j <= i; j++){
				if(workspace[v] > 0){
					int nSlots1 = j;
					int nSlots2 = i-j;
					
					//sum should be i
					//(j,i-j)
					double pr1 = pi.prNext1(x,nSlots1, nSlots2);
					
					//extend s1;
					//-> (i+1)*(i+2)/2 + nSlot1 + 1
					if(nSlots1 < nMax1){
						int v1 = (i+1)*(i+2)/2 + nSlots1 +1;
						workspace[v1] += pr1*workspace[v];
					}
					//extend s2;
					//-> (i+1)*(i+2)/2 + nSlot1 + 0
					if(nSlots2 < nMax2){
						int v2 = (i+1)*(i+2)/2 + nSlots1;
						workspace[v2] += (1-pr1)*workspace[v];
					}
					workspace[v] = 0;
				}
				v++;
			}
		}
	}
	
	public static class ExecutionInfo{
		Input x;
		int nSlots1;
		int nSlots2;
		double r;
		
		public static double p(ExecutionInfo e){
			return e.r;
		}
	}
	
	public static class Configuration{
		static final int n = 1;
		static final int m = 3;
		static final int n_weights = 22;
		double[] weights; 
		Policy pi;
		
		Configuration(double[] weights){
			this.weights = weights;
			pi = new Policy(n,m,weights);
		}
		
		public static Configuration localPrior(Configuration c, Random rng){
			double[] ws = new double[n_weights];
			for(int i = 0; i < n_weights; i++){
				ws[i] = Math.max(-100, Math.min(c.weights[i]+5*rng.nextGaussian(), 100));
			}
			return new Configuration(ws);
		}
		
		public static Configuration globalPrior(Random rng){
			double[] ws = new double[n_weights];
			for(int i = 0; i < ws.length; i++){
				ws[i] = 200*rng.nextDouble()-100;
			}
			return new Configuration(ws);
		}
		
		public Policy toPolicy(){
			return pi;
		}
		
		public String toString(){
			String str = "";
			for(int i = 0; i < weights.length; i++){
				str += ","+weights[i];
			}
			return str.substring(1);
		}
		
	}
	
	static class Policy{
		static double T = 0; //extreme: 0.5
		MLP nn;
		
		Policy(int n, int m, double... weights){
			nn = new MLP(5,1,n,m,weights);
		}

		double prNext1(Input x, int nSlots1, int nSlots2) {
			int slots = nSlots1 + nSlots2;
			int offset = (slots*(slots+1))/2;
			int v = offset + nSlots1;

			nn.predict(x.f1[v]);
			double pr1 = nn.output[0];
			nn.predict(x.f2[v]);
			double pr2 = nn.output[0];
			if(pr1 == pr2){
				return 0.5;
			}else{
				return pr1/(pr1+pr2);
			}
		}
		
		int selectNext(Input x, int nSlots1, int nSlots2, Random rng){
			return rng.nextDouble() < prNext1(x,nSlots1,nSlots2)? 1 : 2;
		}
	}
	
	
	
	
	//TODO: improve modularity (split up into multiple auxiliary classes)
	final static String folder= "HH_data";
	final static long[][] seeds;
	final static int[] hh1s;
	final static int[] hh2s;
	
	static{
		seeds = new long[98][32];
		try (BufferedReader br = new BufferedReader(new FileReader(folder+"/input_seeds.csv"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	String[] tokens = line.split(",");
		    	int x = Integer.parseInt(tokens[0]);
		    	for(int i = 0; i < 32; i++){
		    		seeds[x][i] = Long.parseLong(tokens[1+i]);
		    	}
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
		hh1s = new int[91];
		hh2s = new int[91];
		int v = 0;
		for(int i = 1; i < 14; i++){
			for(int j = 0; j < i; j++){
				hh1s[v] = i;
				hh2s[v] = j;
				v++;
			}
		}
	}

	static enum HH{
		AcceptAllHH,
		AcceptNoWorseHH,
		FairShareILS,
		NoRestartFairShareILS,
		EPH,
		GIHH,
		/*ASAP-HH*/
		GDeluge,
		GreedyHH,
		HyperILS,
		NaiveHH,
		NaiveMemAlg,
		TSHyperHeuristic,
		TSHyperHeuristicAA,
		TSHyperHeuristicNaiveAcceptance
	}
	
	static Input inputForID(int id, Random rng){ //0-8917
		if(id < 0 || id > 8917){
			return null;
		}
		int x = id%98;
		id -= x;
		id /= 98;
		HH hh1 = HH.values()[hh1s[id]];
		HH hh2 = HH.values()[hh2s[id]];
		long seed = seeds[x][rng.nextInt(32)];
		return new Input(x,seed,hh1,hh2);
	}

	static class Trace{
		List<Long> times;
		List<Double> bests;
		List<Integer> index;
		
		protected Trace(List<Long> t, List<Double> b, List<Integer> index){
			this.times = t;
			this.bests = b;
			this.index = index;
		}
		
		protected Trace(List<Long> t, List<Double> b){
			this.times = t;
			this.bests = b;
			//create an index
			index = new ArrayList<Integer>(nSlots+1);
			long time = 0;
			int lastIndex = 0;
			for(int i = 0; i <= nSlots; i++){
				while(lastIndex < times.size() && times.get(lastIndex) < time){
					lastIndex++;
				}
				index.add(lastIndex);
				time += time_per_slot;
			}
		}
		
		public double getBest(){
			if(bests.size() > 0){
				return bests.get(bests.size()-1);
			}else{
				return Double.POSITIVE_INFINITY;
			}
		}
		
		public long getTimeForSQ(double sq, boolean inclusive){
			int i = bests.size();
			while(i > 0 && bests.get(i-1) < sq){i--;}
			if(inclusive){
				while(i > 0 && bests.get(i-1) == sq){i--;}
			}
			return i < times.size()? times.get(i) : Long.MAX_VALUE;	
		}
		
		public Trace getView(int nSlots){
			return new Trace(
					times.subList(0, index.get(nSlots)),
					bests.subList(0, index.get(nSlots)),
					index.subList(0, nSlots+1)
					);
		}
		
		static Trace fromFile(String path) throws FileNotFoundException, IOException{
			List<Long> times = new ArrayList<Long>();
			List<Double> bests = new ArrayList<Double>();
			try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			    String line;
			    while ((line = br.readLine()) != null) {
			       // process the line.
			    	String[] tokens = line.split(",");
			    	times.add(Long.parseLong(tokens[0]));
			    	bests.add(Double.parseDouble(tokens[1]));
			    }
			}
			return new Trace(times,bests);
		}
	}
	
	public static class Input{
		Trace t1;
		Trace t2;
		double[] desirabilities;
		double[][] f1;
		double[][] f2;
		
		public Input(int x, long seed, HH hh1, HH hh2){
			String file1 = folder+"/"+x+"_"+seed+"_"+hh1;
			String file2 = folder+"/"+x+"_"+seed+"_"+hh2;
			try {
				t1 = Trace.fromFile(file1);
				t2 = Trace.fromFile(file2);
			} catch (IOException e) {
				e.printStackTrace();
			}
			compute_desirabilities();
			compute_features(); //inputs for NN
		}
		
		double[][] extract_inputs(int nSlots1, int nSlots2){
			Trace t1v,t2v,t1d,t2d;
			t1v = t1d = t1.getView(nSlots1);
			t2v = t2d = t2.getView(nSlots2);
			if(nSlots1 > nSlots2){
				t1d = t1.getView(nSlots2);
			}else if(nSlots1 < nSlots2){
				t2d = t2.getView(nSlots1);
			}
			double best1 = t1v.getBest() < t2v.getBest()? 1 : t1v.getBest() == t2v.getBest()? 0.5 : 0;
			double best2 = 1-best1;
			double bestbudget1 = t1d.getBest() < t2d.getBest()? 1 : t1d.getBest() == t2d.getBest()? 0.5 : 0;
			double bestbudget2 = 1-bestbudget1;
			double elapsed = (double)(nSlots1+nSlots2)/nSlots;
			double tdiff1 = (double)(nSlots1-nSlots2)/nSlots;
			double tdiff2 = - tdiff1;
			double ahead; //ahead ranges from [-1,1]
			if(t1v.getBest() < t2v.getBest() && nSlots2 > 0){
				ahead = (double)(nSlots2*time_per_slot-t1v.getTimeForSQ(t2v.getBest(), false))/budget;
			}else if(t1v.getBest() > t2v.getBest() && nSlots1 > 0){
				ahead = (double)(nSlots1*time_per_slot-t2v.getTimeForSQ(t1v.getBest(), false))/budget;
			}else{
				ahead = 0;
			}
			return new double[][]{{best1,bestbudget1,elapsed,tdiff1,ahead},{best2,bestbudget2,elapsed,tdiff2,ahead}};
		}
		
		private void compute_features(){
			int size = ((nSlots+1) * nSlots)/2;
			f1 = new double[size][3];
			f2 = new double[size][3];
			int v = 0;
			for(int i = 0; i < nSlots; i++){
				for(int j = 0; j <= i; j++){
					int nSlots1 = j;
					int nSlots2 = i-j;
					double[][] inputs = extract_inputs(nSlots1,nSlots2);
					f1[v] = inputs[0];
					f2[v] = inputs[1];
					v++;
				}
			}
		}
		
		private void compute_desirabilities(){
			//compute desirabilities
			double[] sqs = new double[nSlots+1];
			for(int i = 0; i <= nSlots; i++){
				sqs[i] = Math.min(t1.getView(i).getBest(), t2.getView(nSlots-i).getBest());
			}
			int[][] bews = new int[nSlots+1][3];
			for(int i = 0; i < nSlots; i++){
				for(int j = i+1; j <= nSlots; j++){
					if(sqs[i] < sqs[j]){
						bews[i][2]++;
						bews[j][0]++;
					}else if(sqs[i] > sqs[j]){
						bews[i][0]++;
						bews[j][2]++;
					}else{
						bews[i][1]++;
						bews[j][1]++;
					}
				}
			}
			desirabilities = new double[nSlots+1];
			for(int i = 0; i <= nSlots; i++){
				desirabilities[i] = (bews[i][0]+bews[i][1]/2.0)/nSlots;
			}
		}
		
		public double getDesirability(int nSlots1, int nSlots2){
			return desirabilities[nSlots1];
		}
		
	}
	
}

