package edu.rosehulman.tft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class BabyOnBoardConnectionService extends Service {

	private int readBufferPosition;
	private byte[] readBuffer;
	private boolean stopWorker;
	
	private InputStream mBtInputStream;
	private OutputStream mBtOutputStream;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.d("BT", "Starting Service!");

		BluetoothDevice btd = intent.getParcelableExtra(MainActivity.KEY_BLUETOOTH_DEVICE);

		UUID uuid = btd.getUuids()[0].getUuid();
		BluetoothSocket sock = null;
		try {
			sock = btd.createRfcommSocketToServiceRecord(uuid);
			sock.connect();

			final Handler handler = new Handler();

			mBtOutputStream = sock.getOutputStream();
			mBtInputStream = sock.getInputStream();
			
			mBtOutputStream.write("Hello from Phone".getBytes());

			readBufferPosition = 0;
			readBuffer = new byte[1024];
			stopWorker = false;

			Thread workerThread = new Thread(new Runnable() {
				final byte delimiter = '\n'; // newline

				public void run() {
					while (!Thread.currentThread().isInterrupted() && !stopWorker) {
						try {
							int bytesAvailable = mBtInputStream.available();
							if (bytesAvailable > 0) {
								byte[] packetBytes = new byte[bytesAvailable];
								mBtInputStream.read(packetBytes);
								for (int i = 0; i < bytesAvailable; i++) {

									byte b = packetBytes[i];

									if (b == delimiter) {
										byte[] encodedBytes = new byte[readBufferPosition];
										System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
										final String data = new String(encodedBytes, "US-ASCII");
										readBufferPosition = 0;

										handler.post(new Runnable() {
											public void run() {

												// baby on board logic
												
												Log.d("BT", data);
											}
										});

									} else {
										readBuffer[readBufferPosition++] = b;
									}
								}
							}
						} catch (IOException ex) {
							// This could be a problem. Needs to restart after.
							stopWorker = true;
						}
					}
				}
			});

			workerThread.start();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Can maybe use this to adjust HW settings
		return null;
	}

}
