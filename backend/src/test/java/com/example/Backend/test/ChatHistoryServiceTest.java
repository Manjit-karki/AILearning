package com.example.Backend.test;

import com.example.Backend.model.ChatHistory;
import com.example.Backend.repository.ChatHistoryRepository;
import com.example.Backend.services.ChatHistoryService;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private ChatHistoryRepository chatHistoryRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ChatHistoryService chatHistoryService;

    private String userId;
    private String documentId;
    private ChatHistory mockChatHistory;

    @BeforeEach
    void setUp() {
        userId = "user123";
        documentId = "doc456";
        mockChatHistory = ChatHistory.builder()
                .userId(userId)
                .documentId(documentId)
                .build();
    }

    @Nested
    @DisplayName("Standard CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("createChatHistory - Should build and save new ChatHistory")
        void createChatHistory_ShouldSaveAndReturn() {
            when(chatHistoryRepository.save(any(ChatHistory.class))).thenReturn(mockChatHistory);

            ChatHistory result = chatHistoryService.createChatHistory(userId, documentId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getDocumentId()).isEqualTo(documentId);
            verify(chatHistoryRepository).save(any(ChatHistory.class));
        }

        @Test
        @DisplayName("getByUserAndDocument - Should return ChatHistory when present")
        void getByUserAndDocument_WhenExists_ShouldReturnHistory() {
            when(chatHistoryRepository.findByUserIdAndDocumentId(userId, documentId))
                    .thenReturn(Optional.of(mockChatHistory));

            ChatHistory result = chatHistoryService.getByUserAndDocument(userId, documentId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("getByUserAndDocument - Should throw exception when missing")
        void getByUserAndDocument_WhenNotFound_ShouldThrowException() {
            when(chatHistoryRepository.findByUserIdAndDocumentId(userId, documentId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatHistoryService.getByUserAndDocument(userId, documentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Chat history not found");
        }

        @Test
        @DisplayName("delete - Should delegate deletion to repository")
        void delete_ShouldCallRepositoryDelete() {
            chatHistoryService.delete(userId, documentId);

            verify(chatHistoryRepository).deleteByUserIdAndDocumentId(userId, documentId);
        }
    }

    @Nested
    @DisplayName("GetOrCreate Flow")
    class GetOrCreateOperations {

        @Test
        @DisplayName("getOrCreate - Should return existing entry when found without saving")
        void getOrCreate_WhenExists_ShouldReturnExisting() {
            when(chatHistoryRepository.findByUserIdAndDocumentId(userId, documentId))
                    .thenReturn(Optional.of(mockChatHistory));

            ChatHistory result = chatHistoryService.getOrCreate(userId, documentId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            verify(chatHistoryRepository, never()).save(any(ChatHistory.class));
        }

        @Test
        @DisplayName("getOrCreate - Should create and return new entry when missing")
        void getOrCreate_WhenNotFound_ShouldCreateNew() {
            when(chatHistoryRepository.findByUserIdAndDocumentId(userId, documentId))
                    .thenReturn(Optional.empty());
            when(chatHistoryRepository.save(any(ChatHistory.class))).thenReturn(mockChatHistory);

            ChatHistory result = chatHistoryService.getOrCreate(userId, documentId);

            assertThat(result).isNotNull();
            verify(chatHistoryRepository).save(any(ChatHistory.class));
        }
    }

    @Nested
    @DisplayName("MongoTemplate Update & Push Operations")
    class MongoTemplateOperations {

        @Test
        @DisplayName("appendMessage - Deep verification of $push payload structure")
        void appendMessage_DeepPayloadVerification() {
            ChatHistory.Message message = new ChatHistory.Message();

            chatHistoryService.appendMessage(userId, documentId, message);

            ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
            ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);

            verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(ChatHistory.class));

            Query query = queryCaptor.getValue();
            Update update = updateCaptor.getValue();

            assertThat(query.getQueryObject().get("userId")).isEqualTo(userId);
            assertThat(query.getQueryObject().get("documentId")).isEqualTo(documentId);

            Document updateDoc = update.getUpdateObject();
            Document pushDoc = (Document) updateDoc.get("$push");
            assertThat(pushDoc).isNotNull();
            assertThat(pushDoc.get("messages")).isEqualTo(message);
        }

        @Test
        @DisplayName("appendMessages - Deep verification of $push and $each operator")
        void appendMessages_DeepPayloadVerification() {
            ChatHistory.Message msg1 = new ChatHistory.Message();
            ChatHistory.Message msg2 = new ChatHistory.Message();
            List<ChatHistory.Message> messages = List.of(msg1, msg2);

            chatHistoryService.appendMessages(userId, documentId, messages);

            ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
            verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(ChatHistory.class));

            Document updateDoc = updateCaptor.getValue().getUpdateObject();
            assertThat(updateDoc.containsKey("$push")).isTrue();

            Document pushDoc = (Document) updateDoc.get("$push");
            assertThat(pushDoc).isNotNull();

            // Inspect the modifier object without casting directly to Document
            Object messagesModifier = pushDoc.get("messages");
            assertThat(messagesModifier).isNotNull();
            assertThat(messagesModifier.toString()).contains("$each");
        }

        @Test
        @DisplayName("appendMessages - Should execute update even with empty message list")
        void appendMessages_WithEmptyList_ShouldExecuteUpdate() {
            chatHistoryService.appendMessages(userId, documentId, Collections.emptyList());

            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(ChatHistory.class));
        }

        @Test
        @DisplayName("clearMessages - Deep verification that $set targets empty list")
        void clearMessages_DeepPayloadVerification() {
            chatHistoryService.clearMessages(userId, documentId);

            ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
            verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(ChatHistory.class));

            Document updateDoc = updateCaptor.getValue().getUpdateObject();
            Document setDoc = (Document) updateDoc.get("$set");

            assertThat(setDoc).isNotNull();
            assertThat(setDoc.get("messages")).isEqualTo(Collections.emptyList());
        }

        @Test
        @DisplayName("appendMessage - Should execute cleanly when Mongo matches 0 documents")
        void appendMessage_WhenNoDocumentMatches_ShouldNotThrowException() {
            UpdateResult unacknowledgedResult = UpdateResult.acknowledged(0L, 0L, null);
            when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ChatHistory.class)))
                    .thenReturn(unacknowledgedResult);

            chatHistoryService.appendMessage(userId, documentId, new ChatHistory.Message());

            verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(ChatHistory.class));
        }
    }

    @Nested
    @DisplayName("Exception Propagation and Edge Cases")
    class ExceptionAndEdgeCases {

        @Test
        @DisplayName("getByUserAndDocument - Should propagate database exception when Spring Data fails")
        void getByUserAndDocument_OnDatabaseFailure_ShouldPropagate() {
            when(chatHistoryRepository.findByUserIdAndDocumentId(userId, documentId))
                    .thenThrow(new QueryTimeoutException("Database connection timed out"));

            assertThatThrownBy(() -> chatHistoryService.getByUserAndDocument(userId, documentId))
                    .isInstanceOf(QueryTimeoutException.class)
                    .hasMessageContaining("Database connection timed out");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("createChatHistory - Should accept null or blank parameters if builder allows")
        void createChatHistory_WithNullOrEmptyInputs_ShouldPassToRepository(String invalidInput) {
            when(chatHistoryRepository.save(any(ChatHistory.class))).thenReturn(mockChatHistory);

            ChatHistory result = chatHistoryService.createChatHistory(invalidInput, invalidInput);

            assertThat(result).isNotNull();
            verify(chatHistoryRepository).save(any(ChatHistory.class));
        }
    }
}