/*********************************************************************/
/* Copyright (c) 2014 TOYOTA MOTOR CORPORATION. All rights reserved. */
/*********************************************************************/

package com.example.sample;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sample.data.DataSnapshot;
import com.example.sample.data.DataStore;

public class SampleActivity extends Activity implements ICommNotify{
	private static final int REQUEST_BTDEVICE_SELECT = 1;
	private Button _btnConnect;
	private Button _btnDisconnect;
	private Button _btnSelectDevice;
    private Button _btnDrift;
    private Button _btnSafety;
	private TextView _tvDataLabel;
    private TextView _tvData2Label;

    public DataStore store;

	/* declaration of Communication class */
	private Communication _comm;

	private Timer _timer;
	private TimerTask _timerTask;

	/* variable of the CAN-Gateway ECU Address */
	private String _strDevAddress = "";

	private final String _tag = "SampleActivity";
	/* interval for sending vehicle signal request (milliseconds) */
	private final int TIMER_INTERVAL = 100;

	private ByteBuffer _buf = null;

    private boolean drift_mode;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /* Create the Communication class */
        _comm = new Communication();
        /* Set the Notification interface */
        _comm.setICommNotify(this);

        _tvDataLabel = (TextView)findViewById(R.id.textView_signal);
        _tvData2Label = (TextView)findViewById(R.id.textView_signal2);
        _btnConnect = (Button)findViewById(R.id.button_connect);
        _btnDisconnect = (Button)findViewById(R.id.button_disconnect);
        _btnDrift = (Button)findViewById(R.id.button_drift);
        _btnSafety = (Button)findViewById(R.id.button_safety);
        _btnSelectDevice = (Button)findViewById(R.id.button_select);
        _btnConnect.setOnClickListener(_onClickListener);
        _btnDisconnect.setOnClickListener(_onClickListener);
        _btnSelectDevice.setOnClickListener(_onClickListener);
        _btnDrift.setOnClickListener(_onClickListener);
        _btnSafety.setOnClickListener(_onClickListener);

        drift_mode = true;
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void finish() {
		stopTimer();
        /* Set the Notification interface */
        _comm.setICommNotify(null);
		/* Close the session */
		_comm.closeSession();

		super.finish();
	}	
    
