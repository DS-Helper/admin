import { instance } from "./axios";
import type { CustomerInquiryListResponse } from "@/types/customer";

export const getCustomer = async (): Promise<CustomerInquiryListResponse> => {
  const response = await instance.get<CustomerInquiryListResponse>(
    "/inquires/un-replied",
  );
  return response.data;
};
