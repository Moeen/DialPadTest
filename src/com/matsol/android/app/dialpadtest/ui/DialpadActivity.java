package com.matsol.android.app.dialpadtest.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.matsol.android.app.dialpadtest.R;
import com.matsol.android.app.dialpadtest.utils.DigitsEditText;
import com.matsol.android.app.dialpadtest.utils.IncomingCallReceiver;
import com.matsol.android.app.dialpadtest.utils.SipSettings;

/**
 * @author Matsol
 * 
 *         Main activity which run when application starts Dailer layout is
 *         defined int his class
 * 
 */
public class DialpadActivity extends SherlockActivity implements
		OnClickListener {
	private ImageButton buttons[] = new ImageButton[13];
	private final static int BUTTON_IDS[] = { R.id.button0, R.id.button1,
			R.id.button2, R.id.button3, R.id.button4, R.id.button5,
			R.id.button6, R.id.button7, R.id.button8, R.id.button9,
			R.id.button0, R.id.buttonstar, R.id.buttonpound };
	private ImageButton btnDelete, btnCall;

	private DigitsEditText txtLCD;

	public String sipAddress = null;

	public SipManager manager = null;
	public SipProfile me = null;
	public SipAudioCall call = null;
	public IncomingCallReceiver callReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		initConfig();

	}

	@Override
	public void onStart() {
		super.onStart();
		// initialize manager on start of the application
		initializeManager();
	}

	/**
	 * function to initialize all the variables
	 */
	private void initConfig() {
		registerWifiInfos();
		txtLCD = (DigitsEditText) findViewById(R.id.digit_lcd);
		btnDelete = (ImageButton) findViewById(R.id.deleteButton);
		btnCall = (ImageButton) findViewById(R.id.dialButton);
		int i = 0;
		for (int id : BUTTON_IDS) {
			final int t = i;
			buttons[i] = (ImageButton) findViewById(id);
			buttons[i].setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					txtLCD.append(buttons[t].getContentDescription().toString());
				}
			});
			i++;
		}
		btnDelete.setOnClickListener(this);
		btnCall.setOnClickListener(this);
		registerIncomingCalls();
	}

	/**
	 * function to register application for Receiving incoming calls
	 */
	private void registerIncomingCalls() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.SipDemo.INCOMING_CALL");
		callReceiver = new IncomingCallReceiver();
		this.registerReceiver(callReceiver, filter);
	}

	/**
	 * function to register RSSI Change and WIFI connect/disconnect listener
	 */
	private void registerWifiInfos() {
		BroadcastReceiver myRssiChangeReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				int newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
				if (newRssi > -90) {
					AlertDialog.Builder b = new AlertDialog.Builder(
							DialpadActivity.this);
					b.setMessage("Your Signal is very low right now");
					b.setTitle("Caution");
					b.setNeutralButton("OK", null);
					b.show();

				}
			}

		};
		registerReceiver(myRssiChangeReceiver, new IntentFilter(
				WifiManager.RSSI_CHANGED_ACTION));
		BroadcastReceiver wifiConnectionReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				ConnectivityManager myConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo myNetworkInfo = myConnManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				if (!myNetworkInfo.isConnected()) {
					AlertDialog.Builder d = new AlertDialog.Builder(
							DialpadActivity.this);
					d.setMessage("It seems you have disconnected to WIFI. Please connect to your WIFI");
					d.setNeutralButton("OK", null);
					d.show();
				}
			}

		};
		registerReceiver(wifiConnectionReceiver, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));

	}

	// remove all on going calls and unregister receivers on application destroy
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (call != null) {
			call.close();
		}

		closeLocalProfile();

		if (callReceiver != null) {
			this.unregisterReceiver(callReceiver);
		}
	}

	/**
	 * initialize SipMangar's object
	 */
	public void initializeManager() {
		if (manager == null) {
			manager = SipManager.newInstance(this);
		}

		initializeLocalProfile();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == R.id.action_setting) {
			updatePreferences();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * initializing local profile for user to connect with SIP Server
	 */
	public void initializeLocalProfile() {
		if (manager == null) {
			return;
		}

		if (me != null) {
			closeLocalProfile();
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		String username = prefs.getString("namePref", "");
		String domain = prefs.getString("domainPref", "");
		String password = prefs.getString("passPref", "");

		if (username.length() == 0 || domain.length() == 0
				|| password.length() == 0) {
			AlertDialog.Builder d = new AlertDialog.Builder(
					DialpadActivity.this);
			d.setMessage("Please update your Settings");
			d.setPositiveButton("Go To Settings",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							updatePreferences();
						}
					});
			d.setNegativeButton("Cancel", null);
			d.show();
			return;
		}

		try {
			SipProfile.Builder builder = new SipProfile.Builder(username,
					domain);
			builder.setPassword(password);
			me = builder.build();

			Intent i = new Intent();
			i.setAction("android.SipDemo.INCOMING_CALL");
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i,
					Intent.FILL_IN_DATA);
			manager.open(me, pi, null);

			manager.setRegistrationListener(me.getUriString(),
					new SipRegistrationListener() {
						public void onRegistering(String localProfileUri) {
							updateStatus("Registering with SIP Server...");
						}

						public void onRegistrationDone(String localProfileUri,
								long expiryTime) {
							updateStatus("Ready");
						}

						public void onRegistrationFailed(
								String localProfileUri, int errorCode,
								String errorMessage) {
							updateStatus("Registration failed.  Please check settings."
									+ "\n MSG: " + errorMessage);
						}
					});
		} catch (java.text.ParseException pe) {
			updateStatus("Connection Error.");
		} catch (SipException se) {
			updateStatus("Connection error.");
		}
	}

	/**
	 * closing local profile
	 */
	public void closeLocalProfile() {
		if (manager == null) {
			return;
		}
		try {
			if (me != null) {
				manager.close(me.getUriString());
			}
		} catch (Exception ee) {
			Log.d("WalkieTalkieActivity/onDestroy",
					"Failed to close local profile.", ee);
		}
	}

	/**
	 * function to initiate call whenever user click on the call button
	 */
	public void initiateCall() {

		try {
			SipAudioCall.Listener listener = new SipAudioCall.Listener() {

				@Override
				public void onCallEstablished(SipAudioCall call) {
					call.startAudio();
					call.setSpeakerMode(true);
					if (call.isMuted()) {
						call.toggleMute();
					}
					updateStatus(call);
				}

				@Override
				public void onCallEnded(SipAudioCall call) {
					updateStatus("Ready.");
				}

				@Override
				public void onCalling(SipAudioCall call) {
					// TODO Auto-generated method stub
					super.onCalling(call);
					updateStatus("calling..");
				}

				@Override
				public void onError(SipAudioCall call, int errorCode,
						String errorMessage) {
					// TODO Auto-generated method stub
					super.onError(call, errorCode, errorMessage);
					updateStatus("Error: " + errorMessage);

				}
			};
			Log.e("uri string", me.getUriString());
			Log.e("user", sipAddress + "@" + me.getSipDomain());

			call = manager.makeAudioCall(me.getUriString(), sipAddress + "@"
					+ me.getSipDomain(), listener, 30);

		} catch (Exception e) {
			Log.e("WalkieTalkieActivity/InitiateCall",
					"Error when trying to close manager.", e);
			if (me != null) {
				try {
					manager.close(me.getUriString());
				} catch (Exception ee) {
					Log.e("WalkieTalkieActivity/InitiateCall",
							"Error when trying to close manager.", ee);
					ee.printStackTrace();
				}
			}
			if (call != null) {
				call.close();
			}
		}
	}

	/**
	 * function to throw Toast within application
	 * 
	 * @param String
	 *            status
	 * 
	 */
	public void updateStatus(final String status) {
		// Be a good citizen. Make sure UI changes fire on the UI thread.
		this.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(DialpadActivity.this, status, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	/**
	 * function to throw Toast within application
	 * 
	 * @param SipAudioCall
	 *            call
	 */
	public void updateStatus(SipAudioCall call) {
		String useName = call.getPeerProfile().getDisplayName();
		if (useName == null) {
			useName = call.getPeerProfile().getUserName();
		}
		updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		// when delete button is clicked
		case R.id.deleteButton:
			if (txtLCD.getText().toString().length() > 0) {
				String str = txtLCD.getText().toString();
				str = str.substring(0, str.length() - 1);
				txtLCD.setText(str);
				txtLCD.setSelection(txtLCD.getText().length());
			}
			break;

		case R.id.dialButton:
			// when dial button is clicked
			if (txtLCD.getText().length() > 0) {
				sipAddress = txtLCD.getText().toString();
				initiateCall();
			} else {
				Toast.makeText(DialpadActivity.this, "Please enter a number",
						Toast.LENGTH_LONG).show();
			}
			break;
		}
	}

	/**
	 * update preferences whenever you want
	 */
	public void updatePreferences() {
		Intent settingsActivity = new Intent(getBaseContext(),
				SipSettings.class);
		startActivity(settingsActivity);
	}
}
