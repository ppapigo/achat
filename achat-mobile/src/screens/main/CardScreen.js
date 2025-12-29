import { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { Sparkles, Lock, Check } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

// Mock 카드(증강) 데이터
const MOCK_CARDS = [
  { id: '1', name: '역전', type: 'REVERSE', icon: '🔄', rarity: 'common', owned: true, description: '상대 메시지가 거꾸로 보입니다' },
  { id: '2', name: '터보', type: 'TURBO_CASTER', icon: '⚡', rarity: 'rare', owned: true, description: '메시지 쿨타임 40% 감소' },
  { id: '3', name: '방어', type: 'SHIELD', icon: '🛡️', rarity: 'common', owned: true, description: '이번 라운드 디버프 무효화' },
  { id: '4', name: '증폭', type: 'AMPLIFY', icon: '📢', rarity: 'epic', owned: false, description: '메시지가 강조되어 보입니다' },
  { id: '5', name: '은신', type: 'STEALTH', icon: '👻', rarity: 'rare', owned: true, description: '닉네임이 숨겨집니다' },
  { id: '6', name: '미러', type: 'MIRROR', icon: '🪞', rarity: 'epic', owned: false, description: '상대 증강을 복사합니다' },
  { id: '7', name: '빙결', type: 'FREEZE', icon: '❄️', rarity: 'legendary', owned: false, description: '상대 쿨타임 2배 증가' },
  { id: '8', name: '혼돈', type: 'CHAOS', icon: '🌀', rarity: 'legendary', owned: false, description: '랜덤 효과 발동' },
  { id: '9', name: '퀘스트', type: 'QUEST_GIVER', icon: '📜', rarity: 'rare', owned: true, description: '퀘스트를 생성합니다' },
  { id: '10', name: '타임시프트', type: 'TIME_SHIFTER', icon: '⏰', rarity: 'epic', owned: false, description: '쿨타임 20% 감소' },
];

const RARITY_COLORS = {
  common: '#9e9e9e',
  rare: '#2196f3',
  epic: '#9c27b0',
  legendary: '#ff9800',
};

const RARITY_NAMES = {
  common: '일반',
  rare: '레어',
  epic: '에픽',
  legendary: '전설',
};

const TABS = ['전체', '보유', '미보유'];

export default function CardScreen() {
  const [activeTab, setActiveTab] = useState('전체');
  const [selectedCard, setSelectedCard] = useState(null);

  const filteredCards = MOCK_CARDS.filter((card) => {
    if (activeTab === '보유') return card.owned;
    if (activeTab === '미보유') return !card.owned;
    return true;
  });

  const ownedCount = MOCK_CARDS.filter((c) => c.owned).length;

  const renderCard = ({ item }) => (
    <TouchableOpacity
      style={[
        styles.card,
        !item.owned && styles.cardLocked,
        selectedCard?.id === item.id && styles.cardSelected,
      ]}
      onPress={() => setSelectedCard(item)}
      activeOpacity={0.8}
    >
      <View style={[styles.cardRarity, { backgroundColor: RARITY_COLORS[item.rarity] }]} />
      <Text style={styles.cardIcon}>{item.icon}</Text>
      <Text style={styles.cardName}>{item.name}</Text>
      {!item.owned && (
        <View style={styles.lockOverlay}>
          <Lock size={20} color={colors.textMuted} />
        </View>
      )}
      {item.owned && (
        <View style={styles.ownedBadge}>
          <Check size={12} color={colors.success} />
        </View>
      )}
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      {/* 헤더 */}
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Sparkles size={24} color={colors.augment} />
          <Text style={styles.headerTitle}>카드 도감</Text>
        </View>
        <Text style={styles.headerCount}>
          {ownedCount}/{MOCK_CARDS.length} 수집
        </Text>
      </View>

      {/* 탭 */}
      <View style={styles.tabs}>
        {TABS.map((tab) => (
          <TouchableOpacity
            key={tab}
            style={[styles.tab, activeTab === tab && styles.tabActive]}
            onPress={() => setActiveTab(tab)}
          >
            <Text style={[styles.tabText, activeTab === tab && styles.tabTextActive]}>
              {tab}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* 카드 그리드 */}
      <FlatList
        data={filteredCards}
        keyExtractor={(item) => item.id}
        renderItem={renderCard}
        numColumns={3}
        contentContainerStyle={styles.cardGrid}
        columnWrapperStyle={styles.cardRow}
      />

      {/* 선택된 카드 상세 */}
      {selectedCard && (
        <View style={styles.detailPanel}>
          <View style={styles.detailHeader}>
            <Text style={styles.detailIcon}>{selectedCard.icon}</Text>
            <View>
              <Text style={styles.detailName}>{selectedCard.name}</Text>
              <Text style={[styles.detailRarity, { color: RARITY_COLORS[selectedCard.rarity] }]}>
                {RARITY_NAMES[selectedCard.rarity]}
              </Text>
            </View>
          </View>
          <Text style={styles.detailDescription}>{selectedCard.description}</Text>
          {!selectedCard.owned && (
            <View style={styles.obtainHint}>
              <Text style={styles.obtainHintText}>🎁 게임 플레이로 획득 가능</Text>
            </View>
          )}
        </View>
      )}
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
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  headerTitle: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  headerCount: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    backgroundColor: colors.bgMedium,
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.md,
    borderRadius: 12,
  },
  tabs: {
    flexDirection: 'row',
    paddingHorizontal: spacing.md,
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  tab: {
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: 20,
    backgroundColor: colors.bgMedium,
  },
  tabActive: {
    backgroundColor: colors.augment,
  },
  tabText: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
  },
  tabTextActive: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  cardGrid: {
    padding: spacing.md,
  },
  cardRow: {
    justifyContent: 'space-between',
    marginBottom: spacing.md,
  },
  card: {
    width: '31%',
    aspectRatio: 0.75,
    backgroundColor: colors.bgMedium,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
    overflow: 'hidden',
  },
  cardLocked: {
    opacity: 0.5,
  },
  cardSelected: {
    borderWidth: 2,
    borderColor: colors.augment,
  },
  cardRarity: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 4,
  },
  cardIcon: {
    fontSize: 32,
    marginBottom: spacing.xs,
  },
  cardName: {
    fontSize: fontSize.sm,
    color: colors.textPrimary,
    fontWeight: '600',
  },
  lockOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.5)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  ownedBadge: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: colors.success + '30',
    alignItems: 'center',
    justifyContent: 'center',
  },
  detailPanel: {
    backgroundColor: colors.bgMedium,
    padding: spacing.lg,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
  },
  detailHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    marginBottom: spacing.md,
  },
  detailIcon: {
    fontSize: 48,
  },
  detailName: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  detailRarity: {
    fontSize: fontSize.sm,
    fontWeight: '600',
  },
  detailDescription: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    lineHeight: 22,
  },
  obtainHint: {
    marginTop: spacing.md,
    padding: spacing.md,
    backgroundColor: colors.bgLight,
    borderRadius: 8,
  },
  obtainHintText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    textAlign: 'center',
  },
});
