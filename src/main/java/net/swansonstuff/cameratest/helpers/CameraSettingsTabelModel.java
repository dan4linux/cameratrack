/**
 * 
 */
package net.swansonstuff.cameratest.helpers;

import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.swansonstuff.cameratest.VideoDevice;

/**
 * @author Dan Swanson
 *
 */
public class CameraSettingsTabelModel extends DefaultTableModel implements TableModel {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	private VideoDevice device;
	public static VideoDeviceManager videoDeviceManager = VideoDeviceManager.getInstance();

	/**
	 * 
	 */
	public CameraSettingsTabelModel() {
		super(new Object[][] {
				{"", ""},
			},
			new String[] {
				"Field", "Value"
			});
	}

	/**
	 * @param rowCount
	 * @param columnCount
	 */
	public CameraSettingsTabelModel(int rowCount, int columnCount) {
		super(rowCount, columnCount);
	}

	/**
	 * @param columnNames
	 * @param rowCount
	 */
	public CameraSettingsTabelModel(Vector columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	/**
	 * @param columnNames
	 * @param rowCount
	 */
	public CameraSettingsTabelModel(Object[] columnNames, int rowCount) {
		super(columnNames, rowCount);
	}

	/**
	 * @param data
	 * @param columnNames
	 */
	public CameraSettingsTabelModel(Vector data, Vector columnNames) {
		super(data, columnNames);
	}

	/**
	 * @param data
	 * @param columnNames
	 */
	public CameraSettingsTabelModel(Object[][] data, Object[] columnNames) {
		super(data, columnNames);
	}
	
	public CameraSettingsTabelModel loadDeviceProperties() {
		while (getRowCount() > 0) {
			this.removeRow(0);
		}
		
		for (Entry<Object, Object>  setting : videoDeviceManager.getDeviceSettings().entrySet()) {
			addRow(new Object[] {setting.getKey(), setting.getValue()});
		}
		return this;
	}
	
	@Override
	public void fireTableCellUpdated(int row, int column) {
		super.fireTableCellUpdated(row, column);
		if (this.device != null) {
			videoDeviceManager.updateDeviceSetting(getValueAt(row, 0), getValueAt(row, 1));
		}
	}

}
