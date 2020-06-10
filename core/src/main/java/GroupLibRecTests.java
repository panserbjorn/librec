import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configuration.Resource;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.model.GroupDataModel;
import net.librec.data.model.RingGroupDataModel;
import net.librec.data.model.group.GroupModeling;
import net.librec.eval.EvalContext;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.math.structure.SequentialSparseVector;
import net.librec.math.structure.SymmMatrix;
import net.librec.recommender.GroupRecommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import net.librec.tool.driver.RecDriver;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;
import net.librec.util.ReflectionUtil;

public class GroupLibRecTests {

	public static void main(String[] args) throws Exception {

//		driverTest("..\\core\\target\\classes\\ringTest.properties");

//		movielens1MRandomGroupGeneration();
//		movielens1MSimilarGroupGeneration();

//		Long seed = conf.getLong("rec.random.seed");
//        if (seed != null) {
//            Randoms.seed(seed);
//        }
//		ratingUseCase("useCaseRatingBiasedMFThesis.properties", "movielens1m", "::");
//		ratingUseCase("useCaseRatingUserKNNThesis.properties", "movielens1m", "::");
//		rankingUseCase("useCaseBPRThesis.properties");
//		rankingUseCase("useCaseBiasedMFRankThesis.properties");

//		amazonRandomGroupGeneration();
//		amazonSimilarGroupGeneration();

//		ratingUseCase("useCaseRatingBiasedMFThesis.properties", "amazon", ",");
//		ratingUseCase("useCaseRatingUserKNNThesis.properties", "amazon", ",");
//		rankingUseCase("useCaseBPRThesis.properties", "amazon", ",");

//		videogamesRandomGroupGeneration();
//		videoGamesSimilarGroupGeneration();

//		groceriesSimilarGroupGeneration();
//		groceriesRandomGroupGeneration();
//		toolsSimilarGroupGeneration();

//		ratingUseCase("useCaseRatingBiasedMFThesis.properties", "videogames", ",");
//		ratingUseCase("useCaseRatingUserKNNThesis.properties", "videogames", ",");
//		rankingUseCase("useCaseBPRThesis.properties", "videogames", ",");
//		rankingUseCase("useCaseBiasedMFRankThesis.properties", "videogames", ",");

//		rankingUseCase("useCaseBPRThesis.properties", "groceries", ",");
//		rankingUseCase("useCaseBiasedMFRankThesis.properties" , "groceries", ",");

//		ratingUseCase("useCaseRatingBiasedMFThesis.properties", "amazon", ",");
//		ratingUseCase("useCaseRatingUserKNNThesis.properties", "amazon", ",");
//		rankingUseCase("useCaseBPRThesis.properties", "amazon", ",");
//		rankingUseCase("useCaseBiasedMFRankThesis.properties", "amazon", ",");

//		ratingUseCase("useCaseRatingBiasedMFThesis.properties", "videogames", ",");
//		ratingUseCase("useCaseRatingUserKNNThesis.properties", "videogames", ",");
//		rankingUseCase("useCaseBPRThesis.properties", "videogames", ",");
//		rankingUseCase("useCaseBiasedMFRankThesis.properties", "videogames", ",");

//		Configuration conf = new Configuration(false);
//		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/similar");
//		conf.setStrings("data.input.path", "videogames");
//		conf.setStrings("dfs.data.dir", "../../../DataSets");
//		conf.setStrings("data.column.format", "UIR");
//		conf.setBoolean("group.save", true);
//		conf.set("data.convert.sep", ",");
//		conf.set("group.builder", "similarGroups");
//		conf.set("data.model.splitter","ratio");
//		conf.set("data.splitter.ratio","rating");
//		conf.setDouble("data.splitter.trainset.ratio",0.8);
//		conf.set("data.model.format","addUtil");
//		
//		conf.setInt("rec.random.seed", 1);
//		
//		TextDataConvertor conv = new TextDataConvertor("UIR", "../../../DataSets/videogames",",");
//		
//		conv.processData();
//		
//		DataFrame matrix = conv.getMatrix();
//		
////		RecommenderSimilarity similarity = new PCCSimilarity();
//		SequentialAccessSparseMatrix preferences = matrix.toSparseMatrix();
//		
//		int numUsers = preferences.rowSize();
//		
//		PearsonsCorrelation pcc = new PearsonsCorrelation();
//
//		SymmMatrix similarity = new SymmMatrix(numUsers);
//		List<Integer> indexList = new ArrayList<>();
//		for (int index = 0; index < numUsers; index++) {
//			indexList.add(index);
//		}
//
//		indexList.parallelStream().forEach((Integer thisIndex) -> {
//			SequentialSparseVector thisVector = preferences.row(thisIndex);
//			if (thisVector.getNumEntries() >= 5) {
//				for (int thatIndex = thisIndex + 1; thatIndex < numUsers; thatIndex++) {
//					SequentialSparseVector thatVector = preferences.row(thatIndex);
//					if (thatVector.getNumEntries() < 5) {
//						continue;
//					}
////					TODO I could change this for checking that the length of the first list in the common items is greater than 5
//					if (!shareMinimumItems(thisVector.getIndices(), thatVector.getIndices(), conf)) {
//						continue;
//					}
//					List<double[]> commonItems = getCommonItems(thisVector, thatVector);
//					double sim = pcc.correlation(commonItems.get(0), commonItems.get(1));
//					if (!Double.isNaN(sim) && sim != 0.0) {
//						similarity.set(thisIndex, thatIndex, sim);
//					}
//				}
//			}
//		});
//		
//		FileWriter myWriter = new FileWriter("videogames_hist.txt");
//		similarity.getData().cellSet().forEach(cell -> {
//			try {
//				myWriter.write(cell.getValue().toString()+"\n");
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
//		myWriter.close();

//		String[] datasets = new String []{"movielens1m", "groceries", "amazon"};
//		String[] formats = new String [] {"UIRT", "UIR", "UIR"};
//		String[] separators = new String[] {"::", ",", ","};
//		Integer[] groups = new Integer[] {20, 50, 100};
//		
//		for (int i = 0 ; i < datasets.length ; i++) {
//			for (Integer groupNumber : groups) {
//				kmeansGroupGeneration(datasets[i], formats[i], groupNumber, separators[i]);
//			}
//		}
//		
		kmeansRingTest("useCaseRatingBiasedMFThesis.properties", "movielens1m", "UIRT", "::");
		kmeansRingTest("useCaseRatingBiasedMFThesis.properties", "groceries", "UIR", ",");
	}

