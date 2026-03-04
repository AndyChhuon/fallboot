import type { SelectedPixel } from "../types";

interface PlaceButtonProps {
  selectedPixel: SelectedPixel | null;
  connected: boolean;
  onPlace: () => void;
}

export function PlaceButton({
  selectedPixel,
  connected,
  onPlace,
}: PlaceButtonProps) {
  return (
    <div className="flex items-center gap-3 shrink-0">
      {selectedPixel && (
        <div className="flex items-center gap-2 text-xs text-zinc-400">
          <span
            className="inline-block h-4 w-4 rounded-sm border border-zinc-600"
            style={{ backgroundColor: selectedPixel.color }}
          />
          <span>
            {selectedPixel.lastUpdatedBy !== null
              ? `User ${selectedPixel.lastUpdatedBy}`
              : "Empty"}
          </span>
        </div>
      )}
      <button
        onClick={onPlace}
        disabled={!connected || !selectedPixel}
        className="rounded-md bg-[#FF4500] px-5 py-1.5 text-sm font-semibold text-white hover:bg-[#e03e00] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
      >
        Place
      </button>
    </div>
  );
}
