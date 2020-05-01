/**
 * 
 */
package net.librec.group;

import java.util.List;

/**
 * @author Joaqui
 *
 */
public class MostPleasure extends GroupRecommender {

	@Override
	protected Double groupModeling(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a->a).max().getAsDouble();
	}

}
