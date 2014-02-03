package edu.rosehulman.tft;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final String KEY_BLUETOOTH_DEVICE = "key_bluetooth_device";

	private static final int REQUEST_ENABLE_BT = 0;
	
	private BluetoothAdapter mBluetoothAdapter;
	private ListView mPairedDevicesListView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Paired Devices -- ListView + Adapter 
		mPairedDevicesListView = (ListView)findViewById(R.id.paired_devices_list_view);
		final BluetoothDevicesAdapter pairedDeviesAdapter = new BluetoothDevicesAdapter(this);
		mPairedDevicesListView.setAdapter(pairedDeviesAdapter);
		
		mPairedDevicesListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				BluetoothDevice btd = pairedDeviesAdapter.getItem(position);
				Toast.makeText(MainActivity.this, "selected " + btd.getName(), Toast.LENGTH_SHORT).show();
				
				// do something to connect to it
				createStartConnectionDialog(btd);
			}
		});
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// device has no bluetooth
			finish(); 
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		// Get the paired devices into the adapter
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
		    for (BluetoothDevice device : pairedDevices) {
		        pairedDeviesAdapter.add(device);
		    }
		}
	}
	
	private void createStartConnectionDialog(final BluetoothDevice btd) {
		DialogFragment df = new DialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
				
				alertBuilder.setTitle(R.string.connect_with_device_dialog_title);
				alertBuilder.setMessage(btd.getName() + "  " + btd.getAddress());
				
				alertBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do some connecting first!
						
						Intent babyOnBoardConnectionService = new Intent(MainActivity.this, BabyOnBoardConnectionService.class);
						babyOnBoardConnectionService.putExtra(KEY_BLUETOOTH_DEVICE, btd);
						
						MainActivity.this.startService(babyOnBoardConnectionService);
						
						dialog.dismiss();
					}
				});
				
				alertBuilder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				
				return alertBuilder.create();
			}
		};
		
		df.show(getFragmentManager(), "bt");
	}

	private class BluetoothDevicesAdapter extends BaseAdapter {
		
		private Context mContext;
		private ArrayList<BluetoothDevice> mDevices;
		
		public BluetoothDevicesAdapter(Context context) {
			mContext = context;
			mDevices = new ArrayList<BluetoothDevice>();
		}

		public void add(BluetoothDevice device) {
			mDevices.add(device);
		}

		@Override
		public int getCount() {
			return mDevices.size();
		}

		@Override
		public BluetoothDevice getItem(int position) {
			return mDevices.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DeviceView dv = null;
			if (convertView == null) {
				dv = new DeviceView(mContext);
			} else {
				dv = (DeviceView)convertView;
			}
			
			dv.setDeviceName(mDevices.get(position).getName());
			dv.setDeviceAddress(mDevices.get(position).getAddress());
			
			return dv;
		}
	}
	
	private class DeviceView extends LinearLayout {

		private Context mContext;
		private TextView mDeviceNameTextView;
		private TextView mDeviceAddressTextView;
		
		public DeviceView(Context context) {
			super(context);
			mContext = context;
			
			LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
			inflater.inflate(R.layout.device_view, this);
			mDeviceNameTextView = (TextView)findViewById(R.id.device_name);
			mDeviceAddressTextView = (TextView)findViewById(R.id.device_address);
		}
		
		public void setDeviceName(String name) {
			mDeviceNameTextView.setText(name);
		}
		
		public void setDeviceAddress(String address) {
			mDeviceAddressTextView.setText(address);
		}
	}
}
