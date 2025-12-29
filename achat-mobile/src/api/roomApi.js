import api from './client';

export const roomApi = {
  // 방 목록 조회
  getRooms: () => api.get('/rooms'),
  
  // 방 상세 조회
  getRoom: (roomId) => api.get(`/rooms/${roomId}`),
  
  // 방 생성
  createRoom: (data) => api.post('/rooms', data),
  
  // 방 참가
  joinRoom: (roomId) => api.post(`/rooms/${roomId}/join`),
  
  // 방 나가기
  leaveRoom: (roomId) => api.post(`/rooms/${roomId}/leave`),
  
  // 서브방 목록
  getSubRooms: (roomId) => api.get(`/rooms/${roomId}/subrooms`),
  
  // 서브방 생성
  createSubRoom: (roomId, name) => api.post(`/rooms/${roomId}/subrooms`, { name }),
  
  // 서브방 삭제
  deleteSubRoom: (roomId, subRoomId) => api.delete(`/rooms/${roomId}/subrooms/${subRoomId}`),
};

export const matchApi = {
  // 빠른매칭 큐 참가
  joinQueue: (playerCount = 8) => api.post(`/match/join?playerCount=${playerCount}`),
  
  // 빠른매칭 큐 나가기
  leaveQueue: () => api.post('/match/leave'),
  
  // 매칭 상태 확인
  getStatus: () => api.get('/match/status'),
};

export const augmentApi = {
  // 증강 옵션 조회
  getOptions: (roomId) => api.get(`/rooms/${roomId}/augments/options`),
  
  // 증강 선택
  pick: (roomId, augmentId) => api.post(`/rooms/${roomId}/augments/pick`, { augmentId }),
};
