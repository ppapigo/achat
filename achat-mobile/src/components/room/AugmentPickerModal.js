import { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TouchableOpacity,
  Animated,
} from 'react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

// 증강 타입별 아이콘/색상
const AUGMENT_STYLES = {
  REVERSE: { icon: '🔄', color: '#e74c3c', name: '역전' },
  SPEED: { icon: '⚡', color: '#f39c12', name: '가속' },
  SHIELD: { icon: '🛡️', color: '#3498db', name: '방어' },
  AMPLIFY: { icon: '📢', color: '#9b59b6', name: '증폭' },
  STEALTH: { icon: '👻', color: '#1abc9c', name: '은신' },
  MIRROR: { icon: '🪞', color: '#e91e63', name: '미러' },
  FREEZE: { icon: '❄️', color: '#00bcd4', name: '빙결' },
  CHAOS: { icon: '🌀', color: '#ff5722', name: '혼돈' },
};

// 증강 카드 컴포넌트
function AugmentCard({ augment, selected, onSelect }) {
  const style = AUGMENT_STYLES[augment.type] || AUGMENT_STYLES.CHAOS;
  
  return (
    <TouchableOpacity
      style={[
        styles.card,
        selected && styles.cardSelected,
        { borderColor: selected ? style.color : colors.border },
      ]}
      onPress={() => onSelect(augment)}
      activeOpacity={0.8}
    >
      <View style={[styles.cardIconBg, { backgroundColor: style.color + '30' }]}>
        <Text style={styles.cardIcon}>{style.icon}</Text>
      </View>
      <Text style={[styles.cardName, { color: style.color }]}>{style.name}</Text>
      <Text style={styles.cardDesc} numberOfLines={2}>
        {augment.description}
      </Text>
      {selected && (
        <View style={[styles.selectedBadge, { backgroundColor: style.color }]}>
          <Text style={styles.selectedBadgeText}>선택됨</Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

// 증강 선택 모달
export default function AugmentPickerModal({ 
  visible, 
  augments, 
  onSelect, 
  onTimeout,
  timeLimit = 15,
}) {
  const [selectedAugment, setSelectedAugment] = useState(null);
  const [timeLeft, setTimeLeft] = useState(timeLimit);
  const [confirmed, setConfirmed] = useState(false);

  // 타이머
  useEffect(() => {
    if (!visible) {
      setSelectedAugment(null);
      setTimeLeft(timeLimit);
      setConfirmed(false);
      return;
    }

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          // 시간 초과 시 랜덤 선택
          if (!confirmed) {
            const randomAugment = augments[Math.floor(Math.random() * augments.length)];
            onTimeout?.(randomAugment);
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [visible, confirmed]);

  const handleSelect = (augment) => {
    setSelectedAugment(augment);
  };

  const handleConfirm = () => {
    if (selectedAugment) {
      setConfirmed(true);
      onSelect(selectedAugment);
    }
  };

  const isLowTime = timeLeft <= 5;

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent={true}
    >
      <View style={styles.overlay}>
        <View style={styles.container}>
          {/* 헤더 */}
          <View style={styles.header}>
            <Text style={styles.title}>✨ 증강을 선택하세요!</Text>
            <View style={[styles.timerBadge, isLowTime && styles.timerBadgeLow]}>
              <Text style={[styles.timerText, isLowTime && styles.timerTextLow]}>
                {timeLeft}초
              </Text>
            </View>
          </View>

          {/* 카드 목록 */}
          <View style={styles.cardContainer}>
            {augments.map((augment) => (
              <AugmentCard
                key={augment.id}
                augment={augment}
                selected={selectedAugment?.id === augment.id}
                onSelect={handleSelect}
              />
            ))}
          </View>

          {/* 확인 버튼 */}
          <TouchableOpacity
            style={[
              styles.confirmButton,
              !selectedAugment && styles.confirmButtonDisabled,
            ]}
            onPress={handleConfirm}
            disabled={!selectedAugment}
          >
            <Text style={styles.confirmButtonText}>
              {selectedAugment ? '선택 확정' : '카드를 선택하세요'}
            </Text>
          </TouchableOpacity>

          {/* 안내 문구 */}
          <Text style={styles.hint}>
            시간 내 선택하지 않으면 랜덤으로 적용됩니다
          </Text>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    justifyContent: 'flex-end',
  },
  container: {
    backgroundColor: colors.bgMedium,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: spacing.lg,
    paddingBottom: spacing.xl + 20,
  },

  // 헤더
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.lg,
  },
  title: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  timerBadge: {
    backgroundColor: colors.bgLight,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: 16,
  },
  timerBadgeLow: {
    backgroundColor: colors.danger + '30',
  },
  timerText: {
    color: colors.textPrimary,
    fontSize: fontSize.md,
    fontWeight: 'bold',
  },
  timerTextLow: {
    color: colors.danger,
  },

  // 카드 컨테이너
  cardContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: spacing.sm,
    marginBottom: spacing.lg,
  },

  // 카드
  card: {
    flex: 1,
    backgroundColor: colors.bgLight,
    borderRadius: 16,
    padding: spacing.md,
    alignItems: 'center',
    borderWidth: 2,
    borderColor: colors.border,
    position: 'relative',
  },
  cardSelected: {
    backgroundColor: colors.bgDark,
  },
  cardIconBg: {
    width: 50,
    height: 50,
    borderRadius: 25,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  cardIcon: {
    fontSize: 28,
  },
  cardName: {
    fontSize: fontSize.md,
    fontWeight: 'bold',
    marginBottom: spacing.xs,
  },
  cardDesc: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
    textAlign: 'center',
    lineHeight: 16,
  },
  selectedBadge: {
    position: 'absolute',
    top: -8,
    right: -8,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderRadius: 10,
  },
  selectedBadgeText: {
    color: colors.textPrimary,
    fontSize: fontSize.xs,
    fontWeight: 'bold',
  },

  // 확인 버튼
  confirmButton: {
    backgroundColor: colors.primary,
    paddingVertical: spacing.md,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  confirmButtonDisabled: {
    backgroundColor: colors.textMuted,
  },
  confirmButtonText: {
    color: colors.textPrimary,
    fontSize: fontSize.md,
    fontWeight: 'bold',
  },

  // 안내
  hint: {
    color: colors.textMuted,
    fontSize: fontSize.xs,
    textAlign: 'center',
  },
});
