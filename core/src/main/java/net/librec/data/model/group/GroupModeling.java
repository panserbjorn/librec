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

import net.librec.conf.Configured;
import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public abstract class GroupModeling extends Configured{

	/**
	 * 
	 */
	public GroupModeling() {
		// TODO Auto-generated constructor stub
	}
	
	public abstract ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings);
	
	protected static Map<Integer, List<Double>> fromMemberToItemScores(
			Map<Integer, List<KeyValue<Integer, Double>>> groupScores) {
		Map<Integer, List<Double>> groupScoresByItem = new HashMap<Integer, List<Double>>();

		for (Entry<Integer, List<KeyValue<Integer, Double>>> member : groupScores.entrySet()) {
			for (KeyValue<Integer, Double> rating : member.getValue()) {
				if (!groupScoresByItem.containsKey(rating.getKey())) {
					groupScoresByItem.put(rating.getKey(), new ArrayList<Double>());
				}
				groupScoresByItem.get(rating.getKey()).add(rating.getValue());
			}
		}
		return groupScoresByItem;
	}

	protected static List<List<KeyValue<Integer, Double>>> fromMemberMapToList(
			Map<Integer, List<KeyValue<Integer, Double>>> groupScores) {
		return groupScores.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
	}

}
