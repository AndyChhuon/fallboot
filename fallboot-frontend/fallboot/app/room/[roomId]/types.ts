export interface PixelData {
  color: string;
  lastUpdatedBy: number | null;
}

export interface SelectedPixel {
  x: number;
  y: number;
  color: string;
  lastUpdatedBy: number | null;
}

export interface RoomSnapshotResponse {
  roomUID: string;
  snapshotUrl: string;
  seq: number;
}

export interface SnapshotMessage {
  type: "snapshot";
  url: string;
  seq: number;
}
