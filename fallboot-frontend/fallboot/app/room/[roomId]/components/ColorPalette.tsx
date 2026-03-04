import { PALETTE } from "../constants";

interface ColorPaletteProps {
  selectedColor: string;
  onColorSelect: (color: string) => void;
}

export function ColorPalette({
  selectedColor,
  onColorSelect,
}: ColorPaletteProps) {
  return (
    <div className="flex flex-1 items-center justify-center gap-1 flex-wrap">
      {PALETTE.map((color) => (
        <button
          key={color}
          onClick={() => onColorSelect(color)}
          className={`h-7 w-7 rounded-sm transition-all ${
            selectedColor === color
              ? "ring-2 ring-white ring-offset-1 ring-offset-[#0d0d1a] scale-110"
              : "hover:scale-110"
          }`}
          style={{
            backgroundColor: color,
            border:
              color === "#FFFFFF"
                ? "1px solid #555"
                : color === "#000000"
                  ? "1px solid #333"
                  : "1px solid transparent",
          }}
          title={color}
        />
      ))}
    </div>
  );
}
