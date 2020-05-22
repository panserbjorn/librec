/**
 * 
 */
package net.librec.data.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * @author Joaqui
 *
 */
public class RandomGroupBuilder extends GroupBuilder {
	
	private BiMap<String, Integer>groupMapping ;
	
	private Map<Integer, List<Integer>> groups;

	/**
	 * 
	 */
	public RandomGroupBuilder() {
		super();
		groups = new HashMap<Integer, List<Integer>>();
		this.groupMapping = HashBiMap.create();
	}

	@Override
	public void generateGroups() {
		int numUsers = preferences.rowSize();
		int groupSize = conf.getInt("group.random.groupSize", 10);
		List<Integer> userindices = IntStream.range(0, numUsers).boxed().collect(Collectors.toList());
		java.util.Collections.shuffle(userindices);
		for (int i = 0; i < userindices.size(); i+=groupSize) {
			int groupIndex = groups.size();
			groupMapping.put(Integer.toString(groupIndex), groupIndex);
			groups.put(groupIndex, userindices.subList(i, Math.min(userindices.size(), i+groupSize)));
		}	
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
}
