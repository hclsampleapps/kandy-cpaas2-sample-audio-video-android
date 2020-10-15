package com.hcl.kandy.cpass.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.hcl.kandy.cpass.App;
import com.hcl.kandy.cpass.R;
import com.rbbn.cpaas.mobile.call.api.CallInterface;
import com.rbbn.cpaas.mobile.call.api.CallService;
import com.rbbn.cpaas.mobile.call.api.IncomingCallInterface;
import com.rbbn.cpaas.mobile.call.api.OutgoingCallCreationCallback;
import com.rbbn.cpaas.mobile.call.api.OutgoingCallInterface;
import com.rbbn.cpaas.mobile.utilities.exception.MobileError;
import com.rbbn.cpaas.mobile.utilities.exception.MobileException;

import java.util.HashMap;

import static com.hcl.kandy.cpass.call.CallConstants.CALL_INTENT_KEY;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALLEE_KEY;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_CAN_SEND_VIDEO;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_DOUBLE_M_LINE;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_ID_KEY;
import static com.hcl.kandy.cpass.call.CallConstants.EVENT_AUDIO_ROUTE_CHANGED;
import static com.hcl.kandy.cpass.call.CallConstants.EVENT_CALL_OPERATION_PERFORMED;
import static com.hcl.kandy.cpass.call.CallConstants.EVENT_CALL_STATUS_CHANGED;
import static com.hcl.kandy.cpass.call.CallConstants.EVENT_KEY;
import static com.hcl.kandy.cpass.call.CallConstants.EVENT_MEDIA_ATTRIBUTES_CHANGED;
import static com.hcl.kandy.cpass.call.IncomingCallState.ACCEPT_AUDIO;
import static com.hcl.kandy.cpass.call.IncomingCallState.ACCEPT_VIDEO;
import static com.hcl.kandy.cpass.call.IncomingCallState.IGNORE;
import static com.hcl.kandy.cpass.call.IncomingCallState.REJECT;


public class CallActivity extends AppCompatActivity implements IncomingCallFragment.OnIncomingCallActionChangeListener, ActiveCallFragment.OnActiveCallActionChangeListener {
    private static final String TAG = "CallActivity";
    private static Pair<String, Bundle> activeCall;
    private HashMap<String, Bundle> incomingCalls = new HashMap<>(1);
    private BroadcastReceiver callEventsBroadcastReceiver;

    public static boolean isActiveCallExist() {
        return activeCall != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setWindowFeatures();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        startForegroundCallService();
        setContentView(R.layout.activity_call);
        CameraHelper.setSupportedVideoSizes();
        parseCallIntent(getIntent());
    }