	OnClickListener _onClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			Button btn = (Button)v;
			if (btn == _btnConnect){
				if (_comm.isCommunication()){
					return;
				}
				/* Open the session */
				if (!_comm.openSession(_strDevAddress)){
					showAlertDialog("OpenSession Failed");
				};
			}else if(btn == _btnDisconnect){	
				stopTimer();
				/* Close the session */
				_comm.closeSession();
			}else if(btn == _btnSelectDevice){	
				Intent intent = new Intent(SampleActivity.this,DeviceListActivity.class);
				startActivityForResult(intent, REQUEST_BTDEVICE_SELECT);
			}else if(btn == _btnDrift){
                ((TextView)findViewById(R.id.firstLabel)).setText("Countersteer Rating");
                ((TextView)findViewById(R.id.secondLabel)).setText("Drift Score");
                drift_mode = true;
            }else if(btn == _btnSafety){
                ((TextView)findViewById(R.id.firstLabel)).setText("Live Fuel Economy (km/l)");
                ((TextView)findViewById(R.id.secondLabel)).setText("Average Fuel Economy (km/l)");
                drift_mode = false;
            }
		}
	};

	@Override
	public void notifyReceiveData(Object data) {
		Log.d(_tag,String.format("RECEIVE"));
		ByteBuffer rcvData = (ByteBuffer)data;

		/* Combine received messages */
		if(isCombineFrame(rcvData) == true){
			/* all data received */
			if (isFrameCheck(_buf) != true)
			{
				/* frame error */
				_buf.clear();
				_buf = null;
				return;
			}
			else
			{
				rcvData = _buf;
				_buf.clear();
				_buf = null;
			}
		}
		else
		{
			/* all data not received */
			return;
		}

		byte tmps[] = rcvData.array();
		int len = rcvData.limit();
		/* Analyze the message */
		if (isCarInfoGetFrame(rcvData) == true && len >= 8){
			/* message of vehicle signal request */
			String strDataLat = "";
            String strDataLon = "";

			/* Number of signals */
			int dataCount = (int)tmps[4] & 0xff;
			int index = 5;
			/* Vehicle signal */

            Log.d(_tag, "Data count " + dataCount);

            DataSnapshot dataSnapshot = new DataSnapshot();
			for (int i = 0 ; i < dataCount ; i++){
				int tmpData = toUint16Value(tmps, index);
				int signalID = (tmpData & 0x0fff);
				int stat 	 = ((tmpData >> 12) & 0x0f);

                long value;

                if (Constants.isSigned(signalID)) {
                    value = toInt32Value(tmps, index + 2);
                } else {
                    value = toUint32Value(tmps, index + 2);
                }

                dataSnapshot.addData(signalID, value);

//				Log.d(_tag,String.format("SIGNALID = %d, SIGNALSTAT = %d, VALUE = %d", signalID,stat,value));

                index += 6;
			}

            store.addSnapshot(dataSnapshot);

            boolean oversteering = false;

            if (drift_mode) {
                // Remember to change text
                strDataLat = Long.toString(dataSnapshot.calculateCountersteerRating());
                strDataLon = Long.toString(store.getDriftScore());

                if (dataSnapshot.calculateCountersteerRating() > 100l) {
                    oversteering = true;
                }
            } else {
                // Remember to change text
                strDataLat = Double.toString(dataSnapshot.getKmplSinceLast());
                strDataLon = Double.toString(dataSnapshot.getKmplCumulative());

                if (dataSnapshot.getKmplSinceLast() < 5) {
                    oversteering = true;
                }
            }
            Log.d(_tag, "Countersteer rating = " + strDataLat + "; Drift score = " + strDataLon);

            Log.d(_tag, "Updating values on screen");
            updateContents(strDataLat, strDataLon, oversteering);
		}else{
			Log.d(_tag,"UNKNOWN FRAME");
		}
	}

	/* Notify Bluetooth state of change */
	@Override
	public void notifyBluetoothState(int nState) {
		String strState;
		if (nState == Communication.STATE_NONE){
			/* non status */
			strState = "NOTE";
		}
		else if (nState == Communication.STATE_CONNECTING){
			/* connecting */
			strState = "CONNECTING";
		}
		else if (nState == Communication.STATE_CONNECTED){
			/* connected */
			strState = "CONNECTED";
		}
		else if (nState == Communication.STATE_CONNECT_FAILED){
			/* connect failed */
			strState = "CONNECT_FAILED";
		}
		else if (nState == Communication.STATE_DISCONNECTED){
			/* disconnected */
			_buf = null;
			strState = "DISCONNECTED";
		}
		else{
			/* unknown */
			strState = "UNKNOWN";
		}
		dspToast(strState);
		
		Log.d(_tag,String.format("STATE = %s",strState));
		if(nState == Communication.STATE_CONNECTED){
			/* delay time                                            */
			/* (Connect to the CAN-Gateway -> Send the first message */
			_handler.sendMessageDelayed(_handler.obtainMessage(), 2000);
		}
	}

	Handler _handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			/* Send the message of vehicle signal request */
			startTimer(TIMER_INTERVAL);
		}
	};
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_BTDEVICE_SELECT){
			if (resultCode == Activity.RESULT_OK) {
				_strDevAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
			}
		}
	}	
	
	private void updateContents(final String strDataLatitude, final String strDataLongitude, final boolean oversteering){
		_handler.post(new Runnable(){
			@Override
			public void run() {
				_tvDataLabel.setText(strDataLatitude);
                _tvData2Label.setText(strDataLongitude);
                if (oversteering) {
                    _tvDataLabel.setBackgroundColor(Color.RED);
                } else {
                    _tvDataLabel.setBackgroundColor(Color.BLACK);
                }
			}
		});
	}

    /* Create the message of vehicle signal request */
    private ByteBuffer createCarInfo(){
		/* e.g.) request of Engine Revolution Speed */
        byte[] buf = new byte[78];
        buf[0] = 0x7e;
        buf[3] = 0x01;
        buf[4] = 0x23;
        buf[buf.length - 1 ] = 0x7f;
        int length = buf.length;

		/* Set the message length */
        buf[1] = (byte)(((length - 6) >> 8) & 0xff);
        buf[2] = (byte)((length - 6) & 0xff);

		/* Set the request signal IDs */
        buf[6] = (byte)(Constants.TIMESTAMP);
        buf[8] = (byte)(Constants.ACCELERATOR_POSITION);
        buf[10] = (byte)(Constants.BRAKE_PEDAL_STATUS);
        buf[12] = (byte)(Constants.PARKING_BRAKE_STATUS);
        buf[14] = (byte)(Constants.AT_SHIFT_POSITION);
        buf[16] = (byte)(Constants.MANUAL_MODE_STATUS);
        buf[18] = (byte)(Constants.TRANSMISSION_GEAR_POSITION);
        buf[20] = (byte)(Constants.STEERING_WHEEL_ANGLE);
        buf[22] = (byte)(Constants.DOORS_STATUS);
        buf[24] = (byte)(Constants.SEATBELTS_STATUS);
        buf[26] = (byte)(Constants.HEADLAMP_STATUS);


        buf[28] = (byte)(Constants.ENGINE_REVOLUTION_SPEED);
        buf[30] = (byte)(Constants.VEHICLE_SPEED);
        buf[32] = (byte)(Constants.ACCELERATION_FRONT_BACK);
        buf[34] = (byte)(Constants.ACCELERATION_TRANSVERSE);
        buf[36] = (byte)(Constants.YAW_RATE);
        buf[38] = (byte)(Constants.ODOMETER);
        buf[40] = (byte)(Constants.FUEL_CONSUMPTION);
        buf[42] = (byte)(Constants.OUTSIDE_TEMPERATURE);
        buf[44] = (byte)(Constants.ENGINE_COOLANT_TEMPERATURE);
        buf[46] = (byte)(Constants.ENGINE_OIL_TEMPERATURE);
        buf[48] = (byte)(Constants.TRANSMISSION_TYPE);


        buf[50] = (byte)(Constants.GPS_TIME);
        buf[52] = (byte)(Constants.LATITUDE);
        buf[54] = (byte)(Constants.NORTH_OR_SOUTH);
        buf[56] = (byte)(Constants.LONGITUDE);
        buf[58] = (byte)(Constants.EAST_OR_WEST);
        buf[60] = (byte)(Constants.GPS_QUALITY);
        buf[62] = (byte)(Constants.NUMBER_OF_SATELLITES);
        buf[64] = (byte)(Constants.ANTENNA_ALTITUDE);
        buf[66] = (byte)(Constants.ALTITUDE_UNITS);
        buf[68] = (byte)(Constants.GEOIDAL_SEPARATION);
        buf[70] = (byte)(Constants.GEOIDAL_SEPARATION_UNITS);
        buf[72] = (byte)(Constants.SPEED_OVER_GROUND);
        buf[74] = (byte)(Constants.COURSE_OVER_GROUND);


		/* Calculate and set the CRC */
        int crc = calcCRC(buf, 1, buf.length - 4);
		/* Convert endian from little to big */
        buf[length - 3] = (byte)((crc >> 8) & 0xff);
        buf[length - 2] = (byte)(crc & 0xff);
        return ByteBuffer.wrap(buf);
    }

    /* Create the message of vehicle signal request */
    private ByteBuffer createCarInfoGetFrame(){
		/* e.g.) request of Engine Revolution Speed */
        byte[] buf = {0x7e,0x00,0x00,0x01,0x01,0x00,0x00,0x00,0x00,0x7f};
        int length = buf.length;
		/* Set the message length */
        buf[1] = (byte)(((length - 6) >> 8) & 0xff);
        buf[2] = (byte)((length - 6) & 0xff);
		/* Set the request signal ID */
        buf[6] = (byte)(Constants.ENGINE_REVOLUTION_SPEED);
		/* Calculate and set the CRC */
        int crc = calcCRC(buf, 1, buf.length - 4);
		/* Convert endian from little to big */
        buf[length - 3] = (byte)((crc >> 8) & 0xff);
        buf[length - 2] = (byte)(crc & 0xff);
        return ByteBuffer.wrap(buf);
    }

	private void startTimer(int timerCount){
        this.store = new DataStore();
		stopTimer();
		_timer = new Timer(false);
		_timerTask = new TimerTask() {
			public void run(){
				/* Send the message of vehicle signal request */
				_comm.writeData(createCarInfo());
			}
		};
		_timer.schedule(_timerTask,0,timerCount);
	}
	
	private void stopTimer(){
		if (_timer != null){
			_timer.cancel();
			_timer = null;
		}
	}
	
	private void showAlertDialog(String strMessage){
		AlertDialog.Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle(strMessage);
		dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				/* non-treated */
			}
		});
		dlg.show();
	}

	/* Combine received messages */
	public boolean isCombineFrame(ByteBuffer frame){
		frame.position(0);
		byte[] rcv = new byte[frame.limit()];
		frame.get(rcv, 0, frame.limit());

		/* Buffer for received message */
		if(_buf == null){
			_buf = ByteBuffer.allocate(rcv.length);
			_buf.put(rcv);
		}else{
			byte[] tmp = _buf.array();
			ByteBuffer newBuf = ByteBuffer.allocate(tmp.length + rcv.length);
			newBuf.put(tmp);
			newBuf.put(rcv);
			_buf = newBuf;
		}

		/* Check the message length */
		byte[] tmps = _buf.array();
		int len = _buf.limit();
		int dataLen = this.toUint16Value(tmps, 1);
		if ((dataLen + 6) > len){
			/* all data not received */
			return false;
		}
		else
		{
			/* all data received */
			return true;
		}
	}
	
	private boolean isFrameCheck(ByteBuffer frame){
		byte[] tmps = frame.array();
		int len = frame.limit();
		if(len < 3){
			Log.d(_tag,"FRAME LENGTH ERROR1");
			return false;
		}
		int dataLen = this.toUint16Value(tmps, 1);
		if ((dataLen + 6) != len){
			Log.d(_tag,"FRAME LENGTH ERROR2");
			return false;
		}
		if (tmps[0] != 0x7E){
			Log.d(_tag,"HEADER ERROR");
			return false;
		}
		if (tmps[len - 1] != 0x7F){
			Log.d(_tag,"FOOTER ERROR");
			return false;
		}
		if (tmps[3] != 0x11){
			Log.d(_tag,"FRAME TYPE ERROR");
			return false;
		}
		int crc = this.toUint16Value(tmps, len - 3);
		int calcCrc = this.calcCRC(tmps, 1, len - 4);
		if (crc != calcCrc){
			Log.d(_tag,"CRC ERROR");
			return false;
		}
		return true;
	}
		
	private boolean isCarInfoGetFrame(ByteBuffer frame){
		byte tmp = frame.get(3);
		if (tmp == 0x11){
			return true;
		}
		return false;
	}
	
    private int calcCRC(byte[] buffer, int index, int length) {
		int crcValue = 0x0000;
	    boolean flag;
	    boolean c15;
	    for( int i = 0; i < length; i++ ) {
	        for(int j = 0; j < 8; j++){
	            flag = ( (buffer[i + index] >> (7 - j) ) & 0x0001)==1;
	            c15  = ((crcValue >> 15 & 1) == 1);
	            crcValue <<= 1;
	            if(c15 ^ flag){
	                crcValue ^= 0x1021;
	            }
	        }
	    }
	    crcValue ^= 0x0000;
	    crcValue &= 0x0000ffff;
	    return crcValue;
    } 	
		
    private int toUint16Value(byte[] buffer, int index) {
    	int value = 0;
    	value |= (buffer[index + 0] << 8) & 0x0000ff00;
    	value |= (buffer[index + 1] << 0) & 0x000000ff;
    	return value & 0xffff;
    }
    
    private long toUint32Value(byte[] buffer, int index) {
    	int value = 0;
    	value |= (buffer[index + 0] << 24) & 0xff000000;
    	value |= (buffer[index + 1] << 16) & 0x00ff0000;
    	value |= (buffer[index + 2] <<  8) & 0x0000ff00;
    	value |= (buffer[index + 3] <<  0) & 0x000000ff;
    	return value & 0xffffffffL;
    }

    private long toInt32Value(byte[] buffer, int index) {
        long value = 0;
        value |= (buffer[index + 0] << 24) & 0xff000000;
        value |= (buffer[index + 1] << 16) & 0x00ff0000;
        value |= (buffer[index + 2] <<  8) & 0x0000ff00;
        value |= (buffer[index + 3] <<  0) & 0x000000ff;

        long signBit = 1 << 32;

        if ((value & signBit) > 0) {
            value = value - signBit;
            value = value ^ 0xffffffff;
            value = - value;
        }

        return value;
    }

	private void dspToast(final String strToast){
		_handler.post(new Runnable(){
			@Override
			public void run() {
				Toast toast = Toast.makeText(SampleActivity.this, strToast, Toast.LENGTH_SHORT);
				toast.show();
			}
		});
	}
	
}