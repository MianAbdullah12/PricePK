package com.example.pricepk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MessagesFragment extends Fragment {

    private EditText messageInput;
    private Button sendButton;
    private TextView messageDisplay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        messageDisplay = view.findViewById(R.id.message_display);

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageToAllUsers(message);
                messageInput.setText("");
            }
        });

        // Fetch and display broadcast messages
        fetchBroadcastMessages();

        return view;
    }

    private void sendMessageToAllUsers(String message) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages").push();
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sender", "admin");
        messageData.put("timestamp", System.currentTimeMillis());
        messagesRef.setValue(messageData, (error, ref) -> {
            if (error == null) {
                Toast.makeText(getContext(), "Message sent successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Error sending message: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchBroadcastMessages() {
        DatabaseReference broadcastRef = FirebaseDatabase.getInstance().getReference("broadcast_messages");
        broadcastRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder messages = new StringBuilder();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.getValue(String.class);
                    if (message != null) {
                        messages.append(message).append("\n");
                    }
                }
                messageDisplay.setText(messages.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }
}