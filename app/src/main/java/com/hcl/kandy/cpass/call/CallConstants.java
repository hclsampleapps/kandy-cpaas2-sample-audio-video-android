package com.hcl.kandy.cpass.call;

public final class CallConstants {
    // Call start intents
    public static final String CALL_INTENT_KEY = "callIntent";
    public static final int CALL_INTENT_INCOMING_CALL = 1;
    public static final int CALL_INTENT_OUTGOING_CALL = 2;

    // Call start param keys
    public static final String CALL_PARAMS_CALL_ID_KEY = "callId";
    public static final String CALL_PARAMS_CALLER_KEY = "caller";
    public static final String CALL_PARAMS_CALLER_NAME_KEY = "callerName";
    public static final String CALL_PARAMS_CALLEE_KEY = "callee";
    public static final String CALL_PARAMS_CALLEE_NAME_KEY = "calleeName";
    public static final String CALL_PARAMS_CALL_CAN_SEND_VIDEO = "callCanSendVideo";
    public static final String CALL_PARAMS_CALL_DOUBLE_M_LINE = "isDoubleMLine";


    // Call event publishing keys
    public final static String EVENT_KEY = "event";
    public final static String EVENT_CALL_STATUS_CHANGED = "CALL_STATE_CHANGED";
    public final static String EVENT_MEDIA_ATTRIBUTES_CHANGED = "MEDIA_STATE_CHANGED";
    public final static String EVENT_AUDIO_ROUTE_CHANGED = "AUDIO_ROUTE_CHANGED";
    public final static String EVENT_CALL_OPERATION_PERFORMED = "CALL_OPERATION_PERFORMED";

    // Call events
    public final static String CALL_OPERATION_HOLD_SUCCESS = "CALL_OPERATION_HOLD_SUCCESS";
    public final static String CALL_OPERATION_UNHOLD_SUCCESS = "CALL_OPERATION_UNHOLD_SUCCESS";
    public static final String CALL_OPERATION_UNHOLD_FAIL = "CALL_OPERATION_UNHOLD_FAIL";
    public static final String CALL_OPERATION_HOLD_FAIL = "CALL_OPERATION_HOLD_FAIL";
    public static final String CALL_OPERATION_MUTE_SUCCESS = "CALL_OPERATION_MUTE_SUCCESS";
    public static final String CALL_OPERATION_MUTE_FAIL = "CALL_OPERATION_MUTE_FAIL";
    public static final String CALL_OPERATION_UNMUTE_FAIL = "CALL_OPERATION_UNMUTE_FAIL";
}
