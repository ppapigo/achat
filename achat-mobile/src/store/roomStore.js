import { create } from 'zustand';
import { roomApi, matchApi } from '../api/roomApi';

export const useRoomStore = create((set, get) => ({
  // 방 관련
  rooms: [],
  currentRoom: null,
  subRooms: [],
  isLoading: false,
  error: null,

  // 매칭 관련
  isMatching: false,
  matchStatus: null,

  // 방 목록 조회
  fetchRooms: async () => {
    set({ isLoading: true, error: null });
    try {
      const res = await roomApi.getRooms();
      set({ rooms: res.data.data || [], isLoading: false });
    } catch (error) {
      console.error('Fetch rooms error:', error);
      set({ error: error.message, isLoading: false });
    }
  },

  // 방 생성
  createRoom: async (roomData) => {
    set({ isLoading: true });
    try {
      const res = await roomApi.createRoom(roomData);
      const newRoom = res.data.data;
      set((state) => ({
        rooms: [...state.rooms, newRoom],
        currentRoom: newRoom,
        isLoading: false,
      }));
      return newRoom;
    } catch (error) {
      console.error('Create room error:', error);
      set({ error: error.message, isLoading: false });
      throw error;
    }
  },

  // 방 참가
  joinRoom: async (roomId) => {
    set({ isLoading: true });
    try {
      const res = await roomApi.joinRoom(roomId);
      set({ currentRoom: res.data.data, isLoading: false });
      return res.data.data;
    } catch (error) {
      console.error('Join room error:', error);
      set({ error: error.message, isLoading: false });
      throw error;
    }
  },

  // 방 나가기
  leaveRoom: async () => {
    const { currentRoom } = get();
    if (!currentRoom) return;
    
    try {
      await roomApi.leaveRoom(currentRoom.id);
    } catch (error) {
      console.error('Leave room error:', error);
    }
    set({ currentRoom: null, subRooms: [] });
  },

  // 서브방 목록 조회
  fetchSubRooms: async (roomId) => {
    try {
      const res = await roomApi.getSubRooms(roomId);
      set({ subRooms: res.data.data || [] });
    } catch (error) {
      console.error('Fetch sub rooms error:', error);
    }
  },

  // 서브방 생성
  createSubRoom: async (roomId, name) => {
    try {
      const res = await roomApi.createSubRoom(roomId, name);
      const newSubRoom = res.data.data;
      set((state) => ({
        subRooms: [...state.subRooms, newSubRoom],
      }));
      return newSubRoom;
    } catch (error) {
      console.error('Create sub room error:', error);
      throw error;
    }
  },

  // 서브방 삭제
  deleteSubRoom: async (roomId, subRoomId) => {
    try {
      await roomApi.deleteSubRoom(roomId, subRoomId);
      set((state) => ({
        subRooms: state.subRooms.filter((r) => r.id !== subRoomId),
      }));
    } catch (error) {
      console.error('Delete sub room error:', error);
      throw error;
    }
  },

  // === 빠른매칭 ===
  
  // 매칭 큐 참가
  joinMatchQueue: async (playerCount) => {
    set({ isMatching: true, error: null });
    try {
      const res = await matchApi.joinQueue(playerCount);
      set({ matchStatus: res.data.data });
      return res.data.data;
    } catch (error) {
      console.error('Join match queue error:', error);
      set({ isMatching: false, error: error.message });
      throw error;
    }
  },

  // 매칭 큐 나가기
  leaveMatchQueue: async () => {
    try {
      await matchApi.leaveQueue();
    } catch (error) {
      console.error('Leave match queue error:', error);
    }
    set({ isMatching: false, matchStatus: null });
  },

  // 매칭 상태 확인
  checkMatchStatus: async () => {
    try {
      const res = await matchApi.getStatus();
      const status = res.data.data;
      
      if (status.matched && status.roomId) {
        // 매칭 완료!
        set({ isMatching: false, matchStatus: status });
        return status;
      }
      
      set({ matchStatus: status });
      return status;
    } catch (error) {
      console.error('Check match status error:', error);
      return null;
    }
  },

  // 현재 방 설정
  setCurrentRoom: (room) => set({ currentRoom: room }),
  
  // 에러 초기화
  clearError: () => set({ error: null }),
}));
