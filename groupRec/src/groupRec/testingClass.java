/**
 * 
 */
package groupRec;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.model.TextDataModel;
import net.librec.eval.EvalContext;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.ranking.NormalizedDCGEvaluator;
import net.librec.eval.ranking.PrecisionEvaluator;
import net.librec.eval.rating.RMSEEvaluator;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DataSet;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.cf.ItemKNNRecommender;
import net.librec.recommender.cf.ranking.BPRRecommender;
import net.librec.recommender.item.RecommendedList;
import net.librec.similarity.CosineSimilarity;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;

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
		firstExample();
//		The second example uses the filmtrust dataset with a KNN item recommender
//		secondExample();
//		The third example uses a ranked recommender with the movieLens 100-k with BPR recommender
//		thirdExample();
//		The fourth examples uses a ranked recommender with movieLens 100-k and ItemKnn recommender
//		fourthExample();

		System.out.println("This should have ended");

	}

	static void firstExample() throws LibrecException {
		// build data model
		Configuration conf = new Configuration();
		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		Randoms.seed(1);
		// TODO Revert this to TextDataModel instead of GroupDataModel once finished
		// testing
		GroupDataModel dataModel = new GroupDataModel(conf);
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

		RecommendedList recommendedList = recommender.recommendRating(testDataSet);

		EvalContext evalContx = new EvalContext(conf, recommender, testDataSet);

		double result = evaluator.evaluate(evalContx);

		System.out.println("The result for the RMSE evaluation is:" + result);
	}

	static void secondExample() throws LibrecException {
		// build data model (This step is the configuration of the informations for the
		// recommender)
//				Configuration conf = new Configuration(false);
		Configuration conf = new Configuration();

//				// TODO Find out how to do this through the resource (.properties) files instead
		// of just setting everything manually
//				Resource resource = new Resource("librec.properties");
//			    conf.addResource(resource);

		conf.set("dfs.data.dir", "C:/Users/Joaqui/GroupLibRec/librec/data");
		conf.set("data.input.path", "filmtrust/rating");
		conf.set("data.column.format", "UIR");

		Randoms.seed(1);
		TextDataModel dataModel = new TextDataModel(conf);
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

		RecommendedList recommendedList = recommender.recommendRating(testDataSet);

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
		RecommendedList recommendedList = recommender.recommendRank();

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

}
