import { View, Text, StyleSheet, TouchableOpacity, useColorScheme } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";

export function QuickActions() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);

  const actions = [
    { icon: "add-circle", label: "Expense", route: "/expense/new", color: "#007AFF" },
    { icon: "camera", label: "Scan", route: "/scan-receipt", color: "#34C759" },
    { icon: "people", label: "Group", route: "/group/new", color: "#FF9500" },
    { icon: "swap-horizontal", label: "Settle", route: "/settle", color: "#5856D6" },
  ];

  return (
    <View style={styles.container}>
      {actions.map((action, index) => (
        <TouchableOpacity
          key={index}
          style={styles.action}
          onPress={() => router.push(action.route as any)}
        >
          <View style={[styles.iconContainer, { backgroundColor: action.color + "20" }]}>
            <Ionicons name={action.icon as any} size={24} color={action.color} />
          </View>
          <Text style={styles.label}>{action.label}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}

const createStyles = (colorScheme: "light" | "dark" | null | undefined) =>
  StyleSheet.create({
    container: {
      flexDirection: "row",
      justifyContent: "space-between",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      padding: 16,
    },
    action: {
      alignItems: "center",
      flex: 1,
    },
    iconContainer: {
      width: 48,
      height: 48,
      borderRadius: 24,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 8,
    },
    label: {
      fontSize: 12,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
    },
  });
