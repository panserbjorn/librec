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
	
	private List<Number> users;
	
	private VectorBasedSequentialSparseVector centroid;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Number> getUsers() {
		return users;
	}

	public void setUsers(List<Number> users) {
		this.users = users;
	}
	
	public void addUser(Integer newUser) {
		this.getUsers().add(newUser);
	}

	public VectorBasedSequentialSparseVector getCentroid() {
		return centroid;
	}

	public void setCentroid(VectorBasedSequentialSparseVector centroid) {
		this.centroid = centroid;
	}
	
	public void clear() {
		this.users.clear();
	}
	
	

}
