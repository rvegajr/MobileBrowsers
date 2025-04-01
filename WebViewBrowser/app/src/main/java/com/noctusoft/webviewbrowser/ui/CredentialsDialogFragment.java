package com.noctusoft.webviewbrowser.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.noctusoft.webviewbrowser.CredentialsManager;
import com.noctusoft.webviewbrowser.R;
import com.noctusoft.webviewbrowser.model.Credentials;

/**
 * Dialog fragment for managing website credentials.
 */
public class CredentialsDialogFragment extends DialogFragment {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText domainEditText;
    private CredentialsManager credentialsManager;
    private Callback callback;

    /**
     * Interface for dialog callbacks.
     */
    public interface Callback {
        void onCredentialsSaved(String username, String password);
        void onCredentialsDeleted();
    }

    /**
     * Creates a new instance of the dialog.
     *
     * @param domain The domain to pre-fill
     * @return A new instance of the dialog
     */
    public static CredentialsDialogFragment newInstance(String domain) {
        CredentialsDialogFragment fragment = new CredentialsDialogFragment();
        Bundle args = new Bundle();
        args.putString("domain", domain);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            callback = (Callback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement Callback");
        }
        credentialsManager = CredentialsManager.getInstance(context);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_credentials, null);
        
        usernameEditText = view.findViewById(R.id.username_edit_text);
        passwordEditText = view.findViewById(R.id.password_edit_text);
        domainEditText = view.findViewById(R.id.domain_edit_text);
        Button saveButton = view.findViewById(R.id.btn_save_credentials);
        Button clearButton = view.findViewById(R.id.btn_clear_credentials);
        
        // Set domain from arguments
        String domain = "";
        if (getArguments() != null) {
            domain = getArguments().getString("domain", "");
            domainEditText.setText(domain);
        }
        
        // Load existing credentials if available
        loadCredentials(domain);
        
        // Set button click listeners
        saveButton.setOnClickListener(v -> saveCredentials());
        clearButton.setOnClickListener(v -> clearCredentials());
        
        builder.setTitle(R.string.credentials_title)
               .setView(view)
               .setNegativeButton(R.string.cancel, (dialog, id) -> dismiss());
        
        return builder.create();
    }
    
    /**
     * Loads saved credentials for the given domain.
     *
     * @param domain The domain to load credentials for
     */
    private void loadCredentials(String domain) {
        if (domain == null || domain.isEmpty()) {
            return;
        }
        
        Credentials credentials = credentialsManager.loadCredentials(domain);
        if (credentials != null) {
            usernameEditText.setText(credentials.getUsername());
            passwordEditText.setText(credentials.getPassword());
        }
    }
    
    /**
     * Saves credentials entered in the dialog.
     */
    private void saveCredentials() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String domain = domainEditText.getText().toString().trim();
        
        if (domain.isEmpty()) {
            Toast.makeText(getContext(), "Domain is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (credentialsManager.saveCredentials(username, password, domain)) {
            Toast.makeText(getContext(), R.string.credentials_saved, Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onCredentialsSaved(username, password);
            }
            dismiss();
        } else {
            Toast.makeText(getContext(), R.string.credentials_error, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Clears saved credentials for the current domain.
     */
    private void clearCredentials() {
        String domain = domainEditText.getText().toString().trim();
        
        if (domain.isEmpty()) {
            Toast.makeText(getContext(), "Domain is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (credentialsManager.deleteCredentials(domain)) {
            Toast.makeText(getContext(), R.string.credentials_deleted, Toast.LENGTH_SHORT).show();
            usernameEditText.setText("");
            passwordEditText.setText("");
            if (callback != null) {
                callback.onCredentialsDeleted();
            }
        } else {
            Toast.makeText(getContext(), R.string.credentials_error, Toast.LENGTH_SHORT).show();
        }
    }
}
