"use client";

import { useQuery } from "@tanstack/react-query";
import { getPost } from "@/lib/api/helpStory";
import { useHelpStoryStore } from "@/lib/store/helpStoryStore";

export const helpStoryPostQueryKey = (postId: string) => ["helpStoryPost", postId] as const;

export function useHelpStoryPostQuery(postId: string | undefined) {
  const postsList = useHelpStoryStore((s) => s.postsList);
  const fromList =
    postId && postsList?.posts ? postsList.posts.find((p) => p.postId === postId) : undefined;

  return useQuery({
    queryKey: postId ? helpStoryPostQueryKey(postId) : ["helpStoryPost", "none"],
    queryFn: () => getPost(postId!),
    enabled: Boolean(postId),
    placeholderData: fromList,
  });
}
