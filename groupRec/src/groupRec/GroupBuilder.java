/**
 * 
 */
package groupRec;

import java.util.Collection;

/**
 * @author Joaqui
 *
 *         This class will be responsible for the group creation and modeling
 *
 */
public abstract class GroupBuilder {

	/***
	 * Process that generates the models (It will generate the groups internally,
	 * but more information about the process should be displayed somehow)
	 */
	protected abstract void generateGroups();
	
	public abstract String getGenerationStatistics();

	public abstract Collection<Integer> getGroupList();

	public abstract Collection<Integer> getGroupMembers(int groupId);

	public abstract int getUserRating(int user);

	protected abstract int numberOfGroupRatingsForItem(int group, int item);

	public abstract Collection<Integer> getGroupRatingsForItem(int group, int item);

}
