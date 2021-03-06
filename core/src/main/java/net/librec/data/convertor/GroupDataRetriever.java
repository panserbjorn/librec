/**
 * 
 */
package net.librec.data.convertor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.librec.data.model.GroupBuilder;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.SequentialAccessSparseMatrix;

/**
 * @author Joaqui
 *
 */
public class GroupDataRetriever extends GroupBuilder{

	private String path;

	private BiMap<String, Integer> groupMapping;

	private Map<Integer, Integer> assignations;

	private Map<Integer, List<Integer>> groups;
	
	private List<Map<Integer, String>> memberStatistics;

	private BufferedReader br;
	
	@Override
	public boolean isExhaustive() {
		boolean exhaustive = conf.getBoolean("group.external.exhaustive", false);
		return exhaustive;
	}
	
	@Override
	public void setUp(DataFrame df, SequentialAccessSparseMatrix preferences) {
		super.setUp(df, preferences);
		this.path = conf.get("group.external.path");
		this.assignations = new HashMap<Integer, Integer>();
		this.groups = new HashMap<Integer, List<Integer>>();
		this.groupMapping = HashBiMap.create();
		this.memberStatistics = new ArrayList<Map<Integer,String>>();
	}

	public void process() {
		String line = "";
		try {
			br = new BufferedReader(new FileReader(this.path));
			boolean containsStatistics = false;
			boolean firstLine = true;
			while ((line = br.readLine()) != null) {
				String[] splitted = line.split(",");
				String user = splitted[0];
				String group = splitted[1];
				if (firstLine & splitted.length > 2) {
					containsStatistics = true;
					for (int i = 2; i < splitted.length; i++) {
						this.memberStatistics.add(new HashMap<Integer, String>());
					}
				}
				Integer groupMap = 0;
				Integer userMap = DataFrame.getUserIds().get(user);
				if (!groupMapping.containsKey(group)) {
					groupMap = groupMapping.size();
					groupMapping.put(group, groupMap);
					groups.put(groupMap, new ArrayList<Integer>());
				} else {
					groupMap = groupMapping.get(group);
				}
				groups.get(groupMap).add(userMap);
				getGroupAssignation().put(userMap, groupMap);
				
				if (containsStatistics) {
					for (int i = 2; i < splitted.length; i++) {
						this.memberStatistics.get(i-2).put(userMap, splitted[i]);
					}
				}
				
				firstLine = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Map<Integer, Integer> getGroupAssignation() {
		return this.assignations;
	}

	public Map<Integer, List<Integer>> getGroups() {
		return this.groups;
	}

	public BiMap<String, Integer> getGroupMapping() {
		return this.groupMapping;
	}

	@Override
	public void generateGroups() {
		this.process();
	}

	@Override
	public Map<Integer, Integer> getAssignation() {
		return this.getGroupAssignation();
	}
	
	@Override
	public List<Map<Integer, String>> getMemberStatistics() {
		return this.memberStatistics;
	}

}
