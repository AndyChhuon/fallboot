"use client";

import { useRef, useState } from "react";
import { useParams } from "next/navigation";
import { Client } from "@stomp/stompjs";
import { useAuth } from "../../context/AuthContext";
import Link from "next/link";

export default function RoomPage() {
  const { roomId } = useParams();
  const { accessToken } = useAuth();
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState<string[]>([]);
  const [color, setColor] = useState("#ff0000");
  const [x, setX] = useState(0);
  const [y, setY] = useState(0);
  const clientRef = useRef<Client | null>(null);

  const connect = () => {
    const client = new Client({
      brokerURL: process.env.NEXT_PUBLIC_WS_URL,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/room/${roomId}`, (message) => {
          setMessages((prev) => [...prev, message.body]);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error("STOMP error:", frame.headers["message"]),
    });
    client.activate();
    clientRef.current = client;
  };

  const disconnect = () => {
    clientRef.current?.deactivate();
    clientRef.current = null;
    setConnected(false);
  };

  const sendPixel = () => {
    if (!clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: `/app/update-pixel/${roomId}`,
      body: JSON.stringify({ color, x, y }),
    });
  };

  return (
    <div className="flex min-h-screen flex-col items-center gap-6 bg-zinc-50 p-8 dark:bg-black">
      <div className="flex w-full max-w-lg items-center justify-between">
        <Link href="/" className="text-blue-500 hover:underline">Back to Rooms</Link>
        <h1 className="text-xl font-semibold text-black dark:text-white">Room {roomId}</h1>
      </div>

      <button
        onClick={connected ? disconnect : connect}
        className="rounded-full px-6 py-3 font-medium text-white"
        style={{ backgroundColor: connected ? "#dc2626" : "#16a34a" }}
      >
        {connected ? "Disconnect" : "Connect"}
      </button>

      <div className="flex w-full max-w-lg gap-2">
        <input
          type="color"
          value={color}
          onChange={(e) => setColor(e.target.value)}
          className="h-10 w-10 cursor-pointer rounded border-0"
        />
        <input
          type="number"
          value={x}
          onChange={(e) => setX(Number(e.target.value))}
          placeholder="X"
          className="w-20 rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-black dark:border-zinc-700 dark:bg-zinc-900 dark:text-white"
        />
        <input
          type="number"
          value={y}
          onChange={(e) => setY(Number(e.target.value))}
          placeholder="Y"
          className="w-20 rounded-lg border border-zinc-200 bg-white px-3 py-2 text-sm text-black dark:border-zinc-700 dark:bg-zinc-900 dark:text-white"
        />
        <button
          onClick={sendPixel}
          disabled={!connected}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          Send
        </button>
      </div>

      <div className="w-full max-w-lg rounded-lg border border-zinc-200 bg-white p-4 dark:border-zinc-700 dark:bg-zinc-900">
        <h2 className="mb-2 text-sm font-medium text-zinc-500">Messages</h2>
        <div className="h-64 overflow-y-auto font-mono text-sm text-black dark:text-white">
          {messages.length === 0 ? (
            <p className="text-zinc-400">No messages yet...</p>
          ) : (
            messages.map((msg, i) => (
              <div key={i} className="border-b border-zinc-100 py-1 dark:border-zinc-800">
                {msg}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
