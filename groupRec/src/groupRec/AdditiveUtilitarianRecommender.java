/**
 * 
 */
package groupRec;

import java.util.List;

/**
 * @author Joaqui
 *
 */
public class AdditiveUtilitarianRecommender extends GroupRecommender {


	@Override
	protected Double groupModeling(List<Double> groupScores) {
		return groupScores.stream().mapToDouble(a -> a).average().getAsDouble();
	}

}
