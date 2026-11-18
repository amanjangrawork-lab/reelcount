import React, {useCallback, useState} from 'react';
import {ScrollView, StyleSheet, Text, View} from 'react-native';
import {useFocusEffect} from '@react-navigation/native';
import {BarChart, DonutChart, DonutSlice, LineChart} from '../components/Charts';
import {DailyTotal, RangeSummary, ReelCount, TRACKED_APPS} from '../native/ReelCount';
import {colors, spacing} from '../theme';

export default function StatsScreen() {
  const [week, setWeek] = useState<DailyTotal[]>([]);
  const [month, setMonth] = useState<DailyTotal[]>([]);
  const [summary, setSummary] = useState<RangeSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [weekData, monthData, monthSummary] = await Promise.all([
        ReelCount.getDailyTotals(7),
        ReelCount.getDailyTotals(30),
        ReelCount.getRangeSummary(30),
      ]);
      setWeek(weekData);
      setMonth(monthData);
      setSummary(monthSummary);
      setError(null);
    } catch (nativeError) {
      setError(nativeError instanceof Error ? nativeError.message : 'Could not load stats.');
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  const weekValues = week.map(day => day.total);
  const weekLabels = week.map(day => day.label);
  const monthValues = month.map(day => day.total);

  const slices: DonutSlice[] = TRACKED_APPS.map(app => ({
    label: app.name,
    value: summary?.perApp[app.pkg] ?? 0,
  }));

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      {error ? <Text style={styles.error}>{error}</Text> : null}

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Last 7 days</Text>
        <BarChart values={weekValues.length > 0 ? weekValues : [0]} labels={weekLabels} />
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Last 30 days</Text>
        <LineChart values={monthValues.length > 0 ? monthValues : [0]} />
        <Text style={styles.caption}>
          {summary
            ? `${summary.total} reels · average ${summary.averagePerDay}/day · best day ${summary.bestDay}`
            : 'No data yet'}
        </Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>By app (30 days)</Text>
        <DonutChart slices={slices} />
      </View>

      <Text style={styles.footnote}>Charts are drawn from the local database only.</Text>
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
    fontWeight: '700',
    marginBottom: spacing.sm,
  },
  caption: {
    color: colors.muted,
    fontSize: 12,
    marginTop: spacing.sm,
  },
  footnote: {
    color: colors.muted,
    fontSize: 12,
    textAlign: 'center',
  },
  error: {
    color: colors.danger,
    fontSize: 13,
    marginBottom: spacing.md,
    textAlign: 'center',
  },
});
