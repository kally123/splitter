import { useState, useEffect } from "react";
import { View, Text, TextInput, TouchableOpacity, StyleSheet, useColorScheme, ScrollView, Alert } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router, useLocalSearchParams } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

const CATEGORIES = [
  { id: "food", name: "Food & Dining", icon: "restaurant" },
  { id: "transport", name: "Transportation", icon: "car" },
  { id: "entertainment", name: "Entertainment", icon: "game-controller" },
  { id: "shopping", name: "Shopping", icon: "bag" },
  { id: "utilities", name: "Utilities", icon: "flash" },
  { id: "rent", name: "Rent", icon: "home" },
  { id: "travel", name: "Travel", icon: "airplane" },
  { id: "other", name: "Other", icon: "ellipsis-horizontal" },
];

const SPLIT_TYPES = [
  { id: "equal", name: "Equal", description: "Split evenly" },
  { id: "exact", name: "Exact", description: "Enter specific amounts" },
  { id: "percentage", name: "Percentage", description: "Split by percentages" },
];

export default function NewExpenseScreen() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);
  const params = useLocalSearchParams();

  const [description, setDescription] = useState(params.merchantName as string || "");
  const [amount, setAmount] = useState(params.amount as string || "");
  const [category, setCategory] = useState("food");
  const [splitType, setSplitType] = useState("equal");
  const [selectedGroup, setSelectedGroup] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showCategories, setShowCategories] = useState(false);

  // Mock groups - would come from API
  const groups = [
    { id: "1", name: "Apartment 4B" },
    { id: "2", name: "Trip to Paris" },
    { id: "3", name: "Work Lunch" },
  ];

  useEffect(() => {
    if (params.fromReceipt === "true") {
      // Pre-fill from receipt scan
      Alert.alert("Receipt Scanned", "We extracted some details from your receipt. Please verify and complete the expense.");
    }
  }, []);

  const handleSave = async () => {
    if (!description) {
      Alert.alert("Error", "Please enter a description");
      return;
    }
    if (!amount || isNaN(parseFloat(amount))) {
      Alert.alert("Error", "Please enter a valid amount");
      return;
    }
    if (!selectedGroup) {
      Alert.alert("Error", "Please select a group");
      return;
    }

    setIsLoading(true);
    try {
      // API call would go here
      setTimeout(() => {
        setIsLoading(false);
        router.replace("/(tabs)");
      }, 1000);
    } catch (error) {
      setIsLoading(false);
      Alert.alert("Error", "Failed to create expense");
    }
  };

  return (
    <SafeAreaView style={styles.container} edges={["bottom"]}>
      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {/* Amount */}
        <View style={styles.amountContainer}>
          <Text style={styles.currencySymbol}>$</Text>
          <TextInput
            style={styles.amountInput}
            placeholder="0.00"
            placeholderTextColor="#8e8e93"
            keyboardType="decimal-pad"
            value={amount}
            onChangeText={setAmount}
          />
        </View>

        {/* Description */}
        <View style={styles.section}>
          <Text style={styles.label}>Description</Text>
          <TextInput
            style={styles.input}
            placeholder="What was this expense for?"
            placeholderTextColor="#8e8e93"
            value={description}
            onChangeText={setDescription}
          />
        </View>

        {/* Group Selection */}
        <View style={styles.section}>
          <Text style={styles.label}>Group</Text>
          <View style={styles.groupList}>
            {groups.map((group) => (
              <TouchableOpacity
                key={group.id}
                style={[
                  styles.groupItem,
                  selectedGroup === group.id && styles.groupItemSelected,
                ]}
                onPress={() => setSelectedGroup(group.id)}
              >
                <Text
                  style={[
                    styles.groupItemText,
                    selectedGroup === group.id && styles.groupItemTextSelected,
                  ]}
                >
                  {group.name}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Category */}
        <View style={styles.section}>
          <Text style={styles.label}>Category</Text>
          <TouchableOpacity
            style={styles.categorySelector}
            onPress={() => setShowCategories(!showCategories)}
          >
            <Ionicons
              name={CATEGORIES.find((c) => c.id === category)?.icon as any}
              size={20}
              color="#007AFF"
            />
            <Text style={styles.categorySelectorText}>
              {CATEGORIES.find((c) => c.id === category)?.name}
            </Text>
            <Ionicons
              name={showCategories ? "chevron-up" : "chevron-down"}
              size={20}
              color="#8e8e93"
            />
          </TouchableOpacity>
          {showCategories && (
            <View style={styles.categoryGrid}>
              {CATEGORIES.map((cat) => (
                <TouchableOpacity
                  key={cat.id}
                  style={[
                    styles.categoryItem,
                    category === cat.id && styles.categoryItemSelected,
                  ]}
                  onPress={() => {
                    setCategory(cat.id);
                    setShowCategories(false);
                  }}
                >
                  <Ionicons
                    name={cat.icon as any}
                    size={24}
                    color={category === cat.id ? "#007AFF" : "#8e8e93"}
                  />
                  <Text
                    style={[
                      styles.categoryItemText,
                      category === cat.id && styles.categoryItemTextSelected,
                    ]}
                  >
                    {cat.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          )}
        </View>

        {/* Split Type */}
        <View style={styles.section}>
          <Text style={styles.label}>Split Type</Text>
          <View style={styles.splitTypeList}>
            {SPLIT_TYPES.map((type) => (
              <TouchableOpacity
                key={type.id}
                style={[
                  styles.splitTypeItem,
                  splitType === type.id && styles.splitTypeItemSelected,
                ]}
                onPress={() => setSplitType(type.id)}
              >
                <Text
                  style={[
                    styles.splitTypeName,
                    splitType === type.id && styles.splitTypeNameSelected,
                  ]}
                >
                  {type.name}
                </Text>
                <Text style={styles.splitTypeDescription}>{type.description}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </ScrollView>

      {/* Save Button */}
      <View style={styles.footer}>
        <TouchableOpacity
          style={[styles.saveButton, isLoading && styles.saveButtonDisabled]}
          onPress={handleSave}
          disabled={isLoading}
        >
          <Text style={styles.saveButtonText}>
            {isLoading ? "Saving..." : "Save Expense"}
          </Text>
        </TouchableOpacity>
      </View>
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
      flex: 1,
      padding: 16,
    },
    amountContainer: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      paddingVertical: 32,
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 16,
      marginBottom: 16,
    },
    currencySymbol: {
      fontSize: 36,
      fontWeight: "600",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    amountInput: {
      fontSize: 48,
      fontWeight: "bold",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
      minWidth: 100,
      textAlign: "center",
    },
    section: {
      marginBottom: 24,
    },
    label: {
      fontSize: 14,
      fontWeight: "600",
      color: "#8e8e93",
      marginBottom: 8,
      textTransform: "uppercase",
    },
    input: {
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 12,
      padding: 16,
      fontSize: 16,
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    groupList: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 8,
    },
    groupItem: {
      paddingHorizontal: 16,
      paddingVertical: 10,
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 20,
      borderWidth: 2,
      borderColor: "transparent",
    },
    groupItemSelected: {
      borderColor: "#007AFF",
      backgroundColor: "#007AFF20",
    },
    groupItemText: {
      fontSize: 14,
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    groupItemTextSelected: {
      color: "#007AFF",
      fontWeight: "600",
    },
    categorySelector: {
      flexDirection: "row",
      alignItems: "center",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 12,
      padding: 16,
      gap: 12,
    },
    categorySelectorText: {
      flex: 1,
      fontSize: 16,
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    categoryGrid: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 8,
      marginTop: 8,
    },
    categoryItem: {
      width: "48%",
      flexDirection: "row",
      alignItems: "center",
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 12,
      padding: 12,
      gap: 8,
    },
    categoryItemSelected: {
      backgroundColor: "#007AFF20",
    },
    categoryItemText: {
      fontSize: 14,
      color: "#8e8e93",
    },
    categoryItemTextSelected: {
      color: "#007AFF",
      fontWeight: "500",
    },
    splitTypeList: {
      gap: 8,
    },
    splitTypeItem: {
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderRadius: 12,
      padding: 16,
      borderWidth: 2,
      borderColor: "transparent",
    },
    splitTypeItemSelected: {
      borderColor: "#007AFF",
      backgroundColor: "#007AFF20",
    },
    splitTypeName: {
      fontSize: 16,
      fontWeight: "600",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
    },
    splitTypeNameSelected: {
      color: "#007AFF",
    },
    splitTypeDescription: {
      fontSize: 13,
      color: "#8e8e93",
      marginTop: 2,
    },
    footer: {
      padding: 16,
      backgroundColor: colorScheme === "dark" ? "#1c1c1e" : "#ffffff",
      borderTopWidth: 1,
      borderTopColor: colorScheme === "dark" ? "#38383a" : "#e5e5e5",
    },
    saveButton: {
      backgroundColor: "#007AFF",
      borderRadius: 12,
      height: 56,
      alignItems: "center",
      justifyContent: "center",
    },
    saveButtonDisabled: {
      opacity: 0.6,
    },
    saveButtonText: {
      fontSize: 18,
      fontWeight: "600",
      color: "#ffffff",
    },
  });
