import { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  Modal,
  Alert,
} from 'react-native';
import { colors, spacing, fontSize } from '../../styles/theme';
import { useRoomStore } from '../../store/roomStore';
import { useAuthStore } from '../../store/authStore';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';

// 빠른매칭 인원 선택 옵션
const QUICK_MATCH_OPTIONS = [6, 8, 10];

// 시즌방 카드 컴포넌트
function SeasonRoomCard({ room, onJoin }) {
  const statusColors = {
    WAITING: colors.success,
    PLAYING: colors.warning,
    FINISHED: colors.textMuted,
  };
  
  const statusText = {
    WAITING: '대기중',
    PLAYING: '게임중',
    FINISHED: '종료됨',
  };

  return (
    <TouchableOpacity 
      style={styles.roomCard} 
      onPress={() => onJoin(room)}
      activeOpacity={0.8}
    >
      <View style={styles.roomHeader}>
        <View style={styles.roomTitleRow}>
          <Text style={styles.rankBadge}>#{room.rank || '-'}</Text>
          <Text style={styles.roomName} numberOfLines={1}>{room.name}</Text>
        </View>
        <View style={[styles.statusBadge, { backgroundColor: statusColors[room.status] }]}>
          <Text style={styles.statusText}>{statusText[room.status]}</Text>
        </View>
      </View>
      
      <View style={styles.roomInfo}>
        <Text style={styles.roomHost}>👤 {room.hostNickname}</Text>
        <Text style={styles.roomMeta}>
          👥 {room.currentPlayers}명 참여중
        </Text>
      </View>
      
      {room.subRoomCount > 0 && (
        <View style={styles.subRoomInfo}>
          <Text style={styles.subRoomText}>
            📁 서브방 {room.subRoomCount}개
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

// 시즌방 생성 모달
function CreateSeasonRoomModal({ visible, onClose, onCreate }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);

  const handleCreate = async () => {
    if (!name.trim()) {
      Alert.alert('입력 오류', '방 이름을 입력하세요');
      return;
    }

    setLoading(true);
    try {
      await onCreate({
        name: name.trim(),
        description: description.trim(),
        roomType: 'SEASON',
      });
      setName('');
      setDescription('');
      onClose();
    } catch (error) {
      Alert.alert('오류', error.message || '방 생성에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent={true}
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>🏆 시즌방 만들기</Text>
          
          <Input
            label="방 이름"
            placeholder="시즌방 이름을 지어주세요"
            value={name}
            onChangeText={setName}
          />
          
          <Input
            label="방 설명 (선택)"
            placeholder="방에 대한 설명을 적어주세요"
            value={description}
            onChangeText={setDescription}
            multiline
          />
          
          <View style={styles.modalButtons}>
            <Button
              title="취소"
              onPress={onClose}
              variant="outline"
              style={styles.modalButton}
            />
            <Button
              title="만들기"
              onPress={handleCreate}
              loading={loading}
              style={styles.modalButton}
            />
          </View>
        </View>
      </View>
    </Modal>
  );
}

// 빠른매칭 인원 선택 모달
function QuickMatchModal({ visible, onClose, onStart }) {
  const [selectedCount, setSelectedCount] = useState(6);
  const [loading, setLoading] = useState(false);

  const handleStart = async () => {
    setLoading(true);
    try {
      await onStart(selectedCount);
      onClose();
    } catch (error) {
      Alert.alert('오류', '매칭에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent={true}
      onRequestClose={onClose}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>⚡ 빠른매칭</Text>
          
          <View style={styles.matchInfo}>
            <Text style={styles.matchInfoItem}>⏱️ 10분 제한</Text>
            <Text style={styles.matchInfoItem}>🎯 라운드 3번</Text>
            <Text style={styles.matchInfoItem}>✨ 증강 3번</Text>
          </View>
          
          <Text style={styles.inputLabel}>인원 선택</Text>
          <View style={styles.countButtons}>
            {QUICK_MATCH_OPTIONS.map((count) => (
              <TouchableOpacity
                key={count}
                style={[
                  styles.countButton,
                  selectedCount === count && styles.countButtonActive,
                ]}
                onPress={() => setSelectedCount(count)}
              >
                <Text style={[
                  styles.countButtonText,
                  selectedCount === count && styles.countButtonTextActive,
                ]}>
                  {count}명
                </Text>
              </TouchableOpacity>
            ))}
          </View>
          
          <View style={styles.modalButtons}>
            <Button
              title="취소"
              onPress={onClose}
              variant="outline"
              style={styles.modalButton}
            />
            <Button
              title="매칭 시작"
              onPress={handleStart}
              loading={loading}
              style={styles.modalButton}
            />
          </View>
        </View>
      </View>
    </Modal>
  );
}

// 메인 홈 화면
export default function HomeScreen({ navigation }) {
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [quickMatchModalVisible, setQuickMatchModalVisible] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  
  const { rooms, isLoading, fetchRooms, createRoom, joinRoom, joinMatchQueue } = useRoomStore();
  const logout = useAuthStore((state) => state.logout);

  useEffect(() => {
    fetchRooms();
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await fetchRooms();
    setRefreshing(false);
  }, []);

  const handleJoinRoom = async (room) => {
    try {
      await joinRoom(room.id);
      navigation.navigate('Room', { roomId: room.id, roomType: 'SEASON' });
    } catch (error) {
      Alert.alert('오류', '방 참가에 실패했습니다');
    }
  };

  const handleCreateRoom = async (roomData) => {
    try {
      const newRoom = await createRoom(roomData);
      if (newRoom) {
        navigation.navigate('Room', { roomId: newRoom.id, roomType: roomData.roomType || 'SEASON' });
      }
    } catch (error) {
      Alert.alert('오류', error.message || '방 생성에 실패했습니다');
    }
  };

  const handleQuickMatch = async (playerCount) => {
    try {
      await joinMatchQueue(playerCount);
      navigation.navigate('Matching', { playerCount });
    } catch (error) {
      Alert.alert('오류', error.message || '매칭에 실패했습니다');
    }
  };

  return (
    <View style={styles.container}>
      {/* 헤더 */}
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Text style={styles.headerTitle}>🎮 A.chat</Text>
        </View>
        
        {/* 빠른매칭 버튼 */}
        <TouchableOpacity 
          style={styles.quickMatchButton}
          onPress={() => setQuickMatchModalVisible(true)}
        >
          <Text style={styles.quickMatchText}>⚡ 빠른매칭</Text>
        </TouchableOpacity>
        
        <TouchableOpacity onPress={logout} style={styles.logoutButton}>
          <Text style={styles.logoutText}>로그아웃</Text>
        </TouchableOpacity>
      </View>

      {/* 시즌방 섹션 헤더 */}
      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>🏆 시즌방</Text>
        <Text style={styles.sectionSubtitle}>상위 10개 방은 시즌 종료 후에도 유지됩니다</Text>
      </View>

      {/* 시즌방 목록 */}
      <FlatList
        data={rooms}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <SeasonRoomCard room={item} onJoin={handleJoinRoom} />
        )}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
          />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyIcon}>🏆</Text>
            <Text style={styles.emptyText}>아직 시즌방이 없어요</Text>
            <Text style={styles.emptySubtext}>첫 번째 시즌방을 만들어보세요!</Text>
          </View>
        }
      />

      {/* 시즌방 만들기 버튼 */}
      <TouchableOpacity
        style={styles.fab}
        onPress={() => setCreateModalVisible(true)}
        activeOpacity={0.8}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>

      {/* 시즌방 생성 모달 */}
      <CreateSeasonRoomModal
        visible={createModalVisible}
        onClose={() => setCreateModalVisible(false)}
        onCreate={handleCreateRoom}
      />

      {/* 빠른매칭 모달 */}
      <QuickMatchModal
        visible={quickMatchModalVisible}
        onClose={() => setQuickMatchModalVisible(false)}
        onStart={handleQuickMatch}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },
  
  // 헤더
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.md,
    paddingTop: spacing.xl,
    backgroundColor: colors.bgMedium,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  headerLeft: {
    flex: 1,
  },
  headerTitle: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.primary,
  },
  quickMatchButton: {
    backgroundColor: colors.accent,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: 20,
    marginRight: spacing.sm,
  },
  quickMatchText: {
    color: colors.bgDark,
    fontWeight: 'bold',
    fontSize: fontSize.sm,
  },
  logoutButton: {
    padding: spacing.sm,
  },
  logoutText: {
    color: colors.textMuted,
    fontSize: fontSize.sm,
  },
  
  // 섹션 헤더
  sectionHeader: {
    padding: spacing.md,
    paddingBottom: spacing.sm,
  },
  sectionTitle: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  sectionSubtitle: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    marginTop: spacing.xs,
  },
  
  // 방 목록
  listContent: {
    padding: spacing.md,
    paddingTop: 0,
    paddingBottom: 100,
  },
  
  // 시즌방 카드
  roomCard: {
    backgroundColor: colors.bgMedium,
    borderRadius: 16,
    padding: spacing.md,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  roomHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  roomTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    gap: spacing.sm,
  },
  rankBadge: {
    backgroundColor: colors.warning,
    color: colors.bgDark,
    fontWeight: 'bold',
    fontSize: fontSize.xs,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderRadius: 8,
    overflow: 'hidden',
  },
  roomName: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.textPrimary,
    flex: 1,
  },
  statusBadge: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: 8,
  },
  statusText: {
    color: colors.bgDark,
    fontSize: fontSize.xs,
    fontWeight: 'bold',
  },
  roomInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  roomHost: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
  },
  roomMeta: {
    color: colors.accent,
    fontSize: fontSize.sm,
    fontWeight: '600',
  },
  subRoomInfo: {
    marginTop: spacing.sm,
    paddingTop: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  subRoomText: {
    color: colors.textMuted,
    fontSize: fontSize.sm,
  },
  
  // Empty state
  emptyContainer: {
    alignItems: 'center',
    paddingVertical: spacing.xl * 2,
  },
  emptyIcon: {
    fontSize: 64,
    marginBottom: spacing.md,
  },
  emptyText: {
    fontSize: fontSize.lg,
    color: colors.textPrimary,
    fontWeight: 'bold',
  },
  emptySubtext: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    marginTop: spacing.xs,
  },
  
  // FAB
  fab: {
    position: 'absolute',
    right: spacing.lg,
    bottom: spacing.lg,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 5,
    shadowColor: colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  fabDisabled: {
    backgroundColor: colors.textMuted,
  },
  fabText: {
    fontSize: 32,
    color: colors.textPrimary,
    fontWeight: '300',
  },
  
  // 모달 공통
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: colors.bgMedium,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: spacing.lg,
    paddingBottom: spacing.xl,
  },
  modalTitle: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  modalButtons: {
    flexDirection: 'row',
    gap: spacing.md,
    marginTop: spacing.lg,
  },
  modalButton: {
    flex: 1,
  },
  inputLabel: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
    marginBottom: spacing.sm,
  },
  
  // 빠른매칭 모달
  matchInfo: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: colors.bgLight,
    borderRadius: 12,
    padding: spacing.md,
    marginBottom: spacing.lg,
  },
  matchInfoItem: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
  },
  countButtons: {
    flexDirection: 'row',
    gap: spacing.md,
    marginBottom: spacing.md,
  },
  countButton: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.md,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: colors.border,
  },
  countButtonActive: {
    borderColor: colors.accent,
    backgroundColor: colors.accent + '20',
  },
  countButtonText: {
    color: colors.textSecondary,
    fontSize: fontSize.lg,
    fontWeight: 'bold',
  },
  countButtonTextActive: {
    color: colors.accent,
  },
  
  // 시즌방 생성 모달
  limitWarning: {
    backgroundColor: colors.danger + '20',
    borderRadius: 12,
    padding: spacing.lg,
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  limitWarningText: {
    color: colors.danger,
    fontSize: fontSize.md,
    fontWeight: 'bold',
  },
  limitWarningSubtext: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
    marginTop: spacing.xs,
  },
  remainingText: {
    color: colors.success,
    fontSize: fontSize.sm,
    textAlign: 'center',
    marginBottom: spacing.md,
  },
});
