import { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Animated,
  Alert,
} from 'react-native';
import { colors, spacing, fontSize } from '../../styles/theme';
import { useRoomStore } from '../../store/roomStore';
import Button from '../../components/common/Button';

export default function MatchingScreen({ route, navigation }) {
  const { playerCount } = route.params;
  const { isMatching, matchStatus, checkMatchStatus, leaveMatchQueue } = useRoomStore();
  
  const [elapsedTime, setElapsedTime] = useState(0);
  const spinValue = useRef(new Animated.Value(0)).current;
  const pulseValue = useRef(new Animated.Value(1)).current;

  // 스핀 애니메이션
  useEffect(() => {
    const spin = Animated.loop(
      Animated.timing(spinValue, {
        toValue: 1,
        duration: 2000,
        useNativeDriver: true,
      })
    );
    spin.start();

    return () => spin.stop();
  }, []);

  // 펄스 애니메이션
  useEffect(() => {
    const pulse = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseValue, {
          toValue: 1.2,
          duration: 1000,
          useNativeDriver: true,
        }),
        Animated.timing(pulseValue, {
          toValue: 1,
          duration: 1000,
          useNativeDriver: true,
        }),
      ])
    );
    pulse.start();

    return () => pulse.stop();
  }, []);

  // 경과 시간 타이머
  useEffect(() => {
    const timer = setInterval(() => {
      setElapsedTime((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  // 매칭 상태 폴링
  useEffect(() => {
    const pollInterval = setInterval(async () => {
      const status = await checkMatchStatus();
      if (status?.matched && status?.roomId) {
        clearInterval(pollInterval);
        // 매칭 완료! 방으로 이동
        navigation.replace('Room', { 
          roomId: status.roomId, 
          roomType: 'QUICK' 
        });
      }
    }, 2000); // 2초마다 체크

    return () => clearInterval(pollInterval);
  }, []);

  const handleCancel = async () => {
    Alert.alert(
      '매칭 취소',
      '매칭을 취소하시겠습니까?',
      [
        { text: '아니오', style: 'cancel' },
        {
          text: '예',
          onPress: async () => {
            await leaveMatchQueue();
            navigation.goBack();
          },
        },
      ]
    );
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const spin = spinValue.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

  return (
    <View style={styles.container}>
      {/* 매칭 애니메이션 */}
      <View style={styles.animationContainer}>
        <Animated.View
          style={[
            styles.outerRing,
            { transform: [{ rotate: spin }] },
          ]}
        />
        <Animated.View
          style={[
            styles.innerCircle,
            { transform: [{ scale: pulseValue }] },
          ]}
        >
          <Text style={styles.playerCountText}>{playerCount}명</Text>
        </Animated.View>
      </View>

      {/* 상태 텍스트 */}
      <Text style={styles.statusText}>매칭 중...</Text>
      <Text style={styles.subText}>다른 플레이어를 찾고 있습니다</Text>

      {/* 매칭 정보 */}
      <View style={styles.infoContainer}>
        <View style={styles.infoItem}>
          <Text style={styles.infoLabel}>경과 시간</Text>
          <Text style={styles.infoValue}>{formatTime(elapsedTime)}</Text>
        </View>
        <View style={styles.infoDivider} />
        <View style={styles.infoItem}>
          <Text style={styles.infoLabel}>게임 모드</Text>
          <Text style={styles.infoValue}>빠른매칭</Text>
        </View>
      </View>

      {/* 게임 규칙 안내 */}
      <View style={styles.rulesContainer}>
        <Text style={styles.rulesTitle}>게임 규칙</Text>
        <Text style={styles.ruleItem}>⏱️ 10분 제한</Text>
        <Text style={styles.ruleItem}>🎯 라운드 3번</Text>
        <Text style={styles.ruleItem}>✨ 증강 3번</Text>
      </View>

      {/* 취소 버튼 */}
      <Button
        title="매칭 취소"
        onPress={handleCancel}
        variant="outline"
        style={styles.cancelButton}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.lg,
  },

  // 애니메이션
  animationContainer: {
    width: 150,
    height: 150,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.xl,
  },
  outerRing: {
    position: 'absolute',
    width: 150,
    height: 150,
    borderRadius: 75,
    borderWidth: 3,
    borderColor: colors.primary,
    borderTopColor: 'transparent',
    borderRightColor: 'transparent',
  },
  innerCircle: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  playerCountText: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },

  // 상태 텍스트
  statusText: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
    marginBottom: spacing.xs,
  },
  subText: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    marginBottom: spacing.xl,
  },

  // 정보
  infoContainer: {
    flexDirection: 'row',
    backgroundColor: colors.bgMedium,
    borderRadius: 16,
    padding: spacing.lg,
    marginBottom: spacing.xl,
  },
  infoItem: {
    flex: 1,
    alignItems: 'center',
  },
  infoDivider: {
    width: 1,
    backgroundColor: colors.border,
    marginHorizontal: spacing.md,
  },
  infoLabel: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginBottom: spacing.xs,
  },
  infoValue: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.accent,
  },

  // 규칙
  rulesContainer: {
    backgroundColor: colors.bgMedium,
    borderRadius: 16,
    padding: spacing.lg,
    width: '100%',
    marginBottom: spacing.xl,
  },
  rulesTitle: {
    fontSize: fontSize.md,
    fontWeight: 'bold',
    color: colors.textPrimary,
    marginBottom: spacing.md,
    textAlign: 'center',
  },
  ruleItem: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textAlign: 'center',
    marginVertical: spacing.xs,
  },

  // 취소 버튼
  cancelButton: {
    width: '100%',
  },
});
