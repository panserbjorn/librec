/**
 * 
 */
package net.librec.data.model.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class CopelandRuleModel extends GroupModeling {

	@Override
	public ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings) {
		
		List<List<KeyValue<Integer, Double>>> groupRatingByMember = fromMemberMapToList(groupInidividualRatings);

		BiMap<Integer, Integer> itemTemporalMap = HashBiMap.create();
		for (List<KeyValue<Integer, Double>> memberRatings : groupRatingByMember) {
			for (KeyValue<Integer, Double> rating : memberRatings) {
				if (!itemTemporalMap.containsKey(rating.getKey())) {
					itemTemporalMap.put(rating.getKey(), itemTemporalMap.size());
				}
			}
		}
//		Initialize confrontation table
		Integer numberItems = itemTemporalMap.size();
		List<int[]> itemConfrontationCount = new ArrayList<int[]>();
		for (int i = 0; i < numberItems - 1; i++) {
			int[] confrontationRow = new int[numberItems - (i + 1)];
			Arrays.fill(confrontationRow, 0);
			itemConfrontationCount.add(confrontationRow);
		}
//		Collect the result of all the confrontations
		for (List<KeyValue<Integer, Double>> memeberRatings : groupRatingByMember) {
			List<KeyValue<Integer, Double>> sortedRatings = memeberRatings.stream().sorted(Map.Entry.comparingByValue())
					.collect(Collectors.toList());
			Collections.reverse(sortedRatings);
			List<Integer> itemsProcessed = new ArrayList<Integer>();
			for (KeyValue<Integer, Double> item : sortedRatings) {
				for (Integer winner : itemsProcessed) {
					Integer looserMapping = itemTemporalMap.get(item.getKey());
					Integer winnerMapping = itemTemporalMap.get(winner);
					if (winnerMapping < looserMapping) {
						Integer itemPosition = winnerMapping;
						Integer looserPosition = looserMapping - (winnerMapping + 1);
						itemConfrontationCount.get(itemPosition)[looserPosition]++;
					} else {
						Integer looserPosition = looserMapping;
						Integer winnerPosition = winnerMapping - (looserMapping + 1);
						itemConfrontationCount.get(looserPosition)[winnerPosition]--;
					}
				}
				itemsProcessed.add(item.getKey());
			}
			List<Integer> missing = itemTemporalMap.keySet().stream().filter(item -> !itemsProcessed.contains(item)).collect(Collectors.toList());
			for (Integer winner : itemsProcessed) {
				for (Integer looser : missing) {
					Integer looserMapping = itemTemporalMap.get(looser);
					Integer winnerMapping = itemTemporalMap.get(winner);
					if (winnerMapping < looserMapping) {
						Integer itemPosition = winnerMapping;
						Integer looserPosition = looserMapping - (winnerMapping + 1);
						itemConfrontationCount.get(itemPosition)[looserPosition]++;
					} else {
						Integer looserPosition = looserMapping;
						Integer winnerPosition = winnerMapping - (looserMapping + 1);
						itemConfrontationCount.get(looserPosition)[winnerPosition]--;
					}
				}
			}
		}

//		Compute the winning numbers
		int[] winningCount = new int[numberItems];
		Arrays.fill(winningCount, 0);
		for (int i = 0; i < numberItems - 1; i++) {
			for (int j = i + 1; j < numberItems; j++) {
				int jPosition = j - (i + 1);
				Integer confrontationCount = itemConfrontationCount.get(i)[jPosition];
				if (confrontationCount > 0) {
					winningCount[i]++;
					winningCount[j]--;
				} else if (confrontationCount < 0) {
					winningCount[j]++;
					winningCount[i]--;
				}
			}
		}

//		Generate result without mapping
		BiMap<Integer, Integer> inverse = itemTemporalMap.inverse();
		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer, Double>>();
		for (int i = 0; i < winningCount.length; i++) {
			int score = winningCount[i];
			groupRanking.add(new KeyValue<Integer, Double>(inverse.get(i), score + 0.0));
		}

		return groupRanking;
	}

}
