"use client";

import { useQuery } from "@tanstack/react-query";
import { getCustomer } from "@/lib/api/customer";

export const customerInquiriesQueryKey = ["customerInquiries"] as const;

export function useCustomerInquiriesQuery() {
  return useQuery({
    queryKey: customerInquiriesQueryKey,
    queryFn: getCustomer,
  });
}
