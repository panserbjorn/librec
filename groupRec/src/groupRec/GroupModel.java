/**
 * 
 */
package groupRec;

/**
 * @author Joaqui
 * This class will be responsible for the abstraction of the group modeling
 */
public abstract class GroupModel {
	
	protected abstract void generateModel(GroupDataModel groupData);
	
	public abstract int getGroupRating(int group);
	

}
