import React, {useCallback, useEffect, useState} from 'react';
import {ActivityIndicator, ScrollView, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';
import RootNavigator from './src/navigation/RootNavigator';
import OnboardingScreen from './src/screens/OnboardingScreen';
import {acceptConsent, finishOnboarding, hasAcceptedConsent, hasFinishedOnboarding} from './src/storage/firstRun';
import {colors, spacing} from './src/theme';

type Stage = 'loading' | 'consent' | 'onboarding' | 'app';

const CONSENT_TEXT =
  'ReelCount uses Android\u2019s Accessibility Service to detect Reels and Shorts being viewed. It cannot read passwords, messages, or any content. All data stays on your device.';

export default function App() {
  const [stage, setStage] = useState<Stage>('loading');

  useEffect(() => {
    let mounted = true;

    const boot = async () => {
      try {
        if (!(await hasAcceptedConsent())) {
          if (mounted) {
            setStage('consent');
          }
          return;
        }
        if (!(await hasFinishedOnboarding())) {
          if (mounted) {
            setStage('onboarding');
          }
          return;
        }
        if (mounted) {
          setStage('app');
        }
      } catch {
        if (mounted) {
          setStage('consent');
        }
      }
    };

    void boot();

    return () => {
      mounted = false;
    };
  }, []);

  const onAccept = useCallback(async () => {
    try {
      await acceptConsent();
    } finally {
      setStage('onboarding');
    }
  }, []);

  const onFinishOnboarding = useCallback(async () => {
    try {
      await finishOnboarding();
    } finally {
      setStage('app');
    }
  }, []);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.root} edges={['top', 'left', 'right']}>
        {stage === 'loading' ? (
          <View style={styles.center}>
            <ActivityIndicator color={colors.accent} />
            <Text style={styles.loadingText}>Loading ReelCount…</Text>
          </View>
        ) : null}

        {stage === 'consent' ? (
          <ScrollView contentContainerStyle={styles.consentWrap}>
            <Text style={styles.consentEmoji}>👋</Text>
            <Text style={styles.consentTitle}>Before we start</Text>
            <Text style={styles.consentBody}>{CONSENT_TEXT}</Text>

            <View style={styles.consentCard}>
              <Text style={styles.consentPoint}>• No internet permission at all.</Text>
              <Text style={styles.consentPoint}>• No analytics, ads or trackers.</Text>
              <Text style={styles.consentPoint}>• Counts are stored in a local database you can wipe at any time.</Text>
            </View>

            <TouchableOpacity style={styles.primaryButton} onPress={() => void onAccept()}>
              <Text style={styles.primaryButtonText}>I understand, continue</Text>
            </TouchableOpacity>
          </ScrollView>
        ) : null}

        {stage === 'onboarding' ? <OnboardingScreen onContinue={() => void onFinishOnboarding()} /> : null}

        {stage === 'app' ? <RootNavigator /> : null}
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  root: {
    backgroundColor: colors.background,
    flex: 1,
  },
  center: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
  },
  loadingText: {
    color: colors.muted,
    fontSize: 14,
    marginTop: spacing.sm,
  },
  consentWrap: {
    padding: spacing.lg,
    paddingBottom: spacing.xl,
  },
  consentEmoji: {
    fontSize: 40,
    textAlign: 'center',
  },
  consentTitle: {
    color: colors.text,
    fontSize: 26,
    fontWeight: '700',
    marginTop: spacing.sm,
    textAlign: 'center',
  },
  consentBody: {
    color: colors.text,
    fontSize: 15,
    lineHeight: 23,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  consentCard: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: 16,
    borderWidth: 1,
    marginTop: spacing.lg,
    padding: spacing.md,
  },
  consentPoint: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 21,
    marginBottom: spacing.xs,
  },
  primaryButton: {
    backgroundColor: colors.accent,
    borderRadius: 14,
    marginTop: spacing.lg,
    paddingVertical: spacing.md,
  },
  primaryButtonText: {
    color: colors.onAccent,
    fontSize: 16,
    fontWeight: '700',
    textAlign: 'center',
  },
});
