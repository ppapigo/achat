import { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
} from 'react-native';
import { Users, Plus, MessageCircle, BellOff, UserPlus, Circle } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';
import AddFriendModal from '../../components/party/AddFriendModal';
import CreatePartyModal from '../../components/party/CreatePartyModal';

// Mock 파티(채팅방) 데이터
const MOCK_PARTIES = [
  {
    id: '1',
    name: '불꽃 파티',
    lastMessage: '오늘 저녁에 한판?',
    lastMessageTime: '방금',
    memberCount: 4,
    unreadCount: 3,
    muted: false,
  },
  {
    id: '2',
    name: '증강 연구팀',
    lastMessage: '역전 증강 개사기임 ㅋㅋ',
    lastMessageTime: '10분 전',
    memberCount: 6,
    unreadCount: 0,
    muted: false,
  },
  {
    id: '3',
    name: '랭크 듀오',
    lastMessage: '사진을 보냈습니다',
    lastMessageTime: '1시간 전',
    memberCount: 2,
    unreadCount: 0,
    muted: true,
  },
  {
    id: '4',
    name: '대학 동기들',
    lastMessage: '다음주 번개 ㄱ?',
    lastMessageTime: '어제',
    memberCount: 8,
    unreadCount: 12,
    muted: false,
  },
];

// Mock 친구 목록
const MOCK_FRIENDS = [
  { id: '1', name: '철수', level: 15, status: 'online', statusMessage: '게임 중 🎮' },
  { id: '2', name: '영희', level: 12, status: 'online', statusMessage: '증강 연구 중...' },
  { id: '3', name: '민수', level: 20, status: 'away', statusMessage: '잠깐 자리비움' },
  { id: '4', name: '지현', level: 8, status: 'online', statusMessage: '' },
  { id: '5', name: '동욱', level: 18, status: 'offline', statusMessage: '내일 봐요~' },
  { id: '6', name: '수진', level: 25, status: 'offline', statusMessage: '' },
  { id: '7', name: '준호', level: 11, status: 'online', statusMessage: '랭크 ㄱㄱ' },
  { id: '8', name: '미나', level: 14, status: 'away', statusMessage: '밥 먹는 중' },
];

const STATUS_COLORS = {
  online: colors.success,
  away: colors.warning,
  offline: colors.textMuted,
};

