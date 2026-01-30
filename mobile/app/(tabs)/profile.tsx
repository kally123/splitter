import { View, Text, StyleSheet, TouchableOpacity, ScrollView, useColorScheme, Image } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";

export default function ProfileScreen() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);

  const menuItems = [
    { icon: "person", title: "Account Settings", route: "/settings/account" },
    { icon: "card", title: "Payment Methods", route: "/settings/payments" },
    { icon: "notifications", title: "Notifications", route: "/settings/notifications" },
    { icon: "shield-checkmark", title: "Privacy", route: "/settings/privacy" },
    { icon: "help-circle", title: "Help & Support", route: "/help" },
    { icon: "information-circle", title: "About", route: "/about" },
  ];

  return (
    <SafeAreaView style={styles.container} edges={["bottom"]}>
      <ScrollView contentContainerStyle={styles.content}>
        {/* Profile Header */}
        <View style={styles.profileHeader}>
          <View style={styles.avatarContainer}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>JD</Text>
            </View>
            <TouchableOpacity style={styles.editButton}>
              <Ionicons name="camera" size={16} color="#ffffff" />
            </TouchableOpacity>
          </View>
          <Text style={styles.name}>John Doe</Text>
          <Text style={styles.email}>john.doe@example.com</Text>
        </View>

        {/* Stats */}
        <View style={styles.statsContainer}>
          <View style={styles.stat}>
            <Text style={styles.statValue}>12</Text>
            <Text style={styles.statLabel}>Groups</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.stat}>
            <Text style={styles.statValue}>48</Text>
            <Text style={styles.statLabel}>Expenses</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.stat}>
            <Text style={styles.statValue}>$1.2k</Text>
            <Text style={styles.statLabel}>This Month</Text>
          </View>
        </View>

        {/* Menu */}
        <View style={styles.menu}>
          {menuItems.map((item, index) => (
            <TouchableOpacity
              key={index}
              style={styles.menuItem}
              onPress={() => router.push(item.route as any)}
            >
              <View style={styles.menuItemLeft}>
                <Ionicons name={item.icon as any} size={22} color="#007AFF" />
                <Text style={styles.menuItemTitle}>{item.title}</Text>
              </View>
              <Ionicons name="chevron-forward" size={20} color="#8e8e93" />
            </TouchableOpacity>
          ))}
        </View>

        {/* Logout */}
        <TouchableOpacity style={styles.logoutButton}>
          <Ionicons name="log-out" size={22} color="#FF3B30" />
          <Text style={styles.logoutText}>Log Out</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const createStyles = (colorScheme: "light" | "dark" | null | undefined) =>
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: colorScheme === "dark" ? "#000000" : "#f5f5f5",
    },
    content: {
      padding: 16,
    },
    profileHeader: {
      alignItems: "center",
      marginBottom: 24,
    },
    avatarContainer: {
      position: "relative",
      marginBottom: 12,
    },
    avatar: {
      width: 100,
      height: 100,
      borderRadius: 50,
      backgroundColor: "#007AFF",
      alignItems: "center",
      justifyContent: "center",
    },
    avatarText: {
      fontSize: 36,
      fontWeight: "600",
      color: "#ffffff",
    },
    editButton: {
      position: "absolute",
      bottom: 0,
      right: 0,
      width: 32,
      height: 32,
      borderRadius: 16,
      backgroundColor: "#34C759",
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 3,
      borderColor: colorScheme === "dark" ? "#000000" : "#f5f5f5",
    },
    name: {
      fontSize: 24,
      fontWeight: "bold",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    email: {
      fontSize: 16,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      marginTop: 4,
    },
    statsContainer: {
      flexDirection: "row",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      padding: 20,
      marginBottom: 24,
    },
    stat: {
      flex: 1,
      alignItems: "center",
    },
    statValue: {
      fontSize: 24,
      fontWeight: "bold",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    statLabel: {
      fontSize: 14,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      marginTop: 4,
    },
    statDivider: {
      width: 1,
      backgroundColor: colorScheme === "dark" ? "#38383a" : "#e5e5e5",
    },
    menu: {
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      marginBottom: 24,
    },
    menuItem: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      padding: 16,
      borderBottomWidth: 0.5,
      borderBottomColor: colorScheme === "dark" ? "#38383a" : "#e5e5e5",
    },
    menuItemLeft: {
      flexDirection: "row",
      alignItems: "center",
    },
    menuItemTitle: {
      fontSize: 16,
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
      marginLeft: 12,
    },
    logoutButton: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      padding: 16,
    },
    logoutText: {
      fontSize: 16,
      fontWeight: "600",
      color: "#FF3B30",
      marginLeft: 8,
    },
  });
