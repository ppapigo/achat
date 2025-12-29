import { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  FlatList,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { ArrowLeft, Heart, MessageCircle, Share2, MoreVertical, Send, CornerDownRight, X } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

// Mock 댓글 (대댓글 포함)
const MOCK_COMMENTS = [
  { 
    id: '1', 
    author: '댓글러1', 
    content: '좋은 글이네요!', 
    createdAt: '10분 전', 
    likes: 3,
    replies: [
      { id: '1-1', author: '작성자', content: '감사합니다!', createdAt: '8분 전', likes: 1, isAuthor: true },
      { id: '1-2', author: '댓글러3', content: '저도 동의해요', createdAt: '5분 전', likes: 0 },
    ]
  },
  { 
    id: '2', 
    author: '댓글러2', 
    content: '저도 같은 생각이에요', 
    createdAt: '30분 전', 
    likes: 1,
    replies: []
  },
  { 
    id: '3', 
    author: '질문자', 
    content: '이 증강은 어디서 얻나요?', 
    createdAt: '1시간 전', 
    likes: 2,
    replies: [
      { id: '3-1', author: '작성자', content: '랭크 매치에서 획득 가능해요!', createdAt: '50분 전', likes: 5, isAuthor: true },
    ]
  },
];

export default function PostDetailScreen({ route, navigation }) {
  const { post } = route.params;
  
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(post?.likeCount || 0);
  const [comments, setComments] = useState(MOCK_COMMENTS);
  const [newComment, setNewComment] = useState('');
  const [replyTo, setReplyTo] = useState(null); // { commentId, author }

  const handleLike = () => {
    setLiked(!liked);
    setLikeCount(prev => liked ? prev - 1 : prev + 1);
  };

  const handleReply = (comment) => {
    setReplyTo({ commentId: comment.id, author: comment.author });
  };

  const handleCancelReply = () => {
    setReplyTo(null);
  };

  const handleSubmitComment = () => {
    if (!newComment.trim()) return;

    if (replyTo) {
      // 대댓글 추가
      const reply = {
        id: `${replyTo.commentId}-${Date.now()}`,
        author: '나',
        content: newComment.trim(),
        createdAt: '방금',
        likes: 0,
        isMe: true,
      };

      setComments(prev => prev.map(comment => {
        if (comment.id === replyTo.commentId) {
          return {
            ...comment,
            replies: [...(comment.replies || []), reply]
          };
        }
        return comment;
      }));

      setReplyTo(null);
    } else {
      // 일반 댓글 추가
      const comment = {
        id: Date.now().toString(),
        author: '나',
        content: newComment.trim(),
        createdAt: '방금',
        likes: 0,
        isMe: true,
        replies: [],
      };

      setComments(prev => [...prev, comment]);
    }

    setNewComment('');
  };

  const handleDeletePost = () => {
    Alert.alert(
      '삭제',
      '게시글을 삭제할까요?',
      [
        { text: '취소', style: 'cancel' },
        { 
          text: '삭제', 
          style: 'destructive',
          onPress: () => {
            navigation.goBack();
          }
        },
      ]
    );
  };

  const handleMore = () => {
    Alert.alert(
      '메뉴',
      '',
      [
        { text: '수정', onPress: () => navigation.navigate('CreatePost', { post, isEdit: true }) },
        { text: '삭제', style: 'destructive', onPress: handleDeletePost },
        { text: '취소', style: 'cancel' },
      ]
    );
  };

  // 대댓글 렌더링
  const renderReply = (reply, isLast) => (
    <View key={reply.id} style={[styles.replyItem, isLast && styles.replyItemLast]}>
      <CornerDownRight size={16} color={colors.textMuted} style={styles.replyIcon} />
      <View style={styles.replyContent}>
        <View style={styles.commentHeader}>
          <View style={styles.commentAuthorRow}>
            <Text style={[styles.commentAuthor, reply.isAuthor && styles.authorBadge]}>
              {reply.author}
            </Text>
            {reply.isAuthor && <Text style={styles.authorTag}>작성자</Text>}
          </View>
          <Text style={styles.commentTime}>{reply.createdAt}</Text>
        </View>
        <Text style={styles.commentText}>{reply.content}</Text>
        <TouchableOpacity style={styles.commentLike}>
          <Heart size={12} color={colors.textMuted} />
          <Text style={styles.commentLikeText}>{reply.likes}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  // 댓글 렌더링
  const renderComment = (item) => (
    <View key={item.id} style={styles.commentContainer}>
      <View style={styles.commentItem}>
        <View style={styles.commentHeader}>
          <View style={styles.commentAuthorRow}>
            <Text style={[styles.commentAuthor, item.isAuthor && styles.authorBadge]}>
              {item.author}
            </Text>
            {item.isAuthor && <Text style={styles.authorTag}>작성자</Text>}
          </View>
          <Text style={styles.commentTime}>{item.createdAt}</Text>
        </View>
        <Text style={styles.commentText}>{item.content}</Text>
        <View style={styles.commentActions}>
          <TouchableOpacity style={styles.commentLike}>
            <Heart size={14} color={colors.textMuted} />
            <Text style={styles.commentLikeText}>{item.likes}</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={styles.replyButton}
            onPress={() => handleReply(item)}
          >
            <MessageCircle size={14} color={colors.textMuted} />
            <Text style={styles.replyButtonText}>답글</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* 대댓글 목록 */}
      {item.replies && item.replies.length > 0 && (
        <View style={styles.repliesContainer}>
          {item.replies.map((reply, index) => 
            renderReply(reply, index === item.replies.length - 1)
          )}
        </View>
      )}
    </View>
  );

  const totalComments = comments.reduce(
    (sum, c) => sum + 1 + (c.replies?.length || 0), 
    0
  );

  return (
    <KeyboardAvoidingView 
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <ArrowLeft size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>게시글</Text>
        <TouchableOpacity onPress={handleMore} style={styles.moreButton}>
          <MoreVertical size={24} color={colors.textPrimary} />
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.content}>
        {/* 게시글 본문 */}
        <View style={styles.postContainer}>
          <View style={styles.postHeader}>
            <View style={styles.categoryBadge}>
              <Text style={styles.categoryText}>{post?.category || '자유'}</Text>
            </View>
            <Text style={styles.postTime}>{post?.createdAt || '방금'}</Text>
          </View>

          <Text style={styles.postTitle}>{post?.title || '제목 없음'}</Text>
          <Text style={styles.postAuthor}>by {post?.author || '익명'}</Text>
          <Text style={styles.postContent}>{post?.content || '내용 없음'}</Text>

          {/* 액션 버튼 */}
          <View style={styles.actions}>
            <TouchableOpacity 
              style={[styles.actionButton, liked && styles.actionButtonActive]}
              onPress={handleLike}
            >
              <Heart 
                size={20} 
                color={liked ? colors.danger : colors.textSecondary}
                fill={liked ? colors.danger : 'transparent'}
              />
              <Text style={[styles.actionText, liked && styles.actionTextActive]}>
                {likeCount}
              </Text>
            </TouchableOpacity>
            <View style={styles.actionButton}>
              <MessageCircle size={20} color={colors.textSecondary} />
              <Text style={styles.actionText}>{totalComments}</Text>
            </View>
            <TouchableOpacity style={styles.actionButton}>
              <Share2 size={20} color={colors.textSecondary} />
              <Text style={styles.actionText}>공유</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* 댓글 섹션 */}
        <View style={styles.commentsSection}>
          <Text style={styles.commentsTitle}>댓글 {totalComments}</Text>
          {comments.map((item) => renderComment(item))}
        </View>
      </ScrollView>

      {/* 답글 대상 표시 */}
      {replyTo && (
        <View style={styles.replyToBar}>
          <Text style={styles.replyToText}>
            <Text style={styles.replyToAuthor}>{replyTo.author}</Text>님에게 답글 작성 중
          </Text>
          <TouchableOpacity onPress={handleCancelReply}>
            <X size={18} color={colors.textMuted} />
          </TouchableOpacity>
        </View>
      )}

      {/* 댓글 입력 */}
      <View style={styles.commentInputContainer}>
        <TextInput
          style={styles.commentInput}
          placeholder={replyTo ? "답글을 입력하세요..." : "댓글을 입력하세요..."}
          placeholderTextColor={colors.textMuted}
          value={newComment}
          onChangeText={setNewComment}
          multiline
          maxLength={300}
        />
        <TouchableOpacity 
          style={[styles.sendButton, !newComment.trim() && styles.sendButtonDisabled]}
          onPress={handleSubmitComment}
          disabled={!newComment.trim()}
        >
          <Send size={20} color={newComment.trim() ? colors.textPrimary : colors.textMuted} />
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  backButton: {
    padding: spacing.xs,
  },
  headerTitle: {
    fontSize: fontSize.lg,
    fontWeight: 'bold',
    color: colors.textPrimary,
  },
  moreButton: {
    padding: spacing.xs,
  },
  content: {
    flex: 1,
  },
  postContainer: {
    padding: spacing.md,
    borderBottomWidth: 8,
    borderBottomColor: colors.bgMedium,
  },
  postHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  categoryBadge: {
    backgroundColor: colors.primary + '30',
    paddingVertical: 4,
    paddingHorizontal: spacing.sm,
    borderRadius: 8,
  },
  categoryText: {
    fontSize: fontSize.xs,
    color: colors.primary,
    fontWeight: '600',
  },
  postTime: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  postTitle: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
    marginBottom: spacing.sm,
  },
  postAuthor: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginBottom: spacing.lg,
  },
  postContent: {
    fontSize: fontSize.md,
    color: colors.textPrimary,
    lineHeight: 24,
    marginBottom: spacing.lg,
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.lg,
    paddingTop: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  actionButtonActive: {},
  actionText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  actionTextActive: {
    color: colors.danger,
  },
  commentsSection: {
    padding: spacing.md,
  },
  commentsTitle: {
    fontSize: fontSize.md,
    fontWeight: '600',
    color: colors.textPrimary,
    marginBottom: spacing.md,
  },
  commentContainer: {
    marginBottom: spacing.md,
  },
  commentItem: {
    backgroundColor: colors.bgMedium,
    padding: spacing.md,
    borderRadius: 12,
  },
  commentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  commentAuthorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  commentAuthor: {
    fontSize: fontSize.sm,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  authorBadge: {
    color: colors.primary,
  },
  authorTag: {
    fontSize: fontSize.xs,
    color: colors.primary,
    backgroundColor: colors.primary + '20',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  commentTime: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  commentText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
    marginBottom: spacing.sm,
    lineHeight: 20,
  },
  commentActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  commentLike: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  commentLikeText: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  replyButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  replyButtonText: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
  },
  // 대댓글 스타일
  repliesContainer: {
    marginLeft: spacing.md,
    marginTop: spacing.sm,
  },
  replyItem: {
    flexDirection: 'row',
    backgroundColor: colors.bgLight,
    padding: spacing.sm,
    paddingLeft: spacing.xs,
    borderRadius: 8,
    marginBottom: spacing.xs,
  },
  replyItemLast: {
    marginBottom: 0,
  },
  replyIcon: {
    marginRight: spacing.xs,
    marginTop: 2,
  },
  replyContent: {
    flex: 1,
  },
  // 답글 대상 바
  replyToBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    backgroundColor: colors.primary + '20',
    borderTopWidth: 1,
    borderTopColor: colors.primary + '30',
  },
  replyToText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  replyToAuthor: {
    color: colors.primary,
    fontWeight: '600',
  },
  commentInputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    padding: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.bgMedium,
  },
  commentInput: {
    flex: 1,
    backgroundColor: colors.bgLight,
    borderRadius: 20,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontSize: fontSize.md,
    color: colors.textPrimary,
    maxHeight: 100,
    marginRight: spacing.sm,
  },
  sendButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: colors.bgLight,
  },
});
