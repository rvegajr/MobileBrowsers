package com.noctusoft.webviewbrowser.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.noctusoft.webviewbrowser.R;
import com.noctusoft.webviewbrowser.VariablesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity for managing variables used for form filling.
 */
public class VariableManagerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VariableAdapter adapter;
    private VariablesManager variablesManager;
    private List<String> variableNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_variable_manager);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.variables_title);
        }

        // Initialize variables manager
        variablesManager = VariablesManager.getInstance(this);

        // Set up RecyclerView
        recyclerView = findViewById(R.id.variables_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VariableAdapter();
        recyclerView.setAdapter(adapter);

        // Set up add variable button
        FloatingActionButton addButton = findViewById(R.id.fab_add_variable);
        addButton.setOnClickListener(v -> showAddVariableDialog());

        // Load variables
        loadVariables();
    }

    /**
     * Loads all variables from the VariablesManager.
     */
    private void loadVariables() {
        variableNames = variablesManager.getAllVariableNames();
        adapter.notifyDataSetChanged();
    }

    /**
     * Shows a dialog to add a new variable.
     */
    private void showAddVariableDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_variable, null);
        final EditText nameEditText = dialogView.findViewById(R.id.variable_name_edit_text);
        final EditText valueEditText = dialogView.findViewById(R.id.variable_value_edit_text);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_variable)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = nameEditText.getText().toString().trim();
                    String value = valueEditText.getText().toString().trim();
                    
                    if (!name.isEmpty()) {
                        variablesManager.setValue(name, value);
                        loadVariables();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Shows a dialog to edit an existing variable.
     */
    private void showEditVariableDialog(String name, String value) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_variable, null);
        final EditText nameEditText = dialogView.findViewById(R.id.variable_name_edit_text);
        final EditText valueEditText = dialogView.findViewById(R.id.variable_value_edit_text);
        
        nameEditText.setText(name);
        valueEditText.setText(value);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newName = nameEditText.getText().toString().trim();
                    String newValue = valueEditText.getText().toString().trim();
                    
                    if (!newName.isEmpty()) {
                        // Remove old variable if name changed
                        if (!newName.equals(name)) {
                            variablesManager.removeVariable(name);
                        }
                        
                        // Save new variable
                        variablesManager.setValue(newName, newValue);
                        loadVariables();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Shows a dialog to confirm deleting a variable.
     */
    private void showDeleteVariableDialog(String name) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage("Delete variable '" + name + "'?")
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    variablesManager.removeVariable(name);
                    loadVariables();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * RecyclerView adapter for variables.
     */
    private class VariableAdapter extends RecyclerView.Adapter<VariableAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_variable, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = variableNames.get(position);
            String value = variablesManager.getValue(name);
            
            holder.nameTextView.setText(name);
            holder.valueTextView.setText(value);
            
            holder.editButton.setOnClickListener(v -> showEditVariableDialog(name, value));
            holder.deleteButton.setOnClickListener(v -> showDeleteVariableDialog(name));
        }

        @Override
        public int getItemCount() {
            return variableNames.size();
        }

        /**
         * ViewHolder for variable items.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView;
            TextView valueTextView;
            Button editButton;
            Button deleteButton;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.variable_name);
                valueTextView = itemView.findViewById(R.id.variable_value);
                editButton = itemView.findViewById(R.id.btn_edit);
                deleteButton = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}
