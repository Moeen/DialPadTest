package com.matsol.android.app.dialpadtest.utils;

import com.matsol.android.app.dialpadtest.ui.DialpadActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;

/**
 * Broadcast Receiver Class for handling incoming calls
 * 
 * @author MatSol
 * 
 */
public class IncomingCallReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SipAudioCall incomingCall = null;
		try {
			// Creating a SipAudioCall listener to listen for incoming calls
			SipAudioCall.Listener listener = new SipAudioCall.Listener() {
				// when phone is ringing
				@Override
				public void onRinging(SipAudioCall call, SipProfile caller) {
					try {
						// auto answering the call for demo purpose
						call.answerCall(30);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			// calling SipManager from DialpadActivity
			DialpadActivity wtActivity = (DialpadActivity) context;
			incomingCall = wtActivity.manager.takeAudioCall(intent, listener);
			incomingCall.answerCall(30);
			incomingCall.startAudio();
			incomingCall.setSpeakerMode(true);
			// check if phone is on mute or not
			if (incomingCall.isMuted()) {
				incomingCall.toggleMute();
			}

			wtActivity.call = incomingCall;

			wtActivity.updateStatus(incomingCall);

		} catch (Exception e) {
			if (incomingCall != null) {
				incomingCall.close();
			}
		}
	}

}
