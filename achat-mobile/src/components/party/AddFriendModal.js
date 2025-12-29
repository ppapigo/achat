import { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { X, Search, UserPlus, Check, Clock, UserX } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

// Mock 검색 결과
const MOCK_SEARCH_RESULTS = [
  { id: '101', name: '게이머123', level: 22, status: 'none' },
  { id: '102', name: '프로게이머', level: 35, status: 'none' },
  { id: '103', name: '뉴비유저', level: 5, status: 'pending' }, // 이미 요청 보냄
  { id: '104', name: '친구임', level: 18, status: 'friend' }, // 이미 친구
];

// Mock 받은 친구 요청
const MOCK_REQUESTS = [
  { id: '201', name: '요청자1', level: 12, message: '같이 게임해요!' },
  { id: '202', name: '요청자2', level: 8, message: '' },
  { id: '203', name: '요청자3', level: 25, message: '랭크 ㄱㄱ' },
];

export default function AddFriendModal({ visible, onClose, onFriendAdded }) {
  const [activeTab, setActiveTab] = useState('search'); // 'search' | 'requests'
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [requests, setRequests] = useState(MOCK_REQUESTS);
  const [loading, setLoading] = useState(false);
  const [sentRequests, setSentRequests] = useState(new Set());

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    
    setLoading(true);
    // Mock API 호출
    setTimeout(() => {
      const filtered = MOCK_SEARCH_RESULTS.filter(u => 
        u.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
      setSearchResults(filtered);
      setLoading(false);
    }, 500);
  };

  const handleSendRequest = (user) => {
    Alert.alert(
      '친구 요청',
      `${user.name}님에게 친구 요청을 보낼까요?`,
      [
        { text: '취소', style: 'cancel' },
        { 
          text: '보내기', 
          onPress: () => {
            setSentRequests(prev => new Set([...prev, user.id]));
            Alert.alert('완료', '친구 요청을 보냈습니다!');
          }
        },
      ]
    );
  };

  const handleAcceptRequest = (request) => {
    setRequests(prev => prev.filter(r => r.id !== request.id));
    Alert.alert('완료', `${request.name}님과 친구가 되었습니다!`);
    onFriendAdded?.(request);
  };

  const handleRejectRequest = (request) => {
    Alert.alert(
      '거절',
      `${request.name}님의 친구 요청을 거절할까요?`,
      [
        { text: '취소', style: 'cancel' },
        { 
          text: '거절', 
          style: 'destructive',
          onPress: () => {
            setRequests(prev => prev.filter(r => r.id !== request.id));
          }
        },
      ]
    );
  };

  const renderSearchResult = ({ item }) => {
    const isSent = sentRequests.has(item.id) || item.status === 'pending';
    const isFriend = item.status === 'friend';

    return (
      <View style={styles.resultItem}>
        <View style={styles.userAvatar}>
          <Text style={styles.avatarText}>{item.name[0]}</Text>
        </View>
        <View style={styles.userInfo}>
          <Text style={styles.userName}>{item.name}</Text>
          <Text style={styles.userLevel}>Lv.{item.level}</Text>
        </View>
        {isFriend ? (
          <View style={styles.statusBadge}>
            <Check size={16} color={colors.success} />
            <Text style={styles.statusText}>친구</Text>
          </View>
        ) : isSent ? (
          <View style={styles.statusBadge}>
            <Clock size={16} color={colors.warning} />
            <Text style={styles.statusText}>대기중</Text>
          </View>
        ) : (
          <TouchableOpacity 
            style={styles.addButton}
            onPress={() => handleSendRequest(item)}
          >
            <UserPlus size={20} color={colors.textPrimary} />
          </TouchableOpacity>
        )}
      </View>
    );
  };

  const renderRequest = ({ item }) => (
    <View style={styles.requestItem}>
      <View style={styles.userAvatar}>
        <Text style={styles.avatarText}>{item.name[0]}</Text>
      </View>
      <View style={styles.requestInfo}>
        <View style={styles.requestHeader}>
          <Text style={styles.userName}>{item.name}</Text>
          <Text style={styles.userLevel}>Lv.{item.level}</Text>
        </View>
        {item.message ? (
          <Text style={styles.requestMessage} numberOfLines={1}>"{item.message}"</Text>
        ) : null}
      </View>
      <View style={styles.requestActions}>
        <TouchableOpacity 
          style={styles.acceptButton}
          onPress={() => handleAcceptRequest(item)}
        >
          <Check size={20} color={colors.textPrimary} />
        </TouchableOpacity>
        <TouchableOpacity 
          style={styles.rejectButton}
          onPress={() => handleRejectRequest(item)}
        >
          <X size={20} color={colors.danger} />
        </TouchableOpacity>
      </View>
    </View>
  );

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent={true}
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.container}>
          {/* 헤더 */}
          <View style={styles.header}>
            <Text style={styles.headerTitle}>친구 추가</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeButton}>
              <X size={24} color={colors.textPrimary} />
            </TouchableOpacity>
          </View>

          {/* 탭 */}
          <View style={styles.tabContainer}>
            <TouchableOpacity
              style={[styles.tab, activeTab === 'search' && styles.tabActive]}
              onPress={() => setActiveTab('search')}
            >
              <Search size={18} color={activeTab === 'search' ? colors.primary : colors.textMuted} />
              <Text style={[styles.tabText, activeTab === 'search' && styles.tabTextActive]}>
                검색
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.tab, activeTab === 'requests' && styles.tabActive]}
              onPress={() => setActiveTab('requests')}
            >
              <UserPlus size={18} color={activeTab === 'requests' ? colors.primary : colors.textMuted} />
              <Text style={[styles.tabText, activeTab === 'requests' && styles.tabTextActive]}>
                받은 요청
              </Text>
              {requests.length > 0 && (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>{requests.length}</Text>
                </View>
              )}
            </TouchableOpacity>
          </View>

          {/* 검색 탭 */}
          {activeTab === 'search' && (
            <View style={styles.content}>
              <View style={styles.searchContainer}>
                <TextInput
                  style={styles.searchInput}
                  placeholder="닉네임으로 검색..."
                  placeholderTextColor={colors.textMuted}
                  value={searchQuery}
                  onChangeText={setSearchQuery}
                  onSubmitEditing={handleSearch}
                  returnKeyType="search"
                />
                <TouchableOpacity style={styles.searchButton} onPress={handleSearch}>
                  <Search size={20} color={colors.textPrimary} />
                </TouchableOpacity>
              </View>

              {loading ? (
                <View style={styles.loading}>
                  <ActivityIndicator size="large" color={colors.primary} />
                </View>
              ) : searchResults.length > 0 ? (
                <FlatList
                  data={searchResults}
                  keyExtractor={(item) => item.id}
                  renderItem={renderSearchResult}
                  contentContainerStyle={styles.listContent}
                />
              ) : searchQuery ? (
                <View style={styles.empty}>
                  <UserX size={48} color={colors.textMuted} />
                  <Text style={styles.emptyText}>검색 결과가 없습니다</Text>
                </View>
              ) : (
                <View style={styles.empty}>
                  <Search size={48} color={colors.textMuted} />
                  <Text style={styles.emptyText}>닉네임을 검색해보세요</Text>
                </View>
              )}
            </View>
          )}

          {/* 받은 요청 탭 */}
          {activeTab === 'requests' && (
            <View style={styles.content}>
              {requests.length > 0 ? (
                <FlatList
                  data={requests}
                  keyExtractor={(item) => item.id}
                  renderItem={renderRequest}
                  contentContainerStyle={styles.listContent}
                />
              ) : (
                <View style={styles.empty}>
                  <UserPlus size={48} color={colors.textMuted} />
                  <Text style={styles.emptyText}>받은 친구 요청이 없습니다</Text>
                </View>
              )}
            </View>
          )}
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  container: {
    backgroundColor: colors.bgDark,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    maxHeight: '85%',
    minHeight: '60%',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  headerTitle: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  closeButton: {
    padding: spacing.xs,
  },
  tabContainer: {
    flexDirection: 'row',
    margin: spacing.md,
    backgroundColor: colors.bgMedium,
    borderRadius: 12,
    padding: 4,
  },
  tab: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.sm,
    borderRadius: 10,
    gap: spacing.xs,
  },
  tabActive: {
    backgroundColor: colors.bgLight,
  },
  tabText: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    fontWeight: '600',
  },
  tabTextActive: {
    color: colors.primary,
  },
  badge: {
    backgroundColor: colors.danger,
    borderRadius: 10,
    paddingHorizontal: 6,
    paddingVertical: 1,
    marginLeft: 4,
  },
  badgeText: {
    color: colors.textPrimary,
    fontSize: 10,
    fontWeight: 'bold',
  },
  content: {
    flex: 1,
  },
  searchContainer: {
    flexDirection: 'row',
    marginHorizontal: spacing.md,
    marginBottom: spacing.md,
    gap: spacing.sm,
  },
  searchInput: {
    flex: 1,
    backgroundColor: colors.bgMedium,
    borderRadius: 12,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontSize: fontSize.md,
    color: colors.textPrimary,
  },
  searchButton: {
    backgroundColor: colors.primary,
    borderRadius: 12,
    width: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  listContent: {
    padding: spacing.md,
    paddingTop: 0,
  },
  loading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.md,
    paddingBottom: 50,
  },
  emptyText: {
    color: colors.textMuted,
    fontSize: fontSize.md,
  },
  resultItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgMedium,
    padding: spacing.md,
    borderRadius: 12,
    marginBottom: spacing.sm,
  },
  userAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primary + '30',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  avatarText: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.primary,
  },
  userInfo: {
    flex: 1,
  },
  userName: {
    fontSize: fontSize.md,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  userLevel: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    marginTop: 2,
  },
  addButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    backgroundColor: colors.bgLight,
    borderRadius: 12,
  },
  statusText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  requestItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgMedium,
    padding: spacing.md,
    borderRadius: 12,
    marginBottom: spacing.sm,
  },
  requestInfo: {
    flex: 1,
  },
  requestHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  requestMessage: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: 4,
    fontStyle: 'italic',
  },
  requestActions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  acceptButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.success,
    alignItems: 'center',
    justifyContent: 'center',
  },
  rejectButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.bgLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
