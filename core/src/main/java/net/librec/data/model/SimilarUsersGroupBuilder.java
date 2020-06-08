/**
 * 
 */
package net.librec.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.librec.math.structure.DataFrame;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class SimilarUsersGroupBuilder extends GroupBuilder {

	private SymmMatrix similarity;
	
	private BiMap<String, Integer>groupMapping ;
	
	private Map<Integer, List<Integer>> groups;
	
	private List<Map<Integer, String>> groupStatistics;

	/**
	 * 
	 */
	public SimilarUsersGroupBuilder() {
		this.groupMapping = HashBiMap.create();
		this.groups = new HashMap<Integer, List<Integer>>();
		this.groupStatistics = new ArrayList<Map<Integer,String>>();
	}

	@Override
	public void setUp(DataFrame df, SequentialAccessSparseMatrix preferences) {
		super.setUp(df, preferences);
		int numUsers = preferences.rowSize();
		
		PearsonsCorrelation pcc = new PearsonsCorrelation();

		similarity = new SymmMatrix(numUsers);
		List<Integer> indexList = new ArrayList<>();
		for (int index = 0; index < numUsers; index++) {
			indexList.add(index);
		}

		indexList.parallelStream().forEach((Integer thisIndex) -> {
			SequentialSparseVector thisVector = preferences.row(thisIndex);
			if (thisVector.getNumEntries() >= 5) {
				for (int thatIndex = thisIndex + 1; thatIndex < numUsers; thatIndex++) {
					SequentialSparseVector thatVector = preferences.row(thatIndex);
					if (thatVector.getNumEntries() < 5) {
						continue;
					}
//					TODO I could change this for checking that the length of the first list in the common items is greater than 5
					if (!shareMinimumItems(thisVector.getIndices(), thatVector.getIndices())) {
						continue;
					}
					List<double[]> commonItems = getCommonItems(thisVector, thatVector);
					double sim = pcc.correlation(commonItems.get(0), commonItems.get(1));
					if (!Double.isNaN(sim) && sim != 0.0) {
						similarity.set(thisIndex, thatIndex, sim);
					}
				}
			}
		});
	}
	
	private List<double[]> getCommonItems (SequentialSparseVector thisVector, SequentialSparseVector thatVector) {
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
        ArrayList<double[]> returnList = new ArrayList<double[]>();
        double[] thisArray = new double[thisList.size()];
        double[] thatArray = new double[thatList.size()];
        for(int i = 0; i < thisArray.length; i++) {
        	thisArray[i] = thisList.get(i);
        	thatArray[i] = thatList.get(i);
        }
        returnList.add(thisArray);
        returnList.add(thatArray);
        return returnList;
	}

	private boolean shareMinimumItems(int[] indices, int[] indices2) {
		int minimumItemsShared = conf.getInt("group.similar.minimumItems", 5);
		int shared = 0;
		int index1 = 0;
		int index2 = 0;
		while (index1 < indices.length & index2 < indices2.length) {
			int item1 = indices[index1];
			int item2 = indices2[index2];
			if (item1 < item2) {
				index1++;
			} else if (item2 < item1) {
				index2++;
			} else {
				index1++;
				index2++;
				shared++;
			}
			if (shared >= minimumItemsShared) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void generateGroups() {
		int groupSize = conf.getInt("group.similar.groupSize", 2);
		int numUsers = preferences.rowSize();
		Map<Integer, List<KeyValue<Integer, Double>>> similarUserCache = new HashMap<Integer, List<KeyValue<Integer, Double>>>();
		Set<Integer> availableUsers = new HashSet<Integer>(IntStream.range(0, numUsers).boxed().collect(Collectors.toList()));
		double similarityThreshold = conf.getDouble("group.similar.similThresh", 0.27D);
		for (Iterator<Integer> userIterator = availableUsers.iterator();userIterator.hasNext();) {
			Integer user = userIterator.next(); 
			List<KeyValue<Integer, Double>> similarUsers = getSimilarUsers(user, similarityThreshold);
			similarUserCache.put(user, similarUsers);
			if (similarUsers.size() < groupSize) {
				userIterator.remove();
			}
		}
		List<Integer> copyAvailableUsers = new ArrayList<Integer>(availableUsers);
		Collections.shuffle(copyAvailableUsers);
		Map<Integer, String> groupStats = new HashMap<Integer, String>();
		for (Iterator<Integer> userIterator = copyAvailableUsers.iterator(); userIterator.hasNext();) {
			Integer user = userIterator.next(); 
			if (!availableUsers.contains(user) ) {
				continue;
			}
			List<KeyValue<Integer, Double>> similarUsers = similarUserCache.get(user);
			List<KeyValue<Integer, Double>> currentSimilarUsers = similarUsers.parallelStream().filter(elem -> availableUsers.contains(elem.getKey())).collect(Collectors.toList());
			if (currentSimilarUsers.size() > groupSize){
				List<Integer> currentGroup = new ArrayList<Integer>();
				currentGroup.add(user);
				for (KeyValue<Integer, Double> otherUser : currentSimilarUsers) {
					if (compatibleWithGroup(currentGroup,otherUser, similarityThreshold)) {
						currentGroup.add(otherUser.getKey());
					}
					if (currentGroup.size() == groupSize) {
						break;
					}
				}
				if (currentGroup.size() == groupSize) {
					double similaritiesAcumulator = 0.0;
					for (int i = 0; i < currentGroup.size()-1; i++) {
						for (int j = i+1 ; j < currentGroup.size(); j++) {
							Integer thisUser = currentGroup.get(i);
							Integer thatUser = currentGroup.get(j);
							double sim = similarity.get(thisUser, thatUser);
							similaritiesAcumulator+=sim;
						}
					}
					int groupNumber = groups.size();
					this.groupMapping.put(Integer.toString(groupNumber), groupNumber);
					this.groups.put(groupNumber, currentGroup);
					double totalNumberParis = (groupSize*(groupSize-1))/2;
					groupStats.put(groupNumber,Double.toString(similaritiesAcumulator/totalNumberParis));
					for (Integer member : currentGroup) {
						availableUsers.remove(member);
					}
				}
			}
		}
		this.groupStatistics.add(groupStats);
	}

	private boolean compatibleWithGroup(List<Integer> currentGroup, KeyValue<Integer, Double> otherUser, double similarityThreshold) {
		for (Integer user : currentGroup) {
			if (similarity.get(user, otherUser.getKey()) < similarityThreshold) {
				return false;
			}
		}
		return true;
	}

	private List<KeyValue<Integer, Double>> getSimilarUsers(Integer user, double similarityThreshold) {
		Map<Integer, Double> row = this.similarity.row(user);
		List<KeyValue<Integer, Double>> similarUsers = new ArrayList<KeyValue<Integer, Double>>();
		for (Integer otherUser : row.keySet()) {
			if (otherUser != user & row.get(otherUser) >= similarityThreshold) {
				similarUsers.add(new KeyValue<Integer, Double>(otherUser, row.get(otherUser)));
			}
		}
		Collections.sort(similarUsers, Map.Entry.comparingByValue());
		return similarUsers;
	}

	@Override
	public Map<Integer, Integer> getAssignation() {
		Map<Integer, Integer> groupAssignation = new HashMap<Integer, Integer>();
		for (Integer group : groups.keySet()) {
			for (Integer member : groups.get(group)) {
				groupAssignation.put(member, group);
			}
		}
		return groupAssignation;
	}

	@Override
	public Map<Integer, List<Integer>> getGroups() {
		return this.groups;
	}

	@Override
	public BiMap<String, Integer> getGroupMapping() {
		return this.groupMapping;
	}
	
	@Override
	public List<Map<Integer, String>> getGroupStatistics() {
		return this.groupStatistics;
	}

}
