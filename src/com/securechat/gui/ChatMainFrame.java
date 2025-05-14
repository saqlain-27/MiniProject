package com.securechat.gui;

import com.securechat.dao.MessageDAO;
import com.securechat.dao.UserDAO;
import com.securechat.model.Message;
import com.securechat.model.User;
import com.securechat.util.SecurityUtil;
import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ChatMainFrame extends JFrame {
    private User currentUser;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private DefaultListModel<String> chatModel;
    private JList<String> chatList;
    private JTextArea messageArea;
    private JButton sendButton;
    private UserDAO userDAO;
    private MessageDAO messageDAO;
    private SecretKey secretKey;
    
    public ChatMainFrame(User user) {
        this.currentUser = user;
        userDAO = new UserDAO();
        messageDAO = new MessageDAO();
        
        try {
            secretKey = SecurityUtil.getStaticKey();
            initializeUI();
            loadUserList();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error initializing encryption: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void initializeUI() {
        setTitle("Secure Chat - " + currentUser.getUsername());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // User list on the left
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFixedCellWidth(150);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadChatWithSelectedUser();
            }
        });
        
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Users"));
        
        // Chat area in the center
        chatModel = new DefaultListModel<>();
        chatList = new JList<>(chatModel);
        chatList.setCellRenderer(new ChatCellRenderer());
        
        JScrollPane chatScrollPane = new JScrollPane(chatList);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Chat"));
        
        // Message input area at the bottom
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageArea = new JTextArea(3, 20);
        messageArea.setLineWrap(true);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        
        messagePanel.add(messageScrollPane, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        
        // Add components to main panel
        mainPanel.add(userScrollPane, BorderLayout.WEST);
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(messagePanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private void loadUserList() {
        // In a real app, you would fetch this from the database
        // For simplicity, we'll add some dummy users
        userListModel.clear();
        
        // Add all users except current user
        // In a real app, you would query the database
        userListModel.addElement("saq");
        userListModel.addElement("saqq");
        userListModel.addElement("user2");
    }
    
    private void loadChatWithSelectedUser() {
        String selectedUsername = userList.getSelectedValue();
        if (selectedUsername == null) return;
        
        User selectedUser = userDAO.getUserByUsername(selectedUsername);
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "User not found", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        chatModel.clear();
        List<Message> messages = messageDAO.getConversation(
            currentUser.getUserId(), selectedUser.getUserId());
        
        for (Message msg : messages) {
            try {
                String decrypted = SecurityUtil.decrypt(msg.getEncryptedMessage(), secretKey);
                String displayText = String.format("%s: %s", 
                    msg.getSenderId() == currentUser.getUserId() ? "You" : selectedUsername, 
                    decrypted);
                chatModel.addElement(displayText);
            } catch (Exception e) {
                System.err.println("Error decrypting message: " + e.getMessage());
                chatModel.addElement("[Error decrypting message]");
            }
        }
    }
    
    private void sendMessage() {
        String selectedUsername = userList.getSelectedValue();
        if (selectedUsername == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to chat with", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String messageText = messageArea.getText().trim();
        if (messageText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a message", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        User receiver = userDAO.getUserByUsername(selectedUsername);
        if (receiver == null) {
            JOptionPane.showMessageDialog(this, "User not found", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Encrypt the message
            String encrypted = SecurityUtil.encrypt(messageText, secretKey);
            
            // For demo purposes, we'll just split to get IV and encrypted message
            // In real app, you might want to store IV separately
            Message message = new Message(
                currentUser.getUserId(), 
                receiver.getUserId(), 
                messageText, 
                encrypted, 
                "iv_placeholder" // In real app, store actual IV
            );
            
            if (messageDAO.saveMessage(message)) {
                chatModel.addElement("You: " + messageText);
                messageArea.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to send message", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error encrypting message: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Custom cell renderer for chat messages
    private class ChatCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
            
            String text = (String) value;
            if (text.startsWith("You:")) {
                label.setHorizontalAlignment(JLabel.RIGHT);
                label.setForeground(Color.BLUE);
            } else {
                label.setHorizontalAlignment(JLabel.LEFT);
                label.setForeground(Color.BLACK);
            }
            
            return label;
        }
    }
}