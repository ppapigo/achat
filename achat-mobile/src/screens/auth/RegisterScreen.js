import { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
  ScrollView,
} from 'react-native';
import { colors, spacing, fontSize } from '../../styles/theme';
import { useAuthStore } from '../../store/authStore';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';

export default function RegisterScreen({ navigation }) {
  const [step, setStep] = useState('register'); // 'register' | 'verify'
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [verifyCode, setVerifyCode] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { register, verifyEmail, resendCode } = useAuthStore();

  const handleRegister = async () => {
    if (!username.trim() || !email.trim() || !password.trim()) {
      Alert.alert('입력 오류', '모든 필드를 입력하세요');
      return;
    }
    if (password !== confirmPassword) {
      Alert.alert('입력 오류', '비밀번호가 일치하지 않습니다');
      return;
    }
    if (password.length < 6) {
      Alert.alert('입력 오류', '비밀번호는 6자 이상이어야 합니다');
      return;
    }

    setLoading(true);
    try {
      await register(email, password, username);
      // 인증 코드 입력 단계로 이동
      setStep('verify');
      Alert.alert('인증 코드 발송', '이메일로 인증 코드가 발송되었습니다');
    } catch (error) {
      Alert.alert(
        '회원가입 실패',
        error.response?.data?.error || error.message || '다시 시도해주세요'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    if (!verifyCode.trim()) {
      Alert.alert('입력 오류', '인증 코드를 입력하세요');
      return;
    }

    setLoading(true);
    try {
      await verifyEmail(email, verifyCode);
      Alert.alert('인증 완료', '회원가입이 완료되었습니다. 로그인해주세요', [
        { text: '확인', onPress: () => navigation.goBack() }
      ]);
    } catch (error) {
      Alert.alert(
        '인증 실패',
        error.response?.data?.error || error.message || '인증 코드를 확인해주세요'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleResendCode = async () => {
    setLoading(true);
    try {
      await resendCode(email);
      Alert.alert('재발송 완료', '인증 코드가 재발송되었습니다');
    } catch (error) {
      Alert.alert('오류', '재발송에 실패했습니다');
    } finally {
      setLoading(false);
    }
  };

  // 인증 코드 입력 단계
  if (step === 'verify') {
    return (
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      >
        <ScrollView 
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
        >
          <Text style={styles.title}>이메일 인증</Text>
          <Text style={styles.subtitle}>{email}로 발송된{'\n'}인증 코드를 입력하세요</Text>
          
          <View style={styles.form}>
            <Input
              label="인증 코드"
              placeholder="6자리 코드 입력"
              value={verifyCode}
              onChangeText={setVerifyCode}
              keyboardType="number-pad"
              maxLength={6}
            />
            
            <Button
              title="인증 확인"
              onPress={handleVerify}
              loading={loading}
              style={styles.verifyButton}
            />
            
            <Button
              title="인증 코드 재발송"
              onPress={handleResendCode}
              variant="outline"
              disabled={loading}
            />
            
            <Button
              title="처음으로"
              onPress={() => setStep('register')}
              variant="secondary"
            />
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    );
  }

  // 회원가입 단계
  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView 
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={styles.title}>회원가입</Text>
        <Text style={styles.subtitle}>A.chat에 오신 것을 환영합니다!</Text>
        
        <View style={styles.form}>
          <Input
            label="닉네임"
            placeholder="게임에서 사용할 이름"
            value={username}
            onChangeText={setUsername}
          />
          <Input
            label="이메일"
            placeholder="example@email.com"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
          />
          <Input
            label="비밀번호"
            placeholder="6자 이상"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />
          <Input
            label="비밀번호 확인"
            placeholder="비밀번호를 다시 입력"
            value={confirmPassword}
            onChangeText={setConfirmPassword}
            secureTextEntry
          />
          
          <Button
            title="가입하기"
            onPress={handleRegister}
            loading={loading}
            style={styles.registerButton}
          />
          
          <Button
            title="로그인으로 돌아가기"
            onPress={() => navigation.goBack()}
            variant="outline"
          />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bgDark,
  },
  content: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.xl,
  },
  title: {
    fontSize: fontSize.xl,
    fontWeight: 'bold',
    color: colors.textPrimary,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: fontSize.md,
    color: colors.textSecondary,
    textAlign: 'center',
    marginTop: spacing.sm,
    marginBottom: spacing.xl,
  },
  form: {
    gap: spacing.xs,
  },
  registerButton: {
    marginTop: spacing.md,
  },
  verifyButton: {
    marginTop: spacing.md,
  },
});
