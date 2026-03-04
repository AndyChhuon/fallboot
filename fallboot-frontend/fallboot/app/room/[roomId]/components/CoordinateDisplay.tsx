import type { SelectedPixel } from "../types";

interface CoordinateDisplayProps {
  cursorCoords: { x: number; y: number } | null;
  selectedPixel: SelectedPixel | null;
}

export function CoordinateDisplay({
  cursorCoords,
  selectedPixel,
}: CoordinateDisplayProps) {
  return (
    <div className="w-28 shrink-0 text-xs font-mono text-zinc-500">
      {cursorCoords
        ? `(${cursorCoords.x}, ${cursorCoords.y})`
        : selectedPixel
          ? `(${selectedPixel.x}, ${selectedPixel.y})`
          : "\u00A0"}
    </div>
  );
}
