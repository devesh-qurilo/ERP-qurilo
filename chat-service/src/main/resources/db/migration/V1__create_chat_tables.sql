-- Drop existing tables if they exist
DROP TABLE IF EXISTS message_deleted_for_users;
DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS chat_rooms;
DROP TABLE IF EXISTS file_attachments;

-- Create file_attachments table
CREATE TABLE file_attachments (
                                  id BIGSERIAL PRIMARY KEY,
                                  file_name VARCHAR(255) NOT NULL,
                                  file_url TEXT NOT NULL,
                                  file_type VARCHAR(100) NOT NULL,
                                  file_size BIGINT,
                                  supabase_path TEXT NOT NULL,
                                  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create chat_rooms table
CREATE TABLE chat_rooms (
                            id VARCHAR(50) PRIMARY KEY,
                            participant1_id VARCHAR(50) NOT NULL,  -- Changed to match entity
                            participant2_id VARCHAR(50) NOT NULL,  -- Changed to match entity
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create chat_messages table
CREATE TABLE chat_messages (
                               id BIGSERIAL PRIMARY KEY,
                               chat_room_id VARCHAR(50) NOT NULL,
                               sender_id VARCHAR(50) NOT NULL,        -- Changed to match entity
                               receiver_id VARCHAR(50) NOT NULL,      -- Changed to match entity
                               content TEXT,
                               message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
                               file_attachment_id BIGINT,
                               status VARCHAR(20) NOT NULL DEFAULT 'SENT',
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id),
                               CONSTRAINT fk_file_attachment FOREIGN KEY (file_attachment_id) REFERENCES file_attachments(id)
);

-- Create message_deleted_for_users table (for soft delete)
CREATE TABLE message_deleted_for_users (
                                           message_id BIGINT NOT NULL,
                                           user_id VARCHAR(50) NOT NULL,          -- Changed to VARCHAR for String employeeId
                                           PRIMARY KEY (message_id, user_id),
                                           CONSTRAINT fk_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_chat_messages_chat_room_id ON chat_messages(chat_room_id);
CREATE INDEX idx_chat_messages_sender_id ON chat_messages(sender_id);
CREATE INDEX idx_chat_messages_receiver_id ON chat_messages(receiver_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);
CREATE INDEX idx_chat_rooms_participant1 ON chat_rooms(participant1_id);
CREATE INDEX idx_chat_rooms_participant2 ON chat_rooms(participant2_id);