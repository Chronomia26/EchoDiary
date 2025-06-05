// ChatMessage.java
package com.bigo143.echodiary;

public class ChatMessage {
    public final String message;
    public final boolean isUser;

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }
}
