import AsyncStorage from '@react-native-async-storage/async-storage';

const CONSENT_KEY = 'reelcount.consent.v1';
const ONBOARDED_KEY = 'reelcount.onboarded.v1';

export async function hasAcceptedConsent(): Promise<boolean> {
  const value = await AsyncStorage.getItem(CONSENT_KEY);
  return value === 'accepted';
}

export async function acceptConsent(): Promise<void> {
  await AsyncStorage.setItem(CONSENT_KEY, 'accepted');
}

export async function hasFinishedOnboarding(): Promise<boolean> {
  const value = await AsyncStorage.getItem(ONBOARDED_KEY);
  return value === 'true';
}

export async function finishOnboarding(): Promise<void> {
  await AsyncStorage.setItem(ONBOARDED_KEY, 'true');
}
