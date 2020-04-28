/**
 * 
 */
package groupRec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.librec.common.LibrecException;
import net.librec.data.structure.AbstractBaseDataEntry;
import net.librec.data.structure.LibrecDataList;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.AbstractRecommender;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.ItemKNNRecommender;
import net.librec.recommender.item.ContextKeyValueEntry;
import net.librec.recommender.item.RecommendedList;

/**
 * @author Joaqui This class will be in charge of the abstraction of the
 *         recommendation variants for the groups
 */
public class GroupRecommender extends AbstractRecommender {
	/**
	 * trainMatrix
	 */
	protected SequentialAccessSparseMatrix trainMatrix;

	/**
	 * testMatrix
	 */
	protected SequentialAccessSparseMatrix testMatrix;

	/**
	 * validMatrix
	 */
	protected SequentialAccessSparseMatrix validMatrix;

	/**
	 * the number of users
	 */
	protected int numUsers;

	/**
	 * the number of items
	 */
	protected int numItems;

	/**
	 * the number of rates
	 */
	protected int numRates;

	/**
	 * Maximum rate of rating times
	 */
	protected double maxRate;

	/**
	 * Minimum rate of rating times
	 */
	protected double minRate;

	/**
	 * a list of rating scales
	 */
	protected static List<Double> ratingScale;

	protected Recommender baseRecommender;

	@Override
	protected void setup() throws LibrecException {
		super.setup();
		this.baseRecommender = new ItemKNNRecommender();
		this.baseRecommender.setContext(this.getContext());
		trainMatrix = (SequentialAccessSparseMatrix) getDataModel().getTrainDataSet();
		testMatrix = (SequentialAccessSparseMatrix) getDataModel().getTestDataSet();
		validMatrix = (SequentialAccessSparseMatrix) getDataModel().getValidDataSet();

		numUsers = trainMatrix.rowSize();
		numItems = trainMatrix.columnSize();
		numRates = trainMatrix.size();
		Set<Double> ratingSet = new HashSet<>();
		for (MatrixEntry matrixEntry : trainMatrix) {
			ratingSet.add(matrixEntry.get());
		}
		ratingScale = new ArrayList<>(ratingSet);
		Collections.sort(ratingScale);
		maxRate = Collections.max(ratingScale);
		minRate = Collections.min(ratingScale);
		if (minRate == maxRate) {
			minRate = 0;
		}
	}


	private RecommendedList buildGroupRecommendations(RecommendedList individualRecomm) {
		Map<Integer, Integer> groupAssignation = ((GroupDataModel) this.getDataModel()).getGroupAssignation();
		Map<Integer, List<Integer>> groups = ((GroupDataModel) this.getDataModel()).getGroups();

//        Aggregate the group ratings in structure
//		TODO This might fail if there are groups that don't have anything inside the test set
		Map<Integer, Map<Integer, List<Double>>> groupRatings = new HashMap<Integer, Map<Integer, List<Double>>>();
		for (Integer group : groups.keySet()) {
			groupRatings.put(group, new HashMap<Integer, List<Double>>());
		}
		Iterator<ContextKeyValueEntry> iter = individualRecomm.iterator();
		while (iter.hasNext()) {
			ContextKeyValueEntry contextKeyValueEntry = iter.next();
			if (contextKeyValueEntry != null) {
				int userId = contextKeyValueEntry.getContextIdx();
				int itemId = contextKeyValueEntry.getKey();
				double value = contextKeyValueEntry.getValue();
				Map<Integer, List<Double>> currentGroupRatings = groupRatings.get(groupAssignation.get(userId));
				if (!currentGroupRatings.containsKey(itemId)) {
					currentGroupRatings.put(itemId, new ArrayList<Double>());
				}
				currentGroupRatings.get(itemId).add(value);
			}
		}

		RecommendedList recommendedList = new RecommendedList(groups.keySet().size());

//		TODO I could parallelize this so that it is FAR more efficient
//		TODO This method only works if all the ratings for the group are in the test. Otherwise I would have to join the predictions for the test and the train ratings of the group
		for (int group = 0; group < groups.size(); group++) {
			recommendedList.addList(new ArrayList<>());
			for (Integer item : groupRatings.get(group).keySet()) {
				List<Double> groupScores = groupRatings.get(group).get(item);
				recommendedList.add(group, item, groupModeling(groupScores));
			}
		}

		return recommendedList;

	}
	
	
//	TODO: this should be an abstract method that the different implementations of the GroupRecommender should implement
	protected Double groupModeling(List<Double> groupScores) {
//		TODO: Need to implement other group models, not only avg
		return groupScores.stream().mapToDouble(a -> a).average().getAsDouble();
	}

	@Override
	public RecommendedList recommendRating(DataSet predictDataSet) throws LibrecException {
//		Get Individual recommendations
		RecommendedList individualRecomm = this.baseRecommender.recommendRating(predictDataSet);
		return buildGroupRecommendations(individualRecomm);
	}

	@Override
	public RecommendedList recommendRating(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
//		Get Individual recommendations
		RecommendedList individualRecommendations = this.baseRecommender.recommendRating(dataList);
		return this.buildGroupRecommendations(individualRecommendations);
	}

	@Override
	public RecommendedList recommendRank() throws LibrecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RecommendedList recommendRank(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void trainModel() throws LibrecException {
		// TODO Auto-generated method stub
		this.baseRecommender.train(getContext());

	}

}
