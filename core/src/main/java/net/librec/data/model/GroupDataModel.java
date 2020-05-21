/**
 * 
 */
package net.librec.data.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.convertor.GroupDataRetriever;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.splitter.GroupDataSplitter;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.recommender.item.KeyValue;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;
import net.librec.util.ReflectionUtil;

/**
 * @author Joaqui This class will be the abstract class in charge of generating
 *         the group models
 *
 */
public abstract class GroupDataModel extends AbstractDataModel {

	private Map<Integer, Integer> Groupassignation;
	private Map<Integer, List<Integer>> Groups;
	private BiMap<String, Integer> groupMapping;
	private List<Map<Integer, String>> userStatistics;
	private int NumberOfGroups;

	/**
	 * Empty constructor.
	 */
	public GroupDataModel() {
		this.groupMapping = HashBiMap.create();
	}

	public GroupDataModel(Configuration conf) {
		this.conf = conf;
		this.groupMapping = HashBiMap.create();
	}

	public BiMap<String, Integer> getGroupMappingdata() {
		return this.groupMapping;
	}

	void saveGroups() {
		Map<Integer, Integer> groupAssignation = this.getGroupAssignation();
		BiMap<String, Integer> userMapping = this.getUserMappingData();
		BiMap<String, Integer> groupMapping = this.getGroupMappingdata();
		String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/groupAssignation"
				+ Integer.toString(NumberOfGroups) + ".csv";
		System.out.println("Result path is " + outputPath);
		BiMap<Integer, String> inverseUserMapping = userMapping.inverse();
		BiMap<Integer, String> inverseGroupMapping = groupMapping.inverse();
		// convert itemList to string
		StringBuilder sb = new StringBuilder();
		SequentialAccessSparseMatrix preferenceMatrix = dataConvertor.getPreferenceMatrix();
		for (Integer userID : groupAssignation.keySet()) {
			String userId = inverseUserMapping.get(userID);
			String groupId = inverseGroupMapping.get(groupAssignation.get(userID));
			StringJoiner joiner = new StringJoiner(",");
			for (Map<Integer,String> stat : this.userStatistics) {
				joiner.add(stat.get(userID)); 	
			}
			String numRatings = Integer.toString(preferenceMatrix.row(userID).getIndices().length);
			sb.append(userId).append(",").append(groupId).append(",");
			if (!joiner.toString().isEmpty()) {
				sb.append(joiner.toString()).append(",");
			}
			sb.append(numRatings).append("\n");
		}
		String resultData = sb.toString();
		// save resultData
		try {
			FileUtil.writeString(outputPath, resultData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void buildConvert() throws LibrecException {
		String[] inputDataPath = conf.get(Configured.CONF_DATA_INPUT_PATH).trim().split(":");
		for (int i = 0; i < inputDataPath.length; i++) {
			inputDataPath[i] = conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + inputDataPath[i];
		}
		String dataColumnFormat = conf.get(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
		dataConvertor = new TextDataConvertor(dataColumnFormat, inputDataPath, conf.get("data.convert.sep", "[\t;, ]"));
		try {
			dataConvertor.processData();
			DataFrame rawData = dataConvertor.getMatrix();
			SequentialAccessSparseMatrix preferenceMatrix = dataConvertor.getPreferenceMatrix();
			
			Class<? extends GroupBuilder> builderLCass;
			try {
				builderLCass = (Class<? extends GroupBuilder>) DriverClassUtil
						.getClass(conf.get("group.builder","kmeans"));
			} catch (ClassNotFoundException e) {
				throw new LibrecException("Group Builder class not found");
			}
			
			GroupBuilder groupBuilder = ReflectionUtil.newInstance((Class<GroupBuilder>) builderLCass, conf);
			groupBuilder.setUp(rawData, preferenceMatrix);
			groupBuilder.generateGroups();
			this.groupMapping = groupBuilder.getGroupMapping();
			this.Groups = groupBuilder.getGroups();
			this.userStatistics = groupBuilder.getMemberStatistics();
			this.Groupassignation = groupBuilder.getAssignation();
			this.NumberOfGroups= Groups.size();
			if (conf.getBoolean("group.save", false)) {
				this.saveGroups();
			}
			LOG.info("Groups Built sucessfully:");
			for (Integer group : this.Groups.keySet()) {
				LOG.info(group.toString() + ": " + Integer.toString(this.Groups.get(group).size()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load data model.
	 *
	 * @throws LibrecException if error occurs during loading
	 */
	@Override
	public void loadDataModel() throws LibrecException {

	}

	/**
	 * Save data model.
	 *
	 * @throws LibrecException if error occurs during saving
	 */
	@Override
	public void saveDataModel() throws LibrecException {

	}

	/**
	 * Get datetime data set.
	 *
	 * @return the datetime data set of data model.
	 */
	@Override
	public DataSet getDatetimeDataSet() {
		return dataConvertor.getDatetimeMatrix();
	}

	/***
	 * Get Group Assignation
	 * 
	 * @return the Group Assignation computed after the buildConvert step
	 */
	public Map<Integer, Integer> getGroupAssignation() {
		return this.Groupassignation;
	}

	public Map<Integer, List<Integer>> getGroups() {
		return this.Groups;
	}

	@Override
	protected void buildSplitter() throws LibrecException {
//		TODO Change this so that it can use any data splitter
		dataSplitter = new GroupDataSplitter(this.Groupassignation, this.Groups, conf);

		dataSplitter.setDataConvertor(dataConvertor);
		dataSplitter.splitData();
		trainDataSet = dataSplitter.getTrainData();
		testDataSet = dataSplitter.getTestData();

	}

	protected static Map<Integer, List<Double>> fromMemberToItemScores(
			Map<Integer, List<KeyValue<Integer, Double>>> groupScores) {
		Map<Integer, List<Double>> groupScoresByItem = new HashMap<Integer, List<Double>>();

		for (Entry<Integer, List<KeyValue<Integer, Double>>> member : groupScores.entrySet()) {
			for (KeyValue<Integer, Double> rating : member.getValue()) {
				if (!groupScoresByItem.containsKey(rating.getKey())) {
					groupScoresByItem.put(rating.getKey(), new ArrayList<Double>());
				}
				groupScoresByItem.get(rating.getKey()).add(rating.getValue());
			}
		}
		return groupScoresByItem;
	}

	protected static List<List<KeyValue<Integer, Double>>> fromMemberMapToList(
			Map<Integer, List<KeyValue<Integer, Double>>> groupScores) {
		return groupScores.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
	}

	public abstract ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings);

	public Table<Integer, Integer, Double> getGroupRatings(SequentialAccessSparseMatrix targetDataset) {
		Table<Integer, Integer, Double> groupRatings = HashBasedTable.create();
		for (Integer group : this.Groups.keySet()) {
			Map<Integer, List<KeyValue<Integer, Double>>> groupsRatings = new HashMap<Integer, List<KeyValue<Integer, Double>>>();
			List<Integer> groupMembers = this.Groups.get(group);
			for (Integer member : groupMembers) {
				List<KeyValue<Integer, Double>> memberRatings = new ArrayList<KeyValue<Integer, Double>>();
				SequentialSparseVector row = targetDataset.row(member);
				int[] itemsRatedByUser = row.getIndices();
				for (int i = 0; i < itemsRatedByUser.length; i++) {
					Integer item = itemsRatedByUser[i];
					memberRatings.add(new KeyValue<Integer, Double>(item, row.getAtPosition(i)));
				}
				if (!memberRatings.isEmpty()) {
					groupsRatings.put(member, memberRatings);
				}
			}
			List<KeyValue<Integer, Double>> groupScores = computeGroupModel(groupsRatings);
			for (KeyValue<Integer, Double> item : groupScores) {
				groupRatings.put(group, item.getKey(), item.getValue());
			}
		}
		return groupRatings;
	}

}
