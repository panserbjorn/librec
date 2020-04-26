/**
 * 
 */
package groupRec;

import java.util.List;
import java.util.Map;

import net.librec.common.LibrecException;
import net.librec.data.DataConvertor;
import net.librec.data.DataSplitter;
import net.librec.data.splitter.AbstractDataSplitter;
import net.librec.math.structure.SequentialAccessSparseMatrix;

/**
 * @author Joaqui
 *
 */
public class GroupDataSplitter extends AbstractDataSplitter {
	
	private Map<Integer, Integer> groupAssignation;
	
	private Map<Integer, List<Integer>> groups;

	public GroupDataSplitter(Map<Integer, Integer> groupAssignation, Map<Integer, List<Integer>> groups) {
		this.groupAssignation = groupAssignation;
		this.groups = groups;
	}

	@Override
	public void splitData() throws LibrecException {
		if (null == this.preferenceMatrix) {
			this.preferenceMatrix = dataConvertor.getPreferenceMatrix(conf);
		}
		double ratio = Double.parseDouble(conf.get("data.splitter.trainset.ratio"));
		trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
		testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
		
//		TODO: Need to set to 0 the ratings that I want to put in test in the train matrix and vice versa
		/*
		 * Procedure:
		 * For each group:
		 * 	Determine the items that at least 70% of the groups has rated
		 * 	If the items account for less than 40% of the user that has rated the least items then set all those items in the test matrix
		 * 	Otherwise, determine the number of items to reach 40% of the smallest user and choose randomly that many items to set in the test matrix. 
		 * NOTE: All the items ratings for the group must be set to 0 in the training matrix and the rest of the items must be set to 0 in the test. Another approach could be to set a percentage of the ratings of the group only in the test set. (to be discussed)
		 */
		
		testMatrix.reshape();
		trainMatrix.reshape();

	}

}
