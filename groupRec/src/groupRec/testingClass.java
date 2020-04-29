/**
 * 
 */
package groupRec;

import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.Table;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configuration.Resource;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.data.splitter.KCVDataSplitter;
import net.librec.data.splitter.LOOCVDataSplitter;
import net.librec.eval.EvalContext;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.ranking.NormalizedDCGEvaluator;
import net.librec.eval.ranking.PrecisionEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.ItemKNNRecommender;
import net.librec.recommender.cf.ranking.BPRRecommender;
import net.librec.recommender.item.RecommendedItem;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.CosineSimilarity;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;

/**
 * @author Joaqui
 *
 */
public class testingClass {

	/**
	 * @param args
	 * @throws LibrecException
	 */
	public static void main(String[] args) throws LibrecException {
		System.out.println("Running examples for single recommender systems");

//		The first example uses the movieLens 100-k with a KNN item recommender
//		firstExample();
//		The second example uses the filmtrust dataset with a KNN item recommender
//		secondExample();
//		The third example uses a ranked recommender with the movieLens 100-k with BPR recommender
//		thirdExample();
//		The fourth examples uses a ranked recommender with movieLens 100-k and ItemKnn recommender with NDCG evaluator
//		fourthExample();
//		The fifth example uses a ranked recommender with movieLens 100-k and ItemKnn recommender with precision evaluator
//		fifthExample();
//		groupExample();
//		fillSpareceMatrixExample();
		groupNewExample();

		System.out.println("This should have ended");

	}

	static void firstExample() throws LibrecException {
		// build data model
		Configuration conf = new Configuration();
		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		Randoms.seed(1);
		// TODO Revert this to TextDataModel instead of GroupDataModel once finished
		// testing
		DataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		// build recommendercontext
		RecommenderContext context = new RecommenderContext(conf, dataModel);

		// build similarity
		conf.set("rec.recommender.similarity.key", "item");
		RecommenderSimilarity similarity = new PCCSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		context.setSimilarity(similarity);

		// build recommender
		conf.set("rec.neighbors.knn.number", "5");
		Recommender recommender = new ItemKNNRecommender();
		recommender.setContext(context);

		// run recommenderalgorithm
		recommender.train(context);

		// evaluate therecommended result
		RecommenderEvaluator evaluator = new RMSEEvaluator();

		DataSet testDataSet = dataModel.getTestDataSet();

//		RecommendedList recommendedList = recommender.recommendRating(testDataSet);

		EvalContext evalContx = new EvalContext(conf, recommender, testDataSet);

		double result = evaluator.evaluate(evalContx);

		System.out.println("The result for the RMSE evaluation is:" + result);
	}

	static void secondExample() throws LibrecException {
		// build data model (This step is the configuration of the informations for the
		// recommender)
		Configuration conf = new Configuration(false);
		Resource resource = new Resource("librec.properties");
		conf.addResource(resource);

		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		conf.set("data.input.path", "filmtrust/rating");
		conf.set("data.column.format", "UIR");

		Randoms.seed(1);
		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();
		dataModel.getTrainDataSet();

		// build recommendercontext
		RecommenderContext context = new RecommenderContext(conf, dataModel);

		// build similarity
		conf.set("rec.recommender.similarity.key", "item");
		RecommenderSimilarity similarity = new PCCSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		context.setSimilarity(similarity);

		// build recommender
		conf.set("rec.neighbors.knn.number", "5");
		Recommender recommender = new ItemKNNRecommender();
		recommender.setContext(context);

		// run recommenderalgorithm
		recommender.train(context);

		// evaluate therecommended result
		RecommenderEvaluator evaluator = new RMSEEvaluator();

		DataSet testDataSet = dataModel.getTestDataSet();

//		RecommendedList recommendedList = recommender.recommendRating(testDataSet);

		EvalContext evalContx = new EvalContext(conf, recommender, testDataSet);

		double result = evaluator.evaluate(evalContx);

		System.out.println("The result for the RMSE evaluation is:" + result);
	}

