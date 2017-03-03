package is4ape.pm;


public interface PerformanceModel<PolicyType,ExecutionType> {
	void update(PolicyType pi, ExecutionType exec);
	double mean(PolicyType pi);
	double uncertainty(PolicyType pi);
	double similarity(PolicyType pi1, PolicyType pi2);
}
