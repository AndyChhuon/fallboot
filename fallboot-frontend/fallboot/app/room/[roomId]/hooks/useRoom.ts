"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import type { MutableRefObject } from "react";
import type { PixelData, RoomSnapshotResponse, SnapshotMessage } from "../types";
import { GRID_SIZE } from "../constants";

const WIDTH = GRID_SIZE;
const HEIGHT = GRID_SIZE;

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
  const wsRef = useRef<WebSocket | null>(null);
  const lastAppliedSeqRef = useRef<number>(-1);
  const [connected, setConnected] = useState(false);

  const applySnapshot = useCallback(
    async (url: string) => {
      try {
        const res = await fetch(url, { cache: "no-store" });
        // S3 returns 403 (not 404) for missing objects when the policy lacks s3:ListBucket.
        // Either way, treat as "no snapshot yet" → empty canvas.
        if (res.status === 404 || res.status === 403) {
          pixelsRef.current.clear();
          redraw();
          return;
        }
        if (!res.ok) {
          console.error("Snapshot fetch failed:", res.status);
          return;
        }
        const blob = await res.blob();
        const bitmap = await createImageBitmap(blob);

        const off = document.createElement("canvas");
        off.width = WIDTH;
        off.height = HEIGHT;
        const ctx = off.getContext("2d");
        if (!ctx) return;
        ctx.drawImage(bitmap, 0, 0);
        const data = ctx.getImageData(0, 0, WIDTH, HEIGHT).data;

        const next = new Map<string, PixelData>();
        for (let y = 0; y < HEIGHT; y++) {
          for (let x = 0; x < WIDTH; x++) {
            const i = (y * WIDTH + x) * 4;
            const a = data[i + 3];
            if (a === 0) continue;
            const r = data[i];
            const g = data[i + 1];
            const b = data[i + 2];
            const hex =
              "#" +
              ((1 << 24) | (r << 16) | (g << 8) | b)
                .toString(16)
                .slice(1)
                .toUpperCase();
            next.set(`${x},${y}`, { color: hex, lastUpdatedBy: null });
          }
        }
        pixelsRef.current = next;
        redraw();
      } catch (err) {
        console.error("applySnapshot error:", err);
      }
    },
    [pixelsRef, redraw]
  );

  useEffect(() => {
    if (!roomId || !accessToken) return;

    let cancelled = false;

    fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/pixels/room/${roomId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
      .then((res) => res.json())
      .then(async (response: RoomSnapshotResponse) => {
        if (cancelled) return;
        lastAppliedSeqRef.current = response.seq;
        await applySnapshot(response.snapshotUrl);
      })
      .catch((err) => console.error("Failed to fetch snapshot URL:", err));

    const ws = new WebSocket(process.env.NEXT_PUBLIC_WS_URL!);
    wsRef.current = ws;

    ws.addEventListener("open", () => {
      ws.send(JSON.stringify({ type: "auth", token: accessToken }));
    });

    ws.addEventListener("message", (event) => {
      let msg: { type?: string; url?: string; seq?: number; message?: string };
      try {
        msg = JSON.parse(event.data);
      } catch {
        return;
      }
      if (msg.type === "authenticated") {
        ws.send(JSON.stringify({ type: "subscribe", roomId }));
        setConnected(true);
        return;
      }
      if (msg.type === "snapshot" && typeof msg.seq === "number" && msg.url) {
        if (msg.seq <= lastAppliedSeqRef.current) return;
        const seq = msg.seq;
        const url = msg.url;
        lastAppliedSeqRef.current = seq;
        applySnapshot(url);
        return;
      }
      if (msg.type === "error") {
        console.error("Server error:", msg.message);
      }
    });

    ws.addEventListener("close", () => setConnected(false));
    ws.addEventListener("error", (e) => console.error("WS error:", e));

    return () => {
      cancelled = true;
      try {
        ws.close();
      } catch {}
    };
  }, [roomId, accessToken, applySnapshot]);

  const placePixel = useCallback(
    (x: number, y: number, color: string) => {
      const ws = wsRef.current;
      if (!ws || ws.readyState !== WebSocket.OPEN) return;
      ws.send(
        JSON.stringify({ type: "pixel", roomId, x, y, color })
      );
      pixelsRef.current.set(`${x},${y}`, { color, lastUpdatedBy: null });
      drawPixel(x, y, color);
      onPixelUpdate(x, y, color, null);
    },
    [roomId, pixelsRef, drawPixel, onPixelUpdate]
  );

  return { connected, placePixel };
}
