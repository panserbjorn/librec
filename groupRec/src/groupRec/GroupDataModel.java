/**
 * 
 */
package groupRec;

import java.io.IOException;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.convertor.TextDataConvertor;
import net.librec.data.model.AbstractDataModel;
import net.librec.math.structure.DataSet;

/**
 * @author Joaqui This class will be the abstract class in charge of generating
 *         the group models
 *
 */
public class GroupDataModel extends AbstractDataModel {
	

	/**
	 * Empty constructor.
	 */
	public GroupDataModel() {
	}
	
	
	public GroupDataModel(Configuration conf) {
		this.conf = conf;
	}

	@Override
	protected void buildConvert() throws LibrecException {
		String[] inputDataPath = conf.get(Configured.CONF_DATA_INPUT_PATH).trim().split(":");
		for (int i = 0; i < inputDataPath.length; i++) {
		    inputDataPath[i] = conf.get(Configured.CONF_DFS_DATA_DIR) + "/" + inputDataPath[i];
        }
        String dataColumnFormat = conf.get(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
        dataConvertor = new GroupTextDataConvertor(dataColumnFormat, inputDataPath, conf.get("data.convert.sep","[\t;, ]"));
        try {
            dataConvertor.processData();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
