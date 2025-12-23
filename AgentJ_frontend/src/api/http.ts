import axios from 'axios';
import { getToken, clearToken } from '@/utils/auth';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 20000,
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers || {} as any;
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    if (error?.response?.status === 401) {
      clearToken();
      window.location.href = '/login';
    }
    console.error('接口请求异常', error);
    return Promise.reject(error);
  },
);

export default http;

