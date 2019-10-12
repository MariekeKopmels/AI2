import java.awt.print.Printable;
import java.util.*;

public class KMeans extends ClusteringAlgorithm
{
	// Number of clusters
	private int k;

	// Dimensionality of the vectors
	private int dim;

	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;

	// Array of k clusters, class cluster is used for easy bookkeeping
	private Cluster[] clusters;

	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and memberlists with the ID's (which are Integer objects) of the datapoints that are member of that cluster.
	// You also want to remember the previous members so you can check if the clusters are stable.
	static class Cluster
	{
		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;

		public Cluster()
		{
			currentMembers = new HashSet<Integer>();
			previousMembers = new HashSet<Integer>();
		}
	}
	// These vectors contains the feature vectors you need; the feature vectors are float arrays.
	// Remember that you have to cast them first, since vectors return objects.
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;

	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.k = k;
		this.trainData = trainData;
		this.testData = testData;
		this.dim = dim;
		prefetchThreshold = 0.5;

		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++)
			clusters[ic] = new Cluster();
	}


	public boolean train()
	{
	 	//implement k-means algorithm here:
		// Step 1: Select an initial random partioning with k clusters
		Random random = new Random();
		for (Integer i = 0; i < trainData.size(); i++) {
			int rand = random.nextInt(k);
			clusters[rand].currentMembers.add(i);
		}

		showMembers();

		if (trainData.size() == 0){
			return false;
		}
		// Step 2: Generate a new partition by assigning each datapoint to its closest cluster center

		// Step 3: recalculate cluster centers

		do{

			// make new members old members
			for (Cluster cluster : clusters) {
				cluster.previousMembers = cluster.currentMembers;
				cluster.currentMembers = new HashSet<>();
				cluster.prototype = new float[trainData.get(0).length];
			}

			// calculate prototype (cluster center) per cluster
			float memberDataLength = trainData.get(0).length;
			for(int indexCluster = 0; indexCluster < k; indexCluster++) {

				//index of prototype
				for(int indexPrototype = 0; indexPrototype < memberDataLength; indexPrototype++) {

					float sum = 0;
					//index of members
					for (int member : clusters[indexCluster].previousMembers) {
						sum += trainData.get(member)[indexPrototype];
					}
					clusters[indexCluster].prototype[indexPrototype] = ( sum / (float) clusters[indexCluster].previousMembers.size() );
				}

			}


			// assignment to new cluster
			for (int memberIndex = 0; memberIndex < trainData.size(); memberIndex++) {
				float[] member = trainData.get(memberIndex);
				Integer bestCluster = null;
				float bestDistance = memberDataLength;
				for (int cluster = 0; cluster < k; cluster++) {
					float[] prototype = clusters[cluster].prototype;
					float currentDistance = 0;
					for (int index = 0; index < memberDataLength; index++){
						currentDistance += Math.pow(member[index] - prototype[index], 2);
					}
					currentDistance = (float) Math.sqrt(currentDistance);
//					System.out.println(currentDistance);
					if (currentDistance < bestDistance) {
						bestCluster = cluster;
						bestDistance = currentDistance;
					}
				}
				clusters[bestCluster].currentMembers.add(memberIndex);
			}
			System.out.println("reassigned");
		// Step 4: repeat until clustermembership stabilizes
		} while(clustersChanged());

		showMembers();

		return true;
	}

	private boolean clustersChanged(){
		for (Cluster cluster : clusters) {
			if (!cluster.currentMembers.containsAll(cluster.previousMembers)) {
				return true;
			}
			if (!cluster.previousMembers.containsAll(cluster.currentMembers)) {
				return true;
			}
		}
		return false;
	}

//	private boolean clustersChanged(){
//		for (Cluster cluster : clusters) {
//			for (Integer member : cluster.previousMembers) {
//				if (!cluster.currentMembers.contains(member)){
//					return false;
//				}
//			}
//		}
//		return true;
//	}


	public boolean test()
	{
		// iterate along all clients. Assumption: the same clients are in the same order as in the testData
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value
		return true;
	}


	// The following members are called by RunClustering, in order to present information to the user
	public void showTest()
	{
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}
	
	public void showMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nMembers cluster["+i+"] :" + clusters[i].currentMembers);
	}
	
	public void showPrototypes()
	{
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster["+ic+"] :");
			
			for (int ip = 0; ip < dim; ip++)
				System.out.print(clusters[ic].prototype[ip] + " ");
			
			System.out.println();
		 }
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}
