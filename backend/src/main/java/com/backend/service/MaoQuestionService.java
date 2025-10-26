package com.backend.service;

import com.backend.entity.MaoQuestion;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MaoQuestionService extends IService<MaoQuestion> {
    
    /**
     * 获取随机一题
     * @return 随机题目
     */
    MaoQuestion getRandomQuestion();
    
    /**
     * 获取指定章节的所有题目
     * @param chapter 章节名称
     * @return 题目列表
     */
    List<MaoQuestion> getQuestionsByChapter(String chapter);
    
    /**
     * 获取单选题集合（40题）
     * @return 单选题列表
     */
    List<MaoQuestion> getSingleChoiceQuestions();
    
    /**
     * 获取多选题集合（10题）
     * @return 多选题列表
     */
    List<MaoQuestion> getMultiChoiceQuestions();
    
    /**
     * 获取所有章节列表
     * @return 章节列表
     */
    List<String> getAllChapters();
}