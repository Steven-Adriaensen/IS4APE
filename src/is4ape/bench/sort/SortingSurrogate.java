package is4ape.bench.sort;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SortingSurrogate {
	Random rng = new Random();
	int inputsize;
	long inputrange;
	double inputorder;
	double inputeqs;
	Map<String,List<Double>> data = new HashMap<String,List<Double>>();
	
	static class Features{
		double f_length;
		double f_range;
		double f_sorted;
		double f_equal;
		
		Features(double f_length, double f_range, double f_sorted, double f_equal){
			this.f_length = f_length;
			this.f_range = f_range;
			this.f_sorted = f_sorted;
			this.f_equal = f_equal;
		}
		
		double[] toVector(){
			return new double[]{f_length,f_range,f_sorted,f_equal};
		}
		
	}
	
	private double scalenorm(double val, double min, double max){
		double shift = (1-min);
		val = val + shift;
		double shiftmax = max + shift;
		val = Math.log(val);
		shiftmax = Math.log(shiftmax);
		return val/shiftmax;
	}
	
	public Features getFeatures(int index){
		return new Features(
				scalenorm(inputsize,2,200000),
				scalenorm(inputrange,-Math.pow(10, 9),Math.pow(10, 9)),
				scalenorm(inputorder,0,1),
				scalenorm(inputeqs,0,1));
	}
	
	public double getPerformanceObservation(SortingAlgo algo){
		String key = algo.toString();
		List<Double> vals = data.get(key);
		return vals.get(rng.nextInt(vals.size()));
	}
	
	SortingSurrogate(String path, int index, Random rng){
		this.rng = rng;
		String file = path+"/"+index+".txt";
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line = br.readLine();
			String[] tokens = line.split(",");
			inputsize = Integer.parseInt(tokens[1]);
			inputrange = Long.parseLong(tokens[2]);
			inputorder = Double.parseDouble(tokens[3]);
			inputeqs = Double.parseDouble(tokens[4]);
    	    while ((line = br.readLine()) != null) {
    	    	tokens = line.split(",");
    	    	String algo = tokens[0];
    	    	List<Double> results = new ArrayList<Double>(tokens.length-1);
    	    	for(int i = 1; i < tokens.length; i++){
    	    		results.add(Double.parseDouble(tokens[i]));
    	    	}
    	    	data.put(algo, results);
    	    }
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