	static void thirdExample() throws LibrecException {
		// build data model
		Configuration conf = new Configuration();
		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		conf.set("rec.recommender.isranking", "true");
		Randoms.seed(1);
		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();
		Randoms.seed(1);

		// build recommendercontext
		RecommenderContext context = new RecommenderContext(conf, dataModel);

		// build similarity
		conf.set("rec.recommender.similarity.key", "item");
		RecommenderSimilarity similarity = new PCCSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		context.setSimilarity(similarity);

		// build recommender
		conf.set("rec.neighbors.knn.number", "5");
		Recommender recommender = new BPRRecommender();
		recommender.setContext(context);

		// run recommenderalgorithm
		recommender.train(context);

		// evaluate therecommended result
		RecommenderEvaluator evaluator = new PrecisionEvaluator();
		evaluator.setTopN(10);

		DataSet testDataSet = dataModel.getTestDataSet();

//				RecommendedList recommendedList = recommender.recommendRank(testDataSet);
//		RecommendedList recommendedList = recommender.recommendRank();

//				EvalContext evalContx = new EvalContext(conf, recommender, testDataSet);
		EvalContext evalContx = new EvalContext(conf, recommender, testDataSet);

		double result = evaluator.evaluate(evalContx);

		System.out.println("The result for the Precision of the top10 evaluation is: " + result);
	}

	static void fourthExample() throws LibrecException {
		// build data model
		Configuration conf = new Configuration();
		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		// build recommender context
		RecommenderContext context = new RecommenderContext(conf, dataModel);

		// build similarity
		conf.set("rec.recommender.similarity.key", "item");
		conf.setBoolean("rec.recommender.isranking", true);
		conf.setInt("rec.similarity.shrinkage", 10);
		RecommenderSimilarity similarity = new CosineSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		context.setSimilarity(similarity);

		// build recommender
		conf.set("rec.neighbors.knn.number", "200");
		Recommender recommender = new ItemKNNRecommender();
		recommender.setContext(context);

		// run recommender algorithm
		recommender.train(context);

		// evaluate the recommended result
		EvalContext evalContext = new EvalContext(conf, recommender, dataModel.getTestDataSet(),
				context.getSimilarity().getSimilarityMatrix(), context.getSimilarities());
		RecommenderEvaluator ndcgEvaluator = new NormalizedDCGEvaluator();
		ndcgEvaluator.setTopN(10);
		double ndcgValue = ndcgEvaluator.evaluate(evalContext);
		System.out.println("ndcg:" + ndcgValue);
	}

	static void fifthExample() throws LibrecException {
		// build data model
		Configuration conf = new Configuration();
		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		// build recommender context
		RecommenderContext context = new RecommenderContext(conf, dataModel);

		// build similarity
		conf.set("rec.recommender.similarity.key", "item");
		conf.setBoolean("rec.recommender.isranking", true);
		conf.setInt("rec.similarity.shrinkage", 10);
//		RecommenderSimilarity similarity = new CosineSimilarity();
		RecommenderSimilarity similarity = new PCCSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		context.setSimilarity(similarity);

		// build recommender
		conf.set("rec.neighbors.knn.number", "200");
		Recommender recommender = new ItemKNNRecommender();
		recommender.setContext(context);

		// run recommender algorithm
		recommender.train(context);

		// evaluate the recommended result
		EvalContext evalContext = new EvalContext(conf, recommender, dataModel.getTestDataSet(),
				context.getSimilarity().getSimilarityMatrix(), context.getSimilarities());
//		RecommenderEvaluator ndcgEvaluator = new NormalizedDCGEvaluator();
		RecommenderEvaluator precisionEvaluator = new PrecisionEvaluator();
		precisionEvaluator.setTopN(10);
		double precision = precisionEvaluator.evaluate(evalContext);
		System.out.println("precision:" + precision);
	}

	static void groupExample() throws LibrecException {
		// build data model
		Configuration conf = new Configuration();
		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		GroupDataModel dataModel = new GroupDataModel(conf);
		dataModel.buildDataModel();

		Map<Integer, Integer> groupAssignation = dataModel.getGroupAssignation();

		Map<Integer, List<Integer>> groups = dataModel.getGroups();

//		DataSet trainDataSet = dataModel.getTrainDataSet();

		// build recommender context
		RecommenderContext context = new RecommenderContext(conf, dataModel);

		// build similarity
		RecommenderSimilarity similarity = new CosineSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		context.setSimilarity(similarity);

		// build recommender
		conf.set("rec.neighbors.knn.number", "200");
		Recommender recommender = new ItemKNNRecommender();
		recommender.setContext(context);

		// run recommender algorithm
		recommender.train(context);

		// evaluate the recommended result
		EvalContext evalContext = new EvalContext(conf, recommender, dataModel.getTestDataSet(),
				context.getSimilarity().getSimilarityMatrix(), context.getSimilarities());
		RecommenderEvaluator ndcgEvaluator = new NormalizedDCGEvaluator();
		ndcgEvaluator.setTopN(10);
		double ndcgValue = ndcgEvaluator.evaluate(evalContext);
		System.out.println("ndcg:" + ndcgValue);
	}

