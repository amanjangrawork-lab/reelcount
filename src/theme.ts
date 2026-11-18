export const colors = {
  background: '#F8FAFC',
  card: '#FFFFFF',
  text: '#0F172A',
  muted: '#64748B',
  accent: '#22C55E',
  border: '#E2E8F0',
  chart: '#0EA5E9',
  underlay: '#F1F5F9',
  danger: '#EF4444',
  onAccent: '#FFFFFF',
  warningBackground: '#FEF3C7',
  warningText: '#92400E',
  successBackground: '#ECFDF5',
  successText: '#065F46',
  successSoft: '#DCFCE7',
};

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
};

export interface BrainState {
  color: string;
  label: string;
  emoji: string;
}

/** Brain state brackets from the product spec. */
export function brainState(total: number): BrainState {
  if (total <= 50) {
    return {color: colors.accent, label: 'Calm', emoji: '🟢'};
  }
  if (total <= 100) {
    return {color: '#EAB308', label: 'Warming up', emoji: '🟡'};
  }
  if (total <= 200) {
    return {color: '#F97316', label: 'Heavy', emoji: '🟠'};
  }
  return {color: colors.danger, label: 'Scroll overload', emoji: '🔴'};
}

export const chartPalette = ['#22C55E', '#0EA5E9', '#F97316', '#A855F7', '#EF4444', '#14B8A6'];

/** Clamps a number into the 10..500 limit range used by the slider. */
export function clampLimit(value: number): number {
  return Math.min(500, Math.max(10, Math.round(value)));
}
