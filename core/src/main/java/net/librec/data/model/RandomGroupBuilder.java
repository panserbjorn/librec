/**
 * 
 */
package net.librec.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.librec.math.structure.SequentialSparseVector;

/**
 * @author Joaqui
 *
 */
public class RandomGroupBuilder extends GroupBuilder {
	
	private BiMap<String, Integer>groupMapping ;
	
	private Map<Integer, List<Integer>> groups;
	
	private List<Map<Integer, String>> groupStatistics;

	/**
	 * 
	 */
	public RandomGroupBuilder() {
		super();
		groups = new HashMap<Integer, List<Integer>>();
		this.groupMapping = HashBiMap.create();
		groupStatistics = new ArrayList<Map<Integer,String>>();
	}

	@Override
	public void generateGroups() {
		int numUsers = preferences.rowSize();
		int groupSize = conf.getInt("group.random.groupSize", 10);
		List<Integer> userindices = IntStream.range(0, numUsers).boxed().collect(Collectors.toList());
		java.util.Collections.shuffle(userindices);
		Map<Integer, String> groupStats = new HashMap<Integer, String>();
		PearsonsCorrelation pcc = new PearsonsCorrelation();
		for (int i = 0; i < userindices.size(); i+=groupSize) {
			int groupIndex = groups.size();
			groupMapping.put(Integer.toString(groupIndex), groupIndex);
			List<Integer> groupMembers =userindices.subList(i, Math.min(userindices.size(), i+groupSize));
			double similaritiesAcumulator = 0.0;
			for (int i1 = 0; i1 < groupMembers.size()-1; i1++) {
				for (int j = i1+1 ; j < groupMembers.size(); j++) {
					Integer thisUser = groupMembers.get(i1);
					Integer thatUser = groupMembers.get(j);
					SequentialSparseVector thisVector = preferences.row(thisUser);
					SequentialSparseVector thatVector = preferences.row(thatUser);
					List<double[]> commonItemsList = getCommonItems(thisVector, thatVector);
					double sim = 0.0;
					if (commonItemsList.get(0).length >= 5) {
						sim = pcc.correlation(commonItemsList.get(0), commonItemsList.get(1));
					}
					similaritiesAcumulator+=sim;
				}
			}
			double totalNumberParis = (groupSize*(groupSize-1))/2;
			groupStats.put(groupIndex,Double.toString(similaritiesAcumulator/totalNumberParis));
			groups.put(groupIndex, groupMembers);
		}	
		this.groupStatistics.add(groupStats);
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
	public boolean isExhaustive() {
		return true;
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
	@Override
	public List<Map<Integer, String>> getGroupStatistics() {
		return this.groupStatistics;
	}
}
