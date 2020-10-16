package com.hcl.kandy.cpass.call;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hcl.kandy.cpass.App;
import com.hcl.kandy.cpass.R;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.rbbn.cpaas.mobile.call.api.CallInterface;
import com.rbbn.cpaas.mobile.call.api.CallService;
import com.rbbn.cpaas.mobile.call.api.CallState;
import com.rbbn.cpaas.mobile.core.webrtc.view.VideoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_CAN_SEND_VIDEO;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_DOUBLE_M_LINE;


public class ActiveCallFragment extends Fragment implements CPaaSCallFragment {
    private final String TAG = this.getClass().getSimpleName();
    private String callId;
    private String callerOrCalleeAddress;
    private String callerOrCalleeName;

    private boolean callCanSendVideo;
    private boolean isDoubleMLine;
    private Timer durationCounter;
    private int callDuration = 0;

    private boolean speakerModeEnabled;
    private boolean userSelectedAnAudioMode;

    private int iconSize = 40;
    private int iconColor = Color.WHITE;

    private ImageButton hangupButton;
    private TextView callDurationTextView, activeCallerName;
    private View  topLevelLayout;
    private OnActiveCallActionChangeListener mListener;
    private ProgressDialog callProgressDialog;

    // Dialpad views
    private MediaPlayer mp;
    private int[] sounds = {R.raw.dtmf0, R.raw.dtmf1, R.raw.dtmf2, R.raw.dtmf3, R.raw.dtmf4, R.raw.dtmf5, R.raw.dtmf6, R.raw.dtmf7, R.raw.dtmf8, R.raw.dtmf9, R.raw.dtmfroot, R.raw.dtmfstar};
    public ActiveCallFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            callId = bundle.getString(CallConstants.CALL_PARAMS_CALL_ID_KEY);
            callCanSendVideo = bundle.getBoolean(CALL_PARAMS_CALL_CAN_SEND_VIDEO);
            isDoubleMLine = bundle.getBoolean(CALL_PARAMS_CALL_DOUBLE_M_LINE);
            callerOrCalleeAddress = bundle.containsKey(CallConstants.CALL_PARAMS_CALLEE_KEY) ?
                    bundle.getString(CallConstants.CALL_PARAMS_CALLEE_KEY) : bundle.getString(CallConstants.CALL_PARAMS_CALLER_KEY);
            callerOrCalleeName = bundle.containsKey(CallConstants.CALL_PARAMS_CALLEE_NAME_KEY) ?
                    bundle.getString(CallConstants.CALL_PARAMS_CALLEE_NAME_KEY) : bundle.getString(CallConstants.CALL_PARAMS_CALLER_NAME_KEY);
            Log.d(TAG, "onCreate: Active call id: " + callId);
        } else {
            Log.d(TAG, "onCreate: Not received any data from intent");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setVideoViews();
        setAudioRoute();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void callStateChanged(CallState.Type state) {
        Log.d(TAG, "callStateChanged: New Call State " + state.toString());

        if (state == CallState.Type.INITIAL) {
            setCallStateLabel("Initializing");
        } else if (state == CallState.Type.IN_CALL) {
            callDurationTextView.setVisibility(View.VISIBLE);
            setCallStateLabel("In Call");
            if (callCanSendVideo || isDoubleMLine) {
                setVideoViews();
            }
            startCallDurationCounter();
        } else if (state == CallState.Type.ON_HOLD) {
            setCallStateLabel("On Hold");
        } else if (state == CallState.Type.REMOTELY_HELD) {
            setCallStateLabel("Remotely Held");
        } else if (state == CallState.Type.ON_DOUBLE_HOLD) {
            setCallStateLabel("On Double Hold");
        } else if (state == CallState.Type.SESSION_PROGRESS) {
            setCallStateLabel("Session Progress");
        } else if (state == CallState.Type.ENDED) {
            setCallStateLabel("Ended");
        } else if (state == CallState.Type.RINGING) {
            setCallStateLabel("Ringing");
        }

    }

    @Override
    public void callOperationPerformed(String callOperation) {
        switch (callOperation) {
            case CallConstants.CALL_OPERATION_HOLD_SUCCESS:
            case CallConstants.CALL_OPERATION_HOLD_FAIL:
            case CallConstants.CALL_OPERATION_UNHOLD_SUCCESS:
            case CallConstants.CALL_OPERATION_UNHOLD_FAIL:
                callProgressDialog.dismiss();
                setMenuIcons();
                break;
        }
    }

    public void setCallStateLabel(String label) {
        TextView stateLabelTv = getActivity().findViewById(R.id.activeCallStateText);

        if (stateLabelTv != null) {
            stateLabelTv.setText(label);
        }
    }

    private void startCallDurationCounter() {
        if (durationCounter != null) return;

        durationCounter = new Timer();
        durationCounter.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                callDuration++;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String durationText = String.format("(%02d:%02d)", callDuration / 60, callDuration % 60);
                        callDurationTextView.setText(durationText);
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_call, container, false);
    }

    private void stopDTMFSoundAndSendDtmf(int padPosition) {
        Log.d(TAG, "stopDTMFSoundAndSendDtmf: " + padPosition);

        if (mp != null) mp.release();
        if (getActiveCall() != null && getActiveCall().getCallState().getType() == CallState.Type.IN_CALL) {
            if (padPosition < 10) {
                getActiveCall().sendDTMF(String.format("%d", padPosition).toCharArray()[0]);
            } else if (padPosition == 10) {
                getActiveCall().sendDTMF("#".toCharArray()[0]);
            } else if (padPosition == 11) {
                getActiveCall().sendDTMF("*".toCharArray()[0]);
            }
        }
    }

    private void playDTMFSound(int padPosition) {
        Log.d(TAG, "playDTMFSound: " + padPosition);

        if (mp != null) mp.release();

        mp = MediaPlayer.create(getContext(), sounds[padPosition]);
        mp.setLooping(true);
        mp.start();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        callProgressDialog = new ProgressDialog(getContext());
        topLevelLayout = getActivity().findViewById(R.id.active_call_layout);
        hangupButton = getActivity().findViewById(R.id.activeCallHangupButton);
        hangupButton.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_call_end).color(Color.RED).sizeDp(48));
        activeCallerName = getActivity().findViewById(R.id.activeCallCallerName);
        callDurationTextView = getActivity().findViewById(R.id.callDurationText);

        activeCallerName.setText(getCallerNameString());

        setMenuIcons();
        setClickListeners();
    }

    private String getCallerNameString() {
        return String.format("%s (%s)",
                TextUtils.isEmpty(callerOrCalleeName) ? "-" : callerOrCalleeName,
                TextUtils.isEmpty(callerOrCalleeAddress) ? "-" : callerOrCalleeAddress);
    }

    public void switchCam() {
        List<List<Camera.Size>> supportedVideoSizes = CameraHelper.getSupportedVideoSizes();

        if (supportedVideoSizes != null) {

            final Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.cam_picker);
            dialog.setTitle("Select Cam & Res");

            final Spinner resolutions = dialog.findViewById(R.id.camResolutions);

            populateSpinner(Camera.CameraInfo.CAMERA_FACING_FRONT, resolutions);

            ((RadioButton) dialog.findViewById(R.id.frontCam)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    populateSpinner(Camera.CameraInfo.CAMERA_FACING_FRONT, resolutions);
                }
            });

            ((RadioButton) dialog.findViewById(R.id.backCam)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    populateSpinner(Camera.CameraInfo.CAMERA_FACING_BACK, resolutions);
                }
            });

            dialog.findViewById(R.id.doneButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    RadioButton front = dialog.findViewById(R.id.frontCam);
                    int camPosition = front.isChecked() ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
                    if (getActiveCall() != null) {

                        if (supportedVideoSizes.get(camPosition).size() != 0)
                            getActiveCall().setCaptureDevice(camPosition, supportedVideoSizes.get(camPosition).get(resolutions.getSelectedItemPosition()), null);
                        else
                            getActiveCall().setCaptureDevice(camPosition, null, null);
                    }

                    dialog.dismiss();
                }
            });

            dialog.show();

        } else {
            Toast.makeText(getContext(), "Cannot change the resolution, supported video sizes are not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateSpinner(int camPosition, Spinner resolutions) {
        List<List<Camera.Size>> supportedVideoSizes = CameraHelper.getSupportedVideoSizes();

        List sizes = new ArrayList<>();
        for (Camera.Size size : supportedVideoSizes.get(camPosition))
            sizes.add(size.width + " x " + size.height);

        ArrayAdapter<Camera.Size> adapter = new ArrayAdapter<Camera.Size>(getContext(), android.R.layout.simple_spinner_item, sizes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutions.setAdapter(adapter);
    }

    private void setClickListeners() {
        Log.d(TAG, "setClickListeners");
        hangupButton.setOnClickListener(v -> publishOutgoingCallActionChange(ActiveCallState.HANGUP));
//        callControlButtonGroup.setOnClickListener(v -> hideShowControls());
        topLevelLayout.setOnClickListener(v -> hideShowControls());
    }

    private void setAudioRoute() {
        if (!userSelectedAnAudioMode && getActiveCall() != null && getActiveCall().getMediaAttributes().getLocalVideo()) {
            enableSpeakerMode();
            Log.d(TAG, "setAudioRoute: local video is enabled so enabling speaker mode by default");
        }
    }

    private void enableSpeakerMode() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            Log.d(TAG, "enableSpeakerMode: routing audio to speaker");
            audioManager.setMode(AudioManager.STREAM_MUSIC);
            audioManager.setSpeakerphoneOn(true);
            speakerModeEnabled = true;
            setMenuIcons();
        } else {
            Log.e(TAG, "enableSpeakerMode: Audio manager is not available");
        }
    }

    private void toggleCamera() {
        if (getActiveCall() != null && getActiveCall().getMediaAttributes().getLocalVideo()) {
            Log.d(TAG, "toggleCamera: toggleing");
            int activeCamera = getActiveCall().getActiveCamera();
            if (activeCamera == Camera.CameraInfo.CAMERA_FACING_BACK) {
                activeCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
            } else {
                activeCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
            }

            getActiveCall().setCaptureDevice(activeCamera, null, null);
            setMenuIcons();
        } else {
            Log.e(TAG, "toggleCamera: error");
        }

    }

    private void toggleDTMFView() {
       /* if (keyPadView.getVisibility() == View.INVISIBLE) {
            activeButtonGroup.setVisibility(View.INVISIBLE);
            keyPadView.setVisibility(View.VISIBLE);
        } else {
            keyPadView.setVisibility(View.INVISIBLE);
            hideShowControls();
        }*/
    }

    public void hideShowControls() {
        Log.d(TAG, "hideShowControls: ");
      /*  if (keyPadView.getVisibility() != View.VISIBLE) {
            if (callControlButtonGroup.getVisibility() != View.VISIBLE) {
                callControlButtonGroup.setVisibility(View.VISIBLE);
                activeButtonGroup.setVisibility(View.VISIBLE);
            } else {
                callControlButtonGroup.setVisibility(View.INVISIBLE);
            }
        }*/
    }

    public void setVideoViews() {
        if (getActiveCall() != null) {
            Log.d(TAG, "setVideoViews: setting video video for callId " + getActiveCall().getId());
            VideoView remoteView = getActivity().findViewById(R.id.remoteVideoView);
            VideoView localView = getActivity().findViewById(R.id.localVideoView);

            getActiveCall().setLocalVideoView(localView);
            getActiveCall().setRemoteVideoView(remoteView);
        } else {
            Log.d(TAG, "setVideoViews: Cannot set videoview, active call does not exist");
        }
    }

    public void setMenuIcons() {
        if (getActiveCall() != null) {
            Log.d(TAG, "setMenuIcons: setting it");
        } else {
            Log.e(TAG, "setMenuIcons: Not setting it ");
        }
    }

    @Nullable
    private CallInterface getActiveCall() {
        App app = (App) getActivity().getApplicationContext();
        CallService cPaaSCallService = app.getCpass()
                .getCallService();

        for (CallInterface call : cPaaSCallService.getActiveCalls()) {
            if (call.getId().equals(callId)) {
                Log.d(TAG, "getActiveCall: success " + callId);
                return call;
            }
        }

        Log.e(TAG, "getActiveCall: error " + callId);
        return null;

    }

    public void publishOutgoingCallActionChange(ActiveCallState state) {
        if (mListener != null) {
            mListener.onActiveCallActionChange(state, callId);
            Log.d(TAG, "publishOutgoingCallActionChange: " + state);
        } else {
            Log.e(TAG, "publishOutgoingCallActionChange: listener is null");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnActiveCallActionChangeListener) {
            Log.d(TAG, "onAttach: setting listeners");
            mListener = (OnActiveCallActionChangeListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnActiveCallActionChangeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (durationCounter != null) durationCounter.cancel();
    }

    private void showCallProgressDialog(String message) {
        callProgressDialog.setMessage(message);
        callProgressDialog.setCancelable(false);
        callProgressDialog.show();
    }

    public interface OnActiveCallActionChangeListener {
        void onActiveCallActionChange(ActiveCallState action, String callId);
    }
}
