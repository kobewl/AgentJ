import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import './styles/index.css';

const app = createApp(App);

app.use(router);
app.use(ElementPlus);

app.config.errorHandler = (err, instance, info) => {
  console.error('Vue Error:', err, info);
};

app.mount('#app');

