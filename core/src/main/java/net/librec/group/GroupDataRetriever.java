/**
 * 
 */
package net.librec.group;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.librec.math.structure.DataFrame;

/**
 * @author Joaqui
 *
 */
public class GroupDataRetriever {

	private String path;

	private BiMap<String, Integer> groupMapping;

	private Map<Integer, Integer> assignations;

	private Map<Integer, List<Integer>> groups;

	private BufferedReader br;

	/**
	 * 
	 */
	public GroupDataRetriever(String path) {
		this.path = path;
		this.assignations = new HashMap<Integer, Integer>();
		this.groups = new HashMap<Integer, List<Integer>>();
		this.groupMapping = HashBiMap.create();
	}

	public void process() {
//		TODO terminar este m�todo
		String line = "";
		try {
			br = new BufferedReader(new FileReader(this.path));
			while ((line = br.readLine()) != null) {
//				TODO the splitting could be in the configuration. 
				String[] splitted = line.split(",");
				String user = splitted[0];
				String group = splitted[1];
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
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

}
