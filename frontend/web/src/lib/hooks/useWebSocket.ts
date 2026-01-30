"use client";

import { useEffect, useRef, useCallback, useState } from "react";
import { useAuthStore } from "@/lib/stores";
import { useQueryClient } from "@tanstack/react-query";
import { useToast } from "@/components/ui/use-toast";

interface WebSocketMessage {
  type: string;
  payload: any;
}

export function useWebSocket() {
  const { accessToken, isAuthenticated } = useAuthStore();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  const connect = useCallback(() => {
    if (!isAuthenticated || !accessToken) return;

    const wsUrl = `${process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080"}/ws?token=${accessToken}`;

    try {
      const ws = new WebSocket(wsUrl);

      ws.onopen = () => {
        console.log("WebSocket connected");
        setIsConnected(true);
      };

      ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data);
          handleMessage(message);
        } catch (error) {
          console.error("Failed to parse WebSocket message:", error);
        }
      };

      ws.onclose = () => {
        console.log("WebSocket disconnected");
        setIsConnected(false);

        // Reconnect after 5 seconds
        reconnectTimeoutRef.current = setTimeout(() => {
          connect();
        }, 5000);
      };

      ws.onerror = (error) => {
        console.error("WebSocket error:", error);
      };

      wsRef.current = ws;
    } catch (error) {
      console.error("Failed to connect WebSocket:", error);
    }
  }, [isAuthenticated, accessToken]);

  const handleMessage = useCallback(
    (message: WebSocketMessage) => {
      switch (message.type) {
        case "EXPENSE_CREATED":
          queryClient.invalidateQueries({ queryKey: ["expenses"] });
          queryClient.invalidateQueries({ queryKey: ["balances"] });
          toast({
            title: "New expense added",
            description: message.payload.description,
          });
          break;

        case "EXPENSE_UPDATED":
          queryClient.invalidateQueries({ queryKey: ["expenses"] });
          queryClient.invalidateQueries({ queryKey: ["balances"] });
          break;

        case "EXPENSE_DELETED":
          queryClient.invalidateQueries({ queryKey: ["expenses"] });
          queryClient.invalidateQueries({ queryKey: ["balances"] });
          break;

        case "SETTLEMENT_CREATED":
          queryClient.invalidateQueries({ queryKey: ["settlements"] });
          queryClient.invalidateQueries({ queryKey: ["balances"] });
          toast({
            title: "Settlement recorded",
            description: "A payment has been recorded",
          });
          break;

        case "SETTLEMENT_CONFIRMED":
          queryClient.invalidateQueries({ queryKey: ["settlements"] });
          queryClient.invalidateQueries({ queryKey: ["balances"] });
          break;

        case "GROUP_UPDATED":
          queryClient.invalidateQueries({ queryKey: ["groups"] });
          break;

        case "GROUP_MEMBER_JOINED":
          queryClient.invalidateQueries({ queryKey: ["groups"] });
          toast({
            title: "New member joined",
            description: `${message.payload.displayName} joined the group`,
          });
          break;

        case "NOTIFICATION":
          queryClient.invalidateQueries({ queryKey: ["notifications"] });
          break;

        case "HEARTBEAT":
          // Respond to heartbeat
          wsRef.current?.send(JSON.stringify({ type: "HEARTBEAT_ACK" }));
          break;

        default:
          console.log("Unknown message type:", message.type);
      }
    },
    [queryClient, toast]
  );

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setIsConnected(false);
  }, []);

  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    reconnect: connect,
    disconnect,
  };
}
