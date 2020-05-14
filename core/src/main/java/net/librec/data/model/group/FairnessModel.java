/**
 * 
 */
package net.librec.data.model.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.librec.data.model.GroupDataModel;
import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class FairnessModel extends GroupDataModel {

	@Override
	public ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings) {
		
		List<List<KeyValue<Integer, Double>>> groupRatingByMember = fromMemberMapToList(groupInidividualRatings);
		Integer topN = conf.getInt("rec.recommender.ranking.topn", 10);
		
		groupRatingByMember.forEach(memberRating -> memberRating.sort(Collections.reverseOrder(Map.Entry.comparingByValue())));

		List<Integer> rank = new ArrayList<Integer>();
		Set<Integer> emptyMembers = new HashSet<Integer>();

		int index = 0;
		while (rank.size() < topN) {
			int positionMember = index++ % groupRatingByMember.size();
			List<KeyValue<Integer, Double>> memberRating = groupRatingByMember.get(positionMember);
			Optional<KeyValue<Integer, Double>> item = memberRating.stream().filter(kv -> !rank.contains(kv.getKey()))
					.findFirst();
			if (item.isPresent()) {
				rank.add(item.get().getKey());
			} else {
				emptyMembers.add(positionMember);
			}
			if(emptyMembers.size() == groupRatingByMember.size()) { 
//				All members have expressed all their votes.
				break;
			}
		}

		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer, Double>>();

		for (int i = 0; i < rank.size(); i++) {
			Integer item = rank.get(i);
			groupRanking.add(new KeyValue<Integer, Double>(item, rank.size() - i + 0.0));
		}

		return groupRanking;
	}

}
