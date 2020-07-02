package com.hcl.kandy.cpass.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;

import com.hcl.kandy.cpass.R;

public class BaseFragment extends Fragment {
    ProgressDialog loading = null;

    public void showMessage(View mEt, String message) {
        Snackbar.make(mEt, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loading = new ProgressDialog(getContext());
        loading.setCancelable(false);
        loading.setMessage(this.getString(R.string.loading));
        loading.setProgressStyle(ProgressDialog.STYLE_SPINNER);

    }


    public void hideProgressBAr() {
        if (loading != null && loading.isShowing())
            loading.dismiss();
    }

    public void showProgressBar(String message) {
        if (!TextUtils.isEmpty(message))
            loading.setMessage(message);
        loading.show();
    }
}
