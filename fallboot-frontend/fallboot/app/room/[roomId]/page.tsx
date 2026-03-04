"use client";

import { useCallback, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { useAuth } from "../../context/AuthContext";
import type { PixelData } from "./types";
import { PALETTE } from "./constants";
import { useCanvasInteraction } from "./hooks/useCanvasInteraction";
import { useRoom } from "./hooks/useRoom";
import { RoomHeader } from "./components/RoomHeader";
import { PixelCanvas } from "./components/PixelCanvas";
import { Toolbar } from "./components/Toolbar";

export default function RoomPage() {
  const { roomId } = useParams();
  const { accessToken } = useAuth();
  const pixelsRef = useRef<Map<string, PixelData>>(new Map());
  const [selectedColor, setSelectedColor] = useState(PALETTE[0]);

  const {
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
  } = useCanvasInteraction(pixelsRef);

  const onPixelUpdate = useCallback(
    (x: number, y: number, color: string, lastUpdatedBy: number | null) => {
      setSelectedPixel((prev) => {
        if (prev && prev.x === x && prev.y === y) {
          return { ...prev, color, lastUpdatedBy };
        }
        return prev;
      });
    },
    [setSelectedPixel]
  );

  const { connected, placePixel } = useRoom(
    roomId as string,
    accessToken,
    pixelsRef,
    onPixelUpdate,
    redraw,
    drawPixel
  );

  const handlePlace = useCallback(() => {
    if (!selectedPixel) return;
    placePixel(selectedPixel.x, selectedPixel.y, selectedColor);
  }, [selectedPixel, selectedColor, placePixel]);

  return (
    <div className="flex h-screen flex-col bg-[#1a1a2e]">
      <RoomHeader roomId={roomId as string} connected={connected} />
      <PixelCanvas
        containerRef={containerRef}
        canvasRef={canvasRef}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseLeave}
      />
      <Toolbar
        cursorCoords={cursorCoords}
        selectedPixel={selectedPixel}
        selectedColor={selectedColor}
        onColorSelect={setSelectedColor}
        connected={connected}
        onPlace={handlePlace}
      />
    </div>
  );
}
