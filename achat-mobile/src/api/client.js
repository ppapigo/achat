import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

// 개발 시 로컬 IP로 변경 필요
// Android 에뮬레이터: 10.0.2.2
// iOS 시뮬레이터: localhost
// 실제 기기: 컴퓨터 IP (예: 192.168.0.x)
const BASE_URL = 'http://10.0.2.2:8080';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: 토큰 자동 첨부
api.interceptors.request.use(async (config) => {
  const token = await AsyncStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터: 에러 처리 + 토큰 갱신
api.interceptors.response.use(
  (response) => {
    // 백엔드 응답: { success, data, error }
    if (response.data && response.data.success === false) {
      return Promise.reject(new Error(response.data.error || '요청 실패'));
    }
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    
    // 401 에러 시 토큰 갱신 시도
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = await AsyncStorage.getItem('refreshToken');
        if (refreshToken) {
          const res = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken });
          const { accessToken, refreshToken: newRefreshToken } = res.data.data;
          
          await AsyncStorage.setItem('accessToken', accessToken);
          await AsyncStorage.setItem('refreshToken', newRefreshToken);
          
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        // 토큰 갱신 실패 시 로그아웃
        await AsyncStorage.removeItem('accessToken');
        await AsyncStorage.removeItem('refreshToken');
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
