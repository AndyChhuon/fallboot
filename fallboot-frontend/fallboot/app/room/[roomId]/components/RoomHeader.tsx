import Link from "next/link";

interface RoomHeaderProps {
  roomId: string;
  connected: boolean;
}

export function RoomHeader({ roomId, connected }: RoomHeaderProps) {
  return (
    <div className="flex items-center justify-between bg-[#0d0d1a] px-4 py-2 border-b border-[#2a2a4a]">
      <Link
        href="/"
        className="text-sm text-blue-400 hover:text-blue-300 transition-colors"
      >
        &larr; Back
      </Link>
      <h1 className="text-sm font-medium text-white tracking-wide">
        Room {roomId}
      </h1>
      <div className="flex items-center gap-1.5 text-xs text-zinc-400">
        <span
          className={`inline-block h-2 w-2 rounded-full ${
            connected ? "bg-green-400" : "bg-red-400"
          }`}
        />
        {connected ? "Live" : "Offline"}
      </div>
    </div>
  );
}
