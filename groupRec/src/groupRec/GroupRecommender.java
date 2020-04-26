/**
 * 
 */
package groupRec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.librec.common.LibrecException;
import net.librec.data.structure.AbstractBaseDataEntry;
import net.librec.data.structure.BaseDataList;
import net.librec.data.structure.BaseRatingDataEntry;
import net.librec.data.structure.LibrecDataList;
import net.librec.math.structure.DataSet;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.AbstractRecommender;
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

    @Override
    protected void setup() throws LibrecException {
    	super.setup();
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
	

	@Override
	public RecommendedList recommendRating(DataSet predictDataSet) throws LibrecException {
		SequentialAccessSparseMatrix predictMatrix = (SequentialAccessSparseMatrix) predictDataSet;
        LibrecDataList<AbstractBaseDataEntry> librecDataList = new BaseDataList<>();
        for (int userIdx = 0; userIdx < numUsers; ++userIdx) {
            int[] itemIdsArray = predictMatrix.row(userIdx).getIndices();
            AbstractBaseDataEntry baseRatingDataEntry = new BaseRatingDataEntry(userIdx, itemIdsArray);
            librecDataList.addDataEntry(baseRatingDataEntry);
        }

        return this.recommendRating(librecDataList);
	}

	@Override
	public RecommendedList recommendRating(LibrecDataList<AbstractBaseDataEntry> dataList) throws LibrecException {
//		TODO: This will be performed by the secondary recommender (which should be established in the setup)
		int numDataEntries = dataList.size();
        RecommendedList recommendedList = new RecommendedList(numDataEntries);
        for (int contextIdx = 0; contextIdx < numDataEntries; ++contextIdx) {
            recommendedList.addList(new ArrayList<>());
            BaseRatingDataEntry baseRatingDataEntry = (BaseRatingDataEntry) dataList.getDataEntry(contextIdx);
            int userIdx = baseRatingDataEntry.getUserId();
            int[] itemIdsArray = baseRatingDataEntry.getItemIdsArray();
            for (int itemIdx : itemIdsArray) {
                double predictRating = predict(userIdx, itemIdx, true);
                recommendedList.add(contextIdx, itemIdx, predictRating);
            }
        }

        return recommendedList;
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
		
	}

}