	static void fillSpareceMatrixExample() throws LibrecException {
		Configuration conf = new Configuration();

		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		conf.set("rec.recommender.similarity.key", "item");
		conf.set("rec.neighbors.knn.number", "10");

		TextDataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();

		RecommenderContext reccontext = new RecommenderContext(conf, dataModel);

		RecommenderSimilarity similarity = new CosineSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		reccontext.setSimilarity(similarity);

		ItemKNNRecommender rec = new ItemKNNRecommender();
		rec.setContext(reccontext);

		rec.train(reccontext);

		SequentialAccessSparseMatrix trainDataSet = (SequentialAccessSparseMatrix) dataModel.getTrainDataSet();

//		TODO change this because the method is deprecated !!!!!!!!!!!!!!!
		Table<Integer, Integer, Double> trainData = trainDataSet.getDataTable();

		int numUsers = trainDataSet.rowSize();
		int numItems = trainDataSet.columnSize();
		double[][] ratings = new double[numUsers][numItems];
		for (int i = 0; i < numUsers; i++) {
			for (int j = 0; j < numItems; j++) {
				if (trainData.contains(i, j)) {
					ratings[i][j] = trainData.get(i, j);
				} else {
					ratings[i][j] = rec.predict(i, j);
				}
			}
		}

	}

	static void groupNewExample() throws LibrecException {
		Configuration conf = new Configuration(false);
		Resource resource = new Resource("group.properties");
		conf.addResource(resource);

		GroupDataModel dataModel = new GroupDataModel(conf);
		dataModel.buildDataModel();

		RecommenderContext reccontext = new RecommenderContext(conf, dataModel);

		RecommenderSimilarity similarity = new CosineSimilarity();
		similarity.buildSimilarityMatrix(dataModel);
		reccontext.setSimilarity(similarity);

		GroupRecommender rec = new AdditiveUtilitarianRecommender();
		rec.setContext(reccontext);

		rec.train(reccontext);

		RecommendedList groupRecommendations = rec.recommendRating(dataModel.getTestDataSet());

		saveResult(rec.getRecommendedList(groupRecommendations), conf);
//		saveGroups(dataModel.getGroupAssignation(), dataModel.getUserMappingData());

		System.out.println(groupRecommendations.size());

	}

//	NOT NEEDED: THE GROUPDATAMODEL SAVES THE GROUPS IF THEY ARE GENERATED
//	static void saveGroups(Map<Integer, Integer> groupAssignation, BiMap<String,Integer> userMapping) {
//		String outputPath = "./results/GroupAssignation.csv";
//		System.out.println("Result path is " + outputPath);
//		BiMap<Integer, String> inverseUserMapping = userMapping.inverse();
//		// convert itemList to string
//		StringBuilder sb = new StringBuilder();
//		for (Integer userID : groupAssignation.keySet()) {
//			String userId = inverseUserMapping.get(userID);	
//			String groupId = Integer.toString(groupAssignation.get(userID));
//			sb.append(userId).append(",").append(groupId).append("\n");
//		}
//		String resultData = sb.toString();
//		// save resultData
//		try {
//			FileUtil.writeString(outputPath, resultData);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

//	TODO This method should be in the recommender job/driver
	static void saveResult(List<RecommendedItem> recommendedList, Configuration conf) {
		if (recommendedList != null && recommendedList.size() > 0) {
			String outputPath = conf.get("dfs.result.dir") + "/" + conf.get("data.input.path") + "/GroupRec.csv";
			System.out.println("Result path is " + outputPath);
			// convert itemList to string
			StringBuilder sb = new StringBuilder();
			for (RecommendedItem recItem : recommendedList) {
				String groupId = recItem.getUserId();
				String itemId = recItem.getItemId();
				String value = String.valueOf(recItem.getValue());
				sb.append(groupId).append(",").append(itemId).append(",").append(value).append("\n");
			}
			String resultData = sb.toString();
			// save resultData
			try {
				FileUtil.writeString(outputPath, resultData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
