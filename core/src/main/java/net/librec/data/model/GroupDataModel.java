/**
 * 
 */
package net.librec.data.model;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.Set;
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
import net.librec.recommender.item.RecommendedList;
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
		trainDataSet = dataSplitter.getTrainData();
		testDataSet = dataSplitter.getTestData();

	}

	public Double getGroupRating(List<Double> groupScores) {
//		TODO Add this parameter to the configuration parameter list
		String model = conf.get("data.group.model", "addUtil");
		switch (model) {
		case "addUtil":
			return AdditiveUtilitarian(groupScores);
		case "leastMis":
			return LeastMisery(groupScores);
		case "mostPl":
			return MostPleasure(groupScores);
		case "multUtil":
			return MultiplicativeUtilitarian(groupScores);
		default:
//			TODO Should rise an exception here
			LOG.error("Group Data Model not defined. Output to 0 in all instances");
			return 0.0D;
		}
	}

	public ArrayList<KeyValue<Integer, Double>> getGroupRanking(
			Collection<List<KeyValue<Integer, Double>>> groupRatings) {
//		TODO Add this parameter to the configuration parameter list
		String model = conf.get("data.group.model", "borda");
//		TODO Add the rest of the ranking methods to the switch
		switch (model) {
		case "borda":
			return BordaCount(groupRatings);
		case "multUtil":
			return null;
		case "copeland":
			return CopelandRule(groupRatings);
		case "plurality":
			Integer topN = conf.getInt("rec.recommender.ranking.topn", 10);
			return PluralityVoting(groupRatings, topN);
		case "approval":
			Double approvalThreshold = conf.getDouble("rec.recommender.approval");
			return ApprovalVoting(groupRatings, approvalThreshold);
		default:
			return null;
		}
	}

	private static Double AdditiveUtilitarian(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a -> a).average().getAsDouble();
	}

	private static Double LeastMisery(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a -> a).min().getAsDouble();
	}

	private static Double MostPleasure(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a -> a).max().getAsDouble();
	}

	private static Double MultiplicativeUtilitarian(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a -> a).reduce(1, (a, b) -> a * b);
	}

	private static ArrayList<KeyValue<Integer, Double>> BordaCount(
			Collection<List<KeyValue<Integer, Double>>> collection) {
		Map<Integer, List<Integer>> itemsRankings = new HashMap<Integer, List<Integer>>();
		int numberItems = 0;
		for (List<KeyValue<Integer, Double>> individualRating : collection) {
//			Sort items for each member based on rating
			List<KeyValue<Integer, Double>> listIndividualRating = individualRating.stream()
					.sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
			Collections.reverse(listIndividualRating);
			for (int i = 0; i < listIndividualRating.size(); i++) {
				KeyValue<Integer, Double> item = listIndividualRating.get(i);
				if (!itemsRankings.containsKey(item.getKey())) {
					numberItems += 1;
					itemsRankings.put(item.getKey(), new ArrayList<Integer>());
				}
				itemsRankings.get(item.getKey()).add(i);
			}
		}
		ArrayList<KeyValue<Integer, Double>> ranking = new ArrayList<KeyValue<Integer, Double>>();
		final int finalItemSize = numberItems;
		for (Integer item : itemsRankings.keySet()) {
			List<Integer> inversedRankings = itemsRankings.get(item);
//			Invert ranking to points and sum
			Double itemSum = inversedRankings.stream().mapToDouble(a -> finalItemSize - a).sum();
			ranking.add(new KeyValue<Integer, Double>(item, itemSum));
		}
		return ranking;
	}

	private static ArrayList<KeyValue<Integer, Double>> CopelandRule(
			Collection<List<KeyValue<Integer, Double>>> groupRatings) {
//		TODO Need to optimize this. The copeland Rule takes about 4 minutes in moveikLens
//		TODO Now I know that if this is being called, the groupRatings al have the same length

		BiMap<Integer, Integer> itemTemporalMap = HashBiMap.create();
		for (List<KeyValue<Integer, Double>> memberRatings : groupRatings) {
			for (KeyValue<Integer, Double> rating : memberRatings) {
				if (!itemTemporalMap.containsKey(rating.getKey())) {
					itemTemporalMap.put(rating.getKey(), itemTemporalMap.size());
				}
			}
		}
//		Initialize confrontation table
		Integer numberItems = itemTemporalMap.size();
		List<int[]> itemConfrontationCount = new ArrayList<int[]>();
		for (int i = 0; i < numberItems - 1; i++) {
			int[] confrontationRow = new int[numberItems-(i+1)];
			Arrays.fill(confrontationRow, 0);
			itemConfrontationCount.add(confrontationRow);
		}
//		Collect the result of all the confrontations
		for (List<KeyValue<Integer, Double>> memeberRatings : groupRatings) {
			List<KeyValue<Integer, Double>> sortedRatings = memeberRatings.stream().sorted(Map.Entry.comparingByValue())
					.collect(Collectors.toList());
			Collections.reverse(sortedRatings);
			Set<Integer> preferredAgainst = new HashSet<Integer>(itemTemporalMap.keySet());
			for (KeyValue<Integer, Double> item : sortedRatings) {
				preferredAgainst.remove(item.getKey());
				for (Integer looser : preferredAgainst) {
					Integer itemMapping = itemTemporalMap.get(item.getKey());
					Integer looserMapping = itemTemporalMap.get(looser);
					if (itemMapping < looserMapping) {
						Integer itemPosition = itemMapping;
						Integer looserPosition = looserMapping - (itemMapping + 1);
						itemConfrontationCount.get(itemPosition)[looserPosition]++;
					} else {
						Integer looserPosition = looserMapping;
						Integer itemPosition = itemMapping - (looserMapping + 1);
						itemConfrontationCount.get(looserPosition)[itemPosition]--;
					}
				}
			}

		}

//		Compute the winning numbers
		int[] winningCount = new int[numberItems];
		Arrays.fill(winningCount, 0);
		for (int i = 0; i < numberItems - 1; i++) {
			for (int j = i + 1; j < numberItems; j++) {
				int jPosition = j - (i + 1);
				Integer confrontationCount = itemConfrontationCount.get(i)[jPosition];
				if (confrontationCount > 0) {
					winningCount[i]++;
				} else if (confrontationCount < 0) {
					winningCount[j]++;
				}
			}
		}

//		Generate result without mapping
		BiMap<Integer, Integer> inverse = itemTemporalMap.inverse();
		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer, Double>>();
		for (int i = 0; i < winningCount.length; i++) {
			int score = winningCount[i];
			groupRanking.add(new KeyValue<Integer, Double>(inverse.get(i), score + 0.0));
		}

		return groupRanking;
	}

	private static ArrayList<KeyValue<Integer, Double>> PluralityVoting(
			Collection<List<KeyValue<Integer, Double>>> groupRatings, Integer topN) {
		
		groupRatings.forEach(memberRating -> memberRating.sort(Collections.reverseOrder(Map.Entry.comparingByValue())));
		
		List<Integer> rank = new ArrayList<Integer>();
		
		while(rank.size() < topN) {
			Map<Integer, Integer> voting = new HashMap<Integer, Integer>();
			for (List<KeyValue<Integer, Double>> memberRating : groupRatings) {
				Optional<KeyValue<Integer, Double>> seek = memberRating.stream().filter(item -> !rank.contains(item.getKey())).findFirst();
				if (seek.isPresent()) {
					voting.compute(seek.get().getKey(), (k,v) -> (v==null) ? 1: v+1);
				}
			}
			List<Entry<Integer, Integer>> votingResult = voting.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
			Integer highestVoting = votingResult.get(0).getValue();
			List<Integer> highestRankedList = votingResult.stream().filter(item -> item.getValue()==highestVoting).map(item -> item.getKey()).collect(Collectors.toList());
			rank.addAll(highestRankedList);
		}
		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer, Double>>();
		
		for (int i = 0; i < rank.size(); i++) {
			Integer item = rank.get(i);
			groupRanking.add(new KeyValue<Integer, Double>(item, rank.size()-item + 0.0));
		}
		
		return groupRanking;
	}

	private static ArrayList<KeyValue<Integer, Double>> ApprovalVoting(
			Collection<List<KeyValue<Integer, Double>>> groupRatings, Double approvalThreshold) {
		
		Map<Integer, Integer> voting = new HashMap<Integer, Integer>();
		for (List<KeyValue<Integer, Double>> memberRatings : groupRatings) {
			memberRatings.stream().filter(item -> item.getValue() > approvalThreshold).forEach(item -> voting.compute(item.getKey(), (k,v)->(v==null)?1:v+1));
		}
		
		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer,Double>>();
		for (Entry<Integer, Integer> vote : voting.entrySet()) {
			groupRanking.add(new KeyValue<Integer, Double>(vote.getKey(), vote.getValue()+0.0));
		}
		return groupRanking;
	}

	private static ArrayList<KeyValue<Integer, Double>> Fairness(
			Collection<List<KeyValue<Integer, Double>>> groupRatings) {
//		TODO Implement Fairness
		return null;
	}

	public Table<Integer, Integer, Double> getGroupRatings(SequentialAccessSparseMatrix targetDataset) {
		Table<Integer, Integer, Double> groupRatings = HashBasedTable.create();
		Boolean isranking = conf.getBoolean("rec.recommender.isranking", false);
		if (isranking) {
			for (Integer group : this.Groups.keySet()) {
				List<List<KeyValue<Integer, Double>>> groupsRatings = new ArrayList<List<KeyValue<Integer, Double>>>();
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
						groupsRatings.add(memberRatings);
					}
				}
				List<KeyValue<Integer, Double>> groupRanking = this.getGroupRanking(groupsRatings);
				for (KeyValue<Integer, Double> item : groupRanking) {
					groupRatings.put(group, item.getKey(), item.getValue());
				}
			}

		} else {
			for (Integer group : this.Groups.keySet()) {
				Map<Integer, List<Double>> currentGroupRatings = new Hashtable<Integer, List<Double>>();
				List<Integer> groupMembers = this.Groups.get(group);
				for (Integer member : groupMembers) {
					SequentialSparseVector row = targetDataset.row(member);
					int[] itemsRatedByUser = row.getIndices();
					for (int i = 0; i < itemsRatedByUser.length; i++) {
						Integer item = itemsRatedByUser[i];
						if (!currentGroupRatings.containsKey(item)) {
							currentGroupRatings.put(item, new ArrayList<Double>());
						}
						currentGroupRatings.get(item).add(row.getAtPosition(i));
					}
				}
				for (Integer item : currentGroupRatings.keySet()) {
					groupRatings.put(group, item, getGroupRating(currentGroupRatings.get(item)));
				}
			}
		}

		return groupRatings;
	}

}
