package com.hcl.kandy.cpass.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import com.hcl.kandy.cpass.R;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    ProgressDialog loading = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loading = new ProgressDialog(this);
        loading.setCancelable(false);
        loading.setMessage(this.getString(R.string.loading));
        loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }

    public void hideProgressBAr() {
        loading.dismiss();
    }

    public void showProgressBar(String message) {
        if (!TextUtils.isEmpty(message))
            loading.setMessage(message);
        loading.show();
    }

}
