import { create } from "zustand";
import type { HelpStoryPostsListResponse } from "@/types/helpStory";

interface HelpStoryState {
  /** react-query 캐시와 동기화된 활동 게시물 목록 */
  postsList: HelpStoryPostsListResponse | null;
  setPostsListFromQuery: (payload: HelpStoryPostsListResponse) => void;
  resetPostsList: () => void;
}

export const useHelpStoryStore = create<HelpStoryState>((set) => ({
  postsList: null,
  setPostsListFromQuery: (payload) => set({ postsList: payload }),
  resetPostsList: () => set({ postsList: null }),
}));