export default function PartyScreen({ navigation }) {
  const [activeTab, setActiveTab] = useState('chat'); // 'chat' | 'friends'
  const [parties, setParties] = useState(MOCK_PARTIES);
  const [friends, setFriends] = useState(MOCK_FRIENDS);
  const [refreshing, setRefreshing] = useState(false);
  const [showAddFriend, setShowAddFriend] = useState(false);
  const [showCreateParty, setShowCreateParty] = useState(false);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    setTimeout(() => setRefreshing(false), 1000);
  }, []);

  const handlePartyPress = (party) => {
    navigation.navigate('Room', { 
      roomId: party.id, 
      roomName: party.name,
      isParty: true 
    });
  };

  const handleFriendPress = (friend) => {
    // 1:1 채팅으로 이동
    navigation.navigate('Room', {
      roomId: `dm_${friend.id}`,
      roomName: friend.name,
      isDM: true,
      friend: friend,
    });
  };

  const handleStartDM = (friend) => {
    navigation.navigate('Room', {
      roomId: `dm_${friend.id}`,
      roomName: friend.name,
      isDM: true,
      friend: friend,
    });
  };

  // 채팅방 아이템
  const renderParty = ({ item }) => (
    <TouchableOpacity
      style={styles.partyItem}
      onPress={() => handlePartyPress(item)}
      activeOpacity={0.7}
    >
      <View style={styles.avatarContainer}>
        <View style={styles.avatar}>
          <Users size={24} color={colors.textSecondary} />
        </View>
        {item.unreadCount > 0 && (
          <View style={styles.unreadBadge}>
            <Text style={styles.unreadText}>
              {item.unreadCount > 99 ? '99+' : item.unreadCount}
            </Text>
          </View>
        )}
      </View>
      <View style={styles.partyInfo}>
        <View style={styles.partyHeader}>
          <View style={styles.partyNameRow}>
            <Text style={styles.partyName} numberOfLines={1}>{item.name}</Text>
            <Text style={styles.memberCount}>{item.memberCount}</Text>
          </View>
          <Text style={styles.lastTime}>{item.lastMessageTime}</Text>
        </View>
        <View style={styles.partyFooter}>
          <Text 
            style={[styles.lastMessage, item.unreadCount > 0 && styles.lastMessageUnread]} 
            numberOfLines={1}
          >
            {item.lastMessage}
          </Text>
          {item.muted && <BellOff size={14} color={colors.textMuted} />}
        </View>
      </View>
    </TouchableOpacity>
  );

  // 친구 아이템
  const renderFriend = ({ item }) => (
    <TouchableOpacity
      style={styles.friendItem}
      onPress={() => handleFriendPress(item)}
      activeOpacity={0.7}
    >
      <View style={styles.friendAvatarContainer}>
        <View style={styles.friendAvatar}>
          <Text style={styles.friendAvatarText}>{item.name[0]}</Text>
        </View>
        <View style={[styles.statusDot, { backgroundColor: STATUS_COLORS[item.status] }]} />
      </View>
      <View style={styles.friendInfo}>
        <View style={styles.friendNameRow}>
          <Text style={styles.friendName}>{item.name}</Text>
          <Text style={styles.friendLevel}>Lv.{item.level}</Text>
        </View>
        {item.statusMessage ? (
          <Text style={styles.statusMessage} numberOfLines={1}>{item.statusMessage}</Text>
        ) : (
          <Text style={styles.statusText}>
            {item.status === 'online' ? '온라인' : item.status === 'away' ? '자리비움' : '오프라인'}
          </Text>
        )}
      </View>
      <TouchableOpacity 
        style={styles.chatButton}
        onPress={() => handleStartDM(item)}
      >
        <MessageCircle size={20} color={colors.primary} />
      </TouchableOpacity>
    </TouchableOpacity>
  );

  const totalUnread = parties.reduce((sum, p) => sum + p.unreadCount, 0);
  const onlineCount = friends.filter(f => f.status === 'online').length;

  // 친구 목록 정렬: 온라인 > 자리비움 > 오프라인
  const sortedFriends = [...friends].sort((a, b) => {
    const order = { online: 0, away: 1, offline: 2 };
    return order[a.status] - order[b.status];
  });

  return (
    <View style={styles.container}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>파티</Text>
        <TouchableOpacity 
          style={styles.addButton} 
          activeOpacity={0.7}
          onPress={() => setShowCreateParty(true)}
        >
          <Plus size={24} color={colors.textPrimary} />
        </TouchableOpacity>
      </View>

      {/* 탭 전환 */}
      <View style={styles.tabContainer}>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'chat' && styles.tabActive]}
          onPress={() => setActiveTab('chat')}
        >
          <MessageCircle size={18} color={activeTab === 'chat' ? colors.primary : colors.textMuted} />
          <Text style={[styles.tabText, activeTab === 'chat' && styles.tabTextActive]}>
            채팅
          </Text>
          {totalUnread > 0 && (
            <View style={styles.tabBadge}>
              <Text style={styles.tabBadgeText}>{totalUnread}</Text>
            </View>
          )}
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'friends' && styles.tabActive]}
          onPress={() => setActiveTab('friends')}
        >
          <Users size={18} color={activeTab === 'friends' ? colors.primary : colors.textMuted} />
          <Text style={[styles.tabText, activeTab === 'friends' && styles.tabTextActive]}>
            친구
          </Text>
          <View style={styles.onlineBadge}>
            <Circle size={8} color={colors.success} fill={colors.success} />
            <Text style={styles.onlineText}>{onlineCount}</Text>
          </View>
        </TouchableOpacity>
      </View>

      {/* 채팅 목록 */}
      {activeTab === 'chat' && (
        <FlatList
          data={parties}
          keyExtractor={(item) => item.id}
          renderItem={renderParty}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />
          }
          ListEmptyComponent={
            <View style={styles.empty}>
              <MessageCircle size={48} color={colors.textMuted} />
              <Text style={styles.emptyText}>채팅방이 없습니다</Text>
            </View>
          }
        />
      )}

      {/* 친구 목록 */}
      {activeTab === 'friends' && (
        <>
          {/* 친구 추가 버튼 */}
          <TouchableOpacity 
            style={styles.addFriendButton} 
            activeOpacity={0.7}
            onPress={() => setShowAddFriend(true)}
          >
            <UserPlus size={20} color={colors.primary} />
            <Text style={styles.addFriendText}>친구 추가</Text>
          </TouchableOpacity>
          
          <FlatList
            data={sortedFriends}
            keyExtractor={(item) => item.id}
            renderItem={renderFriend}
            contentContainerStyle={styles.listContent}
            refreshControl={
              <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />
            }
            ListEmptyComponent={
              <View style={styles.empty}>
                <Users size={48} color={colors.textMuted} />
                <Text style={styles.emptyText}>친구가 없습니다</Text>
              </View>
            }
          />
        </>
      )}

      {/* 친구 추가 모달 */}
      <AddFriendModal
        visible={showAddFriend}
        onClose={() => setShowAddFriend(false)}
        onFriendAdded={(newFriend) => {
          setFriends(prev => [...prev, { ...newFriend, status: 'online', statusMessage: '' }]);
        }}
      />

      {/* 파티 생성 모달 */}
      <CreatePartyModal
        visible={showCreateParty}
        onClose={() => setShowCreateParty(false)}
        friends={friends}
        onPartyCreated={(newParty) => {
          setParties(prev => [newParty, ...prev]);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.md,
    paddingTop: spacing.lg,
  },
  headerTitle: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  addButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.bgLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tabContainer: {
    flexDirection: 'row',
    marginHorizontal: spacing.md,
    marginBottom: spacing.md,
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
  tabBadge: {
    backgroundColor: colors.danger,
    borderRadius: 10,
    paddingHorizontal: 6,
    paddingVertical: 1,
    marginLeft: 2,
  },
  tabBadgeText: {
    color: colors.textPrimary,
    fontSize: 10,
    fontWeight: 'bold',
  },
  onlineBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    marginLeft: 2,
  },
  onlineText: {
    color: colors.success,
    fontSize: 11,
    fontWeight: '600',
  },
  listContent: {
    flexGrow: 1,
  },
  // 채팅방 스타일
  partyItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  avatarContainer: {
    position: 'relative',
    marginRight: spacing.md,
  },
  avatar: {
    width: 52,
    height: 52,
    borderRadius: 16,
    backgroundColor: colors.bgMedium,
    alignItems: 'center',
    justifyContent: 'center',
  },
  unreadBadge: {
    position: 'absolute',
    top: -4,
    right: -4,
    backgroundColor: colors.danger,
    borderRadius: 10,
    minWidth: 20,
    height: 20,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
  },
  unreadText: {
    color: colors.textPrimary,
    fontSize: fontSize.xs,
    fontWeight: 'bold',
  },
  partyInfo: {
    flex: 1,
  },
  partyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  partyNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: spacing.sm,
  },
  partyName: {
    fontSize: fontSize.md,
    fontWeight: '600',
    color: colors.textPrimary,
    flex: 1,
  },
  memberCount: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    marginLeft: spacing.xs,
  },
  lastTime: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  partyFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  lastMessage: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    flex: 1,
    marginRight: spacing.sm,
  },
  lastMessageUnread: {
    color: colors.textPrimary,
    fontWeight: '500',
  },
  // 친구 스타일
  addFriendButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    marginHorizontal: spacing.md,
    marginBottom: spacing.md,
    padding: spacing.md,
    backgroundColor: colors.bgMedium,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.primary + '50',
    borderStyle: 'dashed',
  },
  addFriendText: {
    color: colors.primary,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  friendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  friendAvatarContainer: {
    position: 'relative',
    marginRight: spacing.md,
  },
  friendAvatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: colors.primary + '30',
    alignItems: 'center',
    justifyContent: 'center',
  },
  friendAvatarText: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.primary,
  },
  statusDot: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 14,
    height: 14,
    borderRadius: 7,
    borderWidth: 2,
    borderColor: colors.bgDark,
  },
  friendInfo: {
    flex: 1,
  },
  friendNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  friendName: {
    fontSize: fontSize.md,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  friendLevel: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    backgroundColor: colors.bgLight,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 8,
  },
  statusMessage: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: 2,
  },
  statusText: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    marginTop: 2,
  },
  chatButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primary + '20',
    alignItems: 'center',
    justifyContent: 'center',
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 100,
    gap: spacing.md,
  },
  emptyText: {
    color: colors.textMuted,
    fontSize: fontSize.md,
  },
});
