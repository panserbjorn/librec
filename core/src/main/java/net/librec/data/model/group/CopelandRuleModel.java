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
		
//		TODO Need to optimize this. The copeland Rule takes about 4 minutes in moveikLens
//		TODO Now I know that if this is being called, the groupRatings al have the same length

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
			Set<Integer> preferredAgainst = new HashSet<Integer>(itemTemporalMap.keySet());
			for (KeyValue<Integer, Double> item : sortedRatings) {
				preferredAgainst.remove(item.getKey());
				for (Integer looser : preferredAgainst) {
					Integer itemMapping = itemTemporalMap.get(item.getKey());
					Integer looserMapping = itemTemporalMap.get(looser);
					if (itemMapping < looserMapping) {
						Integer itemPosition = itemMapping;
						Integer looserPosition = looserMapping - (itemMapping + 1);
						itemConfrontationCount.get(itemPosition)[looserPosition]++;
					} else {
						Integer looserPosition = looserMapping;
						Integer itemPosition = itemMapping - (looserMapping + 1);
						itemConfrontationCount.get(looserPosition)[itemPosition]--;
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
				} else if (confrontationCount < 0) {
					winningCount[j]++;
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
