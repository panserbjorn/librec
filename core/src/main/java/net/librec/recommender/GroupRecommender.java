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
import java.util.stream.Collectors;

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
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedList;
import net.librec.util.DriverClassUtil;
import net.librec.util.Lists;
import net.librec.util.ReflectionUtil;

/**
 * @author Joaqui This class will be in charge of the abstraction of the
 *         recommendation variants for the groups
 */
public class GroupRecommender extends AbstractRecommender {
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
		if (isRanking) {
//        	This is for the base recommender to not shrink the list of items in the ranking recommendations 
//			conf.setInt("rec.recommender.ranking.topn", this.getDataModel().getItemMappingData().size());
//			conf.setInt("rec.group.ranking.topn", this.topN);
		}
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
		Map<Integer, List<Integer>> groups = ((GroupDataModel) this.getDataModel()).getGroups();
		RecommendedList recommendedList = new RecommendedList(groups.keySet().size());

		for (Integer group : groups.keySet()) {
			Map<Integer, List<KeyValue<Integer, Double>>> singleGroupRatings = new HashMap<Integer, List<KeyValue<Integer, Double>>>();
			for (Integer member : groups.get(group)) {
				List<KeyValue<Integer, Double>> memberRatings = individualRecomm.getKeyValueListByContext(member);
				singleGroupRatings.put(member, memberRatings);
			}
			ArrayList<KeyValue<Integer, Double>> groupScore = null;
			if (isRanking) {
				groupScore = ((GroupDataModel) this.getDataModel()).getGroupRanking(singleGroupRatings);
			} else {
				groupScore = ((GroupDataModel) this.getDataModel()).getGroupRatings(singleGroupRatings);
			}
			groupScore.sort(Map.Entry.comparingByKey());
			recommendedList.addList(groupScore);
		}

		return recommendedList;

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
		int originalTopN = conf.getInt("rec.recommender.ranking.topn", 10);
		if (isRanking) {

			conf.setInt("rec.recommender.ranking.topn", this.getDataModel().getItemMappingData().size());
		}
		this.baseRecommender.train(getContext());
		if (isRanking) {
			conf.setInt("rec.recommender.ranking.topn", originalTopN);
		}
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
							groupItemList
									.add(new GenericRecommendedItem(groupId, itemId, contextKeyValueEntry.getValue()));
						}
					}
				}
				return groupItemList;
			}
		}
		return null;
	}

}
