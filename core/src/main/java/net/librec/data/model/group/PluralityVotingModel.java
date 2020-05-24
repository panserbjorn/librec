/**
 * 
 */
package net.librec.data.model.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class PluralityVotingModel extends GroupModeling {

	@Override
	public ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings) {
		
		List<List<KeyValue<Integer, Double>>> groupRatingByMember = fromMemberMapToList(groupInidividualRatings);
		Integer topN = conf.getInt("rec.recommender.ranking.topn", 10);
		
		groupRatingByMember.forEach(memberRating -> memberRating.sort(Collections.reverseOrder(Map.Entry.comparingByValue())));

		List<Integer> rank = new ArrayList<Integer>();

		while (rank.size() < topN) {
			Map<Integer, Integer> voting = new HashMap<Integer, Integer>();
			for (List<KeyValue<Integer, Double>> memberRating : groupRatingByMember) {
				Optional<KeyValue<Integer, Double>> seek = memberRating.stream()
						.filter(item -> !rank.contains(item.getKey())).findFirst();
				if (seek.isPresent()) {
					voting.compute(seek.get().getKey(), (k, v) -> (v == null) ? 1 : v + 1);
				}
			}
			List<Entry<Integer, Integer>> votingResult = voting.entrySet().stream()
					.sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).collect(Collectors.toList());
			if(votingResult.isEmpty()) {
				break;
			}
			Integer highestVoting = votingResult.get(0).getValue();
			List<Integer> highestRankedList = votingResult.stream().filter(item -> item.getValue() == highestVoting)
					.map(item -> item.getKey()).collect(Collectors.toList());
			rank.addAll(highestRankedList);
		}
		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer, Double>>();

		for (int i = 0; i < rank.size(); i++) {
			Integer item = rank.get(i);
			groupRanking.add(new KeyValue<Integer, Double>(item, rank.size() - i + 0.0));
		}

		return groupRanking;
	}

}
