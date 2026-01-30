"use client";

import { useRef, useState, useCallback } from "react";
import { Camera, Upload, X, Check, Loader2, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface ReceiptScannerProps {
  onCapture: (file: File) => void;
  onCancel?: () => void;
  isUploading?: boolean;
  className?: string;
}

export function ReceiptScanner({
  onCapture,
  onCancel,
  isUploading = false,
  className,
}: ReceiptScannerProps) {
  const [mode, setMode] = useState<"select" | "camera" | "preview">("select");
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [capturedFile, setCapturedFile] = useState<File | null>(null);
  const [isCameraActive, setIsCameraActive] = useState(false);
  
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const startCamera = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" }, // Use back camera on mobile
      });
      
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        streamRef.current = stream;
        setIsCameraActive(true);
        setMode("camera");
      }
    } catch (error) {
      console.error("Failed to start camera:", error);
      alert("Could not access camera. Please check permissions.");
    }
  }, []);

  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
      setIsCameraActive(false);
    }
  }, []);

  const capturePhoto = useCallback(() => {
    if (!videoRef.current || !canvasRef.current) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");

    if (!context) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0);

    canvas.toBlob(
      (blob) => {
        if (blob) {
          const file = new File([blob], `receipt-${Date.now()}.jpg`, {
            type: "image/jpeg",
          });
          setCapturedFile(file);
          setCapturedImage(canvas.toDataURL("image/jpeg"));
          stopCamera();
          setMode("preview");
        }
      },
      "image/jpeg",
      0.9
    );
  }, [stopCamera]);

  const handleFileSelect = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      if (!file) return;

      // Validate file type
      if (!file.type.startsWith("image/")) {
        alert("Please select an image file.");
        return;
      }

      // Validate file size (max 10MB)
      if (file.size > 10 * 1024 * 1024) {
        alert("File is too large. Maximum size is 10MB.");
        return;
      }

      const reader = new FileReader();
      reader.onload = () => {
        setCapturedImage(reader.result as string);
        setCapturedFile(file);
        setMode("preview");
      };
      reader.readAsDataURL(file);
    },
    []
  );

  const handleConfirm = useCallback(() => {
    if (capturedFile) {
      onCapture(capturedFile);
    }
  }, [capturedFile, onCapture]);

  const handleRetake = useCallback(() => {
    setCapturedImage(null);
    setCapturedFile(null);
    setMode("select");
  }, []);

  const handleCancel = useCallback(() => {
    stopCamera();
    setCapturedImage(null);
    setCapturedFile(null);
    setMode("select");
    onCancel?.();
  }, [stopCamera, onCancel]);

  return (
    <Card className={cn("w-full max-w-md mx-auto", className)}>
      <CardContent className="p-4">
        {mode === "select" && (
          <div className="space-y-4">
            <h3 className="text-lg font-medium text-center">Add Receipt</h3>
            <div className="grid grid-cols-2 gap-4">
              <Button
                variant="outline"
                className="h-24 flex-col gap-2"
                onClick={startCamera}
              >
                <Camera className="h-8 w-8" />
                <span>Take Photo</span>
              </Button>
              <Button
                variant="outline"
                className="h-24 flex-col gap-2"
                onClick={() => fileInputRef.current?.click()}
              >
                <Upload className="h-8 w-8" />
                <span>Upload File</span>
              </Button>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleFileSelect}
            />
            {onCancel && (
              <Button
                variant="ghost"
                className="w-full"
                onClick={handleCancel}
              >
                Cancel
              </Button>
            )}
          </div>
        )}

        {mode === "camera" && (
          <div className="space-y-4">
            <div className="relative aspect-[3/4] bg-black rounded-lg overflow-hidden">
              <video
                ref={videoRef}
                autoPlay
                playsInline
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 pointer-events-none">
                {/* Scan guide overlay */}
                <div className="absolute inset-4 border-2 border-white/50 rounded-lg" />
                <div className="absolute top-1/2 left-4 right-4 border-t border-dashed border-white/30" />
              </div>
            </div>
            <div className="flex gap-4">
              <Button
                variant="outline"
                className="flex-1"
                onClick={handleCancel}
              >
                <X className="h-4 w-4 mr-2" />
                Cancel
              </Button>
              <Button className="flex-1" onClick={capturePhoto}>
                <Camera className="h-4 w-4 mr-2" />
                Capture
              </Button>
            </div>
            <canvas ref={canvasRef} className="hidden" />
          </div>
        )}

        {mode === "preview" && capturedImage && (
          <div className="space-y-4">
            <div className="relative aspect-[3/4] bg-gray-100 rounded-lg overflow-hidden">
              <img
                src={capturedImage}
                alt="Captured receipt"
                className="w-full h-full object-contain"
              />
            </div>
            <div className="flex gap-4">
              <Button
                variant="outline"
                className="flex-1"
                onClick={handleRetake}
                disabled={isUploading}
              >
                <RotateCcw className="h-4 w-4 mr-2" />
                Retake
              </Button>
              <Button
                className="flex-1"
                onClick={handleConfirm}
                disabled={isUploading}
              >
                {isUploading ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Processing...
                  </>
                ) : (
                  <>
                    <Check className="h-4 w-4 mr-2" />
                    Use Photo
                  </>
                )}
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
