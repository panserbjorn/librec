/**
 * 
 */
package net.librec.data.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;

import net.librec.conf.Configurable;
import net.librec.conf.Configuration;
import net.librec.math.structure.DataFrame;
import net.librec.math.structure.SequentialAccessSparseMatrix;

/**
 * @author Joaqui
 *
 *         This class will be responsible for the group creation and modeling
 *
 */
public abstract class GroupBuilder implements Configurable{

	protected Configuration conf;
	
	protected DataFrame df;
	 
	protected SequentialAccessSparseMatrix preferences;
	
	/***
	 * Process that generates the groups or retrieves them from external sources
	 */
	public abstract void generateGroups();
	
	
	public List<Map<Integer, String>> getMemberStatistics(){
		return new ArrayList<Map<Integer,String>>();
	}
	
	public GroupBuilder() {
	}
	
	public void setUp(DataFrame df, SequentialAccessSparseMatrix preferences) {
		this.df = df;
		this.preferences = preferences;
	}
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	@Override
	public Configuration getConf() {
		return this.conf;
	}
	
	public abstract Map<Integer, Integer> getAssignation();
	
	public abstract Map<Integer, List<Integer>> getGroups();
	
	public abstract BiMap<String, Integer> getGroupMapping();
}
