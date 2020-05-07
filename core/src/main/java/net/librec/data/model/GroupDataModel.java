/**
 * 
 */
package net.librec.data.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.convertor.GroupDataRetriever;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.splitter.GroupDataSplitter;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.util.FileUtil;

/**
 * @author Joaqui This class will be the abstract class in charge of generating
 *         the group models
 *
 */
public class GroupDataModel extends AbstractDataModel {

	private Map<Integer, Integer> Groupassignation;
	private Map<Integer, List<Integer>> Groups;
	private BiMap<String, Integer> groupMapping;
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
		String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/groupAssignation.csv";
		System.out.println("Result path is " + outputPath);
		BiMap<Integer, String> inverseUserMapping = userMapping.inverse();
		BiMap<Integer, String> inverseGroupMapping = groupMapping.inverse();
		// convert itemList to string
		StringBuilder sb = new StringBuilder();
		for (Integer userID : groupAssignation.keySet()) {
			String userId = inverseUserMapping.get(userID);
			String groupId = inverseGroupMapping.get(groupAssignation.get(userID));
			sb.append(userId).append(",").append(groupId).append("\n");
		}
		String resultData = sb.toString();
		// save resultData
		try {
			FileUtil.writeString(outputPath, resultData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
//			TODO: Add this to the configuration list of parameters
			if (!conf.getBoolean("group.external", false)) {
				DataFrame rawData = dataConvertor.getMatrix();
				SequentialAccessSparseMatrix preferenceMatrix = dataConvertor.getPreferenceMatrix();
//				TODO: Add this to the configuration list of parameters
				this.NumberOfGroups = this.conf.getInt("group.number", 10);
				int maxIterations = this.conf.getInt("kmeans.iterations", 30);
				Kmeans groupBuilder = new Kmeans(this.NumberOfGroups, rawData.getRatingScale().get(0),
						rawData.getRatingScale().get(rawData.getRatingScale().size() - 1), preferenceMatrix,
						maxIterations);
				groupBuilder.init();
				groupBuilder.calculate();
				this.Groupassignation = groupBuilder.getAssignation();
				this.Groups = groupBuilder.getGroupMapping();
//				Build the groupMapping data
				for (Integer group : this.Groups.keySet()) {
					this.groupMapping.put(Integer.toString(group), group);
				}
				if (conf.getBoolean("group.save", false)) {
					this.saveGroups();
				}
			} else {
//				TODO: Add this to the configuration list of parameters
				String groupAssignationPath = conf.get("group.external.path");
				GroupDataRetriever retriever = new GroupDataRetriever(groupAssignationPath);
				retriever.process();
				this.groupMapping = retriever.getGroupMapping();
				this.Groups = retriever.getGroups();
				this.Groupassignation = retriever.getGroupAssignation();
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
		dataSplitter = new GroupDataSplitter(this.Groupassignation, this.Groups, conf);

		dataSplitter.setDataConvertor(dataConvertor);
		dataSplitter.splitData();
//		TODO this will be the individual train and test data sets
		trainDataSet = dataSplitter.getTrainData();
		testDataSet = dataSplitter.getTestData();
//		TODO Will have to generate group train and test data sets so that when the 

	}

	public Double getGroupRating(List<Double> groupScores) {
		String model = conf.get("data.group.model", "addUtil");
		switch (model) {
		case "addUtil":
			return AdditiveUtilitarian(groupScores);
		case "leastMis":
			return LeastMisery(groupScores);
		case "mostPl":
			return MostPleasure(groupScores);
		default:
//			TODO Should rise an exception here
			return 0.0D;
		}
	}

	private static Double AdditiveUtilitarian(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a -> a).average().getAsDouble();
	}

	private static Double LeastMisery(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a -> a).min().getAsDouble();
	}
	
	private static Double MostPleasure(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a->a).max().getAsDouble();
	}

}
