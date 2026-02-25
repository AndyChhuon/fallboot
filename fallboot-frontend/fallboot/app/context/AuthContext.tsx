"use client";

import { createContext, useContext, useState, useEffect, ReactNode } from "react";

interface AuthContextType {
  accessToken: string | null;
  login: () => void;
  logout: () => void;
  setAccessToken: (token: string | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);

  useEffect(() => {
    const saved = localStorage.getItem("access_token");
    if (saved) setAccessToken(saved);
  }, []);

  useEffect(() => {
    if (accessToken) {
      localStorage.setItem("access_token", accessToken);
    } else {
      localStorage.removeItem("access_token");
    }
  }, [accessToken]);

  const login = () => {
    const clientId = process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID;
    const domain = process.env.NEXT_PUBLIC_COGNITO_DOMAIN;
    const redirectUri = process.env.NEXT_PUBLIC_REDIRECT_URI;
    window.location.href = `${domain}/oauth2/authorize?response_type=token&client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri!)}&scope=openid+email+profile`;
  };

  const logout = () => {
    setAccessToken(null);
    const clientId = process.env.NEXT_PUBLIC_COGNITO_CLIENT_ID;
    const domain = process.env.NEXT_PUBLIC_COGNITO_DOMAIN;
    const redirectUri = process.env.NEXT_PUBLIC_REDIRECT_URI;
    window.location.href = `${domain}/logout?client_id=${clientId}&logout_uri=${encodeURIComponent(redirectUri!)}`;
  };

  return (
    <AuthContext.Provider value={{ accessToken, login, logout, setAccessToken }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
