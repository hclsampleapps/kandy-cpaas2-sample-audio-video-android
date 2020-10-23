package com.hcl.kandy.cpass.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hcl.kandy.cpass.R;
import com.hcl.kandy.cpass.remote.RestApiClient;
import com.hcl.kandy.cpass.remote.RestApiInterface;
import com.hcl.kandy.cpass.remote.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    public static String access_token = "access_token";
    public static String id_token = "id_token";
    public static String base_url = "base_url";
    public static String login_type = "login_type";
    String[] PERMISSIONS = {
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
    };
    int PERMISSION_ALL = 1;
    LinearLayout llPasswordGrant, llClientCredentials;
    boolean isPasswordGrantLoginType;
    private RestApiInterface mRestApiInterface;
    private TextView mEtUserName;
    private TextView mEtUserPassword;
    private TextView mEtClient;
    private EditText mBaseUrl;
    private EditText mClientId, mClientSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!checkPermission()) {
            getPerMission();
        }
        findViewById(R.id.button_login).setOnClickListener(this);
        mEtUserName = findViewById(R.id.et_user_name);
        mEtUserPassword = findViewById(R.id.et_user_password);
        mEtClient = findViewById(R.id.et_user_client);
        mBaseUrl = findViewById(R.id.et_url);
        llPasswordGrant = findViewById(R.id.ll_password_grant);
        llClientCredentials = findViewById(R.id.ll_client_credentials);
        mClientId = findViewById(R.id.et_client_id);
        mClientSecret = findViewById(R.id.et_client_secret);
        isPasswordGrantLoginType = true;
        ((RadioGroup) findViewById(R.id.rg_login_type_selection))
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        if (checkedId == R.id.rb_password_grant) {
                            isPasswordGrantLoginType = true;
                            llPasswordGrant.setVisibility(View.VISIBLE);
                            llClientCredentials.setVisibility(View.GONE);
                        } else {
                            isPasswordGrantLoginType = false;
                            llClientCredentials.setVisibility(View.VISIBLE);
                            llPasswordGrant.setVisibility(View.GONE);
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("cpass", "onstart mainactivity");
    }

    private boolean validate() {
        if (isPasswordGrantLoginType) {
            if (TextUtils.isEmpty(mBaseUrl.getText().toString()))
                return false;
            else if (TextUtils.isEmpty(mEtUserName.getText().toString()))
                return false;
            else if (TextUtils.isEmpty(mEtUserPassword.getText().toString()))
                return false;
            else if (TextUtils.isEmpty(mEtClient.getText().toString()))
                return false;
            else
                return true;
        } else {
            if (TextUtils.isEmpty(mBaseUrl.getText().toString()))
                return false;
            else if (TextUtils.isEmpty(mClientId.getText().toString()))
                return false;
            else if (TextUtils.isEmpty(mClientSecret.getText().toString()))
                return false;
            else
                return true;
        }
    }

    private void OnLoginClick() {

        if (!checkPermission()) {
            getPerMission();
            return;
        }


        if (!validate()) {
            Toast.makeText(LoginActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
            return;

        }

        Retrofit client = RestApiClient.getClient("https://" + mBaseUrl.getText().toString());
        if (client == null) {
            Toast.makeText(LoginActivity.this, "Please enter correct Fields", Toast.LENGTH_SHORT).show();
            return;
        }
        mRestApiInterface = client.create(RestApiInterface.class);
        if (mRestApiInterface == null) {
            Toast.makeText(LoginActivity.this, "Please enter correct Fields", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgressBar("Login..");

        Call<LoginResponse> responseCall;
        if (isPasswordGrantLoginType) {
            responseCall = mRestApiInterface.loginAPI(
                    mEtUserName.getText().toString(),
                    mEtUserPassword.getText().toString(),
                    mEtClient.getText().toString(),
                    "password",
                    "openid");
        } else {
            responseCall = mRestApiInterface.loginAPIProject(
                    mClientId.getText().toString(),
                    mClientSecret.getText().toString(),
                    "client_credentials",
                    "openid");
        }

        responseCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call,
                                   @NonNull Response<LoginResponse> response) {
                LoginResponse body = response.body();

                if (body != null) {
                    Toast.makeText(LoginActivity.this, "Login Success", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.putExtra(access_token, body.getAccessToken());
                    intent.putExtra(id_token, body.getIdToken());
                    intent.putExtra(base_url, mBaseUrl.getText().toString());
                    intent.putExtra(login_type, isPasswordGrantLoginType);

                    if (!isFinishing()) {
                        hideProgressBAr();
                        startActivity(intent);
                        finish();
                    } else {
                        hideProgressBAr();
                        Log.d("HCL", "login failed");
                        Toast.makeText(LoginActivity.this, "Try again..", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    hideProgressBAr();
                    Log.d("HCL", "login failed");
                    Toast.makeText(LoginActivity.this, "Try again..", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                call.cancel();
                if (!isFinishing()) {
                    hideProgressBAr();
                    Log.d("HCL", "login failed");
                    Toast.makeText(LoginActivity.this, "Try again..", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_login:
                OnLoginClick();
                break;
        }
    }

    private boolean checkPermission() {
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(LoginActivity.this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void getPerMission() {

        if (!checkPermission()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_ALL) {
            if (grantResults.length > 0) {
                String permissionsDenied = "";
                for (String per : PERMISSIONS) {
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        permissionsDenied += "\n" + per;
                    }
                }
            }
        }
    }
}
