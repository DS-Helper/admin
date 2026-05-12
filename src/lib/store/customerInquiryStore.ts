import { create } from "zustand";
import type { CustomerInquiryListResponse } from "@/types/customer";

interface CustomerInquiryState {
  payload: CustomerInquiryListResponse | null;
  setCustomerInquiryList: (data: CustomerInquiryListResponse | null) => void;
  resetCustomerInquiries: () => void;
}

export const useCustomerInquiryStore = create<CustomerInquiryState>((set) => ({
  payload: null,
  setCustomerInquiryList: (data) => set({ payload: data }),
  resetCustomerInquiries: () => set({ payload: null }),
}));
