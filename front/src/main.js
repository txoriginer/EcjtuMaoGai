import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'

const app = createApp(App)
app.use(ElementPlus)
app.config.globalProperties.$axios = axios
axios.defaults.baseURL = 'http://localhost:8080'
app.mount('#app')
