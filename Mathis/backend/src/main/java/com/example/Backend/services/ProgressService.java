package com.example.Backend.services;

import com.example.Backend.repository.QuizRepository;
import com.example.Backend.repository.FlashcardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProgressService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private FlashcardRepository flashcardRepository;

    public Map<String, Object> getDashboardData(String userId, String username) {
        long practiceQuestionsCount = quizRepository.count(); 
        long savedDocsCount = 0; 

        Map<String, Object> response = new HashMap<>();
        response.put("userName", username);
        response.put("hasUnreadNotifications", false);
        response.put("savedDocsCount", savedDocsCount);
        response.put("practiceQuestionsCount", practiceQuestionsCount);
        response.put("recents", new ArrayList<>());

        return response;
    }
}