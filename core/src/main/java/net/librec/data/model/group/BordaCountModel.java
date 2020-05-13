/**
 * 
 */
package net.librec.data.model.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.librec.data.model.GroupDataModel;
import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class BordaCountModel extends GroupDataModel {

	@Override
	public ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings) {
		
		List<List<KeyValue<Integer, Double>>> groupRatingByMember = fromMemberMapToList(groupInidividualRatings);
		
		Map<Integer, List<KeyValue<Integer,Integer>>> itemsRankings = new HashMap<Integer, List<KeyValue<Integer, Integer>>>();
		int numberItems = 0;
		for (List<KeyValue<Integer, Double>> individualRating : groupRatingByMember) {
//			Sort items for each member based on rating
			List<KeyValue<Integer, Double>> listIndividualRating = individualRating.stream()
					.sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
			Collections.reverse(listIndividualRating);
			for (int i = 0; i < listIndividualRating.size(); i++) {
				KeyValue<Integer, Double> item = listIndividualRating.get(i);
				if (!itemsRankings.containsKey(item.getKey())) {
					numberItems += 1;
					itemsRankings.put(item.getKey(), new ArrayList<KeyValue<Integer, Integer>>());
				}
				itemsRankings.get(item.getKey()).add(new KeyValue<Integer, Integer>(i,listIndividualRating.size()));
			}
		}
		ArrayList<KeyValue<Integer, Double>> ranking = new ArrayList<KeyValue<Integer, Double>>();
		final int finalItemSize = numberItems;
		for (Integer item : itemsRankings.keySet()) {
			List<KeyValue<Integer, Integer>> inversedRankings = itemsRankings.get(item);
//			Invert ranking to points and sum
			Double itemSum = inversedRankings.stream().mapToDouble(kv -> 2*(kv.getKey()-kv.getValue())+(finalItemSize-kv.getValue())).sum() + ((finalItemSize-1) * (groupRatingByMember.size()-inversedRankings.size()));
			ranking.add(new KeyValue<Integer, Double>(item, itemSum));
		}
		return ranking;
	}

}