	private static void kmeansRingTest(String confPath, String dataset, String format, String separator)
			throws LibrecException, ClassNotFoundException {
		Configuration conf = new Configuration(false);
		conf.addResource(new Resource(confPath));
//		String [] clusteringExternalFiles = new String[] {"random_2", "random_3", "random_4", "random_5", "random_6", "random_7", "random_8", "similar_2", "similar_3", "similar_4", "similar_5", "similar_6", "similar_7", "similar_8"};
//		String [] clusteringExternalFiles = new String[] {"random_2", "random_3", "random_4", "random_5", "random_6", "random_7", "random_8", "similar_2", "similar_3", "similar_4", "similar_5", "similar_6", "similar_7"};
		String[] clusteringExternalFiles = new String[] { "kmeans20", "kmeans50", "kmeans100" };

		conf.set("data.input.path", dataset);
		conf.set("data.convert.sep", separator);
		conf.setStrings("data.column.format", format);
		conf.setInt("ring.number", 10);

		Long seed = conf.getLong("rec.random.seed");
		if (seed != null) {
			Randoms.seed(seed);
		}

		for (String clustering : clusteringExternalFiles) {
			conf.setStrings("group.external.path",
					"../../../ThesisResults/clusters/kmeans/" + dataset + "/groupAssignation" + clustering + ".csv");
			conf.set("group.model", "addUtil");
			RingGroupDataModel gdm = new RingGroupDataModel();
			gdm.setConf(conf);
			conf.setBoolean("data.convert.read.ready", false);
			gdm.buildDataModel();

			RecommenderContext recContext = new RecommenderContext(conf);

			GroupRecommender rec = new GroupRecommender();
			rec.setContext(recContext);

			while (gdm.hasNextFold()) {
				gdm.nextFold();
				recContext.setDataModel(gdm);

				Class<? extends RecommenderSimilarity> similarityClass = (Class<? extends RecommenderSimilarity>) DriverClassUtil
						.getClass(conf.get("rec.similarity.class"));

				RecommenderSimilarity similarity = ReflectionUtil.newInstance(similarityClass, conf);
				similarity.buildSimilarityMatrix(gdm);
				recContext.setSimilarity(similarity);

				rec.train(recContext);

				DataSet predictDataSet = gdm.getTestDataSet();

				RecommendedList baseRating = rec.getBaseRating(predictDataSet);

				Map<String, Long> timeMeasures = new HashMap<String, Long>();

				String groupMod = "addUtil";
				Class<? extends GroupModeling> groupModelingClass;
				try {
					groupModelingClass = (Class<? extends GroupModeling>) DriverClassUtil.getClass(groupMod);
				} catch (ClassNotFoundException e) {
					throw new LibrecException(e);
				}

				gdm.setGroupModeling(ReflectionUtil.newInstance((Class<GroupModeling>) groupModelingClass, conf));

				String[] ringConfigurations = new String[] { "0,1,2,3,4,5,6,7,8,9", "0,1,2,3,4,5,6,7,8",
						"0,1,2,3,4,5,6,7", "0,1,2,3,4,5,6", "0,1,2,3,4,5", "0,1,2,3,4", "0,1,2,3", "0,1,2", "0,1",
						"0" };

				for (String ringConf : ringConfigurations) {
					System.out.println(ringConf);
					long startTime = System.currentTimeMillis();
					RecommendedList groupRecommendations = rec.buildGroupRecommendations(baseRating);
					long endTime = System.currentTimeMillis();

					timeMeasures.put(ringConf, endTime - startTime);

					EvalContext evc = new EvalContext(conf, groupRecommendations,
							(SequentialAccessSparseMatrix) predictDataSet);

					RecommendedList memberGroundTruth = evc.getGroundTruthList();

					String[] evalClassKeys = conf.getStrings("rec.eval.classes");
					BiMap<Integer, String> userMap = gdm.getUserMappingData().inverse();

					for (String evalClass : evalClassKeys) {
						Class<? extends RecommenderEvaluator> evaluatorClass = (Class<? extends RecommenderEvaluator>) DriverClassUtil
								.getClass(evalClass);
						RecommenderEvaluator eval = ReflectionUtil.newInstance(evaluatorClass, null);

						System.out.println(evalClass);

						Double membersOverallEvaluation = eval.evaluate(recContext, memberGroundTruth,
								groupRecommendations);

						Map<String, KeyValue<Double, Integer>> membersEvaluations = new HashMap<String, KeyValue<Double, Integer>>();

						for (int contextId = 0; contextId < groupRecommendations.size(); contextId++) {
							RecommendedList individualGroupList = new RecommendedList(1);
							individualGroupList.addList((ArrayList<KeyValue<Integer, Double>>) groupRecommendations
									.getKeyValueListByContext(contextId));
							RecommendedList expectedGruopList = new RecommendedList(1);
							expectedGruopList.addList((ArrayList<KeyValue<Integer, Double>>) memberGroundTruth
									.getKeyValueListByContext(contextId));
							Double memberMeasure = eval.evaluate(recContext, expectedGruopList, individualGroupList);
							membersEvaluations.put(userMap.get(contextId), new KeyValue<Double, Integer>(memberMeasure,
									expectedGruopList.getKeySetByContext(0).size()));
						}
						String fileName = ringConf + "_" + clustering + "_";
						saveRecsysResults(fileName, conf, evalClass, membersOverallEvaluation, membersEvaluations);

					}
				}

				//
				saveTimeResults(timeMeasures, clustering, conf, "rating");

			}
		}
	}

