/**
 * 
 */
package net.librec.data.model.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class ApprovalVotingModel extends GroupModeling {

	@Override
	public ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings) {
		
		Double approvalThreshold = conf.getDouble("rec.recommender.approval");
		
		Map<Integer, Integer> voting = new HashMap<Integer, Integer>();
		for (Entry<Integer,List<KeyValue<Integer, Double>>> memberRatings : groupInidividualRatings.entrySet()) {
			memberRatings.getValue().forEach(item -> {if (item.getValue() > approvalThreshold) {voting.compute(item.getKey(), (k, v) -> (v == null) ? 1 : v + 1);}else {voting.compute(item.getKey(), (k, v) -> (v == null) ? 1 : v);} } );
		}

		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer, Double>>();
		for (Entry<Integer, Integer> vote : voting.entrySet()) {
			groupRanking.add(new KeyValue<Integer, Double>(vote.getKey(), vote.getValue() + 0.0));
		}
		return groupRanking;
	}

}
