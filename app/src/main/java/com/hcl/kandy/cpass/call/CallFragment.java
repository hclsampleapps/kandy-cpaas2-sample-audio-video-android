package com.hcl.kandy.cpass.call;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.hcl.kandy.cpass.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class CallFragment extends Fragment {

    private static final String LAST_CALLED_ADDRESS_KEY = "lastCalledAddressKey";
    private static final String DEFAULT_CALLEE_ADDRESS = "";

    EditText participantAddressET;
    FloatingActionButton startCallButton;
    RadioGroup radioButtonGroup;

    public CallFragment() {
        // Required empty public constructor
    }

    public static Fragment newInstance() {
        return new CallFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Prevent input focus to EditText fields behaviour that brings keyboard up
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_call, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        initViews(view);
        initListeners();

        super.onViewCreated(view, savedInstanceState);
    }

    private void initViews(View view) {
        participantAddressET = view.findViewById(R.id.participant_address);
        startCallButton = view.findViewById(R.id.start_call_button);
        radioButtonGroup = view.findViewById(R.id.radio_button_group);

        participantAddressET.setText(getLastCalledAddress());

    }

    private void initListeners() {
        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCall();
            }
        });
    }

    private void startCall() {
        boolean videoChecked;
        boolean doubleMLineChecked;

        switch (radioButtonGroup.getCheckedRadioButtonId()) {
            case R.id.radio_video:
                videoChecked = true;
                doubleMLineChecked = false;
                break;
            case R.id.radio_doublemline:
                videoChecked = false;
                doubleMLineChecked = true;
                break;
            default:
                videoChecked = false;
                doubleMLineChecked = false;
                break;
        }

        Log.d("CallFragment1", "startCall: videoChecked " + videoChecked + " doubleMLineChecked " + doubleMLineChecked);

        String callee = participantAddressET.getText().toString();
        saveLastCalledAddress(callee);

        Bundle bundle = new Bundle();
        bundle.putInt(CallConstants.CALL_INTENT_KEY, CallConstants.CALL_INTENT_OUTGOING_CALL);
        bundle.putBoolean(CallConstants.CALL_PARAMS_CALL_CAN_SEND_VIDEO, videoChecked);
        bundle.putBoolean(CallConstants.CALL_PARAMS_CALL_DOUBLE_M_LINE, doubleMLineChecked);
        bundle.putString(CallConstants.CALL_PARAMS_CALLEE_KEY, parseCalleeAddress(callee));

        startActivity(new Intent(getContext(), CallActivity.class)
                .putExtras(bundle));
    }

    private String parseCalleeAddress(String callee) {
        if (callee.contains("@")) {
            return callee;
        } else {
            String domain = getLoggedInUsername().split("@")[1];
            return TextUtils.isEmpty(domain) ? callee : callee + "@" + domain;
        }
    }

    @NonNull
    private String getLoggedInUsername() {
        return "testUser";
        //return SharedPrefsHelper.getString(SharedPrefsHelper.USERNAME, "");
    }

    private void saveLastCalledAddress(String address) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();

        prefsEditor.putString(LAST_CALLED_ADDRESS_KEY, address);
        prefsEditor.apply();
    }

    @NonNull
    private String getLastCalledAddress() {
        return PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getString(LAST_CALLED_ADDRESS_KEY, DEFAULT_CALLEE_ADDRESS);
    }
}
