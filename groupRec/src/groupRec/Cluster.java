/**
 * 
 */
package groupRec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import net.librec.math.structure.AbstractVector;
import net.librec.math.structure.SequentialSparseVector;

import java.util.List;
import java.util.Map;

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
		public  SequentialSparseVector ratings;
		public int index;
	}
	

	private SequentialSparseVector centroid;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Map<Integer,SequentialSparseVector> getUsers() {
		Map<Integer, SequentialSparseVector> userMap = new HashMap<Integer, SequentialSparseVector>();
		for (clusterUser user : users) {
			userMap.put(user.index, user.ratings);
		}
		return userMap;
	}
	
	public List<Integer> getUsersIds() {
		return this.users.stream().map(x -> x.index).collect(Collectors.toList());
	}

	public void setUsers(List<SequentialSparseVector> users, List<Integer> ids) {
		List<clusterUser> newUsers = new ArrayList<Cluster.clusterUser>();
		for (int i = 0; i < users.size(); i++) {
			clusterUser temp = new clusterUser();
			temp.index = ids.get(i);
			temp.ratings = users.get(i);
			newUsers.add(temp);
			
		}
		this.users = newUsers;
	}

	public void addUser(SequentialSparseVector newUser, Integer id) {
		clusterUser temp = new clusterUser();
		temp.index = id;
		temp.ratings = newUser;
		this.users.add(temp);
	}

	public SequentialSparseVector getCentroid() {
		return centroid;
	}

	public void setCentroid(SequentialSparseVector centroid) {
		this.centroid = centroid;
	}

	public void clear() {
		this.users.clear();
	}

}
