package ceu.marten.services;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import com.bitalino.comm.BITalinoFrame;
import com.bitalino.deviceandroid.BitalinoAndroidDevice;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import ceu.marten.bitadroid.R;
import ceu.marten.model.DeviceConfiguration;
import ceu.marten.model.io.DataManager;
import ceu.marten.ui.NewRecordingActivity;
import ceu.marten.ui.SettingsActivity;

/**
 * Creates a connection with a bioplux device and receives frames sent from
 * device
 * 
 * @author Carlos Marten
 * 
 */
public class BiopluxService extends Service {

	private static final String TAG = BiopluxService.class.getName();

	// messages 'what' fields for the communication with the client
	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_DATA = 2;
	public static final int MSG_RECORDING_DURATION = 3;
	public static final int MSG_SAVED = 4;
	public static final int MSG_CONNECTION_ERROR = 5;

	public static final String KEY_X_VALUE = "xValue";
	public static final String KEY_FRAME_DATA = "frame";

	// Codes for the activity to display the correct error message
	public static final int CODE_ERROR_WRITING_TEXT_FILE = 6;
	public static final int CODE_ERROR_SAVING_RECORDING = 7;

	// Get 80 frames every 50 miliseconds
	private int numberOfFrames;

	public static final int TIMER_TIME = 50;// cambiar

	// Used to synchronize timer and main thread
	private static final Object weAreWritingDataToFileLock = new Object();
	private boolean areWeWritingDataToFile;
	// Used to keep activity running while device screen is turned off
	private PowerManager powerManager;
	private WakeLock wakeLock = null;

	private DeviceConfiguration configuration;
	private BitalinoAndroidDevice connection;

	private Timer timer = null;
	private DataManager dataManager;
	private double samplingFrames;
	private double samplingCounter = 0;
	private double timeCounter = 0;
	private double xValue = 0;
	private boolean drawInBackground = true;
	private boolean killServiceError = false;
	private boolean clientActive = false;
	Notification serviceNotification = null;
	private SharedPreferences sharedPref;

	// Target we publish for clients to send messages to IncomingHandler
	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	// Messenger with interface for sending messages from the service
	private Messenger client = null;

	/**
	 * Handler of incoming messages from clients.
	 */
	@SuppressLint("HandlerLeak")
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				// register client
				client = msg.replyTo;
				clientActive = true;
				// removes notification
				stopForeground(true);

