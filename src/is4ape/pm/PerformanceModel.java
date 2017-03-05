package is4ape.pm;

/**
 * Implementing classes provide estimates of the performance of designs (algorithm instances)
 * based on E', i.e. a sample of executions collected.
 * 
 * @author Steven Adriaensen
 *
 * @param <PolicyType> The type of the design
 * @param <ExecutionType> The type of the execution
 */
public interface PerformanceModel<PolicyType,ExecutionType> {
	/**
	 * Updates the model after a new execution.
	 * 
	 * @param pi: Design used to obtain the new execution.
	 * @param exec: The new execution.
	 */
	void update(PolicyType pi, ExecutionType exec);
	
	/*
	 * Provides an estimate of the mean performance of a given design
	 */
	double mean(PolicyType pi);
	
	/*
	 * Provides an estimate of the uncertainty (expected error) on mean performance estimate of a given design.
	 */
	double uncertainty(PolicyType pi);
	
	/*
	 * Provides an estimate of the similarity of 2 given designs.
	 * 1: Identical
	 * 0: Completely different
	 */
	double similarity(PolicyType pi1, PolicyType pi2);
}
