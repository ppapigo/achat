// TextEncoder polyfill for STOMP (React Native)
if (typeof global.TextEncoder === 'undefined') {
    global.TextEncoder = class TextEncoder {
        encode(str) {
            const buf = new Uint8Array(str.length);
            for (let i = 0; i < str.length; i++) {
                buf[i] = str.charCodeAt(i);
            }
            return buf;
        }
    };
}
if (typeof global.TextDecoder === 'undefined') {
    global.TextDecoder = class TextDecoder {
        decode(buf) {
            return String.fromCharCode.apply(null, new Uint8Array(buf));
        }
    };
}
import { StatusBar } from 'expo-status-bar';
// TextEncoder polyfill for STOMP
import 'text-encoding';

import { SafeAreaProvider } from 'react-native-safe-area-context';
import RootNavigator from './src/navigation/RootNavigator';

export default function App() {
  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <RootNavigator />
    </SafeAreaProvider>
  );
}
