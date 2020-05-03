/**
 * 
 */
package net.librec.recommender.group;

import java.util.List;

import net.librec.recommender.GroupRecommender;

/**
 * @author Joaqui
 *
 */
public class LeastMiseryRecommender extends GroupRecommender {

	@Override
	protected Double groupModeling(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a->a).min().getAsDouble();
	}

}
