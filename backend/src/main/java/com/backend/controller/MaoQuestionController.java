package com.backend.controller;

import com.backend.entity.MaoQuestion;
import com.backend.service.MaoQuestionService;
import com.backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin
public class MaoQuestionController {

    @Autowired
    private MaoQuestionService maoQuestionService;

    @GetMapping("/random")
    public Result<MaoQuestion> getRandomQuestion() {
        return Result.success(maoQuestionService.getRandomQuestion());
    }

    @GetMapping("/chapter/{chapter}")
    public Result<List<MaoQuestion>> getQuestionsByChapter(@PathVariable String chapter) {
        return Result.success(maoQuestionService.getQuestionsByChapter(chapter));
    }

    @GetMapping("/single")
    public Result<List<MaoQuestion>> getSingleChoiceQuestions() {
        return Result.success(maoQuestionService.getSingleChoiceQuestions());
    }

    @GetMapping("/multi")
    public Result<List<MaoQuestion>> getMultiChoiceQuestions() {
        return Result.success(maoQuestionService.getMultiChoiceQuestions());
    }

    @GetMapping("/chapters")
    public Result<List<String>> getAllChapters() {
        return Result.success(maoQuestionService.getAllChapters());
    }
    
    /**
     * 检查答案是否正确
     * @param request 包含题目ID和用户选择的答案列表
     * @return 检查结果
     */
    @PostMapping("/check-answer")
    public Result<Map<String, Object>> checkAnswer(@RequestBody Map<String, Object> request) {
        Integer questionId = (Integer) request.get("questionId");
        List<Integer> userAnswers = (List<Integer>) request.get("userAnswers");
        
        // 调用service获取题目，并确保JSON转换正确
        MaoQuestion question = maoQuestionService.getById(questionId);
        if (question == null) {
            return Result.error("题目不存在");
        }
        
        // 手动转换JSON字符串为List（临时解决方案）
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            if (question.getOptions() != null) {
                question.setOptionsList(mapper.readValue(question.getOptions(), List.class));
            }
            if (question.getCorrectAnswers() != null) {
                question.setCorrectAnswersList(mapper.readValue(question.getCorrectAnswers(), List.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        boolean isCorrect = false;
        List<Integer> correctAnswers = question.getCorrectAnswersList();
        
        if (correctAnswers != null && userAnswers != null) {
            // 检查答案是否完全匹配（数量和内容都要匹配）
            if (correctAnswers.size() == userAnswers.size() && correctAnswers.containsAll(userAnswers)) {
                isCorrect = true;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("isCorrect", isCorrect);
        result.put("correctAnswers", correctAnswers);
        
        return Result.success(result);
    }
}