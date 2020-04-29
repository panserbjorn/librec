/**
 * 
 */
package groupRec;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.model.AbstractDataModel;
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
	private Map<Integer,List<Integer>> Groups;
	private int NumberOfGroups;

	/**
	 * Empty constructor.
	 */
	public GroupDataModel() {
	}

	public GroupDataModel(Configuration conf) {
		this.conf = conf;
	}
	
	void saveGroups() {
		Map<Integer,Integer> groupAssignation = this.getGroupAssignation();
		BiMap<String,Integer> userMapping = this.getUserMappingData();
		String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/groupAssignation.csv";
		System.out.println("Result path is " + outputPath);
		BiMap<Integer, String> inverseUserMapping = userMapping.inverse();
		// convert itemList to string
		StringBuilder sb = new StringBuilder();
		for (Integer userID : groupAssignation.keySet()) {
			String userId = inverseUserMapping.get(userID);	
			String groupId = Integer.toString(groupAssignation.get(userID));
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
				this.NumberOfGroups = this.conf.getInt("group.number", 10);
				Kmeans groupBuilder = new Kmeans(this.NumberOfGroups, rawData.getRatingScale().get(0),
						rawData.getRatingScale().get(rawData.getRatingScale().size() - 1), preferenceMatrix);
				groupBuilder.init();
				groupBuilder.calculate();
				this.Groupassignation = groupBuilder.getAssignation();
				this.Groups = groupBuilder.getGroupMapping();
//				TODO: Should I save the groups here?
				if (conf.getBoolean("group.save",false)) {
					this.saveGroups();
				}
			} else {
//				TODO: Here I need to get the groups from the external file
//				TODO: Add this to the configuration list of parameters
				String groupAssignationPath = conf.get("group.external.path");
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
		dataSplitter = new GroupDataSplitter(this.Groupassignation,this.Groups);
		
        dataSplitter.setDataConvertor(dataConvertor);
        dataSplitter.splitData();
        trainDataSet = dataSplitter.getTrainData();
        testDataSet = dataSplitter.getTestData();
        
//		super.buildSplitter();
	}

}
