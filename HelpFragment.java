package com.example.pricepk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class HelpFragment extends Fragment {

    private EditText helpText;
    private Button helpButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);

        helpText = view.findViewById(R.id.help_text);
        helpButton = view.findViewById(R.id.help_button);

        helpButton.setOnClickListener(v -> {
            String helpQuery = helpText.getText().toString().trim();
            if (!helpQuery.isEmpty()) {
                Toast.makeText(getContext(), "Help query submitted: " + helpQuery, Toast.LENGTH_SHORT).show();
                helpText.setText("");
            } else {
                Toast.makeText(getContext(), "Please enter your query", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}