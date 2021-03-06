/**
 * 
 */
package net.librec.data.splitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.SequentialAccessSparseMatrix;

/**
 * @author Joaqui
 *
 */
public class GroupDataSplitter extends AbstractDataSplitter {

	private Map<Integer, Integer> groupAssignation;

	private Map<Integer, List<Integer>> groups;
	
	public GroupDataSplitter() {
		
	}

	public GroupDataSplitter(Map<Integer, Integer> groupAssignation, Map<Integer, List<Integer>> groups, Configuration conf) {
		this.conf = conf;
		this.setGroupInfo(groupAssignation, groups);
	}
	
	public void setGroupInfo(Map<Integer, Integer> groupAssignation, Map<Integer, List<Integer>> groups) {
		this.groupAssignation = groupAssignation;
		this.groups = groups;
	}

	@Override
	public void splitData() throws LibrecException {
		if (null == this.preferenceMatrix) {
			this.preferenceMatrix = dataConvertor.getPreferenceMatrix(conf);
		}
		trainMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);
		testMatrix = new SequentialAccessSparseMatrix(preferenceMatrix);

		Map<Integer, Set<Integer>> groupRatings = new HashMap<Integer, Set<Integer>>();
		for (Integer group : groups.keySet()) {
			groupRatings.put(group, new HashSet<Integer>());
		}

//		Get all ratings by group
		for (MatrixEntry matrixEntry : preferenceMatrix) {
//			TODO: I could add or maintain only the items that have been rated by a certain number or percentage of group members

			if (groupAssignation.containsKey(matrixEntry.row())) {
				groupRatings.get(groupAssignation.get(matrixEntry.row())).add(matrixEntry.column());
			}
		}

//		Determine the items that will be set to test
		Map<Integer, Set<Integer>> testGroupRating = new HashMap<Integer, Set<Integer>>();
		for (Integer group : groups.keySet()) {
			testGroupRating.put(group, new HashSet<Integer>());
			Set<Integer> groupRat = groupRatings.get(group);
			double percentageOfGroupRatingForTest = conf.getDouble("group.test.item.ratio", 0.05);
			int numTests = (int) Math.round(groupRat.size() * percentageOfGroupRatingForTest);
			System.out.println("NumTests for Group "+ group.toString() + ": " + Integer.toString(numTests));
			if (numTests > 0) {
				try {
					int[] givenPositions = Randoms.nextIntArray(numTests, groupRat.size()-1);
					int testPosition = 0;
					int ratingPosition = 0;
					for (Integer item : groupRat) {
						if (testPosition == givenPositions.length) {
							break;
						}
						if (givenPositions[testPosition] == ratingPosition) {
							testGroupRating.get(group).add(item);
							testPosition++;
						}
						ratingPosition++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

//		Set test and train items
		for (MatrixEntry matrixEntry : preferenceMatrix) {
			if (groupAssignation.containsKey(matrixEntry.row())) {
				Integer groupNumber = groupAssignation.get(matrixEntry.row());
				Set<Integer> groupTestItems = testGroupRating.get(groupNumber);
				Integer item = matrixEntry.column();
				if (groupTestItems.contains(item)) {
					trainMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
				} else {
					testMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
				}
			} else {
				testMatrix.setAtColumnPosition(matrixEntry.row(), matrixEntry.columnPosition(), 0.0D);
			}
		}

		/*
		 * Procedure: For each group: Determine the items that at least 70% of the
		 * groups has rated If the items account for less than 40% of the user that has
		 * rated the least items then set all those items in the test matrix Otherwise,
		 * determine the number of items to reach 40% of the smallest user and choose
		 * randomly that many items to set in the test matrix. NOTE: All the items
		 * ratings for the group must be set to 0 in the training matrix and the rest of
		 * the items must be set to 0 in the test. Another approach could be to set a
		 * percentage of the ratings of the group only in the test set. (to be
		 * discussed)
		 * 
		 * DONE - Procedure (2): For each group determine the number of items rated by the
		 * group (it can be rated by any member or it can be items rated by a certain
		 * number of members) Choose randomly a percentage of those items (5%, 10%; it
		 * might depend on the size of the groups) Set those items as test item for the
		 * group
		 */

		testMatrix.reshape();
		trainMatrix.reshape();

	}

}
