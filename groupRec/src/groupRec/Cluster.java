/**
 * 
 */
package groupRec;

import java.util.List;

import net.librec.math.structure.VectorBasedSequentialSparseVector;

/**
 * @author Joaqui
 *
 * This class will represent the cluster 
 */
public class Cluster {
	
	private int id;
	
	private List<List<Number>> users;
	
	private List<Number> centroid;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<List<Number>> getUsers() {
		return users;
	}

	public void setUsers(List<List<Number>> users) {
		this.users = users;
	}
	
	public void addUser(List<Number> newUser) {
		this.getUsers().add(newUser);
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
