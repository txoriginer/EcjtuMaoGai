<template>
  <div class="quiz-container" >
    <h1 class="quiz-title">毛概选择题练习</h1>
    
    <!-- 练习模式选择 -->
    <div class="mode-selector">
      <div class="mode-tabs">
        <button 
          class="mode-tab" 
          :class="{ active: currentMode === 'random' }"
          @click="switchMode('random')"
        >
          随机练习
        </button>
        <button 
          class="mode-tab" 
          :class="{ active: currentMode === 'chapter' }"
          @click="switchMode('chapter')"
        >
          章节练习
        </button>
      </div>
      
      <!-- 章节选择区域 -->
      <div v-if="currentMode === 'chapter'" class="chapter-selector">
        <label for="chapter-select">选择章节：</label>
        <select 
          id="chapter-select" 
          v-model="selectedChapter"
          @change="loadChapterQuestions"
        >
          <option value="" disabled>请选择章节</option>
          <option v-for="chapter in chapters" :key="chapter" :value="chapter">
            {{ chapter }}
          </option>
        </select>
        <div class="chapter-info" v-if="chapterQuestions.length > 0">
          当前章节共 {{ chapterQuestions.length }} 题，已完成 {{ completedChapterQuestions }} 题
        </div>
        
        <!-- 题目导航栏（可展开/收缩） -->
        <div v-if="chapterQuestions.length > 0" class="question-navigator-container">
          <button 
            class="navigator-toggle-btn"
            @click="isNavigatorExpanded = !isNavigatorExpanded"
          >
            <span>题目导航</span>
            <span class="toggle-icon" :class="{ expanded: isNavigatorExpanded }">▼</span>
          </button>
          
          <transition name="navigator">
            <div v-if="isNavigatorExpanded" class="question-navigator-content">
              <div class="question-numbers">
                <button
                  v-for="(question, index) in chapterQuestions"
                  :key="index"
                  class="question-number-btn"
                  :class="{
                    'current': currentChapterQuestionIndex === index,
                    'answered': answeredChapterQuestions.has(index),
                    'unanswered': !answeredChapterQuestions.has(index)
                  }"
                  @click="goToQuestion(index)"
                >
                  {{ index + 1 }}
                </button>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <!-- 题目信息区域 -->
    <div v-if="currentQuestion" class="question-section">
      <div class="question-header">
        <span class="chapter">{{ currentQuestion.chapter }}</span>
        <span class="question-type">{{ currentQuestion.type === 'single' ? '单选题' : '多选题' }}</span>
      </div>

      <h2 class="question-text">{{ currentQuestion.questionText }}</h2>

      <!-- 选项列表 -->
      <div class="options-list">
        <div
          v-for="(option, index) in optionsList"
          :key="index"
          class="option-item"
          :class="{
            'selected': selectedOptions.includes(index),
            'correct': showResult && correctAnswers.includes(index),
            'incorrect': showResult && selectedOptions.includes(index) && !correctAnswers.includes(index)
          }"
          @click="handleOptionClick(index)"
        >
          <span class="option-letter">{{ String.fromCharCode(65 + index) }}</span>
          <span class="option-text">{{ option }}</span>
          <span v-if="showResult && correctAnswers.includes(index)" class="correct-icon">✓</span>
          <span v-else-if="showResult && selectedOptions.includes(index) && !correctAnswers.includes(index)" class="incorrect-icon">✗</span>
        </div>
      </div>

