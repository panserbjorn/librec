/**
 * 
 */
package net.librec.recommender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;
import net.librec.data.model.GroupDataModel;
import net.librec.data.structure.AbstractBaseDataEntry;
import net.librec.data.structure.LibrecDataList;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.recommender.cf.ItemKNNRecommender;
import net.librec.recommender.item.ContextKeyValueEntry;
import net.librec.recommender.item.GenericRecommendedItem;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedList;
import net.librec.util.DriverClassUtil;
import net.librec.util.Lists;
import net.librec.util.ReflectionUtil;

/**
 * @author Joaqui This class will be in charge of the abstraction of the
 *         recommendation variants for the groups
 */
public abstract class GroupRecommender extends AbstractRecommender {
//	TODO: Verify if I'm still using all this parameters
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
	
	/**
     * Get recommender class. {@code Recommender}.
     *
     * @return recommender class object
     * @throws ClassNotFoundException if can't find the class of recommender
     * @throws IOException            If an I/O error occurs.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Recommender> getBaseRecommenderClass() throws ClassNotFoundException, IOException {
//    	TODO: Add this to the configuration list of parameters
        return (Class<? extends Recommender>) DriverClassUtil.getClass(conf.get("group.base.recommender.class"));
    }

    @SuppressWarnings("unchecked")
	@Override
	protected void setup() throws LibrecException {
		super.setup();
//		TODO: Decide if this should be here or in the job (If I put it in the Job I need to make a special job for the group recommenders)
		try {
			Recommender baseRecom = ReflectionUtil.newInstance((Class<Recommender>) getBaseRecommenderClass(), conf);
			this.baseRecommender = baseRecom;
		} catch (ClassNotFoundException | IOException e) {
			this.baseRecommender = new ItemKNNRecommender();
			e.printStackTrace();
		}
		
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

//      Aggregate the group ratings in structure
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
		
//		Retrieve data from training if any
		for (int group = 0; group < groups.size(); group++) {
			Set<Integer> items = groupRatings.get(group).keySet();
			for (Integer item : items) {
				SequentialSparseVector column = this.trainMatrix.column(item);
				int[] indices = column.getIndices();
				List<Integer> groupUsers = groups.get(group);
				for (int i = 0; i < indices.length; i++) {
					int userRated = indices[i];
					if (groupUsers.contains(userRated)) {
						groupRatings.get(group).get(item).add(column.getAtPosition(i));
					}
				}
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
	
	
	abstract protected Double groupModeling(List<Double> groupScores);

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
		RecommendedList individualRecommendations = this.baseRecommender.recommendRank();
		RecommendedList groupRecommendations = this.buildGroupRecommendations(individualRecommendations);
		groupRecommendations.topNRank(topN);
		return groupRecommendations;
	}

	@Override
	public RecommendedList recommendRank(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
		RecommendedList individualRecommendations = this.baseRecommender.recommendRank(dataList);
		RecommendedList groupRecommendations = this.buildGroupRecommendations(individualRecommendations);
		groupRecommendations.topNRank(topN);
		return groupRecommendations;
	}

	@Override
	protected void trainModel() throws LibrecException {
		this.baseRecommender.train(getContext());

	}
	
	@Override
	public List<RecommendedItem> getRecommendedList(RecommendedList recommendedList) {
        if (recommendedList != null && recommendedList.size() > 0) {
            List<RecommendedItem> groupItemList = new ArrayList<>();
            Iterator<ContextKeyValueEntry> recommendedEntryIter = recommendedList.iterator();
            if (itemMappingData != null && itemMappingData.size() > 0) {
                BiMap<Integer, String> itemMappingInverse = itemMappingData.inverse();
                while (recommendedEntryIter.hasNext()) {
                    ContextKeyValueEntry contextKeyValueEntry = recommendedEntryIter.next();
                    if (contextKeyValueEntry != null) {
                        String groupId = Integer.toString(contextKeyValueEntry.getContextIdx());
                        String itemId = itemMappingInverse.get(contextKeyValueEntry.getKey());
                        if (StringUtils.isNotBlank(groupId) && StringUtils.isNotBlank(itemId)) {
                            groupItemList.add(new GenericRecommendedItem(groupId, itemId, contextKeyValueEntry.getValue()));
                        }
                    }
                }
                return groupItemList;
            }
        }
        return null;
	}

}
