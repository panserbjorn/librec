/**
 * 
 */
package groupRec;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.List;

/**
 * @author Joaqui
 *
 *         This class will represent the cluster
 */
public class Cluster {

	private int id;

	private List<clusterUser> users;
	
	//Class that represents the users with their ids and their ratings
	private class clusterUser {
		public List<Number> ratings;
		public Number id;
	}
	
	private List<List<Number>> transformUsers(){
		return this.users.stream().map(x -> x.ratings).collect(Collectors.toList());
	}

	private List<Number> centroid;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<List<Number>> getUsers() {
		return this.transformUsers();
	}
	
	public List<Number> getUsersIds() {
		return this.users.stream().map(x -> x.id).collect(Collectors.toList());
	}

	public void setUsers(List<List<Number>> users, List<Number> ids) {
		List<clusterUser> newUsers = new ArrayList<Cluster.clusterUser>();
		for (int i = 0; i < users.size(); i++) {
			clusterUser temp = new clusterUser();
			temp.id = ids.get(i);
			temp.ratings = users.get(i);
			newUsers.add(temp);
			
		}
		this.users = newUsers;
	}

	public void addUser(List<Number> newUser, Number id) {
		clusterUser temp = new clusterUser();
		temp.id = id;
		temp.ratings = newUser;
		this.users.add(temp);
	}

	public List<Number> getCentroid() {
		return centroid;
	}

	public void setCentroid(List<Number> centroid) {
		this.centroid = centroid;
	}

	public void clear() {
		this.users.clear();
	}

}
