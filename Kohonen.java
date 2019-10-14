import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.util.*;

public class Kohonen extends ClusteringAlgorithm
{
	// Size of clustersmap
	private int n;

	// Number of epochs
	private int epochs;
	
	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;

	private double initialLearningRate; 
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and a memberlist with the ID's (Integer objects) of the datapoints that are member of that cluster.  
	private Cluster[][] clusters;

	// Vector which contains the train/test data
	private Vector<float[]> trainData;
	private Vector<float[]> testData;
	
	// Results of test()
	private double hitrate;
	private double accuracy;
	
	static class Cluster
	{
			float[] prototype;

			Set<Integer> currentMembers;

			public Cluster()
			{
				currentMembers = new HashSet<Integer>();
			}
	}
	
	public Kohonen(int n, int epochs, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.n = n;
		this.epochs = epochs;
		prefetchThreshold = 0.5;
		initialLearningRate = 0.8;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;       
		
		Random rnd = new Random();

		// Here n*n new cluster are initialized
		clusters = new Cluster[n][n];
		for (int i = 0; i < n; i++)  {
			for (int i2 = 0; i2 < n; i2++) {
				clusters[i][i2] = new Cluster();
				clusters[i][i2].prototype = new float[dim];
				for (int i3 = 0; i3 < dim; i3++){
					clusters[i][i2].prototype[i3] = rnd.nextFloat();
				}
			}
		}
	}

	
	public boolean train()
	{

		double learningRate = 0;
		double squareSize = 0;
		int radius = 0;
		// Step 1: initialize map with random vectors (A good place to do this, is in the initialisation of the clusters)

		// Repeat 'epochs' times:
		for(int currentEpoch = 0; currentEpoch < this.epochs; currentEpoch++){

			// Step 2: Calculate the squareSize and the learningRate, these decrease lineary with the number of epochs.
			learningRate = this.initialLearningRate*(1 - ( (double) currentEpoch/this.epochs) );
			squareSize = ( (double) this.n/2 )*(1 - ( (double) currentEpoch/this.epochs) );
			radius = (int) squareSize;

			// Step 3: Every input vector is presented to the map (always in the same order)
			// For each vector its Best Matching Unit is found, and :
			for (float[] vector : this.trainData) {

				float bestDistance = this.dim;
				int bestClusterDim1 = 0;
				int bestClusterDim2 = 0;


				for (int i = 0; i < this.n; i++) {
					for (int i2 = 0; i2 < this.n; i2++) {

						float currentDistance = 0;
						float[] prototype = clusters[i][i2].prototype;

						for (int index = 0; index < this.dim; index++) {
							currentDistance += Math.pow(vector[index] - prototype[index], 2);
						}
						currentDistance = (float) Math.sqrt(currentDistance);
						if (currentDistance < bestDistance) {
							bestClusterDim1 = i;
							bestClusterDim2 = i2;
							bestDistance = currentDistance;
						}
					}
				}

				// Step 4: All nodes within the neighbourhood of the BMU are changed, you don't have to use distance relative learning.

				/// Security for index out of bound for clusters[][]
				int xBegin = Math.max(bestClusterDim1-radius, 0);
				int xEnd = Math.min(bestClusterDim1+radius, this.n-1);

				int yBegin = Math.max(bestClusterDim2-radius, 0);
				int yEnd = Math.min(bestClusterDim2+radius, this.n-1);

				// Step 4: All nodes within the neighbourhood of the BMU are changed, you don't have to use distance relative learning.
				for (int i = xBegin; i <= xEnd; i++) {
					for (int i2 = yBegin; i2 <= yEnd; i2++) {
						float[] prototype = clusters[i][i2].prototype;

						for (int index = 0; index < this.dim; index++){
							prototype[index] = (float) (1-learningRate) * prototype[index] + (float) learningRate * vector[index];
						}
					}
				}
			}
		}

		/// Assign member to closest cluster
		for (int member = 0; member < trainData.size(); member++){

			float[] memberData = this.trainData.get(member);

			float bestDistance = this.dim;
			int bestClusterDim1 = 0;
			int bestClusterDim2 = 0;

			for (int i = 0; i < this.n; i++) {
				for (int i2 = 0; i2 < this.n; i2++) {

					float currentDistance = 0;
					float[] prototype = clusters[i][i2].prototype;

					for (int index = 0; index < this.dim; index++) {
						currentDistance += Math.pow(memberData[index] - prototype[index], 2);
					}
					currentDistance = (float) Math.sqrt(currentDistance);
					if (currentDistance < bestDistance) {
						bestClusterDim1 = i;
						bestClusterDim2 = i2;
						bestDistance = currentDistance;
					}
				}
			}
			clusters[bestClusterDim1][bestClusterDim2].currentMembers.add(member);
		}

		// Since training kohonen maps can take quite a while, presenting the user with a progress bar would be nice
		return true;
	}
	
