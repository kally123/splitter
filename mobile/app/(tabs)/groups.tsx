import { View, Text, FlatList, StyleSheet, TouchableOpacity, useColorScheme } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Link } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

interface Group {
  id: string;
  name: string;
  memberCount: number;
  balance: number;
  lastActivity: string;
}

// Mock data - would come from API
const mockGroups: Group[] = [
  { id: "1", name: "Apartment 4B", memberCount: 3, balance: -45.50, lastActivity: "2 hours ago" },
  { id: "2", name: "Trip to Paris", memberCount: 5, balance: 120.00, lastActivity: "Yesterday" },
  { id: "3", name: "Office Lunch", memberCount: 8, balance: 0, lastActivity: "3 days ago" },
];

export default function GroupsScreen() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);

  const renderGroup = ({ item }: { item: Group }) => (
    <Link href={`/group/${item.id}`} asChild>
      <TouchableOpacity style={styles.groupCard}>
        <View style={styles.groupIcon}>
          <Ionicons name="people" size={24} color="#007AFF" />
        </View>
        <View style={styles.groupInfo}>
          <Text style={styles.groupName}>{item.name}</Text>
          <Text style={styles.groupMeta}>
            {item.memberCount} members • {item.lastActivity}
          </Text>
        </View>
        <View style={styles.balanceContainer}>
          <Text
            style={[
              styles.balance,
              item.balance > 0 ? styles.positive : item.balance < 0 ? styles.negative : styles.settled,
            ]}
          >
            {item.balance === 0
              ? "Settled"
              : item.balance > 0
              ? `+$${item.balance.toFixed(2)}`
              : `-$${Math.abs(item.balance).toFixed(2)}`}
          </Text>
        </View>
      </TouchableOpacity>
    </Link>
  );

  return (
    <SafeAreaView style={styles.container} edges={["bottom"]}>
      <FlatList
        data={mockGroups}
        renderItem={renderGroup}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <TouchableOpacity style={styles.createButton}>
            <Ionicons name="add-circle" size={24} color="#007AFF" />
            <Text style={styles.createButtonText}>Create New Group</Text>
          </TouchableOpacity>
        }
      />
    </SafeAreaView>
  );
}

const createStyles = (colorScheme: "light" | "dark" | null | undefined) =>
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: colorScheme === "dark" ? "#000000" : "#f5f5f5",
    },
    list: {
      padding: 16,
    },
    createButton: {
      flexDirection: "row",
      alignItems: "center",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      padding: 16,
      borderRadius: 12,
      marginBottom: 16,
    },
    createButtonText: {
      marginLeft: 12,
      fontSize: 16,
      fontWeight: "600",
      color: "#007AFF",
    },
    groupCard: {
      flexDirection: "row",
      alignItems: "center",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      padding: 16,
      borderRadius: 12,
      marginBottom: 12,
    },
    groupIcon: {
      width: 48,
      height: 48,
      borderRadius: 24,
      backgroundColor: colorScheme === "dark" ? "#2c2c2e" : "#f0f0f0",
      alignItems: "center",
      justifyContent: "center",
    },
    groupInfo: {
      flex: 1,
      marginLeft: 12,
    },
    groupName: {
      fontSize: 16,
      fontWeight: "600",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    groupMeta: {
      fontSize: 14,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      marginTop: 2,
    },
    balanceContainer: {
      alignItems: "flex-end",
    },
    balance: {
      fontSize: 16,
      fontWeight: "600",
    },
    positive: {
      color: "#34C759",
    },
    negative: {
      color: "#FF3B30",
    },
    settled: {
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
    },
  });
