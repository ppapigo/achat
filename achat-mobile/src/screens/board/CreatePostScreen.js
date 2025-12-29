import { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { ArrowLeft, ChevronDown } from 'lucide-react-native';
import { colors, spacing, fontSize } from '../../styles/theme';

const CATEGORIES = ['자유', '질문', '공략', '건의'];

export default function CreatePostScreen({ route, navigation }) {
  const { post, isEdit } = route.params || {};

  const [title, setTitle] = useState(post?.title || '');
  const [content, setContent] = useState(post?.content || '');
  const [category, setCategory] = useState(post?.category || '자유');
  const [showCategoryPicker, setShowCategoryPicker] = useState(false);

  const canSubmit = title.trim().length >= 2 && content.trim().length >= 10;

  const handleSubmit = () => {
    if (!canSubmit) {
      Alert.alert('알림', '제목은 2자 이상, 내용은 10자 이상 입력해주세요.');
      return;
    }

    const postData = {
      id: post?.id || Date.now().toString(),
      title: title.trim(),
      content: content.trim(),
      category,
      author: '나',
      createdAt: '방금',
      likeCount: post?.likeCount || 0,
      commentCount: post?.commentCount || 0,
    };

    // TODO: API 호출
    Alert.alert(
      '완료',
      isEdit ? '게시글이 수정되었습니다.' : '게시글이 등록되었습니다.',
      [{ text: '확인', onPress: () => navigation.goBack() }]
    );
  };

  const handleBack = () => {
    if (title.trim() || content.trim()) {
      Alert.alert(
        '나가기',
        '작성 중인 내용이 있습니다. 나가시겠습니까?',
        [
          { text: '취소', style: 'cancel' },
          { text: '나가기', style: 'destructive', onPress: () => navigation.goBack() },
        ]
      );
    } else {
      navigation.goBack();
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={handleBack} style={styles.backButton}>
          <ArrowLeft size={24} color={colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>
          {isEdit ? '게시글 수정' : '글쓰기'}
        </Text>
        <TouchableOpacity
          style={[styles.submitButton, !canSubmit && styles.submitButtonDisabled]}
          onPress={handleSubmit}
          disabled={!canSubmit}
        >
          <Text style={[styles.submitText, !canSubmit && styles.submitTextDisabled]}>
            {isEdit ? '수정' : '등록'}
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.content} keyboardShouldPersistTaps="handled">
        {/* 카테고리 선택 */}
        <TouchableOpacity
          style={styles.categorySelector}
          onPress={() => setShowCategoryPicker(!showCategoryPicker)}
        >
          <Text style={styles.categoryLabel}>카테고리</Text>
          <View style={styles.categoryValue}>
            <Text style={styles.categoryValueText}>{category}</Text>
            <ChevronDown size={20} color={colors.textSecondary} />
          </View>
        </TouchableOpacity>

        {/* 카테고리 옵션 */}
        {showCategoryPicker && (
          <View style={styles.categoryOptions}>
            {CATEGORIES.map((cat) => (
              <TouchableOpacity
                key={cat}
                style={[styles.categoryOption, category === cat && styles.categoryOptionActive]}
                onPress={() => {
                  setCategory(cat);
                  setShowCategoryPicker(false);
                }}
              >
                <Text style={[
                  styles.categoryOptionText,
                  category === cat && styles.categoryOptionTextActive
                ]}>
                  {cat}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        )}

        {/* 제목 입력 */}
        <View style={styles.inputSection}>
          <TextInput
            style={styles.titleInput}
            placeholder="제목을 입력하세요"
            placeholderTextColor={colors.textMuted}
            value={title}
            onChangeText={setTitle}
            maxLength={50}
          />
          <Text style={styles.charCount}>{title.length}/50</Text>
        </View>

        {/* 내용 입력 */}
        <View style={styles.inputSection}>
          <TextInput
            style={styles.contentInput}
            placeholder="내용을 입력하세요 (최소 10자)"
            placeholderTextColor={colors.textMuted}
            value={content}
            onChangeText={setContent}
            multiline
            textAlignVertical="top"
            maxLength={5000}
          />
          <Text style={styles.charCount}>{content.length}/5000</Text>
        </View>
      </ScrollView>

      {/* 하단 안내 */}
      <View style={styles.footer}>
        <Text style={styles.footerText}>
          💡 커뮤니티 규칙을 지켜주세요
        </Text>
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
  submitButton: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: 8,
  },
  submitButtonDisabled: {
    backgroundColor: colors.bgLight,
  },
  submitText: {
    fontSize: fontSize.md,
    fontWeight: '600',
    color: colors.textPrimary,
  },
  submitTextDisabled: {
    color: colors.textMuted,
  },
  content: {
    flex: 1,
  },
  categorySelector: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  categoryLabel: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
  },
  categoryValue: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  categoryValueText: {
    fontSize: fontSize.md,
    color: colors.textPrimary,
    fontWeight: '600',
  },
  categoryOptions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    padding: spacing.md,
    gap: spacing.sm,
    backgroundColor: colors.bgMedium,
  },
  categoryOption: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: 20,
    backgroundColor: colors.bgLight,
  },
  categoryOptionActive: {
    backgroundColor: colors.primary,
  },
  categoryOptionText: {
    fontSize: fontSize.sm,
    color: colors.textSecondary,
  },
  categoryOptionTextActive: {
    color: colors.textPrimary,
    fontWeight: '600',
  },
  inputSection: {
    padding: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  titleInput: {
    fontSize: fontSize.lg,
    color: colors.textPrimary,
    fontWeight: '600',
    paddingVertical: spacing.sm,
  },
  contentInput: {
    fontSize: fontSize.md,
    color: colors.textPrimary,
    minHeight: 200,
    paddingVertical: spacing.sm,
    lineHeight: 24,
  },
  charCount: {
    fontSize: fontSize.xs,
    color: colors.textMuted,
    textAlign: 'right',
    marginTop: spacing.xs,
  },
  footer: {
    padding: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.bgMedium,
  },
  footerText: {
    fontSize: fontSize.sm,
    color: colors.textMuted,
    textAlign: 'center',
  },
});
