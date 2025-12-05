import axios from 'axios';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 20000,
});

http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    console.error('接口请求异常', error);
    return Promise.reject(error);
  },
);

export default http;
