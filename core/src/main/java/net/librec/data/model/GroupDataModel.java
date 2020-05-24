/**
 * 
 */
package net.librec.data.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.DataSplitter;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.model.group.GroupModeling;
import net.librec.data.splitter.GroupDataSplitter;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.MatrixEntry;
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
public class GroupDataModel extends AbstractDataModel {

	private Map<Integer, Integer> Groupassignation;
	private Map<Integer, List<Integer>> Groups;
	private BiMap<String, Integer> groupMapping;
	private List<Map<Integer, String>> userStatistics;
	private int NumberOfGroups;
	private boolean exhaustiveGroups = false;
	
	private GroupModeling gp = null;

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
		if (null == gp) {
			Class<? extends GroupModeling> groupModelingClass;
			try {
				groupModelingClass = (Class<? extends GroupModeling>) DriverClassUtil
						.getClass(conf.get("group.model","addUtil"));
			} catch (ClassNotFoundException e) {
				throw new LibrecException(e);
			}
			
			this.gp = ReflectionUtil.newInstance((Class<GroupModeling>) groupModelingClass, conf);

		}
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
				throw new LibrecException(e);
			}
			
			GroupBuilder groupBuilder = ReflectionUtil.newInstance((Class<GroupBuilder>) builderLCass, conf);
			groupBuilder.setUp(rawData, preferenceMatrix);
			groupBuilder.generateGroups();
			this.groupMapping = groupBuilder.getGroupMapping();
			this.Groups = groupBuilder.getGroups();
			this.userStatistics = groupBuilder.getMemberStatistics();
			this.Groupassignation = groupBuilder.getAssignation();
			this.NumberOfGroups= Groups.size();
			this.exhaustiveGroups = groupBuilder.isExhaustive();
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

	public GroupModeling getGroupModeling() {
		return gp;
	}

	public void setGroupModeling(GroupModeling gp) {
		this.gp = gp;
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
		String splitter = conf.get("data.model.splitter");
		
		try {
            if (dataSplitter == null) {
                dataSplitter = (DataSplitter) ReflectionUtil.newInstance(DriverClassUtil.getClass(splitter), conf);
                if (dataSplitter instanceof GroupDataSplitter) {
                	((GroupDataSplitter) dataSplitter).setGroupInfo(this.Groupassignation, this.Groups);
                }
            }
            if (dataSplitter != null) {
                dataSplitter.setDataConvertor(dataConvertor);
                dataSplitter.splitData();
                if (this.exhaustiveGroups) {
        			trainDataSet = dataSplitter.getTrainData();
        			testDataSet = dataSplitter.getTestData();
        		} else {
        			this.makeSafeSplit(dataSplitter.getTrainData(), dataSplitter.getTestData());
        		}
            }
        } catch (ClassNotFoundException e) {
            throw new LibrecException(e);
        }
	}
	
	@Override
	public void nextFold() {
		if (this.exhaustiveGroups) {
			trainDataSet = dataSplitter.getTrainData();
			testDataSet = dataSplitter.getTestData();
		} else {
			this.makeSafeSplit(dataSplitter.getTrainData(), dataSplitter.getTestData());
		}
        validDataSet = dataSplitter.getValidData();
        // generate next fold by Splitter
	}

	private void makeSafeSplit(SequentialAccessSparseMatrix trainData, SequentialAccessSparseMatrix testData) {
		SequentialAccessSparseMatrix preferences = this.dataConvertor.getPreferenceMatrix(conf);
		SequentialAccessSparseMatrix train = new SequentialAccessSparseMatrix(preferences);
		for (MatrixEntry entry : testData) {
			SequentialSparseVector trainRow = train.row(entry.row());
			int[] indices = trainRow.getIndices();
			int position = -1;
			for (int i = 0; i < indices.length; i++) {
				if (indices[i] == entry.column()) {
					position = i;
					break;
				}
			}
			if (this.Groupassignation.containsKey(entry.row())) {
				System.out.println("Ignored member:"+Integer.toString(entry.row())+ " item:"+Integer.toString(entry.column()));
				train.setAtColumnPosition(entry.row(), position, 0.0D);
			} else {
				testData.setAtColumnPosition(entry.row(), entry.columnPosition(), 0.0D);
			}
		}
		train.reshape();
		testData.reshape();
		trainDataSet = train;
		testDataSet = testData;
	}
	

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
			List<KeyValue<Integer, Double>> groupScores = this.gp.computeGroupModel(groupsRatings);
			for (KeyValue<Integer, Double> item : groupScores) {
				groupRatings.put(group, item.getKey(), item.getValue());
			}
		}
		return groupRatings;
	}

}
