/**
 * 
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.BiMap;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configuration.Resource;
import net.librec.data.model.GroupDataModel;
import net.librec.data.model.group.GroupModeling;
import net.librec.eval.EvalContext;
import net.librec.eval.RecommenderEvaluator;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.GroupRecommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.RecommenderSimilarity;
import net.librec.tool.driver.RecDriver;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;
import net.librec.util.ReflectionUtil;

/**
 * @author Joaqui
 *
 */
public class RecSysExperiments {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		driverTest("..\\core\\target\\classes\\group.properties");
//		driverTest("..\\core\\target\\classes\\group-movielens.properties");
//		movielens1MRandomGroupGeneration();
//		movielens1MSimilarGeneration();
//		ratingUseCase("useCaseRatingBiasedMF.properties");
//		ratingUseCase("useCaseRatingUserKNN.properties");
//		ratingUseCase("useCaseBiasedMF.properties");
//		rankingUseCase("useCaseBPR.properties");
//		rankingUseCase("useCaseBiasedMFRank.properties");
//		rankingUseCase("useCaseUserKNNRank.properties");
	}

	@SuppressWarnings("unchecked")
	private static void rankingUseCase(String confPath) throws LibrecException, ClassNotFoundException{
		Configuration conf = new Configuration(false);
		conf.addResource(new Resource(confPath));
		String [] methodList = new String[] {"addUtil", "leastM", "mostP", "multUtil", "borda", "copeland", "plurality", "approval", "avgWOM", "fairness"};
		String [] clusteringExternalFiles = new String[] {"random_2", "random_3", "random_4", "random_8", "random_k20", "similar_2", "similar_3", "similar_4", "similar_8", "kmeans_20"};
		
		for (String clustering : clusteringExternalFiles) {
			conf.setStrings("group.external.path", "../../../clusters/movielens1m/groupAssignation_"+clustering+".csv");
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
				
				for (String groupMod : methodList) {
//					Retrieve groupmodeling and set it in group model.
					Class<? extends GroupModeling> groupModelingClass;
					try {
						groupModelingClass = (Class<? extends GroupModeling>) DriverClassUtil
								.getClass(groupMod);
					} catch (ClassNotFoundException e) {
						throw new LibrecException(e);
					}
					
					gdm.setGroupModeling(ReflectionUtil.newInstance((Class<GroupModeling>) groupModelingClass, conf));
					
					
					RecommendedList groupRecommendations = rec.buildGroupRecommendations(baseRanking);
					int topN = conf.getInt("rec.recommender.ranking.topn");
					groupRecommendations.topNRank(topN);
					
					EvalContext evc = new EvalContext(conf, groupRecommendations, (SequentialAccessSparseMatrix)predictDataSet);
					
					RecommendedList memberGroundTruth = evc.getGroundTruthList();
					
					String[] evalClassKeys = conf.getStrings("rec.eval.classes");
					BiMap<Integer, String> userMap = gdm.getUserMappingData().inverse();
						

					for (String evalClass : evalClassKeys) {
						Class<? extends RecommenderEvaluator> evaluatorClass = (Class<? extends RecommenderEvaluator>) DriverClassUtil
								.getClass(evalClass);
						RecommenderEvaluator eval = ReflectionUtil.newInstance(evaluatorClass, null);
						eval.setTopN(topN);
						
						System.out.println(evalClass);
						
						Double membersOverallEvaluation = eval.evaluate(recContext,memberGroundTruth, groupRecommendations);
						
						Map<String, KeyValue<Double, Integer>> membersEvaluations = new HashMap<String, KeyValue<Double,Integer>>();
						
						for (int contextId = 0; contextId < groupRecommendations.size(); contextId++) {
							RecommendedList individualGroupList = new RecommendedList(1);
							individualGroupList.addList((ArrayList<KeyValue<Integer, Double>>) groupRecommendations.getKeyValueListByContext(contextId));
							RecommendedList expectedGruopList = new RecommendedList(1);
							expectedGruopList.addList((ArrayList<KeyValue<Integer, Double>>) memberGroundTruth.getKeyValueListByContext(contextId));
							Double memberMeasure = eval.evaluate(recContext, expectedGruopList, individualGroupList);
							membersEvaluations.put(userMap.get(contextId), new KeyValue<Double, Integer>(memberMeasure, expectedGruopList.getKeySetByContext(0).size()));
						}
						String fileName = groupMod+"_"+clustering+"_";
						saveRecsysResults(fileName, conf, evalClass, membersOverallEvaluation, membersEvaluations);
						
					}
				}

				
			}
		}
		
	
	}

	@SuppressWarnings("unchecked")
	private static void ratingUseCase(String confPath) throws LibrecException, ClassNotFoundException {
		Configuration conf = new Configuration(false);
		conf.addResource(new Resource(confPath));
		String [] methodList = new String[] {"addUtil", "leastM", "mostP"};
		String [] clusteringExternalFiles = new String[] {"random_2", "random_3", "random_4", "random_8", "random_k20", "similar_2", "similar_3", "similar_4", "similar_8", "kmeans_20"};
		
		for (String clustering : clusteringExternalFiles) {
			conf.setStrings("group.external.path", "../../../clusters/movielens1m/groupAssignation_"+clustering+".csv");
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
				
				for (String groupMod : methodList) {
//					Retrieve groupmodeling and set it in group model.
					Class<? extends GroupModeling> groupModelingClass;
					try {
						groupModelingClass = (Class<? extends GroupModeling>) DriverClassUtil
								.getClass(groupMod);
					} catch (ClassNotFoundException e) {
						throw new LibrecException(e);
					}
					
					gdm.setGroupModeling(ReflectionUtil.newInstance((Class<GroupModeling>) groupModelingClass, conf));
					
					
					RecommendedList groupRecommendations = rec.buildGroupRecommendations(baseRating);
					
					EvalContext evc = new EvalContext(conf, groupRecommendations, (SequentialAccessSparseMatrix)predictDataSet);
					
					RecommendedList memberGroundTruth = evc.getGroundTruthList();
					
					String[] evalClassKeys = conf.getStrings("rec.eval.classes");
					BiMap<Integer, String> userMap = gdm.getUserMappingData().inverse();
						

					for (String evalClass : evalClassKeys) {
						Class<? extends RecommenderEvaluator> evaluatorClass = (Class<? extends RecommenderEvaluator>) DriverClassUtil
								.getClass(evalClass);
						RecommenderEvaluator eval = ReflectionUtil.newInstance(evaluatorClass, null);
						
						System.out.println(evalClass);
						
						Double membersOverallEvaluation = eval.evaluate(recContext,memberGroundTruth, groupRecommendations);
						
						Map<String, KeyValue<Double, Integer>> membersEvaluations = new HashMap<String, KeyValue<Double,Integer>>();
						
						for (int contextId = 0; contextId < groupRecommendations.size(); contextId++) {
							RecommendedList individualGroupList = new RecommendedList(1);
							individualGroupList.addList((ArrayList<KeyValue<Integer, Double>>) groupRecommendations.getKeyValueListByContext(contextId));
							RecommendedList expectedGruopList = new RecommendedList(1);
							expectedGruopList.addList((ArrayList<KeyValue<Integer, Double>>) memberGroundTruth.getKeyValueListByContext(contextId));
							Double memberMeasure = eval.evaluate(recContext, expectedGruopList, individualGroupList);
							membersEvaluations.put(userMap.get(contextId), new KeyValue<Double, Integer>(memberMeasure, expectedGruopList.getKeySetByContext(0).size()));
						}
						String fileName = groupMod+"_"+clustering+"_";
						saveRecsysResults(fileName, conf, evalClass, membersOverallEvaluation, membersEvaluations);
						
					}
				}

				
			}
		}
		
		
		
		
	}
	
	private static void saveRecsysResults(String fileName, Configuration conf, String evalClass, Double membersOverallEvaluation, Map<String, KeyValue<Double, Integer>> memberEvaluations) {
		String baseRec = conf.get("group.base.recommender.class");
		String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/eval/"+baseRec+"/"+fileName+evalClass.toString()+".txt";
		System.out.println("Evaluation result path is " + outputPath);
		// convert itemList to string
		StringBuilder sb = new StringBuilder();
		sb.append("Members Overall Measure:").append(membersOverallEvaluation.toString()).append("\n");
		sb.append("Members:").append("\n");
		for (String member : memberEvaluations.keySet()) {
			sb.append(member).append(",").append(memberEvaluations.get(member).getKey().toString()).append(",").append(memberEvaluations.get(member).getValue().toString()).append("\n");
		}
		
		String resultData = sb.toString();
		try {
			FileUtil.writeString(outputPath, resultData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	static void movielens1MRandomGroupGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../clusters");
		conf.setStrings("data.input.path", "movielens1m");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", "::");
		conf.set("group.builder", "similarGroups");
		conf.set("data.model.splitter","ratio");
		conf.set("data.splitter.ratio","rating");
		conf.setDouble("data.splitter.trainset.ratio",0.8);
		conf.set("data.model.format","addUtil");
		
		conf.setInt("rec.random.seed", 1);
		
		int[] groupSizeList = new int[]{2,3,4,8};
		for (int i = 0; i < groupSizeList.length; i++) {
			int sizeGroups = groupSizeList[i];
			conf.setInt("group.similar.groupSize", sizeGroups);
			conf.setBoolean("data.convert.read.ready", false);
			GroupDataModel model = new GroupDataModel();
			model.setConf(conf);
			model.buildDataModel();
		}
	}
	
	static void movielens1MSimilarGeneration() throws LibrecException {
		Configuration conf = new Configuration(false);
		conf.setStrings("dfs.result.dir", "../../../clusters");
		conf.setStrings("data.input.path", "movielens1m");
		conf.setStrings("dfs.data.dir", "../../../DataSets");
		conf.setStrings("data.column.format", "UIRT");
		conf.setBoolean("group.save", true);
		conf.set("data.convert.sep", "::");
		conf.set("group.builder", "randomGroups");
		conf.set("data.model.splitter","ratio");
		conf.set("data.splitter.ratio","rating");
		conf.setDouble("data.splitter.trainset.ratio",0.8);
		conf.set("data.model.format","addUtil");
		
		conf.setInt("rec.random.seed", 1);
		
		int[] groupSizeList = new int[]{2,3,4,8};
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
