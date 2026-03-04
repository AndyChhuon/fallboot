"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { MutableRefObject } from "react";
import type { PixelData, SelectedPixel } from "../types";
import {
  GRID_SIZE,
  DEFAULT_COLOR,
  DRAG_THRESHOLD,
  MIN_ZOOM,
  MAX_ZOOM,
} from "../constants";

export function useCanvasInteraction(
  pixelsRef: MutableRefObject<Map<string, PixelData>>
) {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const cameraRef = useRef({ x: 500, y: 500, zoom: 1 });
  const isPanningRef = useRef(false);
  const didDragRef = useRef(false);
  const panStartRef = useRef({ x: 0, y: 0 });
  const cameraStartRef = useRef({ x: 500, y: 500 });
  const selectedPixelRef = useRef<SelectedPixel | null>(null);
  const initialZoomSetRef = useRef(false);
  const rafRef = useRef(0);

  const [selectedPixel, setSelectedPixel] = useState<SelectedPixel | null>(
    null
  );
  const [cursorCoords, setCursorCoords] = useState<{
    x: number;
    y: number;
  } | null>(null);

  useEffect(() => {
    selectedPixelRef.current = selectedPixel;
  }, [selectedPixel]);

  const screenToGrid = useCallback((clientX: number, clientY: number) => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const { x: cx, y: cy, zoom } = cameraRef.current;
    const gx = (clientX - rect.left - rect.width / 2) / zoom + cx;
    const gy = (clientY - rect.top - rect.height / 2) / zoom + cy;
    return { x: Math.floor(gx), y: Math.floor(gy) };
  }, []);

  const redraw = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const { x: cx, y: cy, zoom } = cameraRef.current;
    const w = canvas.width;
    const h = canvas.height;

    ctx.fillStyle = "#1a1a2e";
    ctx.fillRect(0, 0, w, h);

    const visW = w / zoom;
    const visH = h / zoom;
    const startX = Math.max(0, Math.floor(cx - visW / 2));
    const endX = Math.min(GRID_SIZE, Math.ceil(cx + visW / 2));
    const startY = Math.max(0, Math.floor(cy - visH / 2));
    const endY = Math.min(GRID_SIZE, Math.ceil(cy + visH / 2));

    const gridLeft = (0 - cx) * zoom + w / 2;
    const gridTop = (0 - cy) * zoom + h / 2;
    const gridW = GRID_SIZE * zoom;
    const gridH = GRID_SIZE * zoom;
    ctx.fillStyle = DEFAULT_COLOR;
    ctx.fillRect(gridLeft, gridTop, gridW, gridH);

    pixelsRef.current.forEach((data, key) => {
      const [xs, ys] = key.split(",");
      const gx = Number(xs);
      const gy = Number(ys);
      if (gx < startX || gx >= endX || gy < startY || gy >= endY) return;
      if (data.color === DEFAULT_COLOR) return;
      const sx = (gx - cx) * zoom + w / 2;
      const sy = (gy - cy) * zoom + h / 2;
      ctx.fillStyle = data.color;
      ctx.fillRect(
        Math.floor(sx),
        Math.floor(sy),
        Math.ceil(zoom),
        Math.ceil(zoom)
      );
    });

    if (zoom >= 8) {
      ctx.strokeStyle =
        zoom >= 20 ? "rgba(0,0,0,0.2)" : "rgba(0,0,0,0.08)";
      ctx.lineWidth = 0.5;
      ctx.beginPath();
      for (let gx = startX; gx <= endX; gx++) {
        const sx = Math.floor((gx - cx) * zoom + w / 2) + 0.5;
        ctx.moveTo(sx, Math.floor((startY - cy) * zoom + h / 2));
        ctx.lineTo(sx, Math.floor((endY - cy) * zoom + h / 2));
      }
      for (let gy = startY; gy <= endY; gy++) {
        const sy = Math.floor((gy - cy) * zoom + h / 2) + 0.5;
        ctx.moveTo(Math.floor((startX - cx) * zoom + w / 2), sy);
        ctx.lineTo(Math.floor((endX - cx) * zoom + w / 2), sy);
      }
      ctx.stroke();
    }

    const sel = selectedPixelRef.current;
    if (
      sel &&
      sel.x >= startX &&
      sel.x < endX &&
      sel.y >= startY &&
      sel.y < endY
    ) {
      const sx = (sel.x - cx) * zoom + w / 2;
      const sy = (sel.y - cy) * zoom + h / 2;
      ctx.strokeStyle = "#000000";
      ctx.lineWidth = 2;
      ctx.strokeRect(sx - 1, sy - 1, zoom + 2, zoom + 2);
      ctx.strokeStyle = "#FFFFFF";
      ctx.lineWidth = 1;
      ctx.strokeRect(sx - 2.5, sy - 2.5, zoom + 5, zoom + 5);
    }
  }, [pixelsRef]);

  const drawPixel = useCallback(
    (x: number, y: number, color: string) => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext("2d");
      if (!ctx) return;

      const { x: cx, y: cy, zoom } = cameraRef.current;
      const w = canvas.width;
      const h = canvas.height;

      const visW = w / zoom;
      const visH = h / zoom;
      const startX = Math.max(0, Math.floor(cx - visW / 2));
      const endX = Math.min(GRID_SIZE, Math.ceil(cx + visW / 2));
      const startY = Math.max(0, Math.floor(cy - visH / 2));
      const endY = Math.min(GRID_SIZE, Math.ceil(cy + visH / 2));

      if (x < startX || x >= endX || y < startY || y >= endY) return;

      const sx = (x - cx) * zoom + w / 2;
      const sy = (y - cy) * zoom + h / 2;

      ctx.fillStyle = color === DEFAULT_COLOR ? DEFAULT_COLOR : color;
      ctx.fillRect(
        Math.floor(sx),
        Math.floor(sy),
        Math.ceil(zoom),
        Math.ceil(zoom)
      );

      if (zoom >= 8) {
        ctx.strokeStyle =
          zoom >= 20 ? "rgba(0,0,0,0.2)" : "rgba(0,0,0,0.08)";
        ctx.lineWidth = 0.5;
        ctx.beginPath();
        const left = Math.floor(sx) + 0.5;
        const right = Math.floor(sx + zoom) + 0.5;
        const top = Math.floor(sy) + 0.5;
        const bottom = Math.floor(sy + zoom) + 0.5;
        ctx.moveTo(left, top);
        ctx.lineTo(left, bottom);
        ctx.moveTo(right, top);
        ctx.lineTo(right, bottom);
        ctx.moveTo(left, top);
        ctx.lineTo(right, top);
        ctx.moveTo(left, bottom);
        ctx.lineTo(right, bottom);
        ctx.stroke();
      }

      const sel = selectedPixelRef.current;
      if (sel && sel.x === x && sel.y === y) {
        ctx.strokeStyle = "#000000";
        ctx.lineWidth = 2;
        ctx.strokeRect(sx - 1, sy - 1, zoom + 2, zoom + 2);
        ctx.strokeStyle = "#FFFFFF";
        ctx.lineWidth = 1;
        ctx.strokeRect(sx - 2.5, sy - 2.5, zoom + 5, zoom + 5);
      }
    },
    [pixelsRef]
  );

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (e.button !== 0) return;
    isPanningRef.current = true;
    didDragRef.current = false;
    panStartRef.current = { x: e.clientX, y: e.clientY };
    cameraStartRef.current = {
      x: cameraRef.current.x,
      y: cameraRef.current.y,
    };
    if (containerRef.current) containerRef.current.style.cursor = "grabbing";
  }, []);

  const handleMouseMove = useCallback(
    (e: React.MouseEvent) => {
      const grid = screenToGrid(e.clientX, e.clientY);
      if (
        grid.x >= 0 &&
        grid.x < GRID_SIZE &&
        grid.y >= 0 &&
        grid.y < GRID_SIZE
      ) {
        setCursorCoords(grid);
      } else {
        setCursorCoords(null);
      }
      if (!isPanningRef.current) return;
      const dx = e.clientX - panStartRef.current.x;
      const dy = e.clientY - panStartRef.current.y;
      if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
        didDragRef.current = true;
      }
      if (didDragRef.current) {
        cameraRef.current = {
          ...cameraRef.current,
          x: cameraStartRef.current.x - dx / cameraRef.current.zoom,
          y: cameraStartRef.current.y - dy / cameraRef.current.zoom,
        };
        cancelAnimationFrame(rafRef.current);
        rafRef.current = requestAnimationFrame(redraw);
      }
    },
    [screenToGrid, redraw]
  );

  const handleMouseUp = useCallback(
    (e: React.MouseEvent) => {
      isPanningRef.current = false;
      if (containerRef.current)
        containerRef.current.style.cursor = "crosshair";
      if (!didDragRef.current) {
        const grid = screenToGrid(e.clientX, e.clientY);
        if (
          grid.x >= 0 &&
          grid.x < GRID_SIZE &&
          grid.y >= 0 &&
          grid.y < GRID_SIZE
        ) {
          const key = `${grid.x},${grid.y}`;
          const existing = pixelsRef.current.get(key);
          const pixel: SelectedPixel = {
            x: grid.x,
            y: grid.y,
            color: existing?.color ?? DEFAULT_COLOR,
            lastUpdatedBy: existing?.lastUpdatedBy ?? null,
          };
          setSelectedPixel(pixel);
          selectedPixelRef.current = pixel;
          redraw();
        }
      }
    },
    [screenToGrid, redraw, pixelsRef]
  );

  const handleMouseLeave = useCallback(() => {
    setCursorCoords(null);
    isPanningRef.current = false;
    if (containerRef.current) containerRef.current.style.cursor = "crosshair";
  }, []);

  const handleWheel = useCallback(
    (e: WheelEvent) => {
      e.preventDefault();
      const canvas = canvasRef.current;
      if (!canvas) return;
      const rect = canvas.getBoundingClientRect();
      const mx = e.clientX - rect.left;
      const my = e.clientY - rect.top;
      const { x: cx, y: cy, zoom } = cameraRef.current;
      const gridX = (mx - canvas.width / 2) / zoom + cx;
      const gridY = (my - canvas.height / 2) / zoom + cy;
      const factor = e.deltaY < 0 ? 1.15 : 1 / 1.15;
      const newZoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, zoom * factor));
      cameraRef.current = {
        x: gridX - (mx - canvas.width / 2) / newZoom,
        y: gridY - (my - canvas.height / 2) / newZoom,
        zoom: newZoom,
      };
      cancelAnimationFrame(rafRef.current);
      rafRef.current = requestAnimationFrame(redraw);
    },
    [redraw]
  );

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.addEventListener("wheel", handleWheel, { passive: false });
    return () => canvas.removeEventListener("wheel", handleWheel);
  }, [handleWheel]);

  useEffect(() => {
    const container = containerRef.current;
    const canvas = canvasRef.current;
    if (!container || !canvas) return;
    const handleResize = () => {
      canvas.width = container.clientWidth;
      canvas.height = container.clientHeight;
      if (!initialZoomSetRef.current) {
        const fitZoom =
          (Math.min(canvas.width, canvas.height) / GRID_SIZE) * 0.9;
        cameraRef.current.zoom = fitZoom;
        initialZoomSetRef.current = true;
      }
      redraw();
    };
    const observer = new ResizeObserver(handleResize);
    observer.observe(container);
    handleResize();
    return () => observer.disconnect();
  }, [redraw]);

  return {
    containerRef,
    canvasRef,
    cursorCoords,
    selectedPixel,
    setSelectedPixel,
    redraw,
    drawPixel,
    handleMouseDown,
    handleMouseMove,
    handleMouseUp,
    handleMouseLeave,
  };
}
