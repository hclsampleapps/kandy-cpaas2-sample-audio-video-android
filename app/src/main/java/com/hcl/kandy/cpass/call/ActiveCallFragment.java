package com.hcl.kandy.cpass.call;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hcl.kandy.cpass.App;
import com.hcl.kandy.cpass.R;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.rbbn.cpaas.mobile.call.api.CallInterface;
import com.rbbn.cpaas.mobile.call.api.CallService;
import com.rbbn.cpaas.mobile.call.api.CallState;
import com.rbbn.cpaas.mobile.call.api.MediaAttributes;
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
    private ImageButton muteButton, videoOnOffButton, holdButton, toggleCamera, keypadButton, makeOtherEventsVisible, makePreviousEventsVisible, cameraOptionsButton, audioDeviceToggle;
    private TextView callDurationTextView, activeCallerName, muteButtonLabel, holdButtonLabel, audioDeviceToggleLabel;
    private View activeButtonGroup, callControlButtonGroup, topLevelLayout;
    private OnActiveCallActionChangeListener mListener;
    private ProgressDialog callProgressDialog;

    // Dialpad views
    private Button closeKeypadButton;
    private TableLayout keyPadView;
    private EditText dialPadEditText;
    private MediaPlayer mp;
    private int[] sounds = {R.raw.dtmf0, R.raw.dtmf1, R.raw.dtmf2, R.raw.dtmf3, R.raw.dtmf4, R.raw.dtmf5, R.raw.dtmf6, R.raw.dtmf7, R.raw.dtmf8, R.raw.dtmf9, R.raw.dtmfroot, R.raw.dtmfstar};
    private Button buttonDTMF0, buttonDTMF1, buttonDTMF2, buttonDTMF3, buttonDTMF4, buttonDTMF5, buttonDTMF6, buttonDTMF7, buttonDTMF8, buttonDTMF9, buttonDTMFRroot, buttonDTMFSstar;

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

        if (padPosition == 10) dialPadEditText.append("#");
        else if (padPosition == 11) dialPadEditText.append("*");
        else dialPadEditText.append("" + padPosition);
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
        View secondButtonGroupLayout = getActivity().findViewById(R.id.second_event_group);
        // When the activity first created default call buttons should shown and second group must be invisible
        secondButtonGroupLayout.setVisibility(View.INVISIBLE);
        keyPadView = getActivity().findViewById(R.id.keyPadView);
        keyPadView.setVisibility(View.INVISIBLE);
        activeButtonGroup = getActivity().findViewById(R.id.first_event_group);
        muteButton = getActivity().findViewById(R.id.muteButton);
        muteButtonLabel = getActivity().findViewById(R.id.muteButtonLabel);
        videoOnOffButton = getActivity().findViewById(R.id.cameraToggleButton);
        toggleCamera = getActivity().findViewById(R.id.rotateCameraButton);
        holdButton = getActivity().findViewById(R.id.holdButton);
        holdButtonLabel = getActivity().findViewById(R.id.holdButtonLabel);
        keypadButton = getActivity().findViewById(R.id.keypadButton);
        makeOtherEventsVisible = getActivity().findViewById(R.id.makeOtherEventsVisible);
        makePreviousEventsVisible = getActivity().findViewById(R.id.makePreviousEventsVisible);
        callControlButtonGroup = getActivity().findViewById(R.id.call_control_view);
        closeKeypadButton = getActivity().findViewById(R.id.closeDialpad);
        dialPadEditText = getActivity().findViewById(R.id.dialPadTextView);
        cameraOptionsButton = getActivity().findViewById(R.id.cameraOptions);
        audioDeviceToggle = getActivity().findViewById(R.id.audioDeviceToggle);
        audioDeviceToggleLabel = getActivity().findViewById(R.id.audioDeviceToggleLabel);

        //DTMF buttons
        buttonDTMF0 = getActivity().findViewById(R.id.pad0);
        buttonDTMF1 = getActivity().findViewById(R.id.pad1);
        buttonDTMF2 = getActivity().findViewById(R.id.pad2);
        buttonDTMF3 = getActivity().findViewById(R.id.pad3);
        buttonDTMF4 = getActivity().findViewById(R.id.pad4);
        buttonDTMF5 = getActivity().findViewById(R.id.pad5);
        buttonDTMF6 = getActivity().findViewById(R.id.pad6);
        buttonDTMF7 = getActivity().findViewById(R.id.pad7);
        buttonDTMF8 = getActivity().findViewById(R.id.pad8);
        buttonDTMF9 = getActivity().findViewById(R.id.pad9);
        buttonDTMFRroot = getActivity().findViewById(R.id.padRoot);
        buttonDTMFSstar = getActivity().findViewById(R.id.padStar);

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
        activeButtonGroup.setOnClickListener(v -> setMenuIcons());
        muteButton.setOnClickListener(v -> toogleMute());
        videoOnOffButton.setOnClickListener(v -> toggleVideo());

        holdButton.setOnClickListener(v -> toggleHold());
        keypadButton.setOnClickListener(v -> toggleDTMFView());
        closeKeypadButton.setOnClickListener(v -> toggleDTMFView());
        toggleCamera.setOnClickListener(v -> toggleCamera());
        cameraOptionsButton.setOnClickListener(v -> switchCam());
        audioDeviceToggle.setOnClickListener(v -> toggleAudioDevice());
        makeOtherEventsVisible.setOnClickListener(v -> changeVisibilityOfButtonGroups());
        makePreviousEventsVisible.setOnClickListener(v -> changeVisibilityOfButtonGroups());

        setDtmfTouchListeners();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setDtmfTouchListeners() {
        buttonDTMF0.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(0);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(0);
                }
                return false;
            }

        });
        buttonDTMF1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(1);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(1);
                }
                return false;
            }

        });
        buttonDTMF2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(2);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(2);
                }
                return false;
            }

        });
        buttonDTMF3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(3);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(3);
                }
                return false;
            }

        });
        buttonDTMF4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(4);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(3);
                }
                return false;
            }

        });
        buttonDTMF5.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(5);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(5);
                }
                return false;
            }

        });
        buttonDTMF6.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(6);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(6);
                }
                return false;
            }

        });

        buttonDTMF7.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(7);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(7);
                }
                return false;
            }

        });
        buttonDTMF8.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(8);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(8);
                }
                return false;
            }

        });
        buttonDTMF9.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(9);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(9);
                }
                return false;
            }

        });
        buttonDTMFRroot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(10);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(10);
                }
                return false;
            }
        });
        buttonDTMFSstar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playDTMFSound(11);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopDTMFSoundAndSendDtmf(11);
                }
                return false;
            }
        });
    }

    private void setAudioRoute() {
        if (!userSelectedAnAudioMode && getActiveCall() != null && getActiveCall().getMediaAttributes().getLocalVideo()) {
            enableSpeakerMode();
            Log.d(TAG, "setAudioRoute: local video is enabled so enabling speaker mode by default");
        }
    }

    private void toggleAudioDevice() {
        if (speakerModeEnabled) {
            enableEarpieceMode();
            Log.d(TAG, "toggleAudioDevice: switching from speaker to earpiece");
            Toast.makeText(getContext(), "switching from speaker to earpiece", Toast.LENGTH_SHORT).show();
        } else {
            enableSpeakerMode();
            Log.d(TAG, "toggleAudioDevice: switching from earpiece to speaker");
            Toast.makeText(getContext(), "switching from earpiece to speaker", Toast.LENGTH_SHORT).show();
        }
        userSelectedAnAudioMode = true;
    }

    private void enableEarpieceMode() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            Log.d(TAG, "enableEarpiece: routing audio to earpiece");
            audioManager.setMode(AudioManager.STREAM_VOICE_CALL);
            audioManager.setSpeakerphoneOn(false);
            speakerModeEnabled = false;
            setMenuIcons();
        } else {
            Log.e(TAG, "enableEarpiece: Audio manager is not available");
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
            MediaAttributes state = getActiveCall().getMediaAttributes();

            muteButton.setImageDrawable(new IconicsDrawable(getContext()).icon(getActiveCall().isMute()
                    ? GoogleMaterial.Icon.gmd_mic : GoogleMaterial.Icon.gmd_mic_off).color(iconColor).sizeDp(iconSize));
            muteButtonLabel.setText(getActiveCall().isMute() ? "Unmute" : "Mute");
            videoOnOffButton.setImageDrawable(new IconicsDrawable(getContext()).icon(state.getLocalVideo()
                    ? GoogleMaterial.Icon.gmd_videocam_off : GoogleMaterial.Icon.gmd_videocam).color(iconColor).sizeDp(iconSize));
            toggleCamera.setImageDrawable(new IconicsDrawable(getContext()).icon(getActiveCall().getActiveCamera() == Camera.CameraInfo.CAMERA_FACING_BACK
                    ? GoogleMaterial.Icon.gmd_camera_rear : GoogleMaterial.Icon.gmd_camera_front).color(iconColor).sizeDp(iconSize));
            keypadButton.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_dialpad).color(iconColor).sizeDp(iconSize));
            makeOtherEventsVisible.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_loop).color(iconColor).sizeDp(iconSize));
            cameraOptionsButton.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_camera_roll).color(iconColor).sizeDp(iconSize));
            audioDeviceToggle.setImageDrawable(new IconicsDrawable(getContext()).icon(speakerModeEnabled
                    ? GoogleMaterial.Icon.gmd_speaker : GoogleMaterial.Icon.gmd_hearing).color(iconColor).sizeDp(iconSize));
            audioDeviceToggleLabel.setText(speakerModeEnabled ? "Speaker" : "Earpiece");
            makePreviousEventsVisible.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_loop).color(iconColor).sizeDp(iconSize));

            if ((getActiveCall().getCallState().getType() == CallState.Type.ON_HOLD
                    || getActiveCall().getCallState().getType() == CallState.Type.ON_DOUBLE_HOLD)) {
                holdButton.setImageDrawable(
                        new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_play_arrow).color(iconColor).sizeDp(iconSize));
                holdButtonLabel.setText("Unhold");
            } else if (getActiveCall() != null) {
                holdButton.setImageDrawable(
                        new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_pause).color(iconColor).sizeDp(iconSize));
                holdButtonLabel.setText("Hold");
            }
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

    public void changeVisibilityOfButtonGroups() {
        View defaultButtonGroup = getActivity().findViewById(R.id.first_event_group);
        View secondButtonGroup = getActivity().findViewById(R.id.second_event_group);
        if (defaultButtonGroup.getVisibility() == View.VISIBLE) {
            defaultButtonGroup.setVisibility(View.INVISIBLE);
            secondButtonGroup.setVisibility(View.VISIBLE);
            activeButtonGroup = secondButtonGroup;
        } else {
            defaultButtonGroup.setVisibility(View.VISIBLE);
            secondButtonGroup.setVisibility(View.INVISIBLE);
            activeButtonGroup = defaultButtonGroup;
        }
    }

    public void toogleMute() {
        if (getActiveCall() != null && getActiveCall().isMute()) {
            Log.d(TAG, "toogleMute: unmuting");
            getActiveCall().unMute();
            setMenuIcons();
        } else if (getActiveCall() != null) {
            Log.d(TAG, "toogleMute: muting");
            getActiveCall().mute();
            setMenuIcons();
        }
    }

    public void toggleVideo() {
        if (getActiveCall() != null) {
            if (getActiveCall().getMediaAttributes().getLocalVideo()) {
                Log.d(TAG, "toggleVideo: stopping");
                getActiveCall().videoStop();
            } else {
                Log.d(TAG, "toggleVideo: starting");
                getActiveCall().videoStart();
            }
        } else {
            Toast.makeText(getContext(), "Call cannot send video", Toast.LENGTH_SHORT).show();
        }
    }


    public void toggleHold() {
        if (getActiveCall() != null) {
            if (getActiveCall().getCallState().getType() == CallState.Type.ON_DOUBLE_HOLD ||
                    getActiveCall().getCallState().getType() == CallState.Type.ON_HOLD) {
                Log.d(TAG, "togglehold: unholding");
                getActiveCall().unHoldCall();
                showCallProgressDialog("Unholding...");
            } else {
                Log.d(TAG, "togglehold: holding");
                getActiveCall().holdCall();
                showCallProgressDialog("Holding...");
            }
        }

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
