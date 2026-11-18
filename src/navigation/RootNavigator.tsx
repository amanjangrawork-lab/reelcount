import React from 'react';
import {StyleSheet, Text} from 'react-native';
import {NavigationContainer} from '@react-navigation/native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import DashboardScreen from '../screens/DashboardScreen';
import SettingsScreen from '../screens/SettingsScreen';
import StatsScreen from '../screens/StatsScreen';
import {colors} from '../theme';

export type RootTabParamList = {
  Dashboard: undefined;
  Stats: undefined;
  Settings: undefined;
};

const Tab = createBottomTabNavigator<RootTabParamList>();

const ICONS: Record<keyof RootTabParamList, string> = {
  Dashboard: '📊',
  Stats: '📈',
  Settings: '⚙️',
};

export default function RootNavigator() {
  return (
    <NavigationContainer>
      <Tab.Navigator
        screenOptions={({route}) => ({
          headerStyle: styles.header,
          headerTitleStyle: styles.headerTitle,
          headerShadowVisible: false,
          tabBarActiveTintColor: colors.accent,
          tabBarInactiveTintColor: colors.muted,
          tabBarStyle: styles.tabBar,
          tabBarIcon: ({focused}) => (
            <Text style={[styles.icon, !focused && styles.iconMuted]}>{ICONS[route.name]}</Text>
          ),
        })}>
        <Tab.Screen name="Dashboard" component={DashboardScreen} options={{title: 'Today'}} />
        <Tab.Screen name="Stats" component={StatsScreen} options={{title: 'Stats'}} />
        <Tab.Screen name="Settings" component={SettingsScreen} options={{title: 'Settings'}} />
      </Tab.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  header: {
    backgroundColor: colors.background,
  },
  headerTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '700',
  },
  tabBar: {
    backgroundColor: colors.card,
    borderTopColor: colors.border,
  },
  icon: {
    fontSize: 18,
  },
  iconMuted: {
    opacity: 0.5,
  },
});
