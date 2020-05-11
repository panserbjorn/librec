/**
 * 
 */
package net.librec.data.model.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.librec.data.model.GroupDataModel;
import net.librec.recommender.item.KeyValue;

/**
 * @author Joaqui
 *
 */
public class AdditiveUtilitarianModel extends GroupDataModel {

	@Override
	public ArrayList<KeyValue<Integer, Double>> computeGroupModel(
			Map<Integer, List<KeyValue<Integer, Double>>> groupInidividualRatings) {

		Map<Integer, List<Double>> groupScoresByItem = fromMemberToItemScores(groupInidividualRatings);

		ArrayList<KeyValue<Integer, Double>> groupRatings = new ArrayList<KeyValue<Integer, Double>>();
		for (Entry<Integer, List<Double>> itemRatings : groupScoresByItem.entrySet()) {
			Double rating = itemRatings.getValue().stream().mapToDouble(a -> a).average().getAsDouble();
			groupRatings.add(new KeyValue<Integer, Double>(itemRatings.getKey(), rating));
		}

		return groupRatings;
	}

}