<!--      &lt;!&ndash; 结果显示 &ndash;&gt;-->
<!--      <div v-if="showResult" class="result-section">-->
<!--        <div class="result-message" :class="isCorrect ? 'correct-message' : 'incorrect-message'">-->
<!--          {{ isCorrect ? '回答正确！' : '回答错误！' }}-->
<!--        </div>-->
<!--        <div class="correct-answers" v-if="!isCorrect">-->
<!--          <strong>正确答案：</strong>-->
<!--          <span v-for="answer in correctAnswers" :key="answer" class="answer-tag">-->
<!--            {{ String.fromCharCode(65 + answer) }}-->
<!--          </span>-->
<!--        </div>-->
<!--      </div>-->

      <div  class="button-group">
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button
              class="btn-primary"
              @click="handleNextQuestion"
              :disabled="!showResult"
          >
            下一题
          </button>
        </div>

        <!-- 在选项列表下方添加 -->
        <div v-if="currentQuestion?.type === 'multi' && !showResult" class="submit-section">
          <button class="btn-primary" @click="checkAnswer" :disabled="selectedOptions.length === 0">
            提交答案
          </button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-else-if="loading" class="loading-section">
      <div class="loading-spinner"></div>
      <p>正在加载题目...</p>
    </div>

    <!-- 初始状态 -->
    <div v-else class="initial-section">
      <p>点击开始按钮获取随机题目</p>
      <button class="btn-primary" @click="startPractice">开始练习</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import axios from 'axios';

// 响应式数据
const currentQuestion = ref(null);
const selectedOptions = ref([]);
const showResult = ref(false);
const isCorrect = ref(false);
const correctAnswers = ref([]);
const loading = ref(false);

// 章节相关数据
const chapters = ref([]);
const selectedChapter = ref('');
const chapterQuestions = ref([]);
const currentChapterQuestionIndex = ref(-1);
const currentMode = ref('random'); // 'random' 或 'chapter'
const completedChapterQuestions = ref(0);
const answeredChapterQuestions = ref(new Set());
const isNavigatorExpanded = ref(false); // 控制导航栏展开/收缩状态

// 组件挂载时加载章节列表
onMounted(async () => {
  document.addEventListener('keypress', handleKeyPress);
  await loadChapters();
  
  // 组件卸载时移除事件监听
  return () => {
    document.removeEventListener('keypress', handleKeyPress);
  };
});

// 计算属性 - 将JSON字符串转换为选项列表
const optionsList = computed(() => {
  if (!currentQuestion.value) return [];
  try {
    if (currentQuestion.value.optionsList) {
      return currentQuestion.value.optionsList;
    } else if (typeof currentQuestion.value.options === 'string') {
      return JSON.parse(currentQuestion.value.options);
    }
  } catch (e) {
    console.error('解析选项失败:', e);
  }
  return [];
});

// 加载章节列表
const loadChapters = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/questions/chapters');
    if (response.data.code === 200) {
      chapters.value = response.data.data;
    }
  } catch (error) {
    console.error('加载章节列表失败:', error);
  }
};

// 切换练习模式
const switchMode = (mode) => {
  if (currentMode.value !== mode) {
    currentMode.value = mode;
    resetQuizState();
    
    if (mode === 'chapter' && selectedChapter.value) {
      loadChapterQuestions();
    }
  }
};

// 重置测验状态
const resetQuizState = () => {
  currentQuestion.value = null;
  selectedOptions.value = [];
  showResult.value = false;
  isCorrect.value = false;
  correctAnswers.value = [];
  currentChapterQuestionIndex.value = -1;
  completedChapterQuestions.value = 0;
  answeredChapterQuestions.value.clear();
};

