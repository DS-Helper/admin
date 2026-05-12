"use client";

import { useQuery } from "@tanstack/react-query";
import { getPosts } from "@/lib/api/helpStory";

export const helpStoryPostsQueryKey = ["helpStoryPosts"] as const;

export function useHelpStoryPostsQuery() {
  return useQuery({
    queryKey: helpStoryPostsQueryKey,
    queryFn: getPosts,
  });
}