    private void setWindowFeatures() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseCallIntent(intent);
    }

    private void parseCallIntent(@Nullable Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();

//            CPaaSManager.getInstance().getcPaaSCallManager().setContext(this);
            String callId = extras.getString(CALL_PARAMS_CALL_ID_KEY);
            int callIntent = extras.getInt(CALL_INTENT_KEY);

            Log.d(TAG, "parseCallIntent: Received intent. callId: " + callId + " callIntent: " +
                    (callIntent == CallConstants.CALL_INTENT_OUTGOING_CALL ? "Outgoing call" : "Incoming call"));

            if (callIntent == CallConstants.CALL_INTENT_OUTGOING_CALL) {
                String callee = extras.getString(CALL_PARAMS_CALLEE_KEY);
                Log.d(TAG, "parseCallIntent: starting an outgoing call to " + callee);

                createOutgoingCall(extras);
            } else if (callIntent == CallConstants.CALL_INTENT_INCOMING_CALL) {
                incomingCalls.put(callId, extras);
                setCallActivityState(callId, IncomingCallState.RINGING);
            }

        }

    }

    private void setCallActivityState(String callId, CallState state) {
        Log.d(TAG, "setCallActivityState: callId: " + callId + " callState: " + state);
        if (incomingCalls.containsKey(callId)) {
            if (IncomingCallState.RINGING.equals(state)) {
                Log.d(TAG, "setCallActivityState: Incoming call ringing");
                injectIncomingCallFragment(callId);
                registerLocalBroadcastReceiver(callId);
            } else if (ACCEPT_VIDEO.equals(state)) {
                Log.d(TAG, "setCallActivityState: Incoming call accept video");
                convertIncomingCallToActiveCall(callId);
                injectActiveCallFragment(callId);
                registerLocalBroadcastReceiver(callId);
            } else if (ACCEPT_AUDIO.equals(state)) {
                Log.d(TAG, "setCallActivityState: Incoming call accept audio");
                convertIncomingCallToActiveCall(callId);
                injectActiveCallFragment(callId);
                registerLocalBroadcastReceiver(callId);
            } else if (REJECT.equals(state)) {
                Log.d(TAG, "setCallActivityState: Rejecting incoming call " + callId);
                incomingCalls.remove(callId);
                removeFragment(callId);
            } else if (IncomingCallState.ENDED.equals(state)) {
                Log.d(TAG, "setCallActivityState: Ending incoming call " + callId);
                incomingCalls.remove(callId);
                removeFragment(callId);
            } else if (IGNORE.equals(state)) {
                Log.d(TAG, "setCallActivityState: Removing incoming call " + callId);
                incomingCalls.remove(callId);
                removeFragment(callId);
            }
        } else if (activeCall != null && activeCall.first.equals(callId)) {
            if (ActiveCallState.HOLD.equals(state)) {
                Log.d(TAG, "setCallActivityState: Active call hold");
                // TODO
            } else if (ActiveCallState.ENDED.equals(state)) {
                Log.d(TAG, "setCallActivityState: Active call ended");
                activeCall = null;
                removeFragment(callId);
            } else if (ActiveCallState.HANGUP.equals(state)) {
                Log.d(TAG, "setCallActivityState: Active call HANGUP");
                activeCall = null;
                removeFragment(callId);
            } else if (ActiveCallState.TRANSFER.equals(state)) {
                Log.d(TAG, "setCallActivityState: Active call TRANSFER");
                // TODO
            } else if (ActiveCallState.MAKE_VIDEO_CALL.equals(state)) {
                Log.d(TAG, "setCallActivityState: Active call Make Video Call");
                injectActiveCallFragment(callId);
                registerLocalBroadcastReceiver(callId);
            } else if (ActiveCallState.MAKE_AUDIO_CALL.equals(state)) {
                Log.d(TAG, "setCallActivityState: Active call Make Audio Call");
                injectActiveCallFragment(callId);
                registerLocalBroadcastReceiver(callId);
            } else if (ActiveCallState.ERROR.equals(state) || ActiveCallState.NOT_EXIST.equals(state)) {
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                activeCall = null;
                removeFragment(callId);
            }
        } else {
            removeFragment(callId);
        }
    }

    private void convertIncomingCallToActiveCall(String callId) {
        Log.d(TAG, "convertIncomingCallToActiveCall: Promoting " + callId + " to active call");
        Bundle bundle = incomingCalls.get(callId);
        Log.d(TAG, "convertIncomingCallToActiveCall: Removing incoming call " + callId);
        incomingCalls.remove(callId);
        Log.d(TAG, "convertIncomingCallToActiveCall: Adding active call " + callId);
        activeCall = new Pair<>(callId, bundle);
    }

    private void registerLocalBroadcastReceiver(String callId) {
        Log.d(TAG, "registerLocalBroadcastReceiver: registering for callId " + callId);

        callEventsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra(EVENT_KEY);
                String callId = intent.getAction();

                switch (event) {
                    case EVENT_CALL_STATUS_CHANGED:
                        String eventType = intent.getStringExtra(EVENT_CALL_STATUS_CHANGED);
                        Log.d(TAG, "registerLocalBroadcastReceiver: Event: " + event +
                                " Event type: " + eventType + " call id: " + callId);
                        callStateChanged(callId, eventType);
                        break;
                    case EVENT_MEDIA_ATTRIBUTES_CHANGED:
                        break;
                    case EVENT_CALL_OPERATION_PERFORMED:
                        String callOperation = intent.getStringExtra(EVENT_CALL_OPERATION_PERFORMED);
                        Log.d(TAG, "registerLocalBroadcastReceiver: EVENT_CALL_OPERATION_PERFORMED for " + callId
                                + " Operation: " + callOperation);
                        callOperationPerformed(callId, callOperation);
                        break;
                    case EVENT_AUDIO_ROUTE_CHANGED:
                        break;
                }
            }
        };

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(callEventsBroadcastReceiver, new IntentFilter(callId));
    }

    private void callOperationPerformed(String callId, String callOperation) {
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(callId);
        if (fragment != null && fragment.isAdded() && !fragment.isRemoving())
            ((CPaaSCallFragment) fragment).callOperationPerformed(callOperation);
    }

    private void callStateChanged(String callId, String eventType) {
        CPaaSCallFragment cPaaSCallFragment = (CPaaSCallFragment) getSupportFragmentManager().findFragmentByTag(callId);
        if (cPaaSCallFragment != null) {
            cPaaSCallFragment.callStateChanged(com.rbbn.cpaas.mobile.call.api.CallState.Type.valueOf(eventType));
        }

        switch (com.rbbn.cpaas.mobile.call.api.CallState.Type.valueOf(eventType)) {
            case INITIAL:
                break;
            case SESSION_PROGRESS:
                break;
            case IN_CALL:
                break;
            case RINGING:
                break;
            case ON_HOLD:
                setCallActivityState(callId, ActiveCallState.HOLD);
                break;
            case REMOTELY_HELD:
                break;
            case ON_DOUBLE_HOLD:
                break;
            case ENDED:
                if (activeCall != null && activeCall.first.equals(callId)) {
                    Log.d(TAG, "callStateChanged: Active call ENDED");
                    setCallActivityState(callId, ActiveCallState.ENDED);
                } else if (incomingCalls.containsKey(callId)) {
                    Log.d(TAG, "callStateChanged: Incoming Call ENDED");
                    setCallActivityState(callId, IncomingCallState.ENDED);
                }
                break;
            case DIALING:
                break;
            case ANSWERING:
                break;
            case UNKNOWN:
                break;
        }

    }

    private void createOutgoingCall(@NonNull Bundle bundle) {
        App applicationContext = (App) this.getApplicationContext();

        CallService callService = applicationContext.getCpass().getCallService();
        final String[] callId = new String[1];
        String callee = bundle.getString(CALL_PARAMS_CALLEE_KEY);
        boolean isVideoEnabled = bundle.getBoolean(CALL_PARAMS_CALL_CAN_SEND_VIDEO);
        boolean doubleMLine = bundle.getBoolean(CALL_PARAMS_CALL_DOUBLE_M_LINE);

        callService.createOutgoingCall(callee,
                applicationContext.getcPaaSCallManager(), new OutgoingCallCreationCallback() {
                    @Override
                    public void callCreated(OutgoingCallInterface callInteface) {
                        callId[0] = callInteface.getId();
                        bundle.putString(CALL_PARAMS_CALL_ID_KEY, callId[0]);
                        activeCall = new Pair<>(callId[0], bundle);

                        //Map<String, String> map = new HashMap<>();
                        //map.put("X-Emergency", "yes");
                        //map.put("X-GPS","42.686032,23.344565");
                        if (isVideoEnabled) {
                            //callInteface.establishCall(true,map);
                            callInteface.establishCall(true);
                            setCallActivityState(callId[0], ActiveCallState.MAKE_VIDEO_CALL);
                        } else if (doubleMLine) {
                            callInteface.establishCall(false);
                            setCallActivityState(callId[0], ActiveCallState.MAKE_VIDEO_CALL);
                        } else {
                            callInteface.establishAudioCall();
                            setCallActivityState(callId[0], ActiveCallState.MAKE_AUDIO_CALL);
                        }
                    }

                    @Override
                    public void callCreationFailed(MobileError error) {
                        Log.e(TAG, "callCreationFailed: " + error.getErrorMessage());
                        setCallActivityState(callId[0], ActiveCallState.ERROR);
                    }
                });

    }

    private void injectActiveCallFragment(String callId) {
        Log.d(TAG, "injectActiveCallFragment for " + callId);

        ActiveCallFragment activeCallFragment = new ActiveCallFragment();
        activeCallFragment.setArguments(activeCall.second);
        getSupportFragmentManager().beginTransaction().replace(
                R.id.call_fragment_group,
                activeCallFragment, callId).commit();
    }

    private void injectIncomingCallFragment(String callId) {
        Log.d(TAG, "injectIncomingCallFragment for " + callId);

        IncomingCallFragment incomingCallFragment = new IncomingCallFragment();
        incomingCallFragment.setArguments(incomingCalls.get(callId));
        getSupportFragmentManager().beginTransaction()
                .add(R.id.call_fragment_group,
                        incomingCallFragment, callId)
                .commit();
    }

    @Override
    public void onIncomingCallActionChange(IncomingCallState action, String callId) {
        App applicationContext = (App) this.getApplicationContext();

        IncomingCallInterface incomingCall = (IncomingCallInterface) applicationContext
                .getcPaaSCallManager()
                .getActiveCall(callId);

        Log.d(TAG, "onIncomingCallActionChange: " + action + " " + callId);
        if (incomingCall != null) {
            switch (action) {
                case ACCEPT_VIDEO:
                    incomingCall.acceptCall(true);
                    setCallActivityState(callId, ACCEPT_VIDEO);
                    break;
                case ACCEPT_AUDIO:
                    incomingCall.acceptCall(false);
                    setCallActivityState(callId, ACCEPT_AUDIO);
                    break;
                case REJECT:
                    incomingCall.rejectCall();
                    setCallActivityState(callId, REJECT);
                    break;
                case IGNORE:
                    incomingCall.ignoreCall();
                    setCallActivityState(callId, IGNORE);
                    break;
            }
        } else {
            Log.d(TAG, "onIncomingCallActionChange: incoming call is null, not processing the "
                    + action + " action. " + callId);
        }
    }

    private void removeFragment(String fragmentTag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
        Log.d(TAG, "removeFragment: trying to remove " + fragmentTag);

        if (fragment != null) {
            Log.d(TAG, "removeFragment: Fragment is exist, removing it.");
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(fragment)
                    .commit();

        } else {
            Log.d(TAG, "removeFragment: not removing, fragment does not exist");
        }

        if (activeCall == null && incomingCalls.isEmpty()) {
            Log.d(TAG, "removeFragment: Not founding any active or incoming call, finishing the activity");
            this.finish();
        } else {
            Log.d(TAG, "removeFragment: An active call or incoming call is exist, not finishing the activity");
        }
    }

    @Override
    public void onActiveCallActionChange(ActiveCallState action, String callId) {
        App applicationContext = (App) this.getApplicationContext();
        CallInterface activeCall = applicationContext
                .getcPaaSCallManager()
                .getActiveCall(callId);

        Log.d(TAG, "onActiveCallActionChange: " + action);

        if (activeCall != null) {
            switch (action) {
                case HANGUP:
                    try {
                        Log.d(TAG, "onActiveCallActionChange: Ending the call");
                        activeCall.endCall();
                        setCallActivityState(callId, ActiveCallState.HANGUP);
                    } catch (MobileException e) {
                        Log.e(TAG, "onActiveCallActionChange: Cannot end the active call", e);
                        e.printStackTrace();
                        setCallActivityState(callId, ActiveCallState.ERROR);
                    }
                    break;
                case ENDED:
                    setCallActivityState(callId, ActiveCallState.ENDED);
                    break;
            }

        } else {
            Log.d(TAG, "onActiveCallActionChange: Active call does not exist");
            setCallActivityState(callId, ActiveCallState.NOT_EXIST);
        }

    }

    private void unregisterLocalBroadcastReceiver() {
        if (callEventsBroadcastReceiver != null) {
            Log.d(TAG, "unregisterLocalBroadcastReceiver");
            LocalBroadcastManager.getInstance(this).unregisterReceiver(callEventsBroadcastReceiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterLocalBroadcastReceiver();
        stopForegroundCallService();
    }

    private void startForegroundCallService() {
        Log.d(TAG, "startForegroundCallService");
        startService(new Intent(getApplicationContext(), ForegroundCallService.class)
                .setAction(ForegroundCallService.ACTION_START_FOREGROUND_CALL_SERVICE));
    }

    private void stopForegroundCallService() {
        Log.d(TAG, "stopForegroundCallService");
        startService(new Intent(getApplicationContext(), ForegroundCallService.class)
                .setAction(ForegroundCallService.ACTION_STOP_FOREGROUND_CALL_SERVICE));
    }

    @Override
    public void onBackPressed() {
        if (activeCall == null) {
            super.onBackPressed();
        }
    }
}