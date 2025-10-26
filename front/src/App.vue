<script setup>
import { ref } from 'vue';
// 导入主页面组件
import Quiz from './components/Quiz.vue';
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>毛概题库练习系统</h1>
    </header>
    <main class="app-main">
      <Quiz />
    </main>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background-color: #f5f7fa;
  color: #333;
}

.app-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.app-header {
  text-align: center;
  padding: 20px 0;
  margin-bottom: 30px;
  border-bottom: 1px solid #eaeaea;
}

.app-header h1 {
  color: #409eff;
  font-size: 28px;
}

.app-main {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}
</style>
