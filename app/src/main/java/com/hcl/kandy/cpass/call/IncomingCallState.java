package com.hcl.kandy.cpass.call;

public enum IncomingCallState implements CallState {
    RINGING,
    ACCEPT_VIDEO,
    ACCEPT_AUDIO,
    REJECT,
    ENDED,
    IGNORE
}