import {DeviceEventEmitter, NativeModules, TurboModuleRegistry} from 'react-native';

export type PerAppCounts = Record<string, number>;

export interface ReelCountedEvent {
  pkg: string;
  total: number;
  perApp: PerAppCounts;
}

export interface LimitReachedEvent {
  pkg: string;
  total: number;
}

export interface BlockStateEvent {
  blocked: boolean;
}

export interface DailyTotal {
  dateEpochDay: number;
  label: string;
  total: number;
  perApp: PerAppCounts;
}

export interface RangeSummary {
  days: number;
  total: number;
  averagePerDay: number;
  bestDay: number;
  perApp: PerAppCounts;
}

export interface ReelCountSettings {
  limit: number;
  blockEnabled: boolean;
  overlayEnabled: boolean;
  allDayLockUntil: number;
  lockedNow: boolean;
  perAppEnabled: Record<string, boolean>;
}

interface ReelCountNative {
  getTodayCount(): Promise<number>;
  getPerAppCounts(): Promise<PerAppCounts>;
  getDailyTotals(days: number): Promise<DailyTotal[]>;
  getRangeSummary(days: number): Promise<RangeSummary>;
  setLimit(limit: number): Promise<void>;
  setBlockEnabled(enabled: boolean): Promise<void>;
  setOverlayEnabled(enabled: boolean): Promise<void>;
  setPerAppEnabled(pkg: string, enabled: boolean): Promise<void>;
  setAllDayLockUntil(epochMillis: number): Promise<void>;
  getSettings(): Promise<ReelCountSettings>;
  isAccessibilityServiceEnabled(): Promise<boolean>;
  openAccessibilitySettings(): Promise<void>;
  isOverlayPermissionGranted(): Promise<boolean>;
  requestOverlayPermission(): Promise<void>;
}

/**
 * Resolve the native module. `TurboModuleRegistry` is the New Architecture
 * path; React Native's interop layer answers it for modules registered through
 * a ReactPackage. `NativeModules` is the fallback for the old bridge.
 */
const registry = TurboModuleRegistry as unknown as {
  get(name: string): unknown | null;
};

const resolved =
  (registry.get('ReelCount') as ReelCountNative | null) ??
  (NativeModules.ReelCount as ReelCountNative | undefined) ??
  null;

function requireModule(): ReelCountNative {
  if (!resolved) {
    throw new Error('ReelCount native module is unavailable. Rebuild the Android app after changing native code.');
  }
  return resolved;
}

function subscribe<T>(eventName: string, handler: (event: T) => void): () => void {
  const subscription = DeviceEventEmitter.addListener(eventName, handler);
  return () => subscription.remove();
}

export const ReelCount = {
  getTodayCount: (): Promise<number> => requireModule().getTodayCount(),
  getPerAppCounts: (): Promise<PerAppCounts> => requireModule().getPerAppCounts(),
  getDailyTotals: (days: number): Promise<DailyTotal[]> => requireModule().getDailyTotals(days),
  getRangeSummary: (days: number): Promise<RangeSummary> => requireModule().getRangeSummary(days),
  setLimit: (limit: number): Promise<void> => requireModule().setLimit(limit),
  setBlockEnabled: (enabled: boolean): Promise<void> => requireModule().setBlockEnabled(enabled),
  setOverlayEnabled: (enabled: boolean): Promise<void> => requireModule().setOverlayEnabled(enabled),
  setPerAppEnabled: (pkg: string, enabled: boolean): Promise<void> => requireModule().setPerAppEnabled(pkg, enabled),
  setAllDayLockUntil: (epochMillis: number): Promise<void> => requireModule().setAllDayLockUntil(epochMillis),
  getSettings: (): Promise<ReelCountSettings> => requireModule().getSettings(),
  isAccessibilityServiceEnabled: (): Promise<boolean> => requireModule().isAccessibilityServiceEnabled(),
  openAccessibilitySettings: (): Promise<void> => requireModule().openAccessibilitySettings(),
  isOverlayPermissionGranted: (): Promise<boolean> => requireModule().isOverlayPermissionGranted(),
  requestOverlayPermission: (): Promise<void> => requireModule().requestOverlayPermission(),
};

export const ReelCountEventNames = {
  reelCounted: 'onReelCounted',
  limitReached: 'onLimitReached',
  blockStateChanged: 'onBlockStateChanged',
} as const;

export const onReelCounted = (handler: (event: ReelCountedEvent) => void): (() => void) =>
  subscribe<ReelCountedEvent>(ReelCountEventNames.reelCounted, handler);

export const onLimitReached = (handler: (event: LimitReachedEvent) => void): (() => void) =>
  subscribe<LimitReachedEvent>(ReelCountEventNames.limitReached, handler);

export const onBlockStateChanged = (handler: (event: BlockStateEvent) => void): (() => void) =>
  subscribe<BlockStateEvent>(ReelCountEventNames.blockStateChanged, handler);

/**
 * UI mirror of the tracked apps. The native source of truth for packages,
 * labels and view ids is `android/.../util/TrackedApps.kt`; keep the two lists
 * in step when adding an app.
 */
export const TRACKED_APPS = [
  {pkg: 'com.instagram.android', name: 'Instagram'},
  {pkg: 'com.google.android.youtube', name: 'YouTube'},
  {pkg: 'com.facebook.katana', name: 'Facebook'},
  {pkg: 'com.snapchat.android', name: 'Snapchat'},
] as const;
