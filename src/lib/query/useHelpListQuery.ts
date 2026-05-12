"use client";

import { useQuery } from "@tanstack/react-query";
import { getHelpList } from "@/lib/api/help";

export const helpListQueryKey = ["helpList"] as const;

export function useHelpListQuery() {
  return useQuery({
    queryKey: helpListQueryKey,
    queryFn: getHelpList,
  });
}
