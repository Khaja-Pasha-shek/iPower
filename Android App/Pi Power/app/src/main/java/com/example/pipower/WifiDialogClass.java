package com.example.pipower;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.textfield.TextInputLayout;

public class WifiDialogClass extends AppCompatDialogFragment {

    TextInputLayout ssidEditText, passwordEditText;
    private DialogInfoInterface listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.wifi_dialog, null);

        ssidEditText = view.findViewById(R.id.ssidText);
        passwordEditText = view.findViewById(R.id.passText);

        builder.setView(view)
                .setTitle("WiFi")
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                })
                .setPositiveButton("submit", (dialogInterface, i) -> {

                });
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ssidEditText.getEditText().getText().toString().isEmpty()) {
                    Log.d("SSID", "onClick: is empty");
                    ssidEditText.requestFocus();
                    ssidEditText.setError("Required");
                }
                if (passwordEditText.getEditText().getText().length() < 8) {
                    Log.d("Pass length: ", "Password length is lessthan 8 chars");
                    passwordEditText.setError("<8 chars");
                } else {
                    String ssid = ssidEditText.getEditText().getText().toString();
                    String pass = passwordEditText.getEditText().getText().toString();
                    listener.getInformation(ssid, pass);
                    dialog.dismiss();
                }
            }
        });
        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (DialogInfoInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "Must Implement DialogInfoInterface Instance");
        }

    }

    public interface DialogInfoInterface {
        void getInformation(String ssid, String pass);
    }
}
