package com.backend.service.impl;

import com.backend.entity.MaoQuestion;
import com.backend.mapper.MaoQuestionMapper;
import com.backend.service.MaoQuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class MaoQuestionServiceImpl extends ServiceImpl<MaoQuestionMapper, MaoQuestion> implements MaoQuestionService {

    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public MaoQuestion getRandomQuestion() {
        // 获取总题目数
        long count = this.count();
        if (count == 0) {
            return null;
        }
        
        // 生成随机索引
        int randomIndex = new Random().nextInt((int) count);
        
        // 获取随机题目
        List<MaoQuestion> questions = this.list();
        if (randomIndex < questions.size()) {
            return convertQuestion(questions.get(randomIndex));
        }
        
        return null;
    }
    
    /**
     * 转换题目中的JSON字符串为List集合
     */
    private MaoQuestion convertQuestion(MaoQuestion question) {
        try {
            // 转换选项
            if (question.getOptions() != null) {
                question.setOptionsList(objectMapper.readValue(question.getOptions(), List.class));
            }
            // 转换正确答案
            if (question.getCorrectAnswers() != null) {
                question.setCorrectAnswersList(objectMapper.readValue(question.getCorrectAnswers(), List.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return question;
    }

    @Override
    public List<MaoQuestion> getQuestionsByChapter(String chapter) {
        LambdaQueryWrapper<MaoQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaoQuestion::getChapter, chapter)
               .orderByAsc(MaoQuestion::getQuestionNumber);
        return this.list(wrapper).stream()
                .map(this::convertQuestion)
                .collect(Collectors.toList());
    }

    @Override
    public List<MaoQuestion> getSingleChoiceQuestions() {
        LambdaQueryWrapper<MaoQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaoQuestion::getType, "single")
               .last("LIMIT 40");
        return this.list(wrapper).stream()
                .map(this::convertQuestion)
                .collect(Collectors.toList());
    }

    @Override
    public List<MaoQuestion> getMultiChoiceQuestions() {
        LambdaQueryWrapper<MaoQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaoQuestion::getType, "multi")
               .last("LIMIT 10");
        return this.list(wrapper).stream()
                .map(this::convertQuestion)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllChapters() {
        // 获取所有章节，去重
        return this.list().stream()
                .map(MaoQuestion::getChapter)
                .distinct()
                .collect(Collectors.toList());
    }
}