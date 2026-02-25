"use client";

import { useEffect, Suspense } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../context/AuthContext";

function CallbackHandler() {
  const router = useRouter();
  const { setAccessToken } = useAuth();

  useEffect(() => {
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);
    const token = params.get("access_token");

    if (token) {
      setAccessToken(token);
    }
    router.push("/");
  }, [setAccessToken, router]);

  return <p>Logging in...</p>;
}

export default function CallbackPage() {
  return (
    <Suspense fallback={<p>Loading...</p>}>
      <CallbackHandler />
    </Suspense>
  );
}
