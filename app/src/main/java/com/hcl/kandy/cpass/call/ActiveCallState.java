package com.hcl.kandy.cpass.call;

public enum ActiveCallState implements CallState {
    HOLD,
    ENDED,
    HANGUP,
    TRANSFER,
    MAKE_VIDEO_CALL,
    MAKE_AUDIO_CALL,
    ERROR,
    REMOTELY_HELD,
    ON_DOUBLE_HOLD,
    NOT_EXIST
}
