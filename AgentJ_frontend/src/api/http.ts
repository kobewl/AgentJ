import axios from 'axios';
import { getToken } from '@/utils/auth';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 20000,
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    if (error?.response?.status === 401) {
      // 清理 token 并跳转登录
      import('@/utils/auth').then(({ clearToken }) => clearToken());
      window.location.href = '/login';
    }
    console.error('接口请求异常', error);
    return Promise.reject(error);
  },
);

export default http;
