import { View, Text, StyleSheet, useColorScheme } from "react-native";

export function BalanceSummary() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);

  // Mock data - would come from API
  const balance = {
    totalOwed: 145.50,
    totalOwedToYou: 78.25,
  };

  const netBalance = balance.totalOwedToYou - balance.totalOwed;

  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <View style={styles.column}>
          <Text style={styles.label}>You owe</Text>
          <Text style={[styles.value, styles.negative]}>
            ${balance.totalOwed.toFixed(2)}
          </Text>
        </View>
        <View style={styles.divider} />
        <View style={styles.column}>
          <Text style={styles.label}>You're owed</Text>
          <Text style={[styles.value, styles.positive]}>
            ${balance.totalOwedToYou.toFixed(2)}
          </Text>
        </View>
      </View>
      <View style={styles.netContainer}>
        <Text style={styles.netLabel}>Net balance</Text>
        <Text
          style={[
            styles.netValue,
            netBalance >= 0 ? styles.positive : styles.negative,
          ]}
        >
          {netBalance >= 0 ? "+" : ""}${netBalance.toFixed(2)}
        </Text>
      </View>
    </View>
  );
}

const createStyles = (colorScheme: "light" | "dark" | null | undefined) =>
  StyleSheet.create({
    container: {
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      padding: 20,
      marginBottom: 16,
    },
    row: {
      flexDirection: "row",
    },
    column: {
      flex: 1,
      alignItems: "center",
    },
    divider: {
      width: 1,
      backgroundColor: colorScheme === "dark" ? "#38383a" : "#e5e5e5",
    },
    label: {
      fontSize: 14,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      marginBottom: 4,
    },
    value: {
      fontSize: 24,
      fontWeight: "bold",
    },
    positive: {
      color: "#34C759",
    },
    negative: {
      color: "#FF3B30",
    },
    netContainer: {
      marginTop: 16,
      paddingTop: 16,
      borderTopWidth: 1,
      borderTopColor: colorScheme === "dark" ? "#38383a" : "#e5e5e5",
      alignItems: "center",
    },
    netLabel: {
      fontSize: 14,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
    },
    netValue: {
      fontSize: 28,
      fontWeight: "bold",
      marginTop: 4,
    },
  });
