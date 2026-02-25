"use client";

import { useEffect, useState } from "react";
import { useAuth } from "./context/AuthContext";
import Link from "next/link";

interface Room {
  id: string;
  roomName: string;
}

export default function Home() {
  const { accessToken, login, logout } = useAuth();
  const [rooms, setRooms] = useState<Room[]>([]);

  useEffect(() => {
    if (!accessToken) return;

    fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/rooms`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    })
      .then((res) => res.json())
      .then(setRooms)
      .catch((err) => console.error("Failed to fetch rooms:", err));
  }, [accessToken]);

  if (!accessToken) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-zinc-50 dark:bg-black">
        <h1 className="text-2xl font-semibold text-black dark:text-white">Fallboot</h1>
        <button
          onClick={login}
          className="rounded-full bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700"
        >
          Login
        </button>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col items-center gap-6 bg-zinc-50 p-8 dark:bg-black">
      <div className="flex w-full max-w-lg items-center justify-between">
        <h1 className="text-2xl font-semibold text-black dark:text-white">Rooms</h1>
        <button
          onClick={logout}
          className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700"
        >
          Logout
        </button>
      </div>
      <div className="w-full max-w-lg rounded-lg border border-zinc-200 bg-white dark:border-zinc-700 dark:bg-zinc-900">
        {rooms.length === 0 ? (
          <p className="p-4 text-zinc-400">No rooms found.</p>
        ) : (
          rooms.map((room) => (
            <Link
              key={room.id}
              href={`/room/${room.id}`}
              className="block border-b border-zinc-100 p-4 text-black hover:bg-zinc-50 dark:border-zinc-800 dark:text-white dark:hover:bg-zinc-800"
            >
              {room.roomName}
            </Link>
          ))
        )}
      </div>
    </div>
  );
}
