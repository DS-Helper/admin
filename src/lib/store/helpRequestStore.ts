import { create } from "zustand";
import type { HelpListResponse } from "@/types/help";

interface HelpRequestState {
  waitingRequestCount: number;
  acceptedRequestCount: number;
  /** react-query 캐시와 동기화된 최신 목록 페이로드 (상세 등에서 재요청 없이 사용) */
  helpList: HelpListResponse | null;
  setHelpListFromQuery: (payload: HelpListResponse) => void;
  resetCounts: () => void;
}

export const useHelpRequestStore = create<HelpRequestState>((set) => ({
  waitingRequestCount: 0,
  acceptedRequestCount: 0,
  helpList: null,
  setHelpListFromQuery: (payload) =>
    set({
      helpList: payload,
      waitingRequestCount: payload.personalReservations.totalElements,
      acceptedRequestCount: payload.organizationReservations.totalElements,
    }),
  resetCounts: () =>
    set({
      waitingRequestCount: 0,
      acceptedRequestCount: 0,
      helpList: null,
    }),
}));
