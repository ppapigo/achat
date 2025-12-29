import { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TextInput,
  TouchableOpacity,
  FlatList,
  Alert,
} from 'react-native';
import { X, Users, Check, MessageCircle } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

// Mock 친구 목록 (실제로는 props로 받거나 store에서)
const MOCK_FRIENDS = [
  { id: '1', name: '철수', level: 15, status: 'online' },
  { id: '2', name: '영희', level: 12, status: 'online' },
  { id: '3', name: '민수', level: 20, status: 'away' },
  { id: '4', name: '지현', level: 8, status: 'online' },
  { id: '5', name: '동욱', level: 18, status: 'offline' },
  { id: '6', name: '수진', level: 25, status: 'offline' },
  { id: '7', name: '준호', level: 11, status: 'online' },
  { id: '8', name: '미나', level: 14, status: 'away' },
];

const STATUS_COLORS = {
  online: colors.success,
  away: colors.warning,
  offline: colors.textMuted,
};

export default function CreatePartyModal({ visible, onClose, onPartyCreated, friends = MOCK_FRIENDS }) {
  const [partyName, setPartyName] = useState('');
  const [selectedFriends, setSelectedFriends] = useState(new Set());

  const handleToggleFriend = (friendId) => {
    setSelectedFriends(prev => {
      const newSet = new Set(prev);
      if (newSet.has(friendId)) {
        newSet.delete(friendId);
      } else {
        newSet.add(friendId);
      }
      return newSet;
    });
  };

  const handleSelectAll = () => {
    if (selectedFriends.size === friends.length) {
      setSelectedFriends(new Set());
    } else {
      setSelectedFriends(new Set(friends.map(f => f.id)));
    }
  };

  const handleCreate = () => {
    if (!partyName.trim()) {
      Alert.alert('오류', '파티 이름을 입력해주세요');
      return;
    }
    if (selectedFriends.size === 0) {
      Alert.alert('오류', '최소 1명의 친구를 선택해주세요');
      return;
    }

    const selectedFriendsList = friends.filter(f => selectedFriends.has(f.id));
    const newParty = {
      id: Date.now().toString(),
      name: partyName,
      memberCount: selectedFriends.size + 1, // +1 for self
      lastMessage: '파티가 생성되었습니다',
      lastMessageTime: '방금',
      unreadCount: 0,
      muted: false,
      members: selectedFriendsList,
    };

    onPartyCreated?.(newParty);
    
    // Reset
    setPartyName('');
    setSelectedFriends(new Set());
    onClose();

    Alert.alert('완료', `'${partyName}' 파티가 생성되었습니다!`);
  };

  const handleClose = () => {
    setPartyName('');
    setSelectedFriends(new Set());
    onClose();
  };

  const renderFriend = ({ item }) => {
    const isSelected = selectedFriends.has(item.id);

    return (
      <TouchableOpacity
        style={[styles.friendItem, isSelected && styles.friendItemSelected]}
        onPress={() => handleToggleFriend(item.id)}
        activeOpacity={0.7}
      >
        <View style={styles.friendAvatarContainer}>
          <View style={styles.friendAvatar}>
            <Text style={styles.avatarText}>{item.name[0]}</Text>
          </View>
          <View style={[styles.statusDot, { backgroundColor: STATUS_COLORS[item.status] }]} />
        </View>
        <View style={styles.friendInfo}>
          <Text style={styles.friendName}>{item.name}</Text>
          <Text style={styles.friendLevel}>Lv.{item.level}</Text>
        </View>
        <View style={[styles.checkbox, isSelected && styles.checkboxSelected]}>
          {isSelected && <Check size={16} color={colors.textPrimary} />}
        </View>
      </TouchableOpacity>
    );
  };

  // 온라인 우선 정렬
  const sortedFriends = [...friends].sort((a, b) => {
    const order = { online: 0, away: 1, offline: 2 };
    return order[a.status] - order[b.status];
  });

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent={true}
      onRequestClose={handleClose}
    >
      <View style={styles.overlay}>
        <View style={styles.container}>
          {/* 헤더 */}
          <View style={styles.header}>
            <Text style={styles.headerTitle}>파티 만들기</Text>
            <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
              <X size={24} color={colors.textPrimary} />
            </TouchableOpacity>
          </View>

          {/* 파티 이름 입력 */}
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>파티 이름</Text>
            <TextInput
              style={styles.nameInput}
              placeholder="파티 이름을 입력하세요"
              placeholderTextColor={colors.textMuted}
              value={partyName}
              onChangeText={setPartyName}
              maxLength={20}
            />
          </View>

          {/* 친구 선택 */}
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionLabel}>
                친구 초대 ({selectedFriends.size}명 선택)
              </Text>
              <TouchableOpacity onPress={handleSelectAll}>
                <Text style={styles.selectAllText}>
                  {selectedFriends.size === friends.length ? '전체 해제' : '전체 선택'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* 친구 목록 */}
          <FlatList
            data={sortedFriends}
            keyExtractor={(item) => item.id}
            renderItem={renderFriend}
            contentContainerStyle={styles.listContent}
            ListEmptyComponent={
              <View style={styles.empty}>
                <Users size={48} color={colors.textMuted} />
                <Text style={styles.emptyText}>친구가 없습니다</Text>
              </View>
            }
          />

          {/* 생성 버튼 */}
          <View style={styles.footer}>
            <TouchableOpacity
              style={[
                styles.createButton,
                (!partyName.trim() || selectedFriends.size === 0) && styles.createButtonDisabled
              ]}
              onPress={handleCreate}
              disabled={!partyName.trim() || selectedFriends.size === 0}
              activeOpacity={0.8}
            >
              <MessageCircle size={20} color={colors.textPrimary} />
              <Text style={styles.createButtonText}>
                파티 만들기 ({selectedFriends.size + 1}명)
              </Text>
            </TouchableOpacity>
          </View>
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
    maxHeight: '90%',
    minHeight: '70%',
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
  section: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  sectionLabel: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    fontWeight: '600',
    marginBottom: spacing.sm,
  },
  selectAllText: {
    fontSize: fontSize.sm,
    color: colors.primary,
    fontWeight: '600',
  },
  nameInput: {
    backgroundColor: colors.bgMedium,
    borderRadius: 12,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    fontSize: fontSize.md,
    color: colors.textPrimary,
  },
  listContent: {
    padding: spacing.md,
    paddingTop: spacing.sm,
  },
  friendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgMedium,
    padding: spacing.md,
    borderRadius: 12,
    marginBottom: spacing.sm,
  },
  friendItemSelected: {
    backgroundColor: colors.primary + '20',
    borderWidth: 1,
    borderColor: colors.primary,
  },
  friendAvatarContainer: {
    position: 'relative',
    marginRight: spacing.md,
  },
  friendAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.primary + '30',
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.primary,
  },
  statusDot: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 12,
    height: 12,
    borderRadius: 6,
    borderWidth: 2,
    borderColor: colors.bgMedium,
  },
  friendInfo: {
    flex: 1,
  },
  friendName: {
    fontSize: fontSize.md,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  friendLevel: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    marginTop: 2,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: colors.textMuted,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  empty: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 50,
    gap: spacing.md,
  },
  emptyText: {
    color: colors.textMuted,
    fontSize: fontSize.md,
  },
  footer: {
    padding: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  createButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    backgroundColor: colors.primary,
    paddingVertical: spacing.md,
    borderRadius: 12,
  },
  createButtonDisabled: {
    opacity: 0.5,
  },
  createButtonText: {
    color: colors.textPrimary,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
});
