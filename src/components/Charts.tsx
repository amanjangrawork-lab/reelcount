import React from 'react';
import {StyleSheet, Text, View} from 'react-native';
import Svg, {Circle, G, Path, Rect} from 'react-native-svg';
import {chartPalette, colors, spacing} from '../theme';

const VIEW_BOX_WIDTH = 320;

interface BarChartProps {
  values: number[];
  labels: string[];
  height?: number;
  color?: string;
}

/** Simple SVG column chart used for the seven day view. */
export function BarChart({values, labels, height = 180, color = colors.accent}: BarChartProps) {
  const max = Math.max(1, ...values);
  const gap = 8;
  const count = Math.max(1, values.length);
  const barWidth = Math.max(6, (VIEW_BOX_WIDTH - gap * (count + 1)) / count);

  return (
    <View>
      <Svg width="100%" height={height} viewBox={`0 0 ${VIEW_BOX_WIDTH} 180`}>
        <Rect x={0} y={0} width={VIEW_BOX_WIDTH} height={180} rx={12} fill={colors.underlay} />
        {values.map((value, index) => {
          const barHeight = Math.max(2, (value / max) * 140);
          const x = gap + index * (barWidth + gap);
          const y = 160 - barHeight;
          return (
            <Rect
              key={`bar-${index}`}
              x={x}
              y={y}
              width={barWidth}
              height={barHeight}
              rx={Math.min(6, barWidth / 2)}
              fill={color}
              opacity={value === 0 ? 0.25 : 1}
            />
          );
        })}
        <Rect x={0} y={164} width={VIEW_BOX_WIDTH} height={2} fill={colors.border} />
      </Svg>
      <View style={styles.axisRow}>
        {labels.map((label, index) => (
          <Text key={`axis-${index}`} style={styles.axisLabel}>
            {label}
          </Text>
        ))}
      </View>
    </View>
  );
}

interface LineChartProps {
  values: number[];
  height?: number;
  color?: string;
}

/** Simple SVG line chart used for the thirty day view. */
export function LineChart({values, height = 180, color = colors.chart}: LineChartProps) {
  const max = Math.max(1, ...values);
  const step = values.length > 1 ? VIEW_BOX_WIDTH / (values.length - 1) : VIEW_BOX_WIDTH;

  const points = values.map((value, index) => {
    const x = values.length > 1 ? index * step : VIEW_BOX_WIDTH / 2;
    const y = 160 - (value / max) * 140;
    return {x, y};
  });

  const line = points
    .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)},${point.y.toFixed(1)}`)
    .join(' ');

  const area =
    points.length > 0 ? `${line} L${points[points.length - 1].x.toFixed(1)},160 L${points[0].x.toFixed(1)},160 Z` : '';

  return (
    <Svg width="100%" height={height} viewBox={`0 0 ${VIEW_BOX_WIDTH} 180`}>
      <Rect x={0} y={0} width={VIEW_BOX_WIDTH} height={180} rx={12} fill={colors.underlay} />
      <Rect x={0} y={60} width={VIEW_BOX_WIDTH} height={1} fill={colors.border} />
      <Rect x={0} y={110} width={VIEW_BOX_WIDTH} height={1} fill={colors.border} />
      <Rect x={0} y={160} width={VIEW_BOX_WIDTH} height={1} fill={colors.border} />
      {area ? <Path d={area} fill={color} opacity={0.12} /> : null}
      {line ? <Path d={line} stroke={color} strokeWidth={2} fill="none" /> : null}
      {points.map((point, index) =>
        index === points.length - 1 ? (
          <Circle key={`point-${index}`} cx={point.x} cy={point.y} r={3.5} fill={color} />
        ) : null,
      )}
    </Svg>
  );
}

export interface DonutSlice {
  label: string;
  value: number;
}

interface DonutChartProps {
  slices: DonutSlice[];
  size?: number;
}

/** Per-app donut built from stroked circle segments. */
export function DonutChart({slices, size = 200}: DonutChartProps) {
  const data = slices.filter(slice => slice.value > 0);
  const total = data.reduce((sum, slice) => sum + slice.value, 0);
  const radius = 60;
  const circumference = 2 * Math.PI * radius;

  let offset = 0;

  return (
    <View style={styles.donutWrap}>
      <Svg width={size} height={size} viewBox="0 0 160 160">
        <Circle cx={80} cy={80} r={radius} stroke={colors.border} strokeWidth={18} fill="none" />
        <G rotation={-90} origin="80, 80">
          {data.map((slice, index) => {
            const fraction = total > 0 ? slice.value / total : 0;
            const length = fraction * circumference;
            const dash = `${length} ${circumference - length}`;
            const element = (
              <Circle
                key={`slice-${slice.label}`}
                cx={80}
                cy={80}
                r={radius}
                stroke={chartPalette[index % chartPalette.length]}
                strokeWidth={18}
                fill="none"
                strokeDasharray={dash}
                strokeDashoffset={-offset}
                strokeLinecap="butt"
              />
            );
            offset += length;
            return element;
          })}
        </G>
        <Circle cx={80} cy={80} r={44} fill={colors.card} />
      </Svg>

      <View style={styles.legend}>
        {data.length === 0 ? (
          <Text style={styles.legendEmpty}>No reels in this range</Text>
        ) : (
          data.map((slice, index) => (
            <View key={slice.label} style={styles.legendRow}>
              <View style={[styles.legendDot, {backgroundColor: chartPalette[index % chartPalette.length]}]} />
              <Text style={styles.legendLabel}>{slice.label}</Text>
              <Text style={styles.legendValue}>{slice.value}</Text>
            </View>
          ))
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  donutWrap: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: spacing.sm,
  },
  legend: {
    flex: 1,
    marginLeft: spacing.md,
  },
  legendRow: {
    alignItems: 'center',
    flexDirection: 'row',
    marginBottom: spacing.xs,
  },
  legendDot: {
    borderRadius: 5,
    height: 10,
    marginRight: spacing.sm,
    width: 10,
  },
  legendLabel: {
    color: colors.text,
    flex: 1,
    fontSize: 14,
  },
  legendValue: {
    color: colors.muted,
    fontSize: 14,
    fontVariant: ['tabular-nums'],
  },
  legendEmpty: {
    color: colors.muted,
    fontSize: 14,
  },
  axisRow: {
    flexDirection: 'row',
    marginTop: spacing.xs,
  },
  axisLabel: {
    color: colors.muted,
    flex: 1,
    fontSize: 10,
    textAlign: 'center',
  },
});
