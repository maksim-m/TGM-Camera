package me.maxdev.tgmcamera.util;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import me.maxdev.tgmcamera.R;

/**
 * Created by Max on 02.05.2016.
 */
public class PermissionsDeniedDialogFragment extends DialogFragment {

    public static final String TAG = "PermissionsDeniedDialogFragment";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.permissions_denied_dialog_title)
                .setMessage(R.string.permissions_denied_dialog_message)
                .setPositiveButton(R.string.permissions_denied_dialog_positive_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getActivity().finish();
                            }
                        })
                .create();

    }
}
