import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import api from '../api/client';

export const useAuthStore = create((set, get) => ({
  user: null,
  isLoading: true,
  isAuthenticated: false,
  pendingEmail: null, // 인증 대기 중인 이메일

  // 초기화: 저장된 토큰 확인 + 유저 정보 로드
  initialize: async () => {
    try {
      const token = await AsyncStorage.getItem('accessToken');
      if (token) {
        // /auth/me로 유저 정보 가져오기
        const res = await api.get('/auth/me');
        set({ 
          user: res.data.data, 
          isAuthenticated: true, 
          isLoading: false 
        });
      } else {
        set({ isLoading: false });
      }
    } catch (e) {
      console.error('Auth init error:', e);
      // 토큰 만료 등 에러 시 로그아웃 처리
      await AsyncStorage.removeItem('accessToken');
      await AsyncStorage.removeItem('refreshToken');
      set({ isLoading: false, isAuthenticated: false, user: null });
    }
  },

  // 회원가입 (이메일 인증 코드 발송)
  register: async (email, password, username) => {
    const res = await api.post('/auth/register', { email, password, username });
    // 인증 대기 상태로 전환
    set({ pendingEmail: email });
    return res.data.data;
  },

  // 이메일 인증 코드 확인
  verifyEmail: async (email, code) => {
    const res = await api.post('/auth/verify', { email, code });
    set({ pendingEmail: null });
    return res.data.data;
  },

  // 인증 코드 재발송
  resendCode: async (email) => {
    await api.post(`/auth/resend-code?email=${encodeURIComponent(email)}`);
  },

  // 로그인
  login: async (email, password) => {
    const res = await api.post('/auth/login', { email, password });
    const { accessToken, refreshToken } = res.data.data;
    
    await AsyncStorage.setItem('accessToken', accessToken);
    await AsyncStorage.setItem('refreshToken', refreshToken);
    
    // 유저 정보 가져오기
    const userRes = await api.get('/auth/me');
    
    set({ 
      user: userRes.data.data, 
      isAuthenticated: true 
    });
    return res.data.data;
  },

  // 로그아웃
  logout: async () => {
    await AsyncStorage.removeItem('accessToken');
    await AsyncStorage.removeItem('refreshToken');
    set({ user: null, isAuthenticated: false });
  },

  // 프로필 수정
  updateProfile: async (username) => {
    const res = await api.patch('/auth/profile', { username });
    // 유저 정보 갱신
    const { user } = get();
    set({ user: { ...user, username: res.data.data.username } });
    return res.data.data;
  },
}));
