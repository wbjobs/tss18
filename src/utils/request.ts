import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '@/types';

const request: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
});

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-Tenant-Id'] = '1';
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const res = response.data;
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return response;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default request;
