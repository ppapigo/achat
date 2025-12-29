import { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  FlatList,
  TouchableOpacity,
} from 'react-native';
import { Search as SearchIcon, Users, Home } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

// Mock 검색 결과
const MOCK_RESULTS = [
  { id: '1', type: 'room', name: '🔥 불타는 토론방', memberCount: 24 },
  { id: '2', type: 'room', name: '⚔️ 배틀 아레나', memberCount: 18 },
  { id: '3', type: 'user', name: '마스터', level: 15 },
  { id: '4', type: 'user', name: '챔피언', level: 12 },
];

export default function SearchScreen({ navigation }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [activeTab, setActiveTab] = useState('all');

  const handleSearch = (text) => {
    setQuery(text);
    if (text.trim()) {
      const filtered = MOCK_RESULTS.filter((item) =>
        item.name.toLowerCase().includes(text.toLowerCase())
      );
      setResults(filtered);
    } else {
      setResults([]);
    }
  };

  const renderItem = ({ item }) => (
    <TouchableOpacity
      style={styles.resultItem}
      onPress={() => {
        if (item.type === 'room') {
          navigation.navigate('Room', { roomId: item.id });
        }
      }}
    >
      <View style={styles.resultIcon}>
        {item.type === 'room' ? (
          <Home size={20} color={colors.primary} />
        ) : (
          <Users size={20} color={colors.accent} />
        )}
      </View>
      <View style={styles.resultInfo}>
        <Text style={styles.resultName}>{item.name}</Text>
        <Text style={styles.resultMeta}>
          {item.type === 'room' ? `${item.memberCount}명 참여 중` : `Lv.${item.level}`}
        </Text>
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      {/* 검색창 */}
      <View style={styles.searchContainer}>
        <SearchIcon size={20} color={colors.textMuted} style={styles.searchIcon} />
        <TextInput
          style={styles.searchInput}
          placeholder="방 또는 유저 검색..."
          placeholderTextColor={colors.textMuted}
          value={query}
          onChangeText={handleSearch}
        />
      </View>

      {/* 탭 필터 */}
      <View style={styles.tabs}>
        {['all', 'room', 'user'].map((tab) => (
          <TouchableOpacity
            key={tab}
            style={[styles.tab, activeTab === tab && styles.tabActive]}
            onPress={() => setActiveTab(tab)}
          >
            <Text style={[styles.tabText, activeTab === tab && styles.tabTextActive]}>
              {tab === 'all' ? '전체' : tab === 'room' ? '방' : '유저'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* 검색 결과 */}
      {results.length > 0 ? (
        <FlatList
          data={results.filter((r) => activeTab === 'all' || r.type === activeTab)}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={styles.resultList}
        />
      ) : query ? (
        <View style={styles.empty}>
          <Text style={styles.emptyText}>검색 결과가 없습니다</Text>
        </View>
      ) : (
        <View style={styles.empty}>
          <SearchIcon size={48} color={colors.textMuted} />
          <Text style={styles.emptyText}>방이나 유저를 검색해보세요</Text>
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
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgMedium,
    margin: spacing.md,
    borderRadius: 12,
    paddingHorizontal: spacing.md,
  },
  searchIcon: {
    marginRight: spacing.sm,
  },
  searchInput: {
    flex: 1,
    paddingVertical: spacing.md,
    fontSize: fontSize.md,
    color: colors.textPrimary,
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
    backgroundColor: colors.primary,
  },
  tabText: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
  },
  tabTextActive: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  resultList: {
    padding: spacing.md,
  },
  resultItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.bgMedium,
    padding: spacing.md,
    borderRadius: 12,
    marginBottom: spacing.sm,
  },
  resultIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.bgLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.md,
  },
  resultInfo: {
    flex: 1,
  },
  resultName: {
    color: colors.textPrimary,
    fontSize: fontSize.md,
    fontWeight: '600',
  },
  resultMeta: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
    marginTop: 2,
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.md,
  },
  emptyText: {
    color: colors.textMuted,
    fontSize: fontSize.md,
  },
});
