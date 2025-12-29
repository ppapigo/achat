import { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
  TouchableOpacity,
} from 'react-native';
import { colors, spacing, fontSize } from '../../styles/theme';
import { useAuthStore } from '../../store/authStore';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';

export default function LoginScreen({ navigation }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  
  const login = useAuthStore((state) => state.login);

  const handleLogin = async () => {
    if (!email.trim() || !password.trim()) {
      Alert.alert('입력 오류', '이메일과 비밀번호를 입력하세요');
      return;
    }

    setLoading(true);
    try {
      await login(email, password);
    } catch (error) {
      Alert.alert(
        '로그인 실패',
        error.response?.data?.error || error.message || '이메일 또는 비밀번호를 확인하세요'
      );
    } finally {
      setLoading(false);
    }
  };

  // 개발용: 바로 로그인 (Mock)
  const handleDevLogin = async () => {
    useAuthStore.setState({ 
      isAuthenticated: true, 
      user: { id: 'dev-user', username: '테스터', email: 'test@test.com' } 
    });
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.content}>
        {/* 로고 */}
        <View style={styles.logoContainer}>
          <Text style={styles.logo}>A.chat</Text>
          <Text style={styles.tagline}>게임이 되는 채팅</Text>
        </View>

        {/* 입력 폼 */}
        <View style={styles.form}>
          <Input
            placeholder="이메일"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
          />
          <Input
            placeholder="비밀번호"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />
          
          <Button
            title="로그인"
            onPress={handleLogin}
            loading={loading}
            style={styles.loginButton}
          />
          
          <Button
            title="회원가입"
            onPress={() => navigation.navigate('Register')}
            variant="outline"
          />

          {/* 개발용 버튼 */}
          {__DEV__ && (
            <TouchableOpacity onPress={handleDevLogin} style={styles.devButton}>
              <Text style={styles.devButtonText}>🛠 개발용 바로가기</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: spacing.lg,
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: spacing.xl * 2,
  },
  logo: {
    fontSize: fontSize.xxl + 8,
    fontWeight: 'bold',
    color: colors.primary,
  },
  tagline: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    marginTop: spacing.sm,
  },
  form: {
    gap: spacing.sm,
  },
  loginButton: {
    marginTop: spacing.md,
  },
  devButton: {
    marginTop: spacing.lg,
    alignItems: 'center',
    padding: spacing.sm,
  },
  devButtonText: {
    color: colors.textMuted,
    fontSize: fontSize.sm,
  },
});
