/**
 * 
 */
package groupRec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
//import net.librec.math.structure.SequentialSparceVector;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.VectorBasedSequentialSparseVector;
import net.librec.similarity.PCCSimilarity;

/**
 * @author Joaqui
 * 
 *         This class performs the KMeans clustering algorithm
 */
public class Kmeans {

	private int NUM_CLUSTERS = 20;

	private Number MIN_RATING = 0;

	private Number MAX_RATING = 5;

	private SequentialAccessSparseMatrix sparceMatrix;

	private PCCSimilarity sim = new PCCSimilarity();

	private List<Cluster> clusters;

	public Kmeans(int nUM_CLUSTERS, Number mIN_RATING, Number mAX_RATING, SequentialAccessSparseMatrix sparceMatrix) {
		super();
		NUM_CLUSTERS = nUM_CLUSTERS;
		MIN_RATING = mIN_RATING;
		MAX_RATING = mAX_RATING;
		this.sparceMatrix = sparceMatrix;
		this.clusters = new ArrayList<Cluster>();
	}

	public void init() {
		// Initialize the clusters at random positions
		for (int i = 0; i < NUM_CLUSTERS; i++) {
			Cluster c = new Cluster();
			SequentialSparseVector centroidVector = generateRandomCentroidVector();
			c.setCentroid(centroidVector);
			clusters.add(c);
		}
	}

	private SequentialSparseVector generateRandomCentroidVector() {
		int items = this.sparceMatrix.columnSize();
		Random rnd = new Random();
		double[] centroid = new double[items];
		int[] indices = new int[items];
		for (int i = 0; i < items; i++) {
			centroid[i] = rnd.nextDouble()*(this.MAX_RATING.doubleValue()-this.MIN_RATING.doubleValue())+this.MIN_RATING.doubleValue();
			indices[i] = i;
		}
		
		SequentialSparseVector vect = new VectorBasedSequentialSparseVector(items, indices, centroid); 
		return vect;
	}

	public void calculate() {
		boolean finish = false;
		int iteration = 0;

		while (!finish) {
			clearClusters();
			List<SequentialSparseVector> lastCentroids = getCentroids();

			assignCluster();

			calculateCentroids();

			iteration++;

			List<SequentialSparseVector> currentCentroids = getCentroids();

			// Calculates total distance between new and old Centroids
			double distance = 0;
			for (int i = 0; i < lastCentroids.size(); i++) {
				distance += 1 - sim.getCorrelation(lastCentroids.get(i), currentCentroids.get(i));
			}
			System.out.println("#################");
			System.out.println("Iteration: " + iteration);
			System.out.println("Centroid distances: " + distance);

			if (distance == 0) {
				finish = true;
			}
		}

	}

	private void calculateCentroids() {
		int numItems = this.sparceMatrix.columnSize();
		for (Cluster cluster : clusters) {
			Map<Integer,SequentialSparseVector> clusterUsers = cluster.getUsers();
			List<Integer> itemsList = new ArrayList<Integer>();
			Map<Integer,List<Double>> values = new HashMap<Integer, List<Double>>();
			for (Integer user : clusterUsers.keySet()) {
				SequentialSparseVector ratings = clusterUsers.get(user);
				int size = ratings.getNumEntries();
				for (int i = 0; i < size; i++) {
					int index = ratings.getIndexAtPosition(i);
					if (itemsList.contains(index)) {
						values.get(index).add(ratings.getAtPosition(i));
					} else {
						List<Double> cumul = new ArrayList<Double>();
						cumul.add(ratings.getAtPosition(i));
						values.put(index, cumul);
					}
				}
			}
			int[] indices = new int[itemsList.size()];
			double[] avg_values = new double[itemsList.size()];
			for (int i = 0; i < itemsList.size(); i++) {
				indices[i] = itemsList.get(i);
				avg_values[i] = values.get(itemsList.get(i)).stream().mapToDouble(a->a).average().getAsDouble();
			}
			SequentialSparseVector new_centroid = new VectorBasedSequentialSparseVector(numItems, indices, avg_values);
			cluster.setCentroid(new_centroid);
		}

	}

	private List<SequentialSparseVector> getCentroids() {
		List<SequentialSparseVector> centroids = new ArrayList<SequentialSparseVector>();
		for (Cluster cluster : clusters) {
			SequentialSparseVector centroid = cluster.getCentroid();
			SequentialSparseVector copy = centroid.clone();
			centroids.add(copy);
		}
		return centroids;
	}

	private void clearClusters() {
		for (Cluster cluster : clusters) {
			cluster.clear();
		}
	}

	private void assignCluster() {
		double max = Double.MIN_VALUE;
		double min = max;
		int cluster = 0;
		double similarity = 0.0;

		int numUsers = this.sparceMatrix.rowSize();

		for (int i = 0; i < numUsers; i++) {
			SequentialSparseVector user = this.sparceMatrix.row(i);
			min = max;
			for (int j = 0; j < NUM_CLUSTERS; j++) {
				Cluster c = clusters.get(j);
				similarity = sim.getCorrelation(user, c.getCentroid());
				if (similarity > max) {
					max = similarity;
					cluster = j;
				}
			}

			clusters.get(cluster).addUser(user, i);
		}
	}
	
	public Map<Integer,Integer> getAssignation(){
		Map<Integer,Integer> assignment = new HashMap<Integer, Integer>();
		for (int i = 0; i < clusters.size(); i++) {
			List<Integer> users = clusters.get(i).getUsersIds();
			for (Integer user : users) {
				assignment.put(user, i);
			}
		}
		return assignment;
	}


}
