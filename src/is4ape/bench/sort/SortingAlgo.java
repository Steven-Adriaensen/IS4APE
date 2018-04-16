package is4ape.bench.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

abstract public class SortingAlgo {
	
	void sort(ArrayList<Integer> seq){
		sort(seq,0,seq.size());
	}
	
	public String toString(){
		return this.getClass().getSimpleName();
	}
	
	abstract void sort(ArrayList<Integer> seq, int lo, int hi);
	
	static class BubbleSort extends SortingAlgo{
		@Override
		void sort(ArrayList<Integer> seq, int lo, int hi) {
			int n = hi - lo;
			do{
				int new_n = 0;
				for(int i = 1; i < n; i++){
					if(seq.get(i-1) > seq.get(i)){
						int temp = seq.get(i);
						seq.set(i,seq.get(i-1));
		   				seq.set(i-1,temp);
		   				new_n = i;
					}
				}
				n = new_n;
			}while(n > 0);
		}
		
	}
	
	static class SelectionSort extends SortingAlgo{
		@Override
		void sort(ArrayList<Integer> seq, int lo, int hi) {
			for(int i = lo; i < hi-1; i++){
				int min = i;
				for(int j = i+1; j < hi; j++){
					if(seq.get(j) < seq.get(min)){
						min = j;
					}
				}
				int temp = seq.get(i);
				seq.set(i,seq.get(min));
				seq.set(min,temp);
			}
		}
	}
	
	static class InsertionSort extends SortingAlgo{
		@Override
		void sort(ArrayList<Integer> seq, int lo, int hi) {
			for(int i = lo+1; i < hi; i++){
				int j = i;
				while(j > lo && seq.get(j) < seq.get(j-1)){
					int temp = seq.get(j-1);
					seq.set(j-1,seq.get(j));
					seq.set(j,temp);
					j--;
				}
			}
		}
	}
	
	static class QuickSort extends SortingAlgo{
		@Override
		void sort(ArrayList<Integer> seq, int lo, int hi) {
			if(hi <= lo + 1){
				//0 or 1 elements to sort, we're done
				return;
			}
		    int i = lo, j = hi-1;
		    int pivot = seq.get(lo);
		    while (i <= j) {
			    while (seq.get(i)< pivot) {
			    	i++;
			    }
			 	while (seq.get(j) > pivot) {
			 		j--;
			 	}
			 	if (i <= j) {
			    	int temp = seq.get(i);
			  		seq.set(i,seq.get(j));
			  		seq.set(j,temp);
			        i++;
			        j--;
			 	}
		    }
	    	if (lo < j){
	    		sort(seq,lo,j+1);
	    	}
	    	if (i < hi){
	    		sort(seq,i,hi);
	    	}
		}
	}
	
	static class MergeSort extends SortingAlgo{
		Integer[] temp;
		
		@Override
		void sort(ArrayList<Integer> seq, int lo, int hi) {
			temp = new Integer[seq.size()];
			merge_sort(seq,lo,hi);
		}
		void merge_sort(ArrayList<Integer> seq, int lo, int hi){
			if(hi - lo > 1){
				//split the array in 2
				int mid = (lo + hi)/2;
				//sort both sub-arrays
		    	merge_sort(seq,lo,mid);
		    	merge_sort(seq,mid,hi);
		    	//merge both into a sorted array
		    	merge(seq,lo,mid,hi);
			}
		}
		
		private void merge(ArrayList<Integer> seq, int lo, int mid, int hi){
			int i = lo;
			int j = mid;
		    // While there are elements in the left or right runs
		    for (int k = lo; k < hi; k++){
		        // If left run head exists and is <= existing right run head.
		        if (i < mid && (j >= hi || seq.get(i) <= seq.get(j))){
		            temp[k] = seq.get(i);
		            i++;
		        }else{
		            temp[k] = seq.get(j);
		            j++;    
		        }
		    } 
		    //copy res in seq
		    for(int k = lo; k < hi; k++){
		    	seq.set(k,temp[k]);
		    }
		}
	}
	
	static class HeapSort extends SortingAlgo{
		PriorityQueue<Integer> heap;
		
		@Override
		void sort(ArrayList<Integer> seq, int lo, int hi) {
			if(hi-lo > 1){
				heap = new PriorityQueue<Integer>();
				heap.addAll(seq.subList(lo, hi));
				for(int i = lo; i < hi; i++){
					seq.set(i, heap.poll());
				}
			}
 		}
	}
	
	static class JavaSort extends SortingAlgo{
		//TimSort
		@Override
		void sort(ArrayList<Integer> seq, int lo, int hi) {
			Collections.sort(seq.subList(lo, hi));
 		}
	}
	
	//if we limit range, include count sort etc
	
	static class RadixSort extends SortingAlgo{
		// Radix sort Java implementation, based on http://www.geeksforgeeks.org/radix-sort/

	    int count[];

	    void sort(ArrayList<Integer> seq, int lo, int hi){
	    	if(hi-lo > 1){
	    		count = new int[10];
	    		
	    		//Compute range
	    		int[] range = compute_range(seq);
		    	
		    	//Offset w.r.t. min, reduces # sorts + avoids negative (leave design choice open)
		    	for(int i = lo; i < hi; i++){
		    		seq.set(i, seq.get(i)-range[0]);
		    	}
		        
		        // Do counting sort for every digit. Note that instead
		        // of passing digit number, exp is passed. exp is 10^i
		        // where i is current digit number
	    		int m = range[1]-range[0];
		        for (int exp = 1; m/exp > 0; exp *= 10)
		            countSort(seq.subList(lo, hi), exp);
		    	
		    	//restore offset
		    	for(int i = lo; i < hi; i++){
		    		seq.set(i, seq.get(i)+range[0]);
		    	}
	    	}
	    }

	    // A aux function to get maximum value
	    static int[] compute_range(List<Integer> seq){
	    	int[] range = new int[2];
	    	range[0] = seq.get(0);
	    	range[1] = seq.get(0);
	        for (int i = 1; i < seq.size(); i++){
	        	range[0] = Math.min(range[0], seq.get(i));
	        	range[1] = Math.max(range[1], seq.get(i));
	        }
	        return range;
	    }
		 
	    // A function to do counting sort of arr[] according to
	    // the digit represented by exp.
	    void countSort(List<Integer> seq, int exp){
	        Integer output[] = new Integer[seq.size()]; // output array
	        int i;
	        Arrays.fill(count,0);
	 
	        // Store count of occurrences in count[]
	        for (i = 0; i < seq.size(); i++)
	            count[ (seq.get(i)/exp)%10 ]++;
	 
	        // Change count[i] so that count[i] now contains
	        // actual position of this digit in output[]
	        //prefix sum
	        for (i = 1; i < 10; i++)
	            count[i] += count[i - 1];
	 
	        // Build the output array
	        for (i = seq.size() - 1; i >= 0; i--){
	            output[count[(seq.get(i)/exp)%10] - 1] = seq.get(i);
	            count[(seq.get(i)/exp)%10 ]--;
	        }
	 
	        // Copy the output array to arr[], so that arr[] now
	        // contains sorted numbers according to curent digit
	        for (i = 0; i < seq.size(); i++)
	            seq.set(i, output[i]);
	    }
	 
	}
	
	
}
