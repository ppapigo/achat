import { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
} from 'react-native';
import { FileText, Heart, MessageCircle, Clock } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

// Mock 게시글
const MOCK_POSTS = [
  {
    id: '1',
    title: '증강 조합 추천해주세요',
    content: '초보인데 어떤 증강이 좋을까요?',
    author: '뉴비',
    createdAt: '10분 전',
    likeCount: 5,
    commentCount: 12,
    category: '질문',
  },
  {
    id: '2',
    title: '역전 증강으로 역전승 ㅋㅋㅋ',
    content: '진짜 마지막에 역전해서 이김',
    author: '챔피언',
    createdAt: '1시간 전',
    likeCount: 24,
    commentCount: 8,
    category: '자유',
  },
  {
    id: '3',
    title: '시즌 보상 언제 나오나요?',
    content: '시즌 끝났는데 보상이 안 왔어요',
    author: '기다림',
    createdAt: '2시간 전',
    likeCount: 3,
    commentCount: 5,
    category: '질문',
  },
  {
    id: '4',
    title: '신규 증강 아이디어',
    content: '이런 증강 있으면 재밌을 것 같아요',
    author: '아이디어맨',
    createdAt: '3시간 전',
    likeCount: 18,
    commentCount: 21,
    category: '건의',
  },
];

const CATEGORIES = ['전체', '자유', '질문', '공략', '건의'];

export default function BoardScreen({ navigation }) {
  const [posts, setPosts] = useState(MOCK_POSTS);
  const [refreshing, setRefreshing] = useState(false);
  const [activeCategory, setActiveCategory] = useState('전체');

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    // TODO: API 호출
    setTimeout(() => setRefreshing(false), 1000);
  }, []);

  const filteredPosts = activeCategory === '전체'
    ? posts
    : posts.filter((p) => p.category === activeCategory);

  const renderPost = ({ item }) => (
    <TouchableOpacity
      style={styles.postCard}
      onPress={() => {
        // TODO: 게시글 상세로 이동
      }}
    >
      <View style={styles.postHeader}>
        <View style={[styles.categoryBadge, getCategoryStyle(item.category)]}>
          <Text style={styles.categoryText}>{item.category}</Text>
        </View>
        <View style={styles.postMeta}>
          <Clock size={12} color={colors.textMuted} />
          <Text style={styles.postTime}>{item.createdAt}</Text>
        </View>
      </View>

      <Text style={styles.postTitle}>{item.title}</Text>
      <Text style={styles.postContent} numberOfLines={2}>{item.content}</Text>

      <View style={styles.postFooter}>
        <Text style={styles.postAuthor}>{item.author}</Text>
        <View style={styles.postStats}>
          <View style={styles.stat}>
            <Heart size={14} color={colors.danger} />
            <Text style={styles.statText}>{item.likeCount}</Text>
          </View>
          <View style={styles.stat}>
            <MessageCircle size={14} color={colors.textSecondary} />
            <Text style={styles.statText}>{item.commentCount}</Text>
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      {/* 카테고리 탭 */}
      <View style={styles.categories}>
        {CATEGORIES.map((cat) => (
          <TouchableOpacity
            key={cat}
            style={[styles.categoryTab, activeCategory === cat && styles.categoryTabActive]}
            onPress={() => setActiveCategory(cat)}
          >
            <Text style={[styles.categoryTabText, activeCategory === cat && styles.categoryTabTextActive]}>
              {cat}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* 게시글 목록 */}
      <FlatList
        data={filteredPosts}
        keyExtractor={(item) => item.id}
        renderItem={renderPost}
        contentContainerStyle={styles.postList}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.primary}
          />
        }
        ListEmptyComponent={
          <View style={styles.empty}>
            <FileText size={48} color={colors.textMuted} />
            <Text style={styles.emptyText}>게시글이 없습니다</Text>
          </View>
        }
      />

      {/* 글쓰기 FAB */}
      <TouchableOpacity style={styles.fab} activeOpacity={0.8}>
        <Text style={styles.fabText}>✏️</Text>
      </TouchableOpacity>
    </View>
  );
}

function getCategoryStyle(category) {
  switch (category) {
    case '질문':
      return { backgroundColor: colors.primary + '30' };
    case '자유':
      return { backgroundColor: colors.accent + '30' };
    case '공략':
      return { backgroundColor: colors.warning + '30' };
    case '건의':
      return { backgroundColor: colors.augment + '30' };
    default:
      return { backgroundColor: colors.bgLight };
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },
  categories: {
    flexDirection: 'row',
    padding: spacing.md,
    gap: spacing.sm,
  },
  categoryTab: {
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: 20,
    backgroundColor: colors.bgMedium,
  },
  categoryTabActive: {
    backgroundColor: colors.primary,
  },
  categoryTabText: {
    color: colors.textSecondary,
    fontSize: fontSize.sm,
  },
  categoryTabTextActive: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  postList: {
    padding: spacing.md,
    paddingTop: 0,
  },
  postCard: {
    backgroundColor: colors.bgMedium,
    borderRadius: 12,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  postHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  categoryBadge: {
    paddingVertical: 2,
    paddingHorizontal: spacing.sm,
    borderRadius: 8,
  },
  categoryText: {
    fontSize: fontSize.xs,
    color: colors.textPrimary,
    fontWeight: '600',
  },
  postMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  postTime: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  postTitle: {
    fontSize: fontSize.md,
    fontWeight: '600',
    color: colors.textPrimary,
    marginBottom: spacing.xs,
  },
  postContent: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginBottom: spacing.md,
  },
  postFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  postAuthor: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
  },
  postStats: {
    flexDirection: 'row',
    gap: spacing.md,
  },
  stat: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  statText: {
    fontSize: fontSize.xs,
    color: colors.textSecondary,
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
  fab: {
    position: 'absolute',
    right: spacing.lg,
    bottom: spacing.lg,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
  },
  fabText: {
    fontSize: 24,
  },
});
