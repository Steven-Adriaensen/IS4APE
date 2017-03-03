package is4ape.pm;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ImportanceSamplingModelMemoized<PolicyType,ExecutionType> extends ImportanceSamplingModel<PolicyType,ExecutionType>{	
	Map<PolicyType,Record> cache; 
	
	public ImportanceSamplingModelMemoized(Function<ExecutionType, Double> f, BiFunction<PolicyType, ExecutionType, Double> logPr) {
		super(f, logPr);
		cache = new HashMap<PolicyType,Record>();
	}

	@Override
	public void update(PolicyType pi, ExecutionType exec){
		cache.clear();
		super.update(pi, exec);
	}
	
	@Override
	public double mean(PolicyType pi){
		Record rec = getRecord(pi);
		if(rec.mean == null){
			rec.mean = super.mean(pi);
		}
		return rec.mean;
	}
	
	@Override
	public double uncertainty(PolicyType pi){
		Record rec = getRecord(pi);
		if(rec.unc == null){
			rec.unc = super.uncertainty(pi);
		}
		return rec.unc;
	}
	
	@Override
	protected double n(PolicyType pi){
		Record rec = getRecord(pi);
		if(rec.n == null){
			rec.n = super.n(pi);
		}
		return rec.n;
	}
	
	@Override
	protected double std(PolicyType pi){
		Record rec = getRecord(pi);
		if(rec.std == null){
			rec.std = super.std(pi);
		}
		return rec.std;
	}
	
	private Record getRecord(PolicyType pi){
		Record rec;
		if(!cache.containsKey(pi)){
			rec = new Record();
			cache.put(pi,rec);
		}else{
			rec = cache.get(pi);
		}
		return rec;
	}
	
	static class Record{
		Double mean;
		Double unc;
		Double n;
		Double std;
	}
}
