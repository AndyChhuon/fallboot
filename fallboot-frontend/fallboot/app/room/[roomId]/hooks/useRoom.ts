"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import type { MutableRefObject } from "react";
import { Client } from "@stomp/stompjs";
import type { PixelData } from "../types";

export function useRoom(
  roomId: string,
  accessToken: string | null,
  pixelsRef: MutableRefObject<Map<string, PixelData>>,
  onPixelUpdate: (
    x: number,
    y: number,
    color: string,
    lastUpdatedBy: number | null
  ) => void,
  redraw: () => void,
  drawPixel: (x: number, y: number, color: string) => void
) {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!roomId || !accessToken) return;

    fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/pixels/room/${roomId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
      .then((res) => res.json())
      .then(
        (
          pixels: Record<string, {
            x: number;
            y: number;
            color: string;
            lastUpdatedBy: number | null;
          }>
        ) => {
          Object.entries(pixels).forEach(([key, p]) => {
            const [x, y] = key.split(":").map(Number);
            pixelsRef.current.set(`${x},${y}`, {
              color: p.color,
              lastUpdatedBy: p.lastUpdatedBy,
            });
          });
          redraw();
        }
      )
      .catch((err) => console.error("Failed to fetch pixels:", err));

    const client = new Client({
      brokerURL: process.env.NEXT_PUBLIC_WS_URL,
      connectHeaders: { Authorization: `Bearer ${accessToken}` },
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/room/${roomId}`, (msg) => {
          const data = JSON.parse(msg.body);
          const key = `${data.x},${data.y}`;
          pixelsRef.current.set(key, {
            color: data.color,
            lastUpdatedBy: data.lastUpdatedBy ?? null,
          });
          drawPixel(data.x, data.y, data.color);
          onPixelUpdate(
            data.x,
            data.y,
            data.color,
            data.lastUpdatedBy ?? null
          );
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) =>
        console.error("STOMP error:", frame.headers["message"]),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [roomId, accessToken, pixelsRef, redraw, drawPixel, onPixelUpdate]);

  const placePixel = useCallback(
    (x: number, y: number, color: string) => {
      if (!clientRef.current?.connected) return;
      clientRef.current.publish({
        destination: `/app/update-pixel/${roomId}`,
        body: JSON.stringify({ color, x, y }),
      });
    },
    [roomId]
  );

  return { connected, placePixel };
}
