package com.securechat.dao;

import com.securechat.model.Message;
import com.securechat.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    public boolean saveMessage(Message message) {
        String sql = "INSERT INTO messages(sender_id, receiver_id, message_text, encrypted_message, iv) "
                   + "VALUES(?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, message.getSenderId());
            pstmt.setInt(2, message.getReceiverId());
            pstmt.setString(3, message.getMessageText());
            pstmt.setString(4, message.getEncryptedMessage());
            pstmt.setString(5, message.getIv());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
            return false;
        }
    }
    
    public List<Message> getConversation(int user1Id, int user2Id) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) "
                   + "OR (sender_id = ? AND receiver_id = ?) ORDER BY sent_at ASC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, user1Id);
            pstmt.setInt(2, user2Id);
            pstmt.setInt(3, user2Id);
            pstmt.setInt(4, user1Id);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Message message = new Message();
                message.setMessageId(rs.getInt("message_id"));
                message.setSenderId(rs.getInt("sender_id"));
                message.setReceiverId(rs.getInt("receiver_id"));
                message.setMessageText(rs.getString("message_text"));
                message.setEncryptedMessage(rs.getString("encrypted_message"));
                message.setIv(rs.getString("iv"));
                message.setSentAt(rs.getTimestamp("sent_at"));
                messages.add(message);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving messages: " + e.getMessage());
        }
        return messages;
    }
}
