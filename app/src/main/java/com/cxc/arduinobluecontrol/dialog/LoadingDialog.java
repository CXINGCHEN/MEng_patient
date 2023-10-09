package com.cxc.arduinobluecontrol.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialogFragment;


import com.specknet.pdiotapp.R;


public class LoadingDialog extends AppCompatDialogFragment {
    public static final String DEVICE_NAME_KEY = "device_name";
    public static final String TAG = "LoadingDialog";
    private TextView mConnectText;

    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View inflate = layoutInflater.inflate(R.layout.connect_loading_view, viewGroup, false);
        this.mConnectText = (TextView) inflate.findViewById(R.id.loading_text);
        this.mConnectText.setText("Connecting to " + getArguments().getString(DEVICE_NAME_KEY));
        return inflate;
    }

    @Override // androidx.fragment.app.DialogFragment, android.content.DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
    }

    @Override // androidx.fragment.app.DialogFragment, androidx.fragment.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
    }
}
