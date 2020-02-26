/**
 * 
 */
package groupRec;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.model.AbstractDataModel;
import net.librec.math.structure.DataSet;

/**
 * @author Joaqui This class will be the abstract class in charge of generating
 *         the group models
 *
 */
public class GroupDataModel extends AbstractDataModel {
	

	public GroupDataModel() {
	}
	
	public GroupDataModel(Configuration conf) {
		this.conf = conf;
	}

	@Override
	protected void buildConvert() throws LibrecException {
		// TODO Auto-generated method stub
		
	}
	
    /**
     * Load data model.
     *
     * @throws LibrecException if error occurs during loading
     */
    @Override
    public void loadDataModel() throws LibrecException {

    }

    /**
     * Save data model.
     *
     * @throws LibrecException if error occurs during saving
     */
    @Override
    public void saveDataModel() throws LibrecException {

    }

    /**
     * Get datetime data set.
     *
     * @return the datetime data set of data model.
     */
    @Override
    public DataSet getDatetimeDataSet() {
        return dataConvertor.getDatetimeMatrix();
    }
	
	
}
