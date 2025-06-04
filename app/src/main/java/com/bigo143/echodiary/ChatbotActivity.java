package com.bigo143.echodiary;

import static com.google.android.material.internal.ViewUtils.hideKeyboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private EditText inputField;
    private Button sendButton;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private static final List<ChatMessage> GREETING_PROMPT = new ArrayList<>();
    static {
    GREETING_PROMPT.add(new ChatMessage("Greet the user with warm tone", false));}
    private final List<ChatMessage> chatMessages = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER) {
                hideKeyboard();
                sendButton.performClick(); // Simulate send button click
                return true;
            }
            return false;
        });



        sendButton.setOnClickListener(v -> {
            String userInput = inputField.getText().toString().trim();
            if (userInput.isEmpty()) return;

            hideKeyboard();
            inputField.setText("");
            addMessage(userInput, true);

            Executors.newSingleThreadExecutor().execute(() -> {
                String response = GeminiApiHelper.chatWithGemini(this, chatMessages);
                runOnUiThread(() -> addMessage(response, false));
            });
        });

        // AI greeting at startup
        Executors.newSingleThreadExecutor().execute(() -> {
        String greeting = GeminiApiHelper.chatWithGemini(this, GREETING_PROMPT);
            runOnUiThread(() -> addMessage(greeting, false));
        });

    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void addMessage(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
}
