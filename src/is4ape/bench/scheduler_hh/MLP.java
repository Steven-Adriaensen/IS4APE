package is4ape.bench.scheduler_hh;

/**
 * Fully connected multilayer perceptron with sigmoid activation
 * 
 * @author Steven Adriaensen
 *
 */
class MLP{
	static class Neuron{
		double[] weights;
		
		Neuron(double... weights){
			this.weights = weights;
		}
		
		double activate(double... inputs){
			double f = weights[0];
			for(int i = 0; i <inputs.length; i++){
				f += weights[i+1]*inputs[i];
			}
			return 1/(1+Math.exp(-f));
		}
	}
	
	Neuron[][] hidden_units;
	Neuron[] output_unit;
	
	//workspace
	double[] activations;
	double[] new_activations;
	double[] output;
	
	MLP(int x, int y, int n, int m, double... weights){
		int n_weights = (x+1)*m+(n-1)*m*(m+1)+y*(m+1);
		if(weights.length != n_weights){
			System.err.println("Wrong number of weights, expected "+n_weights+" got "+weights.length);
		}
		
		hidden_units = new Neuron[n][m];
		//multi-layer perceptron with n hidden layers
		//first hidden layer:
		int off = 0;
		for(int j = 0; j < m; j++){
			double[] ws = new double[x+1];
			for(int k = 0; k < x+1; k++){
				ws[k] = weights[off+k];
			}
			hidden_units[0][j] = new Neuron(ws);
			off += x+1;
		}
		//n-1 remaining hidden layers
		for(int i = 1; i < n; i++){
			for(int j = 0; j < m; j++){
				double[] ws = new double[m+1];
				for(int k = 0; k < m+1; k++){
					ws[k] = weights[off+k];
				}
				hidden_units[i][j] = new Neuron(ws);
				off += m+1;
			}
		}
		//output layer
		output_unit = new Neuron[y];
		for(int i = 0; i < y; i++){
			double[] ws = new double[m+1];
			for(int k = 0; k < m+1; k++){
				ws[k] = weights[off+k];
			}
			output_unit[i] = new Neuron(ws);
			off += m+1;
		}	
		activations = new double[m];
		new_activations = new double[m];
		output = new double[y];
	}
	
	void predict(double[] inputs){
		//compute activations for the first hidden layer
		for(int j = 0; j < hidden_units[0].length; j++){
			activations[j] = hidden_units[0][j].activate(inputs);
		}
		//feed forward activations
		for(int i = 1; i < hidden_units.length; i++){
			for(int j = 0; j < hidden_units[0].length; j++){
				new_activations[j] = hidden_units[i][j].activate(activations);
			}
			activations = new_activations;
		}
		//compute outputs
		for(int j = 0; j < output.length; j++){
			output[j] = output_unit[j].activate(activations);
		}
	}
	
}