				if (timer == null) {
					timer = new Timer();
					timer.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							processFrames();
						}
					}, 0, TIMER_TIME);
				}
				break;
			case MSG_RECORDING_DURATION:
				dataManager.setDuration(msg.getData().getString(
						NewRecordingActivity.KEY_DURATION));
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Initializes the wake lock and the frames array
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		sharedPref = getSharedPreferences(getPackageName() + "_preferences",
				Context.MODE_MULTI_PROCESS);
		drawInBackground = sharedPref.getBoolean(
				SettingsActivity.KEY_DRAW_IN_BACKGROUND, true);
		powerManager = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"MyWakeLock");
		if ((wakeLock != null) && (wakeLock.isHeld() == false)) {
			wakeLock.acquire();
		}
	}

	/**
	 * Returns the communication channel to the service or null if clients
	 * cannot bind to the service
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind");
		return mMessenger.getBinder();

	}

	/**
	 * Changes the service to be run in the foreground and shows the
	 * notification
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "onUNBind");
		clientActive = false;
		startForeground(R.string.service_id, serviceNotification);
		return true;
	}

	/**
	 * Gets information from the activity extracted from the intent and connects
	 * to bioplux device. Returns a do not re-create flag if killed by system
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e(TAG, "Iniciamos conexion BITALINO");

		String recordingName = intent.getStringExtra(
				NewRecordingActivity.KEY_RECORDING_NAME).toString();
		configuration = (DeviceConfiguration) intent
				.getSerializableExtra(NewRecordingActivity.KEY_CONFIGURATION);
		samplingFrames = (double) configuration.getVisualizationFrequency()
				/ configuration.getSamplingFrequency();

		numberOfFrames = (int) (TIMER_TIME
				* configuration.getVisualizationFrequency() / 1000);// We round
																	// upthe
																	// number of
																	// frames

		Log.i(TAG,
				"numberOfFrames " + numberOfFrames + " receptionFrequency() "
						+ configuration.getVisualizationFrequency());
		Log.i(TAG, "samplingFrames " + samplingFrames);
		// Revisar frames = new Device.Frame[numberOfFrames];
		// Revisar for (mes.length; i++){
		// Revisar frames[i] = new Frame();
		// Revisar }

		if (connectToBiopluxDevice()) {
			dataManager = new DataManager(this, recordingName, configuration);
			createNotification();
		}
		return START_NOT_STICKY; // do not re-create service if system kills it
	}

	/**
	 * Gets and process the frames from the bioplux device. Saves all the frames
	 * receives to a text file and send the requested frames to the activity
	 */
	private int frameSeq = -1;

	// Revisar int i =-1;
	private void processFrames() {
		synchronized (weAreWritingDataToFileLock) {
			areWeWritingDataToFile = true;
		}

		BITalinoFrame[] frames = getFrames(numberOfFrames);
		for (BITalinoFrame frame : frames) {
			// Revisar i++;
			frameSeq = frameSeq + 1 < 16 ? frameSeq + 1 : 0;

			if (!dataManager.writeFrameToTmpFile(frame, frame.getSequence())) {
				sendErrorToActivity(CODE_ERROR_WRITING_TEXT_FILE);
				killServiceError = true;
				stopSelf();
				break;
			}

			if (samplingCounter++ >= samplingFrames) {
				// calculates x value of graphs
				timeCounter++;
				xValue = timeCounter / configuration.getSamplingFrequency()
						* 1000;
				// gets default share preferences with multi-process flag

				if (clientActive || !clientActive && drawInBackground)
					sendFrameToActivity(frame);
				// retains the decimals
				samplingCounter -= samplingFrames;
			}
		}
		synchronized (weAreWritingDataToFileLock) {
			areWeWritingDataToFile = false;
		}
	}

	/**
	 * Get frames from the bioplux device
	 */
	private BITalinoFrame[] getFrames(int numberOfFrames) {
		Log.e(TAG, "BITALINO Read frames");

		BITalinoFrame[] frames = connection.read(numberOfFrames);
		Log.e(TAG, "BITALINO Readed");
		for (BITalinoFrame frame : frames)
			Log.v(TAG, frame.toString());
		return frames;
	}

	/**
	 * Connects to a bioplux device and begins to acquire frames Returns true
	 * connection has established. False if an exception was caught
	 */
	private boolean connectToBiopluxDevice() {

		Log.e(TAG, "connectToBiopluxDevice");
		// BIOPLUX INITIALIZATION
		connection = new BitalinoAndroidDevice(configuration.getMacAddress());
		ArrayList<Integer> activeChannels = configuration.getActiveChannels();
		int[] activeChannelsArray = convertToBitalinoChannelsArray(activeChannels);

		if (connection.connect(configuration.getVisualizationFrequency(),
				activeChannelsArray) != 0) {
			Log.e(TAG, "Bitalino connection error");
			killServiceError = true;
			stopSelf();

			return false;
		}
		if (connection.start() != 0) {
			Log.e(TAG, "Bitalino starting error");
			killServiceError = true;
			stopSelf();
			return false;
		}

		// Revisar
		// connection.BeginAcq(configuration.getVisualizationFrequency(),
		// Revisar
		// configuration.getActiveChannelsAsInteger(),configuration.getNumberOfBits());

		Log.e(TAG,
				"configuration.getNumberOfBits() "
						+ configuration.getNumberOfBits());

		return true;
	}

	private int[] convertToBitalinoChannelsArray(
			ArrayList<Integer> activeChannels) {
		int[] activeChannelsArray = new int[activeChannels.size()];
		Iterator<Integer> iterator = activeChannels.iterator();
		Log.e(TAG, "BITALINO ActiveChannels ");

		for (int i = 0; i < activeChannelsArray.length; i++) {
			activeChannelsArray[i] = iterator.next().intValue()-1;
			Log.e(TAG, "BITALINO ActiveChannels C" + activeChannelsArray[i]);
		}

		return activeChannelsArray;
	}

	private void createNotification() {

		// SET THE BASICS
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.notification)
				.setContentTitle(getString(R.string.bs_notification_title))
				.setContentText(getString(R.string.bs_notification_message));

		// CREATE THE INTENT CALLED WHEN NOTIFICATION IS PRESSED
		Intent newRecordingIntent = new Intent(this, NewRecordingActivity.class);

		// PENDING INTENT
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				newRecordingIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		mBuilder.setContentIntent(pendingIntent);

		// CREATES THE NOTIFICATION AND START SERVICE AS FOREGROUND
		serviceNotification = mBuilder.build();
	}

	/**
	 * Sends frame to activity via message
	 * 
	 * @param frame
	 *            acquired from the bioplux device
	 */
	// comentario �intentar optimizar el env�o de datos?
	private void sendFrameToActivity(BITalinoFrame frame) {
		Bundle b = new Bundle();
		b.putDouble(KEY_X_VALUE, xValue);
		// Revisar

		
		
		//Bitalino always send 6 channels but only active which you selected
		ArrayList<Integer> activeChannels = configuration.getActiveChannels();
		int[] activeChannelsArray = convertToBitalinoChannelsArray(activeChannels);
				
		short[] frameShort = new short[6];
		
		for(int indTemp=0;indTemp<6;indTemp++){
			frameShort[indTemp]=0;
		}

		for(int ind=0; ind<activeChannelsArray.length;ind++){
			frameShort[ind]=(short) (frame.getAnalog(activeChannelsArray[ind]));
		}
		
		
		b.putShortArray(KEY_FRAME_DATA, frameShort);
		Message message = Message.obtain(null, MSG_DATA);
		message.setData(b);
		try {
			client.send(message);
		} catch (RemoteException e) {
			clientActive = false;
			Log.i(TAG, "client is dead");
		}
	}

	/**
	 * Notifies the client that the recording frames were stored properly
	 */
	private void sendSavedNotification() {
		Message message = Message.obtain(null, MSG_SAVED);
		try {
			client.send(message);
		} catch (RemoteException e) {
			Log.e(TAG, "client is dead. Service is being stopped", e);
			killServiceError = true;
			stopSelf();
		}
	}

	/**
	 * Sends the an error code to the client with the corresponding error that
	 * it has encountered
	 */
	private void sendErrorToActivity(int errorCode) {
		try {
			client.send(Message
					.obtain(null, MSG_CONNECTION_ERROR, errorCode, 0));
		} catch (RemoteException e) {
			Log.e(TAG,
					"Exception sending error message to activity. Service is stopping",
					e);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		killServiceError = true;
		stopSelf();
		super.onTaskRemoved(rootIntent);
	}

	/**
	 * Stops the service properly whilst being destroyed
	 */
	private void stopService() {
		if (timer != null)
			timer.cancel();

		while (areWeWritingDataToFile) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e2) {
				Log.e(TAG, "Exception thread is sleeping", e2);
			}
		}
		if (!dataManager.closeWriters())
			sendErrorToActivity(CODE_ERROR_SAVING_RECORDING);
		connection.stop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!killServiceError) {
			stopService();
			new Thread() {
				@Override
				public void run() {
					boolean errorSavingRecording = false;
					if (!dataManager.saveAndCompressFile(client)) {
						errorSavingRecording = true;
						sendErrorToActivity(CODE_ERROR_SAVING_RECORDING);
					}
					if (!errorSavingRecording)
						sendSavedNotification();
					wakeLock.release();
				}
			}.start();
		}
		Log.i(TAG, "service destroyed");
	}
}