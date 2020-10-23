package com.hcl.kandy.cpass.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;

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

}
