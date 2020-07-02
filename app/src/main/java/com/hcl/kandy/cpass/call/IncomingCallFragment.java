package com.hcl.kandy.cpass.call;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hcl.kandy.cpass.R;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.rbbn.cpaas.mobile.call.api.CallState;

import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALLER_KEY;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALLER_NAME_KEY;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_CAN_SEND_VIDEO;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_DOUBLE_M_LINE;
import static com.hcl.kandy.cpass.call.CallConstants.CALL_PARAMS_CALL_ID_KEY;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnIncomingCallActionChangeListener} interface
 * to handle interaction events.
 */
public class IncomingCallFragment extends Fragment implements CPaaSCallFragment {
    private final String TAG = "IncomingCallFragment";
    private TextView callerView;
    private ImageButton acceptWithVideoButton;
    private ImageButton rejectButton;
    private ImageButton acceptAudioOnlyButton;

    private MediaPlayer mediaPlayer;

    private boolean callCanSendVideo;
    private boolean isDoubleMLine;
    private String callerAddress;
    private String callerName;
    private String callId;

    private OnIncomingCallActionChangeListener mListener;

    public IncomingCallFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            callId = bundle.getString(CALL_PARAMS_CALL_ID_KEY);
            callCanSendVideo = bundle.getBoolean(CALL_PARAMS_CALL_CAN_SEND_VIDEO);
            isDoubleMLine = bundle.getBoolean(CALL_PARAMS_CALL_DOUBLE_M_LINE);
            callerAddress = bundle.getString(CALL_PARAMS_CALLER_KEY);
            callerName = bundle.getString(CALL_PARAMS_CALLER_NAME_KEY);
            Log.d(TAG, "onCreate: Received incoming call: " + bundle.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startPlayRingtone();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPlayRingtone();
    }

    private void startPlayRingtone() {
        mediaPlayer = MediaPlayer.create(getContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        mediaPlayer.start();
        mediaPlayer.setLooping(true);
    }

    private void stopPlayRingtone() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_incoming_call, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        callerView = getView().findViewById(R.id.incomingcallCallerName);
        acceptWithVideoButton = getView().findViewById(R.id.incomingcallAcceptWithVideoButton);

        if (!callCanSendVideo && !isDoubleMLine) {
            acceptWithVideoButton.setVisibility(View.GONE);
        }

        acceptAudioOnlyButton = getView().findViewById(R.id.incomingcallAcceptOnlyAudioButton);
        rejectButton = getView().findViewById(R.id.incomingcallRejectButton);

        int size = 48;
        acceptWithVideoButton.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_video_call).color(Color.BLUE).sizeDp(size));
        rejectButton.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_call_end).color(Color.RED).sizeDp(size));
        acceptAudioOnlyButton.setImageDrawable(new IconicsDrawable(getContext()).icon(GoogleMaterial.Icon.gmd_call).color(Color.parseColor("#4CAF50")).sizeDp(size));

        callerView.setText(getCallerNameString());
        setClickListeners();
    }

    private String getCallerNameString() {
        return String.format("%s (%s)", callerName, callerAddress);
    }

    public void publishIncomingCallActionChange(IncomingCallState action) {
        if (mListener != null) {
            mListener.onIncomingCallActionChange(action, callId);
            Log.d(TAG, "publishIncomingCallActionChange: " + action);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnIncomingCallActionChangeListener) {
            mListener = (OnIncomingCallActionChangeListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnIncomingCallActionChangeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void setClickListeners() {
        acceptWithVideoButton.setOnClickListener(v -> acceptCallWithVideo());
        acceptAudioOnlyButton.setOnClickListener(v -> acceptCallAudioOnly());
        rejectButton.setOnClickListener(v -> rejectIncomingCall());
    }

    private void acceptCallWithVideo() {
        publishIncomingCallActionChange(IncomingCallState.ACCEPT_VIDEO);
    }

    private void acceptCallAudioOnly() {
        publishIncomingCallActionChange(IncomingCallState.ACCEPT_AUDIO);
    }

    private void rejectIncomingCall() {
        publishIncomingCallActionChange(IncomingCallState.REJECT);
    }

    private void ignoreCall() {
        publishIncomingCallActionChange(IncomingCallState.IGNORE);
    }

    @Override
    public void callStateChanged(CallState.Type state) {

    }

    @Override
    public void callOperationPerformed(String callOperation) {

    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnIncomingCallActionChangeListener {
        void onIncomingCallActionChange(IncomingCallState action, String callId);
    }


}
