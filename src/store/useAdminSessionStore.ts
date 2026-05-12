import { create } from "zustand";

interface AdminSessionState {
  accessToken: string | null;
  setAccessToken: (token: string | null) => void;
  clearSession: () => void;
}

export const useAdminSessionStore = create<AdminSessionState>((set) => ({
  accessToken: null,
  setAccessToken: (token) => set({ accessToken: token }),
  clearSession: () => set({ accessToken: null }),
}));
