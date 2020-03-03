/**
 * 
 */
package groupRec;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
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

	public Kmeans(int nUM_CLUSTERS, Number mIN_RATING, Number mAX_RATING, SequentialAccessSparseMatrix sparceMatrix,
			List<Cluster> clusters) {
		super();
		NUM_CLUSTERS = nUM_CLUSTERS;
		MIN_RATING = mIN_RATING;
		MAX_RATING = mAX_RATING;
		this.sparceMatrix = sparceMatrix;
		this.clusters = clusters;
	}

	public void init() {
		// Initialize the clusters at random positions
		for (int i = 0; i < NUM_CLUSTERS; i++) {
			Cluster c = new Cluster();

			List<Number> randomSample = generateRandomCentroid(this.MIN_RATING, this.MAX_RATING);
			c.setCentroid(randomSample);
			clusters.add(c);
		}
	}

	private List<Number> generateRandomCentroid(Number min_RATING2, Number max_RATING2) {
		// TODO Auto-generated method stub
		return null;
	}

	public void calculate() {
		boolean finish = false;
		int iteration = 0;

		while (!finish) {
			clearClusters();
			List<List<Number>> lastCentroids = getCentroids();

			assignCluster();

			calculateCentroids();

			iteration++;

			List<List<Number>> currentCentroids = getCentroids();

			// Calculates total distance between new and old Centroids
			double distance = 0;
			for (int i = 0; i < lastCentroids.size(); i++) {
				distance += 1 - sim.getSimilarity(lastCentroids.get(i), currentCentroids.get(i));
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
			List<List<Number>> clusterUsers = cluster.getUsers();
			List<Number> newCentroid = new ArrayList<Number>(numItems);
			for (List<Number> user : clusterUsers) {
				for (int i = 0; i < numItems; i++) {
					newCentroid.set(i, newCentroid.get(i).floatValue() + user.get(i).floatValue());
				}
			}
			for (int i = 0; i < numItems; i++) {
				newCentroid.set(i, newCentroid.get(i).floatValue()/clusterUsers.size());
			}
		}
		
	}

	private List<List<Number>> getCentroids() {
		List<List<Number>> centroids = new ArrayList<List<Number>>(NUM_CLUSTERS);
		for (Cluster cluster : clusters) {
			List<Number> centroid = cluster.getCentroid();
			List<Number> copy = new ArrayList<Number>(centroid);
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
        double max = Double.MAX_VALUE;
        double min = max; 
        int cluster = 0;                 
        double distance = 0.0; 
        
        int numUsers = this.sparceMatrix.rowSize();
        
        for (int i = 0; i < numUsers; i++) {
        	List<Number> user = transformVectorToList(this.sparceMatrix.row(i));
        	min = max;
            for(int j = 0; j < NUM_CLUSTERS; j++) {
            	Cluster c = clusters.get(j);
                distance =  1.0 - sim.getSimilarity(user, c.getCentroid());
                if(distance < min){
                    min = distance;
                    cluster = j;
                }
            }
            
            clusters.get(cluster).addUser(user);
        }
    }

	private List<Number> transformVectorToList(SequentialSparseVector row) {
		// TODO Auto-generated method stub
		return null;
	}

}
