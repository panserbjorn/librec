/**
 * 
 */
package net.librec.data.model;

import java.util.ArrayList;
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
import net.librec.similarity.AbstractRecommenderSimilarity;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;

/**
 * @author Joaqui
 *
 */
public class SimilarUsersGroupBuilder extends GroupBuilder {

	private SymmMatrix similarity;
	
	private BiMap<String, Integer>groupMapping ;
	
	private Map<Integer, List<Integer>> groups;

	/**
	 * 
	 */
	public SimilarUsersGroupBuilder() {
		this.groupMapping = HashBiMap.create();
		this.groups = new HashMap<Integer, List<Integer>>();
	}

	@Override
	public void setUp(DataFrame df, SequentialAccessSparseMatrix preferences) {
		super.setUp(df, preferences);
		int numUsers = preferences.rowSize();

//		TODO Need to change this so that it is not a recommender similarity. 
		AbstractRecommenderSimilarity similarityMetric = new PCCSimilarity();
		
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
					if (!shareMinimumItems(thisVector.getIndices(), thatVector.getIndices())) {
						continue;
					}
					List<double[]> commonItems = getCommonItems(thisVector, thatVector);
					double sim = pcc.correlation(commonItems.get(0), commonItems.get(1));
//					double sim = similarityMetric.getCorrelation(thisVector, thatVector);
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
		int minimumItemsShared = conf.getInt("group.ludovicos.minimumItems", 5);
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

		}
		return shared >= minimumItemsShared;
	}

	@Override
	public void generateGroups() {
		int groupSize = conf.getInt("group.ludovicos.groupSize", 2);
		int numUsers = preferences.rowSize();
		Map<Integer, List<Integer>> similarUserCache = new HashMap<Integer, List<Integer>>();
		Set<Integer> availableUsers = new HashSet<Integer>(IntStream.range(0, numUsers).boxed().collect(Collectors.toList()));
		double similarityThreshold = conf.getDouble("group.ludovicos.similThresh", 0.27D);
		for (Iterator<Integer> userIterator = availableUsers.iterator();userIterator.hasNext();) {
			Integer user = userIterator.next(); 
			List<Integer> similarUsers = getSimilarUsers(user, similarityThreshold);
			similarUserCache.put(user, similarUsers);
			if (similarUsers.size() < groupSize) {
				userIterator.remove();
			}
		}
		Set<Integer> copyAvailableUsers = new HashSet<Integer>(availableUsers);
		for (Iterator<Integer> userIterator = copyAvailableUsers.iterator();userIterator.hasNext();) {
			Integer user = userIterator.next(); 
			if (!availableUsers.contains(user) ) {
				continue;
			}
			List<Integer> similarUsers = similarUserCache.get(user);
			List<Integer> currentSimilarUsers = similarUsers.parallelStream().filter(elem -> availableUsers.contains(elem)).collect(Collectors.toList());
			if (currentSimilarUsers.size() > groupSize){
				System.out.println(currentSimilarUsers.size());
				System.out.println(groupSize);
				List<int[]> combinations = getCombinations(currentSimilarUsers,groupSize);
				System.out.println(combinations.size());
				for (int[] possibelGroup : combinations) {
					if (correctGroup(possibelGroup,availableUsers, similarityThreshold)) {
						List<Integer> groupList = new ArrayList<Integer>();
						for (int member : possibelGroup) {
							groupList.add(member);
							availableUsers.remove(member);
						}
						groupList.add(user);
						this.groups.put(groups.size(), groupList);
						break;
					}
				}
			}
			availableUsers.remove(user);
		}
	}

	private boolean correctGroup(int[] possibelGroup, Set<Integer> availableUsers, double similarityTreshold) {
		List<Integer> possibleG = new ArrayList<Integer>();
		for (int i : possibelGroup) {
			possibleG.add(i);
		}
		List<int[]> pairs = getCombinations(possibleG, 2);
		
		for(int[] pair : pairs) {
			int user1 = pair[0];
			int user2 = pair[1];
			double sim = similarity.get(user1, user2);
			if (sim <= similarityTreshold) {
				return false;
			}
		}
		
		return true;
	}

	private List<int[]> getCombinations(List<Integer> currentSimilarUsers, int groupSize) {
		List<int[]> subsets = new ArrayList<>();

		int[] s = new int[groupSize];                  // here we'll keep indices 
		                                       // pointing to elements in input array

		if (groupSize <= currentSimilarUsers.size()) {
		    // first index sequence: 0, 1, 2, ...
		    for (int i = 0; (s[i] = i) < groupSize - 1; i++);  
		    subsets.add(getSubset(currentSimilarUsers, s));
		    for(;;) {
		        int i;
		        // find position of item that can be incremented
		        for (i = groupSize - 1; i >= 0 && s[i] == currentSimilarUsers.size() - groupSize + i; i--); 
		        if (i < 0) {
		            break;
		        }
		        s[i]++;                    // increment this item
		        for (++i; i < groupSize; i++) {    // fill up remaining items
		            s[i] = s[i - 1] + 1; 
		        }
		        subsets.add(getSubset(currentSimilarUsers, s));
		    }
		}
		return subsets;
	}

	private int[] getSubset(List<Integer> currentSimilarUsers, int[] s) {
		int[] result = new int[s.length]; 
	    for (int i = 0; i < s.length; i++) 
	        result[i] = currentSimilarUsers.get(s[i]);
	    return result;
	}

	private List<Integer> getSimilarUsers(Integer user, double similarityThreshold) {
		Map<Integer, Double> row = this.similarity.row(user);
		List<Integer> similarUsers = new ArrayList<Integer>();
		for (Integer otherUser : row.keySet()) {
			if (row.get(otherUser) >= similarityThreshold) {
				similarUsers.add(otherUser);
			}
		}
		return similarUsers;
	}

	@Override
	public Map<Integer, Integer> getAssignation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Integer, List<Integer>> getGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BiMap<String, Integer> getGroupMapping() {
		// TODO Auto-generated method stub
		return null;
	}

}
