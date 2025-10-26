package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("mao_questions")
public class MaoQuestion {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    private String chapter;
    
    private String type;
    
    @TableField("question_text")
    private String questionText;
    
    private String options;
    
    @TableField("correct_answers")
    private String correctAnswers;
    
    @TableField("question_number")
    private Integer questionNumber;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField(exist = false)
    private List<String> optionsList;
    
    @TableField(exist = false)
    private List<Integer> correctAnswersList;
}