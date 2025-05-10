package com.example.pricepk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class ReportFragment extends Fragment {

    private EditText reportText;
    private Button reportButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        reportText = view.findViewById(R.id.report_text);
        reportButton = view.findViewById(R.id.report_button);

        reportButton.setOnClickListener(v -> {
            String report = reportText.getText().toString().trim();
            if (!report.isEmpty()) {
                Toast.makeText(getContext(), "Report submitted: " + report, Toast.LENGTH_SHORT).show();
                reportText.setText("");
            } else {
                Toast.makeText(getContext(), "Please enter a report", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}