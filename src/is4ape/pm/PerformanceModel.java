package is4ape.pm;

/**
 * Implementing classes provide estimates of the performance of designs (algorithm instances)
 * based on E', i.e. a sample of executions collected.
 * 
 * @author Steven Adriaensen
 *
 * @param <DesignType> The type of the design
 * @param <ExecutionType> The type of the execution
 */
public interface PerformanceModel<DesignType,ExecutionType> {
	/**
	 * Updates the model after a new execution.
	 * 
	 * @param pi: Design used to obtain the new execution.
	 * @param exec: The new execution.
	 */
	void update(DesignType theta, ExecutionType exec);
	
	/*
	 * Provides an estimate of the average-case performance of a given design
	 */
	double o(DesignType theta);
	
	/*
	 * Provides an estimate of the uncertainty (expected error) on mean performance estimate of a given design.
	 */
	double unc(DesignType theta);
	
	double n(DesignType theta);
	/*
	 * Provides an estimate of the similarity of 2 given designs.
	 * 1: Identical
	 * 0: Completely different
	 */
	double sim(DesignType theta1, DesignType theta2);
}
