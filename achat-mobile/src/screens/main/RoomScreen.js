import { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { colors, spacing, fontSize } from '../../styles/theme';
import { useRoomStore } from '../../store/roomStore';
import { useWebSocket, useChatRoom } from '../../hooks/useWebSocket';
import AugmentPickerModal from '../../components/room/AugmentPickerModal';

// Mock 증강 데이터 (나중에 API로 대체)
const MOCK_AUGMENTS = [
  { id: '1', type: 'REVERSE', description: '상대 메시지가 거꾸로 보입니다' },
  { id: '2', type: 'TURBO_CASTER', description: '메시지 쿨타임 40% 감소' },
  { id: '3', type: 'SHIELD', description: '이번 라운드 디버프 무효화' },
];

// 직업 아이콘
const JOB_ICONS = {
  FREELANCER: '🌀',
  GUARDIAN: '🛡️',
  ARCHITECT: '🏗️',
  CONNECTOR: '🔗',
  MAVEN: '📚',
};

// 증강 스타일
const AUGMENT_STYLES = {
  REVERSE: { icon: '🔄', color: '#e74c3c', name: '역전' },
  TURBO_CASTER: { icon: '⚡', color: '#f39c12', name: '터보' },
  TIME_SHIFTER: { icon: '⏰', color: '#3498db', name: '타임시프트' },
  SHIELD: { icon: '🛡️', color: '#3498db', name: '방어' },
  AMPLIFY: { icon: '📢', color: '#9b59b6', name: '증폭' },
  STEALTH: { icon: '👻', color: '#1abc9c', name: '은신' },
  MIRROR: { icon: '🪞', color: '#e91e63', name: '미러' },
  FREEZE: { icon: '❄️', color: '#00bcd4', name: '빙결' },
  QUEST_GIVER: { icon: '📜', color: '#ff9800', name: '퀘스트' },
  CHAOS: { icon: '🌀', color: '#ff5722', name: '혼돈' },
};

// 메시지 컴포넌트
function MessageItem({ message, onLike, onReply, myUserId }) {
  if (message.type === 'system') {
    return (
      <View style={styles.systemMessage}>
        <Text style={styles.systemMessageText}>{message.content}</Text>
      </View>
    );
  }

  const isMe = message.senderId === myUserId;
  const augmentStyle = message.augmentType ? AUGMENT_STYLES[message.augmentType] : null;
  
  return (
    <View style={[styles.messageContainer, isMe && styles.messageContainerMe]}>
      {!isMe && (
        <View style={styles.messageHeader}>
          <Text style={styles.nickname}>
            {message.senderTitle && <Text style={styles.title}>[{message.senderTitle}] </Text>}
            {message.nickname}
          </Text>
          {augmentStyle && (
            <View style={[styles.augmentBadge, { backgroundColor: augmentStyle.color + '30' }]}>
              <Text style={styles.augmentBadgeText}>
                {augmentStyle.icon} {augmentStyle.name}
              </Text>
            </View>
          )}
        </View>
      )}
      
      {/* 답글 미리보기 */}
      {message.replyToPreview && (
        <View style={styles.replyPreview}>
          <Text style={styles.replyPreviewText}>↩️ {message.replyToPreview}</Text>
        </View>
      )}
      
      <TouchableOpacity 
        style={[styles.messageBubble, isMe && styles.messageBubbleMe]}
        onLongPress={() => onReply?.(message)}
        activeOpacity={0.8}
      >
        <Text style={[styles.messageText, isMe && styles.messageTextMe]}>
          {message.appliedContent || message.content}
        </Text>
      </TouchableOpacity>
      
      {/* 좋아요 버튼 */}
      <View style={[styles.messageActions, isMe && styles.messageActionsMe]}>
        <TouchableOpacity 
          style={styles.likeButton}
          onPress={() => onLike?.(message.id)}
        >
          <Text style={styles.likeText}>
            ❤️ {message.likeCount || 0}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

// 라운드 타이머 컴포넌트
function RoundTimer({ currentRound, totalRounds, timeLeft, roomType, activeAugment }) {
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const isLowTime = timeLeft < 60;
  const augmentStyle = activeAugment ? AUGMENT_STYLES[activeAugment.type] : null;

  return (
    <View style={styles.timerContainer}>
      <View style={styles.roundInfo}>
        <Text style={styles.roundText}>
          {roomType === 'QUICK' ? `라운드 ${currentRound}/${totalRounds}` : `라운드 ${currentRound}`}
        </Text>
        {roomType === 'SEASON' && (
          <View style={styles.seasonBadge}>
            <Text style={styles.seasonBadgeText}>시즌</Text>
          </View>
        )}
        {activeAugment && augmentStyle && (
          <View style={[styles.activeAugmentBadge, { backgroundColor: augmentStyle.color + '30' }]}>
            <Text style={styles.activeAugmentText}>
              {augmentStyle.icon} {augmentStyle.name}
            </Text>
          </View>
        )}
      </View>
      <View style={[styles.timerBox, isLowTime && styles.timerBoxLow]}>
        <Text style={styles.timerIcon}>⏱️</Text>
        <Text style={[styles.timerText, isLowTime && styles.timerTextLow]}>
          {formatTime(timeLeft)}
        </Text>
      </View>
    </View>
  );
}

// 참가자 표시
function ParticipantCount({ count, max }) {
  return (
    <View style={styles.participantContainer}>
      <Text style={styles.participantText}>👥 {count}{max ? `/${max}` : '명'}</Text>
    </View>
  );
}

// 쿨타임 알림
function CooldownNotice({ cooldown }) {
  if (!cooldown) return null;
  
  return (
    <View style={styles.cooldownNotice}>
      <Text style={styles.cooldownText}>
        ⏳ {Math.ceil(cooldown.remainingMs / 1000)}초 후 전송 가능
      </Text>
    </View>
  );
}

// 메인 채팅방 화면
export default function RoomScreen({ route, navigation }) {
  const { roomId, roomType = 'SEASON' } = route.params;
  const { currentRoom, leaveRoom } = useRoomStore();
  
  // WebSocket 연결
  const { isConnected } = useWebSocket();
  const { 
    messages, 
    participants, 
    cooldown, 
    error,
    sendMessage, 
    toggleLike,
    addSystemMessage 
  } = useChatRoom(roomId);
  
  const [inputText, setInputText] = useState('');
  const [currentRound, setCurrentRound] = useState(1);
  const [timeLeft, setTimeLeft] = useState(600);
  const [replyTo, setReplyTo] = useState(null);
  
  // 증강 관련 상태
  const [showAugmentPicker, setShowAugmentPicker] = useState(false);
  const [activeAugment, setActiveAugment] = useState(null);
  const [augmentOptions, setAugmentOptions] = useState(MOCK_AUGMENTS);
  
  const flatListRef = useRef(null);
  const myUserId = 'me'; // TODO: authStore에서 가져오기

  // 에러 표시
  useEffect(() => {
    if (error) {
      Alert.alert('알림', error.message || '오류가 발생했습니다');
    }
  }, [error]);

  // 라운드 시작 시 증강 선택 모달 표시
  useEffect(() => {
    const timer = setTimeout(() => {
      setShowAugmentPicker(true);
    }, 1000);
    
    return () => clearTimeout(timer);
  }, [currentRound]);

  // 타이머
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 0) {
          setCurrentRound((r) => r + 1);
          setActiveAugment(null);
          return 600;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  // 증강 선택 핸들러
  const handleAugmentSelect = (augment) => {
    setActiveAugment(augment);
    setShowAugmentPicker(false);
    
    const augmentStyle = AUGMENT_STYLES[augment.type];
    addSystemMessage(`✨ ${augmentStyle.icon} [${augmentStyle.name}] 증강이 활성화되었습니다!`);
  };

  // 메시지 전송
  const handleSend = () => {
    if (!inputText.trim()) return;
    if (!activeAugment) return;
    if (!isConnected) {
      Alert.alert('연결 오류', '서버에 연결되어 있지 않습니다');
      return;
    }

    const success = sendMessage(inputText.trim(), replyTo?.id);
    if (success) {
      setInputText('');
      setReplyTo(null);
    }
    
    setTimeout(() => {
      flatListRef.current?.scrollToEnd({ animated: true });
    }, 100);
  };

  // 답글 설정
  const handleReply = (message) => {
    setReplyTo(message);
  };

  // 방 나가기
  const handleLeave = async () => {
    await leaveRoom();
    navigation.goBack();
  };

  const roomName = currentRoom?.name || '채팅방';
  const participantCount = participants.length || currentRoom?.currentPlayers || 0;
  const maxParticipants = roomType === 'QUICK' ? (currentRoom?.maxPlayers || 6) : null;
  const totalRounds = roomType === 'QUICK' ? 3 : null;

  const inputDisabled = !activeAugment;

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      {/* 연결 상태 표시 */}
      {!isConnected && (
        <View style={styles.connectionBanner}>
          <Text style={styles.connectionText}>🔄 연결 중...</Text>
        </View>
      )}

      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={handleLeave} style={styles.backButton}>
          <Text style={styles.backText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.roomName} numberOfLines={1}>{roomName}</Text>
        <ParticipantCount count={participantCount} max={maxParticipants} />
      </View>

      {/* 라운드 타이머 */}
      <RoundTimer
        currentRound={currentRound}
        totalRounds={totalRounds}
        timeLeft={timeLeft}
        roomType={roomType}
        activeAugment={activeAugment}
      />

      {/* 쿨타임 알림 */}
      <CooldownNotice cooldown={cooldown} />

      {/* 메시지 목록 */}
      <FlatList
        ref={flatListRef}
        data={messages}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <MessageItem 
            message={item} 
            onLike={toggleLike}
            onReply={handleReply}
            myUserId={myUserId}
          />
        )}
        contentContainerStyle={styles.messageList}
        onContentSizeChange={() => {
          flatListRef.current?.scrollToEnd({ animated: false });
        }}
      />

      {/* 답글 표시 */}
      {replyTo && (
        <View style={styles.replyBar}>
          <Text style={styles.replyBarText} numberOfLines={1}>
            ↩️ {replyTo.nickname}: {replyTo.content}
          </Text>
          <TouchableOpacity onPress={() => setReplyTo(null)}>
            <Text style={styles.replyBarClose}>✕</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* 입력창 */}
      <View style={styles.inputContainer}>
        {inputDisabled ? (
          <TouchableOpacity 
            style={styles.selectAugmentHint}
            onPress={() => setShowAugmentPicker(true)}
          >
            <Text style={styles.selectAugmentHintText}>
              ✨ 증강을 선택해주세요
            </Text>
          </TouchableOpacity>
        ) : (
          <>
            <TextInput
              style={styles.textInput}
              value={inputText}
              onChangeText={setInputText}
              placeholder="메시지 입력..."
              placeholderTextColor={colors.textMuted}
              multiline
              maxLength={500}
            />
            <TouchableOpacity
              style={[styles.sendButton, !inputText.trim() && styles.sendButtonDisabled]}
              onPress={handleSend}
              disabled={!inputText.trim()}
            >
              <Text style={styles.sendButtonText}>전송</Text>
            </TouchableOpacity>
          </>
        )}
      </View>

      {/* 증강 선택 모달 */}
      <AugmentPickerModal
        visible={showAugmentPicker}
        augments={augmentOptions}
        onSelect={handleAugmentSelect}
        onTimeout={handleAugmentSelect}
        timeLimit={15}
      />
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },

  // 연결 상태 배너
  connectionBanner: {
    backgroundColor: colors.warning,
    padding: spacing.sm,
    alignItems: 'center',
  },
  connectionText: {
    color: colors.bgDark,
    fontSize: fontSize.sm,
    fontWeight: 'bold',
  },

  // 헤더
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    paddingTop: spacing.xl,
    backgroundColor: colors.bgMedium,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  backButton: {
    padding: spacing.sm,
    marginRight: spacing.sm,
  },
  backText: {
    color: colors.primary,
    fontSize: fontSize.xl,
    fontWeight: 'bold',
  },
  roomName: {
    flex: 1,
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  participantContainer: {
    backgroundColor: colors.bgLight,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: 12,
  },
  participantText: {
    color: colors.accent,
    fontSize: fontSize.sm,
    fontWeight: '600',
  },

  // 라운드 타이머
  timerContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.md,
    backgroundColor: colors.bgMedium,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  roundInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    flex: 1,
    flexWrap: 'wrap',
  },
  roundText: {
    color: colors.textPrimary,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  seasonBadge: {
    backgroundColor: colors.warning,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderRadius: 8,
  },
  seasonBadgeText: {
    color: colors.bgDark,
    fontSize: fontSize.xs,
    fontWeight: 'bold',
  },
  activeAugmentBadge: {
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderRadius: 8,
  },
  activeAugmentText: {
    color: colors.textPrimary,
    fontSize: fontSize.xs,
    fontWeight: '600',
  },
  timerBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgLight,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: 12,
    gap: spacing.xs,
  },
  timerBoxLow: {
    backgroundColor: colors.danger + '30',
  },
  timerIcon: {
    fontSize: fontSize.md,
  },
  timerText: {
    color: colors.textPrimary,
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    fontVariant: ['tabular-nums'],
  },
  timerTextLow: {
    color: colors.danger,
  },

  // 쿨타임 알림
  cooldownNotice: {
    backgroundColor: colors.warning + '30',
    padding: spacing.sm,
    alignItems: 'center',
  },
  cooldownText: {
    color: colors.warning,
    fontSize: fontSize.sm,
  },

  // 메시지 목록
  messageList: {
    padding: spacing.md,
    paddingBottom: spacing.lg,
  },

  // 시스템 메시지
  systemMessage: {
    alignItems: 'center',
    marginVertical: spacing.sm,
  },
  systemMessageText: {
    color: colors.augment,
    fontSize: fontSize.sm,
    backgroundColor: colors.augment + '20',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderRadius: 12,
    overflow: 'hidden',
  },

  // 유저 메시지
  messageContainer: {
    marginVertical: spacing.xs,
    maxWidth: '80%',
  },
  messageContainerMe: {
    alignSelf: 'flex-end',
  },
  messageHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.xs,
    gap: spacing.xs,
    flexWrap: 'wrap',
  },
  nickname: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
    fontWeight: '600',
  },
  title: {
    color: colors.warning,
  },
  augmentBadge: {
    paddingHorizontal: spacing.xs,
    paddingVertical: 1,
    borderRadius: 6,
  },
  augmentBadgeText: {
    fontSize: fontSize.xs,
    color: colors.textPrimary,
  },
  replyPreview: {
    backgroundColor: colors.bgLight,
    padding: spacing.xs,
    borderRadius: 6,
    marginBottom: spacing.xs,
    borderLeftWidth: 2,
    borderLeftColor: colors.primary,
  },
  replyPreviewText: {
    color: colors.textMuted,
    fontSize: fontSize.xs,
  },
  messageBubble: {
    backgroundColor: colors.bgLight,
    padding: spacing.md,
    borderRadius: 16,
    borderTopLeftRadius: 4,
  },
  messageBubbleMe: {
    backgroundColor: colors.primary,
    borderTopLeftRadius: 16,
    borderTopRightRadius: 4,
  },
  messageText: {
    color: colors.textPrimary,
    fontSize: fontSize.md,
    lineHeight: 22,
  },
  messageTextMe: {
    color: colors.textPrimary,
  },
  messageActions: {
    flexDirection: 'row',
    marginTop: spacing.xs,
  },
  messageActionsMe: {
    justifyContent: 'flex-end',
  },
  likeButton: {
    padding: spacing.xs,
  },
  likeText: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },

  // 답글 바
  replyBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgLight,
    padding: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  replyBarText: {
    flex: 1,
    color: colors.textSecondary,
    fontSize: fontSize.sm,
  },
  replyBarClose: {
    color: colors.textMuted,
    fontSize: fontSize.lg,
    paddingHorizontal: spacing.sm,
  },

  // 입력창
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: spacing.md,
    backgroundColor: colors.bgMedium,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    gap: spacing.sm,
  },
  textInput: {
    flex: 1,
    backgroundColor: colors.bgLight,
    borderRadius: 20,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontSize: fontSize.md,
    color: colors.textPrimary,
    maxHeight: 100,
  },
  sendButton: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: 20,
    justifyContent: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: colors.textMuted,
  },
  sendButtonText: {
    color: colors.textPrimary,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  selectAugmentHint: {
    flex: 1,
    backgroundColor: colors.augment + '20',
    borderRadius: 20,
    paddingVertical: spacing.md,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.augment,
    borderStyle: 'dashed',
  },
  selectAugmentHintText: {
    color: colors.augment,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
});
