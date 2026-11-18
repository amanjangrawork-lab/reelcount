import React, {useCallback, useEffect, useRef, useState} from 'react';
import {Animated, RefreshControl, ScrollView, StyleSheet, Text, View} from 'react-native';
import {useFocusEffect} from '@react-navigation/native';
import {onLimitReached, onReelCounted, PerAppCounts, ReelCount, TRACKED_APPS} from '../native/ReelCount';
import {brainState, colors, spacing} from '../theme';

export default function DashboardScreen() {
  const [total, setTotal] = useState(0);
  const [perApp, setPerApp] = useState<PerAppCounts>({});
  const [limit, setLimit] = useState(100);
  const [accessibilityOn, setAccessibilityOn] = useState(true);
  const [limitHit, setLimitHit] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [display, setDisplay] = useState(0);

  const animated = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const listener = animated.addListener(({value}) => {
      setDisplay(Math.round(value));
    });
    return () => {
      animated.removeListener(listener);
    };
  }, [animated]);

  useEffect(() => {
    Animated.timing(animated, {
      toValue: total,
      duration: 450,
      useNativeDriver: false,
    }).start();
  }, [animated, total]);

  const load = useCallback(async () => {
    try {
      const [todayTotal, counts, settings, enabled] = await Promise.all([
        ReelCount.getTodayCount(),
        ReelCount.getPerAppCounts(),
        ReelCount.getSettings(),
        ReelCount.isAccessibilityServiceEnabled(),
      ]);
      setTotal(todayTotal);
      setPerApp(counts);
      setLimit(settings.limit);
      setAccessibilityOn(enabled);
      setLimitHit(settings.blockEnabled && todayTotal >= settings.limit);
    } catch {
      setAccessibilityOn(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  useEffect(() => {
    const unsubscribeCount = onReelCounted(event => {
      setTotal(event.total);
      setPerApp(event.perApp);
    });
    const unsubscribeLimit = onLimitReached(() => {
      setLimitHit(true);
    });
    return () => {
      unsubscribeCount();
      unsubscribeLimit();
    };
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  }, [load]);

  const state = brainState(total);
  const progress = limit > 0 ? Math.min(1, total / limit) : 0;

  const rows = TRACKED_APPS.map(app => ({
    pkg: app.pkg,
    name: app.name,
    count: perApp[app.pkg] ?? 0,
  })).sort((a, b) => b.count - a.count);

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}>
      {!accessibilityOn ? (
        <View style={styles.warning}>
          <Text style={styles.warningText}>
            Accessibility Service is off, so nothing is being counted right now. Open the app's setup screen to turn it
            back on.
          </Text>
        </View>
      ) : null}

      <View style={styles.counterCard}>
        <Text style={styles.counterLabel}>Reels today</Text>
        <Text style={[styles.counterValue, {color: state.color}]}>{display}</Text>

        <View style={styles.brainRow}>
          <Text style={styles.brainEmoji}>{state.emoji}</Text>
          <Text style={[styles.brainLabel, {color: state.color}]}>{state.label}</Text>
        </View>

        <View style={styles.progressTrack}>
          <View style={[styles.progressFill, {backgroundColor: state.color, width: `${progress * 100}%`}]} />
        </View>
        <Text style={styles.progressCaption}>
          {total} of {limit} daily limit
        </Text>

        {limitHit ? <Text style={styles.limitHit}>Limit reached — ReelCount is stepping in.</Text> : null}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Per app</Text>
        {rows.map(row => (
          <View key={row.pkg} style={styles.row}>
            <Text style={styles.rowName}>{row.name}</Text>
            <Text style={styles.rowValue}>{row.count}</Text>
          </View>
        ))}
      </View>

      <Text style={styles.footnote}>Counted on this device only. Pull down to refresh.</Text>
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
  warning: {
    backgroundColor: '#FEF3C7',
    borderRadius: 12,
    marginBottom: spacing.md,
    padding: spacing.md,
  },
  warningText: {
    color: '#92400E',
    fontSize: 13,
    lineHeight: 19,
  },
  counterCard: {
    alignItems: 'center',
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: 20,
    borderWidth: 1,
    padding: spacing.lg,
  },
  counterLabel: {
    color: colors.muted,
    fontSize: 14,
    letterSpacing: 0.5,
    textTransform: 'uppercase',
  },
  counterValue: {
    fontSize: 76,
    fontVariant: ['tabular-nums'],
    fontWeight: '800',
    marginVertical: spacing.xs,
  },
  brainRow: {
    alignItems: 'center',
    flexDirection: 'row',
    marginBottom: spacing.md,
  },
  brainEmoji: {
    fontSize: 18,
    marginRight: spacing.sm,
  },
  brainLabel: {
    fontSize: 16,
    fontWeight: '600',
  },
  progressTrack: {
    backgroundColor: colors.underlay,
    borderRadius: 6,
    height: 10,
    overflow: 'hidden',
    width: '100%',
  },
  progressFill: {
    borderRadius: 6,
    height: 10,
  },
  progressCaption: {
    color: colors.muted,
    fontSize: 12,
    marginTop: spacing.sm,
  },
  limitHit: {
    color: colors.danger,
    fontSize: 13,
    fontWeight: '600',
    marginTop: spacing.md,
    textAlign: 'center',
  },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.border,
    borderRadius: 18,
    borderWidth: 1,
    marginTop: spacing.md,
    padding: spacing.md,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '700',
    marginBottom: spacing.sm,
  },
  row: {
    alignItems: 'center',
    borderTopColor: colors.border,
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm + 2,
  },
  rowName: {
    color: colors.text,
    fontSize: 15,
  },
  rowValue: {
    color: colors.text,
    fontSize: 15,
    fontVariant: ['tabular-nums'],
    fontWeight: '600',
  },
  footnote: {
    color: colors.muted,
    fontSize: 12,
    marginTop: spacing.md,
    textAlign: 'center',
  },
});
