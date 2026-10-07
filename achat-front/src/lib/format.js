export const number = (value = 0) => new Intl.NumberFormat('ko-KR').format(value);
export const date = (value) => value ? new Date(value).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' }) : '';
export const time = (value) => value ? new Date(value).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : '';
export const shortId = (value = '') => value.slice(0, 6);
export const tiers = { UNRANKED: '배치 진행 중', BRONZE: '브론즈', SILVER: '실버', GOLD: '골드', PLATINUM: '플래티넘', DIAMOND: '다이아몬드', MASTER: '마스터', GRANDMASTER: '그랜드마스터', CHALLENGER: '챌린저' };
