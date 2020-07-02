package com.hcl.kandy.cpass.call;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.hcl.kandy.cpass.App;
import com.rbbn.cpaas.mobile.CPaaS;
import com.rbbn.cpaas.mobile.call.api.CallApplicationListener;
import com.rbbn.cpaas.mobile.call.api.CallInterface;
import com.rbbn.cpaas.mobile.call.api.CallService;
import com.rbbn.cpaas.mobile.call.api.CallState;
import com.rbbn.cpaas.mobile.call.api.IncomingCallInterface;
import com.rbbn.cpaas.mobile.call.api.MediaAttributes;
import com.rbbn.cpaas.mobile.call.api.OutgoingCallInterface;
import com.rbbn.cpaas.mobile.utilities.exception.MobileError;

import java.util.Map;

public class CPaaSCallManager implements CallApplicationListener {
    private final String TAG = "CPaaSCallManager";
    private Context context;

    public CallInterface getActiveCall(String callId) {
        App applicationContext = (App) context.getApplicationContext();

        CPaaS cpass = applicationContext.getCpass();
        CallService callService = cpass.getCallService();

        for (CallInterface call : callService.getActiveCalls()) {
            if (call.getId().equals(callId))
                return call;
        }

        return null;

    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private void broadcastCallOperationResult(String callId, String callOperation) {
        Log.d(TAG, "broadcastCallOperationResult: " + callId + " " + callOperation);
        Intent intent = new Intent(callId);
        intent.putExtra(CallConstants.EVENT_KEY, CallConstants.EVENT_CALL_OPERATION_PERFORMED);
        intent.putExtra(CallConstants.EVENT_CALL_OPERATION_PERFORMED, callOperation);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void incomingCall(IncomingCallInterface call) {
        Bundle bundle = new Bundle();
        bundle.putString(CallConstants.CALL_PARAMS_CALL_ID_KEY, call.getId());
        bundle.putString(CallConstants.CALL_PARAMS_CALLER_KEY, call.getCallerAddress());
        bundle.putString(CallConstants.CALL_PARAMS_CALLER_NAME_KEY, call.getCallerName());
        bundle.putInt(CallConstants.CALL_INTENT_KEY, CallConstants.CALL_INTENT_INCOMING_CALL);
        bundle.putBoolean(CallConstants.CALL_PARAMS_CALL_CAN_SEND_VIDEO, call.canSendVideo());

        Log.i(TAG, "incomingCall: id is " + call.getId());
        if (CallActivity.isActiveCallExist()) {
            Toast.makeText(context, "Ignoring incoming call while in the active call", Toast.LENGTH_SHORT).show();
            App applicationContext = (App) context.getApplicationContext();

            ((IncomingCallInterface) applicationContext.getcPaaSCallManager().getActiveCall(call.getId())).ignoreCall();
            return;
        }
        if (context != null) {
            context.startActivity(new Intent(context, CallActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // using this flag is crucial to handling multiple calls, it sends an intent to already created activities, instead of recreating it.
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtras(bundle));
        }
    }

    @Override
    public void callStatusChanged(CallInterface callInterface, CallState callState) {
        Log.d(TAG, "callStatusChanged: Call state is " + callState.getType() + " call id is " + callInterface.getId());
        Intent intent = new Intent(callInterface.getId());
        intent.putExtra(CallConstants.EVENT_KEY, CallConstants.EVENT_CALL_STATUS_CHANGED);
        intent.putExtra(CallConstants.EVENT_CALL_STATUS_CHANGED, callState.getType().toString());

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        boolean success = localBroadcastManager.sendBroadcast(intent);
        String callType = callInterface instanceof OutgoingCallInterface ? "Outgoing" : "Incoming";
        Log.d(TAG, "callStatusChanged: " + callType + " send broadcast " + (success ? "success" : "failure"));
    }

    @Override
    public void mediaAttributesChanged(CallInterface callInterface, MediaAttributes mediaAttributes) {
        Log.d(TAG, "mediaAttributesChanged: MediaAttributes are " + mediaAttributes.toString() + " call id is " + callInterface.getId());
        Intent intent = new Intent(callInterface.getId());
        intent.putExtra(CallConstants.EVENT_KEY, CallConstants.EVENT_MEDIA_ATTRIBUTES_CHANGED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void callAdditionalInfoChanged(CallInterface call, Map<String, String> events) {

    }

    @Override
    public void errorReceived(CallInterface call, MobileError error) {

    }

    @Override
    public void errorReceived(MobileError error) {

    }

    @Override
    public void establishCallSucceeded(OutgoingCallInterface call) {
        Toast.makeText(context, "establishCallSucceeded", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void establishCallFailed(OutgoingCallInterface call, MobileError error) {
        Toast.makeText(context, "establishCallFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void acceptCallSucceed(IncomingCallInterface call) {

    }

    @Override
    public void acceptCallFailed(IncomingCallInterface call, MobileError error) {

    }

    @Override
    public void rejectCallSucceeded(IncomingCallInterface incomingCall) {
        Log.d(TAG, "rejectCallSucceeded: " + incomingCall.getId());
    }

    @Override
    public void rejectCallFailed(IncomingCallInterface call, MobileError error) {
        Log.d(TAG, "rejectCallFailed: " + call.getId());
        Toast.makeText(context, "rejectCallFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void ignoreSucceed(IncomingCallInterface call) {

    }

    @Override
    public void ignoreFailed(IncomingCallInterface call, MobileError error) {
        Toast.makeText(context, "ignoreFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void forwardCallSucceeded(IncomingCallInterface incomingCallInterface) {

    }

    @Override
    public void forwardCallFailed(IncomingCallInterface incomingCallInterface, MobileError mobileError) {

    }

    @Override
    public void videoStopSucceed(CallInterface call) {
        Log.d(TAG, "videoStopSucceed: " + call.getId());
    }

    @Override
    public void videoStopFailed(CallInterface call, MobileError error) {
        Log.d(TAG, "videoStopFailed: " + call.getId());
        Toast.makeText(context, "videoStopFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void videoStartSucceed(CallInterface call) {
        Log.d(TAG, "videoStartSucceed: " + call.getId());
    }

    @Override
    public void videoStartFailed(CallInterface call, MobileError error) {
        Log.d(TAG, "videoStartFailed: " + call.getId());
        Toast.makeText(context, "videoStartFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void muteCallSucceed(CallInterface call) {
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_MUTE_SUCCESS);
    }

    @Override
    public void muteCallFailed(CallInterface call, MobileError error) {
        Toast.makeText(context, "muteCallFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_MUTE_FAIL);
    }

    @Override
    public void unMuteCallSucceed(CallInterface call) {
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_MUTE_SUCCESS);
    }

    @Override
    public void unMuteCallFailed(CallInterface call, MobileError error) {
        Toast.makeText(context, "unMuteCallFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_UNMUTE_FAIL);
    }

    @Override
    public void holdCallSucceed(CallInterface call) {
        Log.d(TAG, "holdCallSucceed: " + call.getId());
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_HOLD_SUCCESS);
    }

    @Override
    public void holdCallFailed(CallInterface call, MobileError error) {
        Log.d(TAG, "holdCallFailed: " + call.getId());
        Toast.makeText(context, "holdCallFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_HOLD_FAIL);
    }

    @Override
    public void unHoldCallSucceed(CallInterface call) {
        Log.d(TAG, "unHoldCallSucceed: " + call.getId());
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_UNHOLD_SUCCESS);
    }

    @Override
    public void unHoldCallFailed(CallInterface call, MobileError error) {
        Log.d(TAG, "unHoldCallFailed: " + call.getId());
        Toast.makeText(context, "unHoldCallFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
        broadcastCallOperationResult(call.getId(), CallConstants.CALL_OPERATION_UNHOLD_FAIL);
    }

    @Override
    public void endCallSucceeded(CallInterface call) {
        Log.d(TAG, "endCallSucceeded: " + call.getId());
    }

    @Override
    public void endCallFailed(CallInterface call, MobileError error) {
        Log.d(TAG, "endCallFailed: " + call.getId());
        Toast.makeText(context, "endCallFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void ringingFeedbackSucceeded(IncomingCallInterface call) {

    }

    @Override
    public void ringingFeedbackFailed(IncomingCallInterface call, MobileError error) {
        Toast.makeText(context, "ringingFeedbackFailed " + error.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void transferCallSucceed(CallInterface callInterface) {

    }

    @Override
    public void transferCallFailed(CallInterface callInterface, MobileError mobileError) {

    }

    @Override
    public void notifyCallProgressChange(CallInterface call) {

    }
}
