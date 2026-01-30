import { View, Text, FlatList, StyleSheet, useColorScheme } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { formatDistanceToNow } from "date-fns";

interface Activity {
  id: string;
  type: "expense" | "payment" | "group" | "invite";
  title: string;
  description: string;
  timestamp: Date;
  amount?: number;
}

// Mock data
const mockActivities: Activity[] = [
  {
    id: "1",
    type: "expense",
    title: "Dinner at Olive Garden",
    description: "You paid $85.50",
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000),
    amount: 85.5,
  },
  {
    id: "2",
    type: "payment",
    title: "John paid you",
    description: "Apartment 4B",
    timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000),
    amount: 45.0,
  },
  {
    id: "3",
    type: "group",
    title: "New group created",
    description: "Weekend Trip",
    timestamp: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
  },
  {
    id: "4",
    type: "expense",
    title: "Groceries",
    description: "Sarah paid $120.00",
    timestamp: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
    amount: 120.0,
  },
];

export default function ActivityScreen() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);

  const getIcon = (type: Activity["type"]) => {
    switch (type) {
      case "expense":
        return "receipt";
      case "payment":
        return "cash";
      case "group":
        return "people";
      case "invite":
        return "person-add";
      default:
        return "ellipse";
    }
  };

  const getIconColor = (type: Activity["type"]) => {
    switch (type) {
      case "expense":
        return "#FF3B30";
      case "payment":
        return "#34C759";
      case "group":
        return "#007AFF";
      case "invite":
        return "#FF9500";
      default:
        return "#8e8e93";
    }
  };

  const renderActivity = ({ item }: { item: Activity }) => (
    <View style={styles.activityItem}>
      <View
        style={[
          styles.iconContainer,
          { backgroundColor: getIconColor(item.type) + "20" },
        ]}
      >
        <Ionicons name={getIcon(item.type) as any} size={20} color={getIconColor(item.type)} />
      </View>
      <View style={styles.activityInfo}>
        <Text style={styles.activityTitle}>{item.title}</Text>
        <Text style={styles.activityDescription}>{item.description}</Text>
        <Text style={styles.activityTime}>
          {formatDistanceToNow(item.timestamp, { addSuffix: true })}
        </Text>
      </View>
      {item.amount && (
        <Text
          style={[
            styles.amount,
            item.type === "payment" ? styles.positive : styles.negative,
          ]}
        >
          {item.type === "payment" ? "+" : ""}${item.amount.toFixed(2)}
        </Text>
      )}
    </View>
  );

  return (
    <SafeAreaView style={styles.container} edges={["bottom"]}>
      <FlatList
        data={mockActivities}
        renderItem={renderActivity}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Ionicons name="receipt-outline" size={64} color="#8e8e93" />
            <Text style={styles.emptyText}>No recent activity</Text>
          </View>
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
    activityItem: {
      flexDirection: "row",
      alignItems: "center",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      padding: 16,
      borderRadius: 12,
      marginBottom: 12,
    },
    iconContainer: {
      width: 40,
      height: 40,
      borderRadius: 20,
      alignItems: "center",
      justifyContent: "center",
    },
    activityInfo: {
      flex: 1,
      marginLeft: 12,
    },
    activityTitle: {
      fontSize: 16,
      fontWeight: "600",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    activityDescription: {
      fontSize: 14,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      marginTop: 2,
    },
    activityTime: {
      fontSize: 12,
      color: colorScheme === "dark" ? "#636366" : "#999999",
      marginTop: 4,
    },
    amount: {
      fontSize: 16,
      fontWeight: "600",
    },
    positive: {
      color: "#34C759",
    },
    negative: {
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    empty: {
      alignItems: "center",
      justifyContent: "center",
      paddingVertical: 64,
    },
    emptyText: {
      fontSize: 16,
      color: "#8e8e93",
      marginTop: 16,
    },
  });
