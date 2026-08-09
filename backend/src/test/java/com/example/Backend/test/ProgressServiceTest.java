package com.example.Backend.test;

import com.example.Backend.repository.FlashcardRepository;
import com.example.Backend.repository.QuizRepository;
import com.example.Backend.services.ProgressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private FlashcardRepository flashcardRepository;

    @InjectMocks
    private ProgressService progressService;

    @Test
    void getDashboardData_returnsExpectedShape() {
        when(quizRepository.count()).thenReturn(42L);

        Map<String, Object> result = progressService.getDashboardData("user-1", "alice");

        assertEquals("alice", result.get("userName"));
        assertEquals(false, result.get("hasUnreadNotifications"));
        assertEquals(0L, result.get("savedDocsCount"));
        assertEquals(42L, result.get("practiceQuestionsCount"));
        assertNotNull(result.get("recents"));
        assertTrue(((java.util.List<?>) result.get("recents")).isEmpty());
    }

    /**
     * KNOWN BUG (flagged in review, not yet fixed): practiceQuestionsCount uses
     * quizRepository.count(), which counts every quiz for every user in the
     * database — userId is accepted as a parameter but never used to scope the
     * query. This test documents that current (incorrect) behavior so it fails
     * loudly once the bug is fixed and this test needs updating to assert
     * per-user scoping instead.
     */
    @Test
    void getDashboardData_currentlyDoesNotScopeCountToUser_knownBug() {
        when(quizRepository.count()).thenReturn(999L);

        Map<String, Object> result = progressService.getDashboardData("user-1", "alice");

        // This asserts the CURRENT (buggy) global count is returned regardless of userId.
        // It should be replaced with a per-user count assertion once ProgressService
        // is fixed to use quizRepository.findByUserId(userId) or similar.
        assertEquals(999L, result.get("practiceQuestionsCount"));
        verify(quizRepository, never()).findByUserId(anyString());
    }
}
