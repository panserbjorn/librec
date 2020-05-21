/**
 * 
 */
package net.librec.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.librec.conf.Configuration;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.VectorBasedSequentialSparseVector;

/**
 * @author Joaqui
 * 
 *         This class performs the KMeans clustering algorithm
 */
public class Kmeans extends GroupBuilder{
	
	private int MAX_ITERATION = 30;

	private int NUM_CLUSTERS = 20;

	private Double MIN_RATING = 0.0;

	private Double MAX_RATING = 5.0;

	private SequentialAccessSparseMatrix sparceMatrix;
	
	private BiMap<String, Integer>groupMapping ;

	private List<Cluster> clusters;

	
	@Override
	public void setUp(DataFrame df, SequentialAccessSparseMatrix preferences) {
		super.setUp(df, preferences);
		this.NUM_CLUSTERS = this.conf.getInt("group.number", 10);
		this.MIN_RATING = df.getRatingScale().get(0);
		this.MAX_RATING = df.getRatingScale().get(df.getRatingScale().size() - 1);
		this.MAX_ITERATION = this.conf.getInt("kmeans.iterations", 30);
		this.groupMapping = HashBiMap.create();
		this.sparceMatrix = preferences;
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
				distance += this.Distance(lastCentroids.get(i), currentCentroids.get(i));
			}
			System.out.println("#################");
			System.out.println("Iteration: " + iteration);
			System.out.println("Centroid distances: " + distance);

			if (distance == 0 || iteration > MAX_ITERATION) {
				finish = true;
			}
		}

	}

	private void calculateCentroids() {
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
						itemsList.add(index);
						List<Double> cumul = new ArrayList<Double>();
						cumul.add(ratings.getAtPosition(i));
						values.put(index, cumul);
					}
				}
			}
			int[] indices = new int[itemsList.size()];
			double[] avg_values = new double[itemsList.size()];
			Collections.sort(itemsList);
			for (int i = 0; i < itemsList.size(); i++) {
				indices[i] = itemsList.get(i);
				avg_values[i] = values.get(itemsList.get(i)).stream().mapToDouble(a->a).average().getAsDouble();
			}
			cluster.moveCentroid(indices, avg_values);
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
		
		double max = Double.MAX_VALUE;
		double min = max;
		int cluster = 0;
		double distance = Double.MAX_VALUE;

		int numUsers = this.sparceMatrix.rowSize();

		for (int i = 0; i < numUsers; i++) {
			SequentialSparseVector user = this.sparceMatrix.row(i);
			min = max;
			for (int j = 0; j < NUM_CLUSTERS; j++) {
				Cluster c = clusters.get(j);
				distance = this.Distance(user, c.getCentroid());
				if (distance == 0.0 ) {
					System.out.println("ZERO");
				}
				if (Double.isNaN(distance)) {
					distance = Double.MAX_VALUE;
				}
				if (distance < min) {
					min = distance;
					cluster = j;
				}
			}

			clusters.get(cluster).addUser(user, i, min);
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
	
	private double Distance(SequentialSparseVector thisVector, SequentialSparseVector thatVector) {
		
		List<Double> thisList = new ArrayList<>();
        List<Double> thatList = new ArrayList<>();

        int thisPosition = 0, thatPosition = 0;
        int thisSize = thisVector.getNumEntries(), thatSize = thatVector.getNumEntries();
        int thisIndex, thatIndex;
        while (thisPosition < thisSize && thatPosition < thatSize) {
            thisIndex = thisVector.getIndexAtPosition(thisPosition);
            thatIndex = thatVector.getIndexAtPosition(thatPosition);
            if (thisIndex == thatIndex) {
                thisList.add(thisVector.getAtPosition(thisPosition));
                thatList.add(thatVector.getAtPosition(thatPosition));
                thisPosition++;
                thatPosition++;
            } else if (thisIndex > thatIndex) {
                thatPosition++;
            } else {
                thisPosition++;
            }
        }
        if (thisList.isEmpty()) {
        	return Double.NaN;
        }
		double sum = 0.0;
		for (int i = 0; i < thisList.size(); i++) {
			sum += Math.pow((thisList.get(i)-thatList.get(i)), 2);
		}
		if(thisList.size() < this.sparceMatrix.columnSize() ) {
			sum+=Math.pow(MAX_RATING-MIN_RATING, 2)*(this.sparceMatrix.columnSize()-thisList.size());
		}
		return Math.sqrt(sum);
	}
	
	
	public Map<Integer, List<Integer>> getGroups(){
		Map<Integer,List<Integer>> groups = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < clusters.size(); i++) {
			groups.put(i, clusters.get(i).getUsersIds());
		}
		return groups;
	}

	public Map<Integer, String> getUsersDistances() {
		Map<Integer, String> userDistances = new HashMap<Integer, String>();
		for (int i = 0; i < clusters.size(); i++) {
			Cluster clust = clusters.get(i);
			userDistances.putAll(clust.getUserDistances());
		}
		return userDistances;
	}

	@Override
	public void generateGroups() {
		this.init();
		this.calculate();
		for (int i = 0 ; i < this.clusters.size(); i++) {
			this.groupMapping.put(Integer.toString(i), i);
		}
	}

	@Override
	public BiMap<String, Integer> getGroupMapping() {
		return this.groupMapping;
	}

	@Override
	public List<Map<Integer, String>> getMemberStatistics() {
		List<Map<Integer, String>> statistics = new ArrayList<Map<Integer,String>>();
		statistics.add(this.getUsersDistances());
		return statistics;
	}

}
