package com.example.pricepk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class OffersFragment extends Fragment {

    private EditText offerText;
    private Button offerButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offers, container, false);

        offerText = view.findViewById(R.id.offer_text);
        offerButton = view.findViewById(R.id.offer_button);

        offerButton.setOnClickListener(v -> {
            String offer = offerText.getText().toString().trim();
            if (!offer.isEmpty()) {
                Toast.makeText(getContext(), "Offer noted: " + offer, Toast.LENGTH_SHORT).show();
                offerText.setText("");
            } else {
                Toast.makeText(getContext(), "Please enter an offer", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}