	public boolean test()
	{
		// iterate along all clients
		// for each client find the cluster of which it is a member
		int[][][] prefetched = new int[this.n][this.n][this.dim];
		int[][] totalPrefetched = new int[this.n][this.n];

		for	(int i=0; i < this.n; i++) {
			for (int i2 = 0; i2 < this.n; i2++) {
				for (int html = 0; html < this.dim; html++) {
					if (clusters[i][i2].prototype[html] < prefetchThreshold) {
						prefetched[i][i2][html] = 0;
					} else {
						prefetched[i][i2][html] = 1;
					}
				}
				totalPrefetched[i][i2] = Arrays.stream(prefetched[i][i2]).sum();
			}
		}

		float hitrateSum=0;
		float accuracySum=0;

		// iterate along all clients.
		for (int member=0; member < testData.size(); member++){
			int memberClusterDim1=0;
			int memberClusterDim2=0;

			//  for each client find the cluster of which it is a member
			for	(int i=0; i < this.n; i++) {
				for (int i2 = 0; i2 < this.n; i2++) {
					if (clusters[i][i2].currentMembers.contains(member)) {
						memberClusterDim1 = i;
						memberClusterDim2 = i2;
						break;
					}
				}
			}

			// get the actual testData (the vector) of this client
			float[] memberData = testData.get(member);

			int hits=0;
			int requests=0;

			for (int i = 0; i < this.dim; i++){
				if (memberData[i] == 1.0) {
					// count number of requests
					requests++;
					if (memberData[i] == prefetched[memberClusterDim1][memberClusterDim2][i]) {
						// count number of hits
						hits++;
					}
				}
			}

			if (requests != 0)
			{
				hitrateSum += (float) hits / requests;
			}
			accuracySum += (float) hits / totalPrefetched[memberClusterDim1][memberClusterDim2];

		}

		// set the global variables hitrate and accuracy to their appropriate value
		this.hitrate =  hitrateSum/testData.size();
		this.accuracy = accuracySum/testData.size();

		return true;
	}


	public void showTest()
	{
		System.out.println("Initial learning Rate=" + initialLearningRate);
		System.out.println("Prefetch threshold=" + prefetchThreshold);
		System.out.println("Hitrate: " + hitrate);
		System.out.println("Accuracy: " + accuracy);
		System.out.println("Hitrate+Accuracy=" + (hitrate + accuracy));
	}
 
 
	public void showMembers()
	{
		for (int i = 0; i < n; i++)
			for (int i2 = 0; i2 < n; i2++)
				System.out.println("\nMembers cluster["+i+"]["+i2+"] :" + clusters[i][i2].currentMembers);
	}

	public void showPrototypes()
	{
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				System.out.print("\nPrototype cluster["+i+"]["+i2+"] :");
				
				for (int i3 = 0; i3 < dim; i3++)
					System.out.print(" " + clusters[i][i2].prototype[i3]);
				
				System.out.println();
			}
		}
	}

	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}

