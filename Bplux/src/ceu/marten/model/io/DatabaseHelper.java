package ceu.marten.model.io;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import ceu.marten.model.DeviceConfiguration;
import ceu.marten.model.DeviceRecording;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Database helper which creates and upgrades the database and provides the DAOs
 * for the app.
 * 
 * @author Carlos Marten
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getName();
	private static final String DATABASE_NAME = "biopluxDB.db";
	private static final int DATABASE_VERSION = 3;

	private Dao<DeviceConfiguration, Integer> deviceConfigDao;
	private Dao<DeviceRecording, Integer> sessionDao;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqliteDatabase,
			ConnectionSource connectionSource) {
		try {
			TableUtils.createTable(connectionSource, DeviceConfiguration.class);
			TableUtils.createTable(connectionSource, DeviceRecording.class);
		} catch (SQLException e) {
			Log.e(TAG, "Unable to create datbase", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqliteDatabase,
			ConnectionSource connectionSource, int oldVer, int newVer) {
		try {
			TableUtils.dropTable(connectionSource, DeviceConfiguration.class, true);
			TableUtils.dropTable(connectionSource, DeviceRecording.class, true);
			onCreate(sqliteDatabase, connectionSource);
		} catch (SQLException e) {
			Log.e(TAG, "Unable to upgrade database from version " + oldVer + " to new " + newVer, e);
		}
	}

	public Dao<DeviceConfiguration, Integer> getDeviceConfigDao() throws SQLException {
		if (deviceConfigDao == null) {
			deviceConfigDao = getDao(DeviceConfiguration.class);
		}
		return deviceConfigDao;
	}

	public Dao<DeviceRecording, Integer> getRecordingDao() throws SQLException {
		if (sessionDao == null) {
			sessionDao = getDao(DeviceRecording.class);
		}
		return sessionDao;
	}
}
