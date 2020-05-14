/**
 * 
 */
package net.librec.data.model.group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.librec.data.model.GroupDataModel;
import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class ApprovalVotingModel extends GroupDataModel {

	@Override
	public ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings) {
		
		Double approvalThreshold = conf.getDouble("rec.recommender.approval");
		
		Map<Integer, Integer> voting = new HashMap<Integer, Integer>();
		for (Entry<Integer,List<KeyValue<Integer, Double>>> memberRatings : groupInidividualRatings.entrySet()) {
//			It is wrong to filter. I should add a 0 as vote instead of 1. 
			memberRatings.getValue().forEach(item -> {if (item.getValue() > approvalThreshold) {voting.compute(item.getKey(), (k, v) -> (v == null) ? 1 : v + 1);}else {voting.compute(item.getKey(), (k, v) -> (v == null) ? 1 : v);} } );
//			List<KeyValue<Integer, Double>> filtered = memberRatings.getValue().stream().filter(item -> item.getValue() > approvalThreshold).collect(Collectors.toList());
//					.forEach(item -> voting.compute(item.getKey(), (k, v) -> (v == null) ? 1 : v + 1));
//			for (KeyValue<Integer, Double> keyValue : filtered) {
//				voting.compute(keyValue.getKey(), (k, v) -> (v == null) ? 1 : v + 1);
//			}
		}

		ArrayList<KeyValue<Integer, Double>> groupRanking = new ArrayList<KeyValue<Integer, Double>>();
		for (Entry<Integer, Integer> vote : voting.entrySet()) {
			groupRanking.add(new KeyValue<Integer, Double>(vote.getKey(), vote.getValue() + 0.0));
		}
		return groupRanking;
	}

}
