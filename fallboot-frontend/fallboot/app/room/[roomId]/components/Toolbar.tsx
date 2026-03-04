import type { SelectedPixel } from "../types";
import { CoordinateDisplay } from "./CoordinateDisplay";
import { ColorPalette } from "./ColorPalette";
import { PlaceButton } from "./PlaceButton";

interface ToolbarProps {
  cursorCoords: { x: number; y: number } | null;
  selectedPixel: SelectedPixel | null;
  selectedColor: string;
  onColorSelect: (color: string) => void;
  connected: boolean;
  onPlace: () => void;
}

export function Toolbar({
  cursorCoords,
  selectedPixel,
  selectedColor,
  onColorSelect,
  connected,
  onPlace,
}: ToolbarProps) {
  return (
    <div className="flex items-center gap-4 bg-[#0d0d1a] px-4 py-3 border-t border-[#2a2a4a]">
      <CoordinateDisplay
        cursorCoords={cursorCoords}
        selectedPixel={selectedPixel}
      />
      <ColorPalette
        selectedColor={selectedColor}
        onColorSelect={onColorSelect}
      />
      <PlaceButton
        selectedPixel={selectedPixel}
        connected={connected}
        onPlace={onPlace}
      />
    </div>
  );
}
