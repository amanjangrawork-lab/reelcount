import React, {useCallback, useState} from 'react';
import {Alert, ScrollView, StyleSheet, Switch, Text, TouchableOpacity, View} from 'react-native';
import Slider from '@react-native-community/slider';
import {useFocusEffect} from '@react-navigation/native';
import {ReelCount, ReelCountSettings, TRACKED_APPS} from '../native/ReelCount';
import {clampLimit, colors, spacing} from '../theme';

interface LockPreset {
  label: string;
  minutes: number;
}

const LOCK_PRESETS: LockPreset[] = [
  {label: 'Off', minutes: 0},
  {label: '30 min', minutes: 30},
  {label: '1 h', minutes: 60},
  {label: '4 h', minutes: 240},
  {label: 'Rest of day', minutes: -1},
];

function lockLabel(settings: ReelCountSettings): string {
  if (!settings.lockedNow) {
    return 'Not locked';
  }
  const remainingMinutes = Math.max(1, Math.round((settings.allDayLockUntil - Date.now()) / 60000));
  const hours = Math.floor(remainingMinutes / 60);
  const minutes = remainingMinutes % 60;
  return hours > 0 ? `Locked for ${hours} h ${minutes} m` : `Locked for ${minutes} m`;
}

export default function SettingsScreen() {
  const [settings, setSettings] = useState<ReelCountSettings | null>(null);
  const [sliderValue, setSliderValue] = useState(100);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const loaded = await ReelCount.getSettings();
      setSettings(loaded);
      setSliderValue(loaded.limit);
      setError(null);
    } catch (nativeError) {
      setError(nativeError instanceof Error ? nativeError.message : 'Could not load settings.');
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  const applyLimit = useCallback(async (value: number) => {
    const clamped = clampLimit(value);
    setSliderValue(clamped);
    try {
      await ReelCount.setLimit(clamped);
      setSettings(current => (current ? {...current, limit: clamped} : current));
    } catch {
      setError('Could not save the new limit.');
    }
  }, []);

  const toggle = useCallback(
    async (key: 'blockEnabled' | 'overlayEnabled', value: boolean) => {
      setSettings(current => (current ? {...current, [key]: value} : current));
      try {
        if (key === 'blockEnabled') {
          await ReelCount.setBlockEnabled(value);
        } else {
          await ReelCount.setOverlayEnabled(value);
        }
      } catch {
        setError('Could not save that setting.');
        void load();
      }
    },
    [load],
  );

  const toggleApp = useCallback(
    async (pkg: string, value: boolean) => {
      setSettings(current =>
        current ? {...current, perAppEnabled: {...current.perAppEnabled, [pkg]: value}} : current,
      );
      try {
        await ReelCount.setPerAppEnabled(pkg, value);
      } catch {
        setError('Could not save that app toggle.');
        void load();
      }
    },
    [load],
  );

  const applyLock = useCallback(
    async (preset: LockPreset) => {
      let until = 0;
      if (preset.minutes === -1) {
        const endOfDay = new Date();
        endOfDay.setHours(23, 59, 59, 999);
        until = endOfDay.getTime();
      } else if (preset.minutes > 0) {
        until = Date.now() + preset.minutes * 60 * 1000;
      }

      try {
        await ReelCount.setAllDayLockUntil(until);
        await load();
      } catch {
        setError('Could not save the lock.');
      }
    },
    [load],
  );

  if (!settings) {
    return (
      <View style={styles.loading}>
        <Text style={styles.loadingText}>{error ?? 'Loading settings…'}</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Daily limit</Text>
        <Text style={styles.limitValue}>{sliderValue} reels</Text>
        <Slider
          minimumValue={10}
          maximumValue={500}
          step={10}
          value={sliderValue}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.border}
          thumbTintColor={colors.accent}
          onValueChange={setSliderValue}
          onSlidingComplete={value => {
            void applyLimit(value);
          }}
        />
        <View style={styles.sliderScale}>
          <Text style={styles.scaleText}>10</Text>
          <Text style={styles.scaleText}>500</Text>
        </View>
      </View>

      <View style={styles.card}>
        <View style={styles.switchRow}>
          <View style={styles.switchTextWrap}>
            <Text style={styles.cardTitle}>Block when limit is reached</Text>
            <Text style={styles.cardHint}>Steps out of Reels and Shorts with a full-screen reminder.</Text>
          </View>
          <Switch
            value={settings.blockEnabled}
            onValueChange={value => {
              void toggle('blockEnabled', value);
            }}
          />
        </View>

        <View style={styles.divider} />

        <View style={styles.switchRow}>
          <View style={styles.switchTextWrap}>
            <Text style={styles.cardTitle}>Floating counter</Text>
            <Text style={styles.cardHint}>A draggable pill with today's count. Tap it for the per-app breakdown.</Text>
          </View>
          <Switch
            value={settings.overlayEnabled}
            onValueChange={value => {
              void toggle('overlayEnabled', value);
            }}
          />
        </View>
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Apps to count</Text>
        {TRACKED_APPS.map(app => (
          <View key={app.pkg} style={styles.switchRow}>
            <Text style={styles.appName}>{app.name}</Text>
            <Switch
              value={settings.perAppEnabled[app.pkg] !== false}
              onValueChange={value => {
                void toggleApp(app.pkg, value);
              }}
            />
          </View>
        ))}
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>All-day lock</Text>
        <Text style={styles.cardHint}>{lockLabel(settings)}</Text>
        <View style={styles.presetRow}>
          {LOCK_PRESETS.map(preset => (
            <TouchableOpacity
              key={preset.label}
              style={styles.preset}
              onPress={() => {
                void applyLock(preset);
              }}>
              <Text style={styles.presetText}>{preset.label}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      <TouchableOpacity
        style={styles.dangerLink}
        onPress={() => {
          Alert.alert(
            'Revoke access',
            'This opens Android settings where you can switch off the ReelCount Accessibility Service.',
            [
              {text: 'Cancel', style: 'cancel'},
              {
                text: 'Open settings',
                onPress: () => {
                  void ReelCount.openAccessibilitySettings();
                },
              },
            ],
          );
        }}>
        <Text style={styles.dangerLinkText}>Revoke Accessibility access</Text>
      </TouchableOpacity>

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Text style={styles.version}>ReelCount 0.1.1 · GPL-3.0 · no network access</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: {
    backgroundColor: colors.background,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xl,
  },
  loading: {
    alignItems: 'center',
    backgroundColor: colors.background,
    flex: 1,
    justifyContent: 'center',
    padding: spacing.lg,
  },
  loadingText: {
    color: colors.muted,
    fontSize: 14,
    textAlign: 'center',
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: 18,
    borderWidth: 1,
    marginBottom: spacing.md,
    padding: spacing.md,
  },
  cardTitle: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '600',
  },
  cardHint: {
    color: colors.muted,
    fontSize: 13,
    lineHeight: 18,
    marginTop: spacing.xs,
  },
  limitValue: {
    color: colors.accent,
    fontSize: 32,
    fontVariant: ['tabular-nums'],
    fontWeight: '700',
    marginVertical: spacing.sm,
  },
  sliderScale: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  scaleText: {
    color: colors.muted,
    fontSize: 11,
  },
  switchRow: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm,
  },
  switchTextWrap: {
    flex: 1,
    paddingRight: spacing.md,
  },
  divider: {
    backgroundColor: colors.border,
    height: StyleSheet.hairlineWidth,
    marginVertical: spacing.xs,
  },
  appName: {
    color: colors.text,
    fontSize: 15,
  },
  presetRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: spacing.sm,
  },
  preset: {
    backgroundColor: colors.underlay,
    borderRadius: 12,
    marginBottom: spacing.sm,
    marginRight: spacing.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  presetText: {
    color: colors.text,
    fontSize: 13,
    fontWeight: '600',
  },
  dangerLink: {
    alignItems: 'center',
    paddingVertical: spacing.md,
  },
  dangerLinkText: {
    color: colors.danger,
    fontSize: 14,
    fontWeight: '600',
  },
  error: {
    color: colors.danger,
    fontSize: 13,
    textAlign: 'center',
  },
  version: {
    color: colors.muted,
    fontSize: 11,
    marginTop: spacing.md,
    textAlign: 'center',
  },
});
