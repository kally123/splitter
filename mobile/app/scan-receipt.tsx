import { useState, useEffect } from "react";
import { View, Text, StyleSheet, TouchableOpacity, useColorScheme, Alert } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { CameraView, CameraType, useCameraPermissions } from "expo-camera";
import { router } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";

export default function ScanReceiptScreen() {
  const colorScheme = useColorScheme();
  const styles = createStyles(colorScheme);
  const [permission, requestPermission] = useCameraPermissions();
  const [isProcessing, setIsProcessing] = useState(false);

  const handleCapture = async () => {
    // In a real app, this would capture the photo and send it for OCR
    setIsProcessing(true);
    
    // Simulate processing
    setTimeout(() => {
      setIsProcessing(false);
      // Navigate to expense form with pre-filled data
      router.replace({
        pathname: "/expense/new",
        params: {
          merchantName: "Sample Restaurant",
          amount: "45.50",
          date: new Date().toISOString(),
          fromReceipt: "true",
        },
      });
    }, 2000);
  };

  const handlePickImage = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      quality: 0.8,
    });

    if (!result.canceled) {
      setIsProcessing(true);
      // Send image for OCR processing
      setTimeout(() => {
        setIsProcessing(false);
        router.replace({
          pathname: "/expense/new",
          params: {
            fromReceipt: "true",
          },
        });
      }, 2000);
    }
  };

  if (!permission) {
    return <View />;
  }

  if (!permission.granted) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.permissionContainer}>
          <Ionicons name="camera-outline" size={64} color="#8e8e93" />
          <Text style={styles.permissionTitle}>Camera Access Required</Text>
          <Text style={styles.permissionText}>
            We need camera access to scan receipts. You can also upload a photo from your gallery.
          </Text>
          <TouchableOpacity style={styles.permissionButton} onPress={requestPermission}>
            <Text style={styles.permissionButtonText}>Grant Permission</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.galleryButton} onPress={handlePickImage}>
            <Text style={styles.galleryButtonText}>Choose from Gallery</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={["bottom"]}>
      <CameraView style={styles.camera} facing="back">
        {/* Overlay */}
        <View style={styles.overlay}>
          <View style={styles.scanArea}>
            <View style={styles.cornerTL} />
            <View style={styles.cornerTR} />
            <View style={styles.cornerBL} />
            <View style={styles.cornerBR} />
          </View>
          <Text style={styles.instructions}>
            Position the receipt within the frame
          </Text>
        </View>

        {/* Controls */}
        <View style={styles.controls}>
          <TouchableOpacity style={styles.galleryIconButton} onPress={handlePickImage}>
            <Ionicons name="images" size={28} color="#ffffff" />
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.captureButton, isProcessing && styles.captureButtonDisabled]}
            onPress={handleCapture}
            disabled={isProcessing}
          >
            {isProcessing ? (
              <Ionicons name="hourglass" size={32} color="#ffffff" />
            ) : (
              <View style={styles.captureButtonInner} />
            )}
          </TouchableOpacity>
          <TouchableOpacity style={styles.closeButton} onPress={() => router.back()}>
            <Ionicons name="close" size={28} color="#ffffff" />
          </TouchableOpacity>
        </View>
      </CameraView>
    </SafeAreaView>
  );
}

const createStyles = (colorScheme: "light" | "dark" | null | undefined) =>
  StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: "#000000",
    },
    camera: {
      flex: 1,
    },
    overlay: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
    },
    scanArea: {
      width: 300,
      height: 400,
      position: "relative",
    },
    cornerTL: {
      position: "absolute",
      top: 0,
      left: 0,
      width: 40,
      height: 40,
      borderTopWidth: 3,
      borderLeftWidth: 3,
      borderColor: "#ffffff",
    },
    cornerTR: {
      position: "absolute",
      top: 0,
      right: 0,
      width: 40,
      height: 40,
      borderTopWidth: 3,
      borderRightWidth: 3,
      borderColor: "#ffffff",
    },
    cornerBL: {
      position: "absolute",
      bottom: 0,
      left: 0,
      width: 40,
      height: 40,
      borderBottomWidth: 3,
      borderLeftWidth: 3,
      borderColor: "#ffffff",
    },
    cornerBR: {
      position: "absolute",
      bottom: 0,
      right: 0,
      width: 40,
      height: 40,
      borderBottomWidth: 3,
      borderRightWidth: 3,
      borderColor: "#ffffff",
    },
    instructions: {
      color: "#ffffff",
      fontSize: 16,
      marginTop: 20,
      textAlign: "center",
    },
    controls: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-around",
      paddingVertical: 30,
      paddingHorizontal: 40,
    },
    galleryIconButton: {
      width: 50,
      height: 50,
      borderRadius: 25,
      backgroundColor: "rgba(255, 255, 255, 0.2)",
      alignItems: "center",
      justifyContent: "center",
    },
    captureButton: {
      width: 80,
      height: 80,
      borderRadius: 40,
      backgroundColor: "#ffffff",
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 4,
      borderColor: "rgba(255, 255, 255, 0.3)",
    },
    captureButtonDisabled: {
      opacity: 0.5,
    },
    captureButtonInner: {
      width: 60,
      height: 60,
      borderRadius: 30,
      backgroundColor: "#ffffff",
    },
    closeButton: {
      width: 50,
      height: 50,
      borderRadius: 25,
      backgroundColor: "rgba(255, 255, 255, 0.2)",
      alignItems: "center",
      justifyContent: "center",
    },
    permissionContainer: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      padding: 40,
      backgroundColor: colorScheme === "dark" ? "#000000" : "#f5f5f5",
    },
    permissionTitle: {
      fontSize: 24,
      fontWeight: "bold",
      color: colorScheme === "dark" ? "#ffffff" : "#000000",
      marginTop: 20,
      marginBottom: 12,
    },
    permissionText: {
      fontSize: 16,
      color: colorScheme === "dark" ? "#8e8e93" : "#666666",
      textAlign: "center",
      marginBottom: 24,
    },
    permissionButton: {
      backgroundColor: "#007AFF",
      paddingHorizontal: 32,
      paddingVertical: 16,
      borderRadius: 12,
      marginBottom: 16,
    },
    permissionButtonText: {
      fontSize: 16,
      fontWeight: "600",
      color: "#ffffff",
    },
    galleryButton: {
      paddingHorizontal: 32,
      paddingVertical: 16,
    },
    galleryButtonText: {
      fontSize: 16,
      fontWeight: "600",
      color: "#007AFF",
    },
  });
