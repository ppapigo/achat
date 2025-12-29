import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert } from 'react-native';
import { User, Settings, Trophy, Gamepad2, LogOut, ChevronRight, Star, Sparkles } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';
import { useAuthStore } from '../../store/authStore';

// Mock 유저 데이터
const MOCK_USER = {
  username: '테스터',
  email: 'test@test.com',
  level: 15,
  exp: 2400,
  maxExp: 3000,
  rank: 'Gold',
  wins: 42,
  losses: 18,
  totalGames: 60,
  favoriteAugment: '터보',
  joinedAt: '2024.01.15',
};

const MOCK_STATS = [
  { label: '총 게임', value: '60', icon: Gamepad2 },
  { label: '승리', value: '42', icon: Trophy },
  { label: '승률', value: '70%', icon: Star },
];

export default function ProfileScreen({ navigation }) {
  const { user, logout } = useAuthStore();
  const displayUser = user || MOCK_USER;

  const handleLogout = () => {
    Alert.alert(
      '로그아웃',
      '정말 로그아웃 하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        { text: '로그아웃', style: 'destructive', onPress: logout },
      ]
    );
  };

  const expProgress = (MOCK_USER.exp / MOCK_USER.maxExp) * 100;

  return (
    <ScrollView style={styles.container}>
      {/* 프로필 카드 */}
      <View style={styles.profileCard}>
        <View style={styles.avatarContainer}>
          <View style={styles.avatar}>
            <User size={40} color={colors.textPrimary} />
          </View>
          <View style={styles.levelBadge}>
            <Text style={styles.levelText}>{MOCK_USER.level}</Text>
          </View>
        </View>
        
        <Text style={styles.username}>{displayUser.username || MOCK_USER.username}</Text>
        <Text style={styles.email}>{displayUser.email || MOCK_USER.email}</Text>
        
        {/* 경험치 바 */}
        <View style={styles.expContainer}>
          <View style={styles.expBar}>
            <View style={[styles.expFill, { width: `${expProgress}%` }]} />
          </View>
          <Text style={styles.expText}>
            {MOCK_USER.exp} / {MOCK_USER.maxExp} EXP
          </Text>
        </View>

        {/* 랭크 */}
        <View style={styles.rankContainer}>
          <Trophy size={16} color={colors.warning} />
          <Text style={styles.rankText}>{MOCK_USER.rank}</Text>
        </View>
      </View>

      {/* 통계 */}
      <View style={styles.statsContainer}>
        {MOCK_STATS.map((stat, index) => (
          <View key={index} style={styles.statItem}>
            <stat.icon size={24} color={colors.primary} />
            <Text style={styles.statValue}>{stat.value}</Text>
            <Text style={styles.statLabel}>{stat.label}</Text>
          </View>
        ))}
      </View>

      {/* 메뉴 */}
      <View style={styles.menuSection}>
        <Text style={styles.menuSectionTitle}>계정</Text>
        
        <TouchableOpacity style={styles.menuItem} onPress={() => navigation.navigate('Cards')}>
          <View style={styles.menuItemLeft}>
            <Sparkles size={20} color={colors.augment} />
            <Text style={styles.menuItemText}>카드 도감</Text>
          </View>
          <ChevronRight size={20} color={colors.textMuted} />
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem}>
          <View style={styles.menuItemLeft}>
            <User size={20} color={colors.textSecondary} />
            <Text style={styles.menuItemText}>프로필 수정</Text>
          </View>
          <ChevronRight size={20} color={colors.textMuted} />
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem}>
          <View style={styles.menuItemLeft}>
            <Trophy size={20} color={colors.textSecondary} />
            <Text style={styles.menuItemText}>전적 상세</Text>
          </View>
          <ChevronRight size={20} color={colors.textMuted} />
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem}>
          <View style={styles.menuItemLeft}>
            <Settings size={20} color={colors.textSecondary} />
            <Text style={styles.menuItemText}>설정</Text>
          </View>
          <ChevronRight size={20} color={colors.textMuted} />
        </TouchableOpacity>
      </View>

      {/* 게임 정보 */}
      <View style={styles.menuSection}>
        <Text style={styles.menuSectionTitle}>게임 정보</Text>
        
        <View style={styles.infoItem}>
          <Text style={styles.infoLabel}>가입일</Text>
          <Text style={styles.infoValue}>{MOCK_USER.joinedAt}</Text>
        </View>

        <View style={styles.infoItem}>
          <Text style={styles.infoLabel}>즐겨쓰는 증강</Text>
          <Text style={styles.infoValue}>⚡ {MOCK_USER.favoriteAugment}</Text>
        </View>
      </View>

      {/* 로그아웃 */}
      <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
        <LogOut size={20} color={colors.danger} />
        <Text style={styles.logoutText}>로그아웃</Text>
      </TouchableOpacity>

      <View style={styles.footer}>
        <Text style={styles.footerText}>A.chat v1.0.0</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },
  profileCard: {
    backgroundColor: colors.bgMedium,
    margin: spacing.md,
    borderRadius: 16,
    padding: spacing.lg,
    alignItems: 'center',
  },
  avatarContainer: {
    position: 'relative',
    marginBottom: spacing.md,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: colors.bgLight,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: colors.primary,
  },
  levelBadge: {
    position: 'absolute',
    bottom: -4,
    right: -4,
    backgroundColor: colors.primary,
    width: 28,
    height: 28,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: colors.bgMedium,
  },
  levelText: {
    color: colors.textPrimary,
    fontSize: fontSize.sm,
    fontWeight: 'bold',
  },
  username: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  email: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginTop: 2,
  },
  expContainer: {
    width: '100%',
    marginTop: spacing.md,
  },
  expBar: {
    height: 8,
    backgroundColor: colors.bgLight,
    borderRadius: 4,
    overflow: 'hidden',
  },
  expFill: {
    height: '100%',
    backgroundColor: colors.primary,
    borderRadius: 4,
  },
  expText: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    textAlign: 'center',
    marginTop: spacing.xs,
  },
  rankContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    marginTop: spacing.md,
    backgroundColor: colors.warning + '20',
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.md,
    borderRadius: 12,
  },
  rankText: {
    color: colors.warning,
    fontSize: fontSize.sm,
    fontWeight: '600',
  },
  statsContainer: {
    flexDirection: 'row',
    backgroundColor: colors.bgMedium,
    marginHorizontal: spacing.md,
    borderRadius: 16,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
    gap: spacing.xs,
  },
  statValue: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  statLabel: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
  },
  menuSection: {
    backgroundColor: colors.bgMedium,
    marginHorizontal: spacing.md,
    borderRadius: 16,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  menuSectionTitle: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    marginBottom: spacing.md,
    fontWeight: '600',
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  menuItemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  menuItemText: {
    fontSize: fontSize.md,
    color: colors.textPrimary,
  },
  infoItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.sm,
  },
  infoLabel: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  infoValue: {
    fontSize: fontSize.sm,
    color: colors.textPrimary,
    fontWeight: '600',
  },
  logoutButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    marginHorizontal: spacing.md,
    marginVertical: spacing.md,
    padding: spacing.md,
    backgroundColor: colors.danger + '20',
    borderRadius: 12,
  },
  logoutText: {
    color: colors.danger,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  footer: {
    alignItems: 'center',
    padding: spacing.lg,
  },
  footerText: {
    color: colors.textMuted,
    fontSize: fontSize.xs,
  },
});
