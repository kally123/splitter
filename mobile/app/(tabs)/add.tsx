import { View, Text, StyleSheet, TouchableOpacity, ScrollView, useColorScheme } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

export default function AddScreen() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);

  const options = [
    {
      icon: "receipt",
      title: "Add Expense",
      description: "Split a bill with friends",
      onPress: () => router.push("/expense/new"),
      color: "#007AFF",
    },
    {
      icon: "camera",
      title: "Scan Receipt",
      description: "Capture and auto-fill expense",
      onPress: () => router.push("/scan-receipt"),
      color: "#34C759",
    },
    {
      icon: "people",
      title: "Create Group",
      description: "Start a new expense group",
      onPress: () => router.push("/group/new"),
      color: "#FF9500",
    },
    {
      icon: "swap-horizontal",
      title: "Settle Up",
      description: "Record a payment",
      onPress: () => router.push("/settle"),
      color: "#5856D6",
    },
  ];

  return (
    <SafeAreaView style={styles.container} edges={["bottom"]}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>What would you like to do?</Text>
        
        <View style={styles.grid}>
          {options.map((option, index) => (
            <TouchableOpacity
              key={index}
              style={styles.optionCard}
              onPress={option.onPress}
            >
              <View style={[styles.iconContainer, { backgroundColor: option.color + "20" }]}>
                <Ionicons name={option.icon as any} size={32} color={option.color} />
              </View>
              <Text style={styles.optionTitle}>{option.title}</Text>
              <Text style={styles.optionDescription}>{option.description}</Text>
            </TouchableOpacity>
          ))}
        </View>
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
    title: {
      fontSize: 24,
      fontWeight: "bold",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
      marginBottom: 24,
      textAlign: "center",
    },
    grid: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 12,
    },
    optionCard: {
      width: "48%",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      padding: 20,
      alignItems: "center",
    },
    iconContainer: {
      width: 64,
      height: 64,
      borderRadius: 32,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 12,
    },
    optionTitle: {
      fontSize: 16,
      fontWeight: "600",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
      marginBottom: 4,
      textAlign: "center",
    },
    optionDescription: {
      fontSize: 13,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      textAlign: "center",
    },
  });
