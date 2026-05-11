import { create } from "zustand";
import { HelpListResponse } from "@/types/help";

interface HelpRequestState {
  waitingRequestCount: number;
  acceptedRequestCount: number;
  setCountsFromHelpList: (payload: HelpListResponse) => void;
  resetCounts: () => void;
}

export const useHelpRequestStore = create<HelpRequestState>((set) => ({
  waitingRequestCount: 0,
  acceptedRequestCount: 0,
  setCountsFromHelpList: (payload) =>
    set({
      waitingRequestCount: payload.personalReservations.totalElements,
      acceptedRequestCount: payload.organizationReservations.totalElements,
    }),
  resetCounts: () =>
    set({
      waitingRequestCount: 0,
      acceptedRequestCount: 0,
    }),
}));