// 加载章节题目
const loadChapterQuestions = async () => {
  if (!selectedChapter.value) return;
  
  loading.value = true;
  resetQuizState();
  
  try {
    const response = await axios.get(`http://localhost:8080/api/questions/chapter/${encodeURIComponent(selectedChapter.value)}`);
    if (response.data.code === 200) {
      chapterQuestions.value = response.data.data;
      if (chapterQuestions.value.length > 0) {
        loadNextChapterQuestion();
      } else {
        alert('该章节暂无题目');
      }
    }
  } catch (error) {
    console.error('加载章节题目失败:', error);
    alert('加载题目失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 加载章节的下一题
const loadNextChapterQuestion = () => {
  if (chapterQuestions.value.length === 0) return;
  
  // 找到下一个未回答的题目，如果所有题目都已回答，则随机选择一个
  let nextIndex = -1;
  for (let i = 0; i < chapterQuestions.value.length; i++) {
    if (!answeredChapterQuestions.value.has(i)) {
      nextIndex = i;
      break;
    }
  }
  
  // 如果所有题目都已回答，随机选择一个
  if (nextIndex === -1) {
    nextIndex = Math.floor(Math.random() * chapterQuestions.value.length);
  }
  
  currentChapterQuestionIndex.value = nextIndex;
  currentQuestion.value = chapterQuestions.value[nextIndex];
  
  // 解析正确答案
  try {
    if (currentQuestion.value.correctAnswersList) {
      correctAnswers.value = currentQuestion.value.correctAnswersList;
    } else if (typeof currentQuestion.value.correctAnswers === 'string') {
      correctAnswers.value = JSON.parse(currentQuestion.value.correctAnswers);
    }
  } catch (e) {
    console.error('解析正确答案失败:', e);
  }
};

// 获取随机题目
const getRandomQuestion = async () => {
  loading.value = true;
  selectedOptions.value = [];
  showResult.value = false;
  isCorrect.value = false;
  correctAnswers.value = [];

  try {
    const response = await axios.get('http://localhost:8080/api/questions/random');
    if (response.data.code === 200) {
      currentQuestion.value = response.data.data;

      // 尝试解析正确答案
      try {
        if (currentQuestion.value.correctAnswersList) {
          correctAnswers.value = currentQuestion.value.correctAnswersList;
        } else if (typeof currentQuestion.value.correctAnswers === 'string') {
          correctAnswers.value = JSON.parse(currentQuestion.value.correctAnswers);
        }
      } catch (e) {
        console.error('解析正确答案失败:', e);
      }
    } else {
      alert('获取题目失败：' + response.data.message);
    }
  } catch (error) {
    console.error('获取题目时发生错误:', error);
    alert('网络错误，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 处理选项点击
const handleOptionClick = async (index) => {
  // 如果已经显示结果，不允许再选择
  if (showResult.value) return;

  // 根据题目类型处理选择
  if (currentQuestion.value.type === 'single') {
    selectedOptions.value = [index];
    // 单选题点击后立即检查答案
    await checkAnswer();
  } else {
    // 多选题允许多选
    const optionIndex = selectedOptions.value.indexOf(index);
    if (optionIndex > -1) {
      selectedOptions.value.splice(optionIndex, 1);
    } else {
      selectedOptions.value.push(index);
    }
  }
};

// 检查答案
const checkAnswer = async () => {
  try {
    const response = await axios.post('http://localhost:8080/api/questions/check-answer', {
      questionId: currentQuestion.value.id,
      userAnswers: selectedOptions.value
    });

    if (response.data.code === 200) {
      isCorrect.value = response.data.data.isCorrect;

      // 确保正确答案已正确解析
      if (response.data.data.correctAnswers) {
        correctAnswers.value = response.data.data.correctAnswers;
      }

      showResult.value = true;
      
      // 记录已回答的章节题目
      if (currentMode.value === 'chapter' && currentChapterQuestionIndex.value >= 0) {
        answeredChapterQuestions.value.add(currentChapterQuestionIndex.value);
        completedChapterQuestions.value = answeredChapterQuestions.value.size;
      }
    }
  } catch (error) {
    console.error('检查答案失败:', error);
    // 如果接口调用失败，本地检查
    localCheckAnswer();
  }
};

// 本地检查答案（备用方案）
const localCheckAnswer = () => {
  if (!correctAnswers.value) return;

  // 检查选择的答案是否与正确答案完全匹配
    if (selectedOptions.value.length === correctAnswers.value.length) {
      const isAllCorrect = selectedOptions.value.every(option =>
        correctAnswers.value.includes(option)
      );
      isCorrect.value = isAllCorrect;
    }

  showResult.value = true;
  
  // 记录已回答的章节题目
  if (currentMode.value === 'chapter' && currentChapterQuestionIndex.value >= 0) {
    answeredChapterQuestions.value.add(currentChapterQuestionIndex.value);
    completedChapterQuestions.value = answeredChapterQuestions.value.size;
  }
};

// 开始练习
const startPractice = () => {
  if (currentMode.value === 'chapter') {
    if (!selectedChapter.value && chapters.value.length > 0) {
      // 如果还没有选择章节，默认选择第一个章节
      selectedChapter.value = chapters.value[0];
    }
    loadChapterQuestions();
  } else {
    getRandomQuestion();
  }
};

// 跳转到指定题目
const goToQuestion = (index) => {
  if (index >= 0 && index < chapterQuestions.value.length) {
    currentChapterQuestionIndex.value = index;
    currentQuestion.value = chapterQuestions.value[index];
    
    // 重置选择和结果状态
    selectedOptions.value = [];
    showResult.value = false;
    isCorrect.value = false;
    
    // 解析正确答案
    try {
      if (currentQuestion.value.correctAnswersList) {
        correctAnswers.value = currentQuestion.value.correctAnswersList;
      } else if (typeof currentQuestion.value.correctAnswers === 'string') {
        correctAnswers.value = JSON.parse(currentQuestion.value.correctAnswers);
      }
    } catch (e) {
      console.error('解析正确答案失败:', e);
    }
  }
};

// 处理下一题按钮点击
const handleNextQuestion = () => {
  if (currentMode.value === 'chapter' && chapterQuestions.value.length > 0) {
    loadNextChapterQuestion();
  } else {
    getRandomQuestion();
  }
  // 重置选择和结果状态
  selectedOptions.value = [];
  showResult.value = false;
  isCorrect.value = false;
};

// 多选题提交答案（按Enter键）
const handleKeyPress = (event) => {
  if (event.key === 'Enter' && currentQuestion.value?.type === 'multi' && selectedOptions.value.length > 0) {
    checkAnswer();
  }
};

// 组件挂载时添加键盘事件监听
onMounted(() => {
  document.addEventListener('keypress', handleKeyPress);

  // 组件卸载时移除事件监听
  return () => {
    document.removeEventListener('keypress', handleKeyPress);
  };
});
</script>

<style scoped>
/* 父容器：控制两个按钮区域并排 */
.button-group {
  display: flex; /* 横向排列 */
  gap: 15px; /* 两个按钮区域的间距（可调整） */
  justify-content: center; /* 整体在页面中水平居中 */
  align-items: center; /* 两个按钮垂直居中对齐 */
  flex-wrap: wrap; /* 响应式换行：手机端自动排成两行 */
  margin-top: 20px; /* 与上方内容的间距（可调整） */
}

/* 移除子容器默认的居中样式（避免冲突） */
.action-buttons, .submit-section {
  text-align: inherit; /* 继承父容器的对齐方式 */
  margin: 0; /* 清除默认边距 */
}

.quiz-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Microsoft YaHei', sans-serif;
}

.quiz-title {
  text-align: center;
  color: #1a73e8;
  margin-bottom: 20px;
  font-size: 28px;
}

/* 练习模式选择器样式 */
.mode-selector {
  margin-bottom: 25px;
  padding: 20px;
  background-color: #f0f7ff;
  border-radius: 8px;
}

.mode-tabs {
  display: flex;
  margin-bottom: 20px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #1a73e8;
}

.mode-tab {
  flex: 1;
  padding: 12px 20px;
  background-color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
  color: #666;
}

.mode-tab:first-child {
  border-right: 1px solid #e0e0e0;
}

.mode-tab:hover {
  background-color: #f8f9fa;
}

.mode-tab.active {
  background-color: #1a73e8;
  color: white;
}

/* 章节选择器样式 */
.chapter-selector {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chapter-selector label {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.chapter-selector select {
  padding: 10px 15px;
  font-size: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
  cursor: pointer;
  transition: border-color 0.3s ease;
}

.chapter-selector select:focus {
  outline: none;
  border-color: #1a73e8;
}

.chapter-info {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

/* 可展开/收缩的题目导航栏样式 */
.question-navigator-container {
  margin-top: 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

.navigator-toggle-btn {
  width: 100%;
  padding: 12px 20px;
  background-color: white;
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  transition: background-color 0.3s ease;
}

.navigator-toggle-btn:hover {
  background-color: #f8f9fa;
}

.toggle-icon {
  font-size: 12px;
  transition: transform 0.3s ease;
}

.toggle-icon.expanded {
  transform: rotate(180deg);
}

.question-navigator-content {
  background-color: white;
  padding: 15px;
}

.question-numbers {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.question-number-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #e0e0e0;
  background-color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.question-number-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.question-number-btn.current {
  background-color: #1a73e8;
  color: white;
  border-color: #1a73e8;
}

.question-number-btn.answered {
  background-color: #e8f5e9;
  border-color: #4caf50;
  color: #4caf50;
}

.question-number-btn.unanswered {
  background-color: white;
  border-color: #e0e0e0;
  color: #666;
}

/* 导航栏展开/收缩动画 */
.navigator-enter-active,
.navigator-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.navigator-enter-from,
.navigator-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.navigator-enter-to,
.navigator-leave-from {
  max-height: 500px;
  opacity: 1;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .question-number-btn {
    width: 35px;
    height: 35px;
    font-size: 12px;
  }
  
  .question-numbers {
    gap: 6px;
  }
  
  .navigator-toggle-btn {
    padding: 10px 15px;
    font-size: 14px;
  }
}

.question-section {
  background-color: #f8f9fa;
  border-radius: 10px;
  padding: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.question-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.question-text {
  font-size: 18px;
  margin-bottom: 25px;
  color: #333;
  line-height: 1.5;
}

.options-list {
  margin-bottom: 25px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 15px;
  margin-bottom: 10px;
  background-color: white;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.option-item:hover:not(.correct):not(.incorrect) {
  border-color: #1a73e8;
  background-color: #f0f7ff;
}

.option-item.selected:not(.correct):not(.incorrect) {
  border-color: #1a73e8;
  background-color: #e3f2fd;
}

.option-item.correct {
  border-color: #4caf50;
  background-color: #e8f5e9;
}

.option-item.incorrect {
  border-color: #f44336;
  background-color: #ffebee;
}

.option-letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background-color: #f0f0f0;
  border-radius: 50%;
  margin-right: 15px;
  font-weight: bold;
  color: #333;
}

.option-text {
  flex: 1;
  font-size: 16px;
}

.correct-icon,
.incorrect-icon {
  font-size: 20px;
  font-weight: bold;
  margin-left: 10px;
}

.correct-icon {
  color: #4caf50;
}

.incorrect-icon {
  color: #f44336;
}

.result-section {
  margin-bottom: 25px;
  padding: 15px;
  border-radius: 8px;
  background-color: white;
}

.result-message {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  padding: 10px;
  border-radius: 5px;
  text-align: center;
}

.correct-message {
  color: #4caf50;
  background-color: #e8f5e9;
}

.incorrect-message {
  color: #f44336;
  background-color: #ffebee;
}

.correct-answers {
  font-size: 16px;
  text-align: center;
}

.answer-tag {
  display: inline-block;
  padding: 5px 10px;
  margin: 0 5px;
  background-color: #4caf50;
  color: white;
  border-radius: 4px;
  font-weight: bold;
}

.action-buttons {
  text-align: center;
}

.btn-primary {
  padding: 12px 30px;
  font-size: 16px;
  font-weight: bold;
  color: white;
  background-color: #1a73e8;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.btn-primary:hover:not(:disabled) {
  background-color: #1557b0;
}

.btn-primary:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.loading-section,
.initial-section {
  text-align: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1a73e8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .quiz-container {
    padding: 10px;
  }

  .question-section {
    padding: 15px;
  }

  .question-text {
    font-size: 16px;
  }

  .option-text {
    font-size: 14px;
  }
}
</style>