	private static void kmeansGroupGeneration(String dataset, String format, Integer groups, String separator)
			throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/kmeans");
		conf.setStrings("data.input.path", dataset);
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", format);
		conf.set("data.convert.sep", separator);
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "kmeans");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		conf.setInt("group.number", groups);
		GroupDataModel model = new GroupDataModel();
		model.setConf(conf);
		model.buildDataModel();
	}

	private static void groceriesRandomGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/random");
		conf.setStrings("data.input.path", "groceries");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "randomGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.random.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}

	}

	private static void toolsSimilarGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/similar");
		conf.setStrings("data.input.path", "tools");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIR");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "similarGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.similar.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}

	}

	private static void groceriesSimilarGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/similar");
		conf.setStrings("data.input.path", "groceries");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIR");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "similarGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.similar.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}

	}

	private static List<double[]> getCommonItems(SequentialSparseVector thisVector, SequentialSparseVector thatVector) {
		List<Double> thisList = new ArrayList<>();
		List<Double> thatList = new ArrayList<>();

		int thisPosition = 0, thatPosition = 0;
		int thisSize = thisVector.getNumEntries(), thatSize = thatVector.getNumEntries();
		int thisIndex, thatIndex;
		while (thisPosition < thisSize && thatPosition < thatSize) {
			thisIndex = thisVector.getIndexAtPosition(thisPosition);
			thatIndex = thatVector.getIndexAtPosition(thatPosition);
			if (thisIndex == thatIndex) {
				thisList.add(thisVector.getAtPosition(thisPosition));
				thatList.add(thatVector.getAtPosition(thatPosition));
				thisPosition++;
				thatPosition++;
			} else if (thisIndex > thatIndex) {
				thatPosition++;
			} else {
				thisPosition++;
			}
		}
		ArrayList<double[]> returnList = new ArrayList<double[]>();
		double[] thisArray = new double[thisList.size()];
		double[] thatArray = new double[thatList.size()];
		for (int i = 0; i < thisArray.length; i++) {
			thisArray[i] = thisList.get(i);
			thatArray[i] = thatList.get(i);
		}
		returnList.add(thisArray);
		returnList.add(thatArray);
		return returnList;
	}

	private static boolean shareMinimumItems(int[] indices, int[] indices2, Configuration conf) {
		int minimumItemsShared = conf.getInt("group.similar.minimumItems", 5);
		int shared = 0;
		int index1 = 0;
		int index2 = 0;
		while (index1 < indices.length & index2 < indices2.length) {
			int item1 = indices[index1];
			int item2 = indices2[index2];
			if (item1 < item2) {
				index1++;
			} else if (item2 < item1) {
				index2++;
			} else {
				index1++;
				index2++;
				shared++;
			}
			if (shared >= minimumItemsShared) {
				return true;
			}
		}
		return false;
	}

	private static void videoGamesSimilarGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/similar");
		conf.setStrings("data.input.path", "videogames");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIR");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "similarGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.similar.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}
	}

	private static void videogamesRandomGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/random");
		conf.setStrings("data.input.path", "videogames");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "randomGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.random.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}

	}

	private static void amazonSimilarGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/similar");
		conf.setStrings("data.input.path", "amazon");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "similarGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.similar.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}

	}

	private static void amazonRandomGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/random");
		conf.setStrings("data.input.path", "amazon");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", ",");
		conf.set("group.builder", "randomGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.random.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}

	}

	@SuppressWarnings("unchecked")
	private static void ratingUseCase(String confPath, String dataset, String separator)
			throws LibrecException, ClassNotFoundException {
		Configuration conf = new Configuration(false);
		conf.addResource(new Resource(confPath));
		String[] methodList = new String[] { "addUtil", "leastM", "mostP" };
//		String [] clusteringExternalFiles = new String[] {"random_2", "random_3", "random_4", "random_5", "random_6", "random_7", "random_8", "similar_2", "similar_3", "similar_4", "similar_5", "similar_6", "similar_7", "similar_8"};
		String[] clusteringExternalFiles = new String[] { "random_2", "random_3", "random_4", "random_5", "random_6",
				"random_7", "random_8", "similar_2", "similar_3", "similar_4", "similar_5", "similar_6", "similar_7" };

		conf.set("data.input.path", dataset);
		conf.set("data.convert.sep", separator);

		Long seed = conf.getLong("rec.random.seed");
		if (seed != null) {
			Randoms.seed(seed);
		}

		for (String clustering : clusteringExternalFiles) {
			String clusterFolder = clustering.split("_")[0];
			conf.setStrings("group.external.path", "../../../ThesisResults/clusters/" + clusterFolder + "/" + dataset
					+ "/groupAssignation_" + clustering + ".csv");
			conf.set("group.model", "addUtil");
			GroupDataModel gdm = new GroupDataModel();
			gdm.setConf(conf);
			conf.setBoolean("data.convert.read.ready", false);
			gdm.buildDataModel();

			RecommenderContext recContext = new RecommenderContext(conf);

			GroupRecommender rec = new GroupRecommender();
			rec.setContext(recContext);

			while (gdm.hasNextFold()) {
				gdm.nextFold();
				recContext.setDataModel(gdm);

				Class<? extends RecommenderSimilarity> similarityClass = (Class<? extends RecommenderSimilarity>) DriverClassUtil
						.getClass(conf.get("rec.similarity.class"));

				RecommenderSimilarity similarity = ReflectionUtil.newInstance(similarityClass, conf);
				similarity.buildSimilarityMatrix(gdm);
				recContext.setSimilarity(similarity);

				rec.train(recContext);

				DataSet predictDataSet = gdm.getTestDataSet();

				RecommendedList baseRating = rec.getBaseRating(predictDataSet);

				Map<String, Long> timeMeasures = new HashMap<String, Long>();

				for (String groupMod : methodList) {
//					Retrieve groupmodeling and set it in group model.
					Class<? extends GroupModeling> groupModelingClass;
					try {
						groupModelingClass = (Class<? extends GroupModeling>) DriverClassUtil.getClass(groupMod);
					} catch (ClassNotFoundException e) {
						throw new LibrecException(e);
					}

					gdm.setGroupModeling(ReflectionUtil.newInstance((Class<GroupModeling>) groupModelingClass, conf));

					long startTime = System.currentTimeMillis();
					RecommendedList groupRecommendations = rec.buildGroupRecommendations(baseRating);
					long endTime = System.currentTimeMillis();

					timeMeasures.put(groupMod, endTime - startTime);

					EvalContext evc = new EvalContext(conf, groupRecommendations,
							(SequentialAccessSparseMatrix) predictDataSet);

					RecommendedList memberGroundTruth = evc.getGroundTruthList();

					String[] evalClassKeys = conf.getStrings("rec.eval.classes");
					BiMap<Integer, String> userMap = gdm.getUserMappingData().inverse();

					for (String evalClass : evalClassKeys) {
						Class<? extends RecommenderEvaluator> evaluatorClass = (Class<? extends RecommenderEvaluator>) DriverClassUtil
								.getClass(evalClass);
						RecommenderEvaluator eval = ReflectionUtil.newInstance(evaluatorClass, null);

						System.out.println(evalClass);

						Double membersOverallEvaluation = eval.evaluate(recContext, memberGroundTruth,
								groupRecommendations);

						Map<String, KeyValue<Double, Integer>> membersEvaluations = new HashMap<String, KeyValue<Double, Integer>>();

						for (int contextId = 0; contextId < groupRecommendations.size(); contextId++) {
							RecommendedList individualGroupList = new RecommendedList(1);
							individualGroupList.addList((ArrayList<KeyValue<Integer, Double>>) groupRecommendations
									.getKeyValueListByContext(contextId));
							RecommendedList expectedGruopList = new RecommendedList(1);
							expectedGruopList.addList((ArrayList<KeyValue<Integer, Double>>) memberGroundTruth
									.getKeyValueListByContext(contextId));
							Double memberMeasure = eval.evaluate(recContext, expectedGruopList, individualGroupList);
							membersEvaluations.put(userMap.get(contextId), new KeyValue<Double, Integer>(memberMeasure,
									expectedGruopList.getKeySetByContext(0).size()));
						}
						String fileName = groupMod + "_" + clustering + "_";
						saveRecsysResults(fileName, conf, evalClass, membersOverallEvaluation, membersEvaluations);

					}
				}
				saveTimeResults(timeMeasures, clustering, conf, "rating");

			}
		}

	}

	@SuppressWarnings("unchecked")
	private static void rankingUseCase(String confPath, String dataset, String separator)
			throws LibrecException, ClassNotFoundException {
		Configuration conf = new Configuration(false);
		conf.addResource(new Resource(confPath));
		String[] methodList = new String[] { "addUtil", "leastM", "mostP", "multUtil", "borda", "plurality", "approval",
				"avgWOM", "fairness" };
//		String [] clusteringExternalFiles = new String[] {"random_2", "random_3", "random_4", "random_5", "random_6", "random_7", "random_8", "similar_2", "similar_3", "similar_4", "similar_5", "similar_6", "similar_7", "similar_8"};
		String[] clusteringExternalFiles = new String[] { "random_2", "random_3", "random_4", "random_5", "random_6",
				"random_7", "random_8", "similar_2", "similar_3", "similar_4", "similar_5", "similar_6", "similar_7" };

		conf.set("data.input.path", dataset);
		conf.set("data.convert.sep", separator);

		Long seed = conf.getLong("rec.random.seed");
		if (seed != null) {
			Randoms.seed(seed);
		}

		for (String clustering : clusteringExternalFiles) {
			String clusterFolder = clustering.split("_")[0];
			conf.setStrings("group.external.path", "../../../ThesisResults/clusters/" + clusterFolder + "/" + dataset
					+ "/groupAssignation_" + clustering + ".csv");
			conf.set("group.model", "addUtil");
			GroupDataModel gdm = new GroupDataModel();
			gdm.setConf(conf);
			conf.setBoolean("data.convert.read.ready", false);
			gdm.buildDataModel();

			RecommenderContext recContext = new RecommenderContext(conf);

			GroupRecommender rec = new GroupRecommender();
			rec.setContext(recContext);

			while (gdm.hasNextFold()) {
				gdm.nextFold();
				recContext.setDataModel(gdm);

				String similarityClassString = conf.get("rec.similarity.class");

				if (similarityClassString != null) {
					Class<? extends RecommenderSimilarity> similarityClass = (Class<? extends RecommenderSimilarity>) DriverClassUtil
							.getClass(conf.get("rec.similarity.class"));

					RecommenderSimilarity similarity = ReflectionUtil.newInstance(similarityClass, conf);
					similarity.buildSimilarityMatrix(gdm);
					recContext.setSimilarity(similarity);
				}

				rec.train(recContext);

				DataSet predictDataSet = gdm.getTestDataSet();

				RecommendedList baseRanking = rec.getBaseRanking();

				Map<String, Long> timeMeasures = new HashMap<String, Long>();

				for (String groupMod : methodList) {
//					Retrieve groupmodeling and set it in group model.
					Class<? extends GroupModeling> groupModelingClass;
					try {
						groupModelingClass = (Class<? extends GroupModeling>) DriverClassUtil.getClass(groupMod);
					} catch (ClassNotFoundException e) {
						throw new LibrecException(e);
					}

					gdm.setGroupModeling(ReflectionUtil.newInstance((Class<GroupModeling>) groupModelingClass, conf));

					long startTime = System.currentTimeMillis();
					RecommendedList groupRecommendations = rec.buildGroupRecommendations(baseRanking);
					long endTime = System.currentTimeMillis();

					timeMeasures.put(groupMod, endTime - startTime);

					int topN = conf.getInt("rec.recommender.ranking.topn");
					groupRecommendations.topNRank(topN);

					EvalContext evc = new EvalContext(conf, groupRecommendations,
							(SequentialAccessSparseMatrix) predictDataSet);

					RecommendedList memberGroundTruth = evc.getGroundTruthList();

					String[] evalClassKeys = conf.getStrings("rec.eval.classes");
					BiMap<Integer, String> userMap = gdm.getUserMappingData().inverse();

					for (String evalClass : evalClassKeys) {
						Class<? extends RecommenderEvaluator> evaluatorClass = (Class<? extends RecommenderEvaluator>) DriverClassUtil
								.getClass(evalClass);
						RecommenderEvaluator eval = ReflectionUtil.newInstance(evaluatorClass, null);
						eval.setTopN(topN);

						System.out.println(evalClass);

						Double membersOverallEvaluation = eval.evaluate(recContext, memberGroundTruth,
								groupRecommendations);

						Map<String, KeyValue<Double, Integer>> membersEvaluations = new HashMap<String, KeyValue<Double, Integer>>();

						for (int contextId = 0; contextId < groupRecommendations.size(); contextId++) {
							RecommendedList individualGroupList = new RecommendedList(1);
							individualGroupList.addList((ArrayList<KeyValue<Integer, Double>>) groupRecommendations
									.getKeyValueListByContext(contextId));
							RecommendedList expectedGruopList = new RecommendedList(1);
							expectedGruopList.addList((ArrayList<KeyValue<Integer, Double>>) memberGroundTruth
									.getKeyValueListByContext(contextId));
							Double memberMeasure = eval.evaluate(recContext, expectedGruopList, individualGroupList);
							membersEvaluations.put(userMap.get(contextId), new KeyValue<Double, Integer>(memberMeasure,
									expectedGruopList.getKeySetByContext(0).size()));
						}
						String fileName = groupMod + "_" + clustering + "_";
						saveRecsysResults(fileName, conf, evalClass, membersOverallEvaluation, membersEvaluations);

					}
				}
				saveTimeResults(timeMeasures, clustering, conf, "ranking");
			}
		}

	}

	private static void saveTimeResults(Map<String, Long> timeMeasures, String clustering, Configuration conf,
			String rankingOrRating) {
		String baseRec = conf.get("group.base.recommender.class");
		String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/time/" + rankingOrRating
				+ "/" + baseRec + "/time_" + clustering + ".txt";
		System.out.println("Time result path is " + outputPath);
		StringBuilder sb = new StringBuilder();
		for (String groupMod : timeMeasures.keySet()) {
			sb.append(groupMod).append(",").append(timeMeasures.get(groupMod).toString()).append(",").append("\n");
		}

		String resultData = sb.toString();
		try {
			FileUtil.writeString(outputPath, resultData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void saveRecsysResults(String fileName, Configuration conf, String evalClass,
			Double membersOverallEvaluation, Map<String, KeyValue<Double, Integer>> memberEvaluations) {
		String baseRec = conf.get("group.base.recommender.class");
		String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/eval/" + baseRec + "/"
				+ fileName + evalClass.toString() + ".txt";
		System.out.println("Evaluation result path is " + outputPath);
		// convert itemList to string
		StringBuilder sb = new StringBuilder();
		sb.append("Members Overall Measure:").append(membersOverallEvaluation.toString()).append("\n");
		sb.append("Members:").append("\n");
		for (String member : memberEvaluations.keySet()) {
			sb.append(member).append(",").append(memberEvaluations.get(member).getKey().toString()).append(",")
					.append(memberEvaluations.get(member).getValue().toString()).append("\n");
		}

		String resultData = sb.toString();
		try {
			FileUtil.writeString(outputPath, resultData);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	static void movielens1MSimilarGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/similar");
		conf.setStrings("data.input.path", "movielens1m");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", "::");
		conf.set("group.builder", "similarGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.similar.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}
	}

	static void movielens1MRandomGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../ThesisResults/clusters/random");
		conf.setStrings("data.input.path", "movielens1m");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", "::");
		conf.set("group.builder", "randomGroups");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		conf.setDouble("data.splitter.trainset.ratio", 0.8);
		conf.set("data.model.format", "addUtil");

		conf.setInt("rec.random.seed", 1);

		int[] groupSizeList = new int[] { 2, 3, 4, 5, 6, 7, 8 };
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.random.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}
	}

	static void driverTest(String configFile) throws Exception {
		RecDriver drive = new RecDriver();
		String[] args = new String[3];
		args[0] = "-exec";
		args[1] = "-conf";
		args[2] = configFile;
		drive.run(args);
	}

}
