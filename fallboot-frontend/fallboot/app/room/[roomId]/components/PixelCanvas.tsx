import type { RefObject } from "react";

interface PixelCanvasProps {
  containerRef: RefObject<HTMLDivElement | null>;
  canvasRef: RefObject<HTMLCanvasElement | null>;
  onMouseDown: (e: React.MouseEvent) => void;
  onMouseMove: (e: React.MouseEvent) => void;
  onMouseUp: (e: React.MouseEvent) => void;
  onMouseLeave: () => void;
}

export function PixelCanvas({
  containerRef,
  canvasRef,
  onMouseDown,
  onMouseMove,
  onMouseUp,
  onMouseLeave,
}: PixelCanvasProps) {
  return (
    <div
      ref={containerRef}
      className="flex-1 overflow-hidden"
      style={{ cursor: "crosshair" }}
    >
      <canvas
        ref={canvasRef}
        className="block"
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        onMouseLeave={onMouseLeave}
      />
    </div>
  );
}
