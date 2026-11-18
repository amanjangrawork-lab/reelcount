import React, {useCallback, useEffect, useState} from 'react';
import {AppState, ScrollView, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {ReelCount} from '../native/ReelCount';
import {colors, spacing} from '../theme';

interface OnboardingScreenProps {
  onContinue: () => void;
}

interface PermissionRowProps {
  title: string;
  description: string;
  granted: boolean;
  actionLabel: string;
  onPress: () => void;
}

function PermissionRow({title, description, granted, actionLabel, onPress}: PermissionRowProps) {
  return (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>{title}</Text>
        <Text style={styles.check}>{granted ? '✅' : '⬜️'}</Text>
      </View>
      <Text style={styles.cardBody}>{description}</Text>
      <TouchableOpacity style={[styles.secondaryButton, granted && styles.secondaryButtonGranted]} onPress={onPress}>
        <Text style={styles.secondaryButtonText}>{granted ? 'Enabled — open settings again' : actionLabel}</Text>
      </TouchableOpacity>
    </View>
  );
}

export default function OnboardingScreen({onContinue}: OnboardingScreenProps) {
  const [accessibilityOn, setAccessibilityOn] = useState(false);
  const [overlayOn, setOverlayOn] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      const [accessibility, overlay] = await Promise.all([
        ReelCount.isAccessibilityServiceEnabled(),
        ReelCount.isOverlayPermissionGranted(),
      ]);
      setAccessibilityOn(accessibility);
      setOverlayOn(overlay);
      setError(null);
    } catch (nativeError) {
      setError(nativeError instanceof Error ? nativeError.message : 'Could not read permission state.');
    }
  }, []);

  useEffect(() => {
    void refresh();
    const subscription = AppState.addEventListener('change', state => {
      if (state === 'active') {
        void refresh();
      }
    });
    return () => subscription.remove();
  }, [refresh]);

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.emoji}>📵</Text>
      <Text style={styles.title}>Let's set up ReelCount</Text>
      <Text style={styles.subtitle}>
        ReelCount needs one special permission to notice when a Reel or Short is on screen. It never reads text,
        messages, photos or passwords — it only checks which app is in front and whether a known video container is
        visible.
      </Text>

      <PermissionRow
        title="1. Accessibility Service"
        description="Android's Accessibility Service is how an app can tell which screen is showing. ReelCount uses it to recognise Reels and Shorts and to press Back when your limit is reached. Find “ReelCount” in the list and switch it on."
        granted={accessibilityOn}
        actionLabel="Enable Accessibility Service"
        onPress={() => {
          void ReelCount.openAccessibilitySettings().catch(() => setError('Could not open settings.'));
        }}
      />

      <PermissionRow
        title="2. Draw over other apps"
        description="Optional. This lets the little counter pill float on top of Instagram, YouTube, Facebook and Snapchat, and shows the full-screen reminder when you hit your limit."
        granted={overlayOn}
        actionLabel="Allow Overlay"
        onPress={() => {
          void ReelCount.requestOverlayPermission().catch(() => setError('Could not open settings.'));
        }}
      />

      <View style={styles.privacyBox}>
        <Text style={styles.privacyTitle}>Private by design</Text>
        <Text style={styles.privacyBody}>
          ReelCount has no internet permission at all. Counts live only in a database on this phone.
        </Text>
      </View>

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <TouchableOpacity
        style={[styles.primaryButton, !accessibilityOn && styles.primaryButtonDisabled]}
        disabled={!accessibilityOn}
        onPress={onContinue}>
        <Text style={styles.primaryButtonText}>Continue</Text>
      </TouchableOpacity>

      {!accessibilityOn ? (
        <Text style={styles.hint}>
          Turn on the Accessibility Service to continue. Come back to this screen and the checkmark will appear
          automatically.
        </Text>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: colors.background,
    padding: spacing.lg,
    paddingBottom: spacing.xl,
  },
  emoji: {
    fontSize: 44,
    marginBottom: spacing.sm,
    textAlign: 'center',
  },
  title: {
    color: colors.text,
    fontSize: 26,
    fontWeight: '700',
    textAlign: 'center',
  },
  subtitle: {
    color: colors.muted,
    fontSize: 15,
    lineHeight: 22,
    marginBottom: spacing.lg,
    marginTop: spacing.sm,
    textAlign: 'center',
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: 16,
    borderWidth: 1,
    marginBottom: spacing.md,
    padding: spacing.md,
  },
  cardHeader: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  cardTitle: {
    color: colors.text,
    flex: 1,
    fontSize: 17,
    fontWeight: '600',
  },
  check: {
    fontSize: 18,
  },
  cardBody: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 20,
    marginTop: spacing.sm,
  },
  secondaryButton: {
    backgroundColor: colors.underlay,
    borderRadius: 12,
    marginTop: spacing.md,
    paddingVertical: spacing.sm + 2,
  },
  secondaryButtonGranted: {
    backgroundColor: colors.successSoft,
  },
  secondaryButtonText: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '600',
    textAlign: 'center',
  },
  privacyBox: {
    backgroundColor: colors.successBackground,
    borderRadius: 14,
    marginTop: spacing.sm,
    padding: spacing.md,
  },
  privacyTitle: {
    color: colors.successText,
    fontSize: 15,
    fontWeight: '700',
  },
  privacyBody: {
    color: colors.successText,
    fontSize: 13,
    lineHeight: 19,
    marginTop: spacing.xs,
  },
  error: {
    color: colors.danger,
    fontSize: 13,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  primaryButton: {
    backgroundColor: colors.accent,
    borderRadius: 14,
    marginTop: spacing.lg,
    paddingVertical: spacing.md,
  },
  primaryButtonDisabled: {
    opacity: 0.4,
  },
  primaryButtonText: {
    color: colors.onAccent,
    fontSize: 16,
    fontWeight: '700',
    textAlign: 'center',
  },
  hint: {
    color: colors.muted,
    fontSize: 12,
    lineHeight: 18,
    marginTop: spacing.md,
    textAlign: 'center',
  },
});
