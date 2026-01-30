import { View, Text, StyleSheet, TouchableOpacity, useColorScheme } from "react-native";
import { Link } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { formatDistanceToNow } from "date-fns";

interface Activity {
  id: string;
  type: "expense" | "payment";
  title: string;
  group: string;
  amount: number;
  timestamp: Date;
  isPayer: boolean;
}

// Mock data
const recentActivities: Activity[] = [
  {
    id: "1",
    type: "expense",
    title: "Dinner at Olive Garden",
    group: "Apartment 4B",
    amount: 85.5,
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000),
    isPayer: true,
  },
  {
    id: "2",
    type: "payment",
    title: "John paid you",
    group: "Apartment 4B",
    amount: 45.0,
    timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000),
    isPayer: false,
  },
  {
    id: "3",
    type: "expense",
    title: "Uber to Airport",
    group: "Trip to Paris",
    amount: 65.0,
    timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
    isPayer: false,
  },
];

export function RecentActivity() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);

  if (recentActivities.length === 0) {
    return (
      <View style={styles.empty}>
        <Ionicons name="receipt-outline" size={48} color="#8e8e93" />
        <Text style={styles.emptyText}>No recent activity</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {recentActivities.slice(0, 5).map((activity) => (
        <Link key={activity.id} href={`/expense/${activity.id}`} asChild>
          <TouchableOpacity style={styles.item}>
            <View
              style={[
                styles.iconContainer,
                {
                  backgroundColor:
                    activity.type === "payment" ? "#34C75920" : "#007AFF20",
                },
              ]}
            >
              <Ionicons
                name={activity.type === "payment" ? "cash" : "receipt"}
                size={20}
                color={activity.type === "payment" ? "#34C759" : "#007AFF"}
              />
            </View>
            <View style={styles.content}>
              <Text style={styles.title}>{activity.title}</Text>
              <Text style={styles.subtitle}>
                {activity.group} • {formatDistanceToNow(activity.timestamp, { addSuffix: true })}
              </Text>
            </View>
            <Text
              style={[
                styles.amount,
                activity.type === "payment" || activity.isPayer
                  ? styles.positive
                  : styles.neutral,
              ]}
            >
              {activity.isPayer ? "You paid " : ""}
              ${activity.amount.toFixed(2)}
            </Text>
          </TouchableOpacity>
        </Link>
      ))}
      <Link href="/activity" asChild>
        <TouchableOpacity style={styles.viewAll}>
          <Text style={styles.viewAllText}>View All Activity</Text>
          <Ionicons name="chevron-forward" size={16} color="#007AFF" />
        </TouchableOpacity>
      </Link>
    </View>
  );
}

const createStyles = (colorScheme: "light" | "dark" | null | undefined) =>
  StyleSheet.create({
    container: {
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      overflow: "hidden",
    },
    item: {
      flexDirection: "row",
      alignItems: "center",
      padding: 16,
      borderBottomWidth: 0.5,
      borderBottomColor: colorScheme === "dark" ? "#38383a" : "#e5e5e5",
    },
    iconContainer: {
      width: 40,
      height: 40,
      borderRadius: 20,
      alignItems: "center",
      justifyContent: "center",
    },
    content: {
      flex: 1,
      marginLeft: 12,
    },
    title: {
      fontSize: 16,
      fontWeight: "500",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    subtitle: {
      fontSize: 13,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      marginTop: 2,
    },
    amount: {
      fontSize: 16,
      fontWeight: "600",
    },
    positive: {
      color: "#34C759",
    },
    neutral: {
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    viewAll: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      padding: 16,
    },
    viewAllText: {
      fontSize: 14,
      fontWeight: "600",
      color: "#007AFF",
      marginRight: 4,
    },
    empty: {
      alignItems: "center",
      justifyContent: "center",
      padding: 40,
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
    },
    emptyText: {
      fontSize: 16,
      color: "#8e8e93",
      marginTop: 12,
    },
  });
