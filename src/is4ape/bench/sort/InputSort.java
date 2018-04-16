package is4ape.bench.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import is4ape.bench.sort.SortingAlgo.BubbleSort;
import is4ape.bench.sort.SortingAlgo.HeapSort;
import is4ape.bench.sort.SortingAlgo.InsertionSort;
import is4ape.bench.sort.SortingAlgo.JavaSort;
import is4ape.bench.sort.SortingAlgo.MergeSort;
import is4ape.bench.sort.SortingAlgo.QuickSort;
import is4ape.bench.sort.SortingAlgo.RadixSort;
import is4ape.bench.sort.SortingAlgo.SelectionSort;

/**
 * This class contains all logic for the static sorting portfolio scenario
 * 
 * @author Steven Adriaensen
 *
 */
public class InputSort {

	static List<SortingAlgo> algos = Arrays.asList(
			new BubbleSort(),
			new SelectionSort(),
			new InsertionSort(),
			new MergeSort(),
			new QuickSort(),
			new HeapSort(),
			new RadixSort(),
			new JavaSort()
	);
	
	public static class ExecutionInfo{
		double[] phi_x;
		int a_sel;
		double p;
		
		public String toString(){
			return algos.get(a_sel)+" on "+Arrays.toString(phi_x);
		}
	}
	
	public static double pr(List<Double> theta, ExecutionInfo e){
		return select(e.phi_x,theta) == e.a_sel? 1 : 0;
	}
	
	public static double p(ExecutionInfo e){
		return e.p;
	}
	
	public static int sample_D_training(Random rng) {
		return rng.nextInt(15000);
	}
	
	public static ExecutionInfo run(int seq_id, List<Double> theta, Random rng){
		SortingSurrogate pModel = new SortingSurrogate("sort_data",seq_id,rng);
		ExecutionInfo e = new ExecutionInfo();
		e.phi_x = pModel.getFeatures(seq_id).toVector();
		e.a_sel = select(e.phi_x,theta);
		e.p = pModel.getPerformanceObservation(algos.get(e.a_sel));
		return e;
	}
	
	static private int select(double[] phi_x, List<Double> theta){
		int sel = -1;
		double besteval = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < 8; i++){
			double eval = theta.get(i)+theta.get(8+i)*phi_x[0]+theta.get(16+i)*phi_x[1]+theta.get(24+i)*phi_x[2]+theta.get(32+i)*phi_x[3];
			if(eval > besteval){
				besteval = eval;
				sel = i;				
			}
		}
		return sel;
	}
	
	public static List<Double> uniformGlobalPrior(Random rng) {
		List<Double> design = new ArrayList<Double>(40);
		for(int i = 0; i < 40; i++){
			design.add(2*rng.nextDouble()-1);
		}
		return design;
	}

	public static List<Double> gaussianLocalPrior(List<Double> current, Random rng){
		List<Double> design = new ArrayList<Double>(40);
		for(int i = 0; i < 40; i++){
			design.add(Math.max(-1, Math.min(current.get(i)+rng.nextGaussian()/40, 1)));
		}
		return design;
	}
	
}
