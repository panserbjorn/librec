/**
 * 
 */
package net.librec.data.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.util.FileUtil;

/**
 * @author Joaqui
 *
 */
public class RingGroupDataModel extends GroupDataModel {

	private Map<Integer, List<List<Integer>>> groupsRings;
	private Map<Integer, Integer> ringAssignation;

	/**
	 * 
	 */
	public RingGroupDataModel() {
		this.ringAssignation = new HashMap<Integer, Integer>();
		this.groupsRings = new HashMap<Integer, List<List<Integer>>>();
	}

	/**
	 * @param conf
	 */
	public RingGroupDataModel(Configuration conf) {
		super(conf);
		this.ringAssignation = new HashMap<Integer, Integer>();
		this.groupsRings = new HashMap<Integer, List<List<Integer>>>();
	}

	private void saveRings() {
		String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/ringAssignation"
				+ Integer.toString(NumberOfGroups) + ".csv";
		BiMap<String, Integer> userMapping = this.getUserMappingData();
		BiMap<Integer, String> inverseUserMapping = userMapping.inverse();
		StringBuilder sb = new StringBuilder();
		for (Integer userID : this.ringAssignation.keySet()) {
			String userId = inverseUserMapping.get(userID);
			sb.append(userId).append(",").append(ringAssignation.get(userID).toString()).append("\n");
		}
		String resultData = sb.toString();
		try {
			FileUtil.writeString(outputPath, resultData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void BuildGroups() throws LibrecException {
		super.BuildGroups();
		Integer numberRings = conf.getInt("ring.number", 5);
		Map<Integer, String> distancesString = this.userStatistics.get(0);
		Map<Integer, Double> distances = new HashMap<Integer, Double>();
		distancesString.forEach((k, v) -> distances.put(k, new Double(v)));
		for (Integer group : this.Groups.keySet()) {
			List<List<Integer>> ringstructures = new ArrayList<List<Integer>>();
			for (int i = 0; i < numberRings; i++) {
				ringstructures.add(new ArrayList<Integer>());
			}
			List<Integer> members = this.Groups.get(group);
			Map<Integer, Double> groupDistances = new HashMap<Integer, Double>();
			members.forEach(member -> groupDistances.put(member, distances.get(member)));
			Double max = Collections.max(groupDistances.values());
			Double min = Collections.min(groupDistances.values());
			Double binsize = (max - min) / numberRings;

			for (Integer member : members) {
				int bin = (int) ((distances.get(member) - min) / binsize);
				if (bin == numberRings) {
					bin = numberRings-1;
				}
				if (bin < 0 || bin > numberRings) {
					throw new LibrecException("Wrong ring assignation");
				}
				ringstructures.get(bin).add(member);
				ringAssignation.put(member, bin);
			}
			this.groupsRings.put(group, ringstructures);
		}
		if (conf.getBoolean("rings.save", false)) {
			this.saveRings();
		}
	}

	@Override
	public ArrayList<KeyValue<Integer, Double>> getGroupModeling(
			Map<Integer, List<KeyValue<Integer, Double>>> singleGroupRatings, Integer group) {
		List<List<Integer>> groupRings = this.groupsRings.get(group);
		int[] rings = conf.getInts("rings.use");
		List<Integer> ringList;
		if (rings != null) {
			ringList = Arrays.stream(rings).boxed().collect(Collectors.toList());
		} else {
			ringList = IntStream.rangeClosed(0, groupRings.size() - 1).boxed().collect(Collectors.toList());
		}
		Set<Integer> includedUsers = new HashSet<Integer>();
		for (Integer ring : ringList) {
			includedUsers.addAll(groupRings.get(ring));
		}
		Map<Integer, List<KeyValue<Integer, Double>>> filteredByRingRatings = singleGroupRatings.entrySet().stream()
				.filter(entry -> includedUsers.contains(entry.getKey()))
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		return super.getGroupModeling(filteredByRingRatings, group);
	}

}
