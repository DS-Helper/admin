import { instance } from "./axios";
import type { CustomerInquiryListResponse } from "@/types/customer";

export const getCustomer = async (): Promise<CustomerInquiryListResponse> => {
  const response = await instance.get<CustomerInquiryListResponse>(
    "/inquires/un-replied",
  );
  return response.data;
};

export const postCustomer = async (inquiryId: string, content: string) => {
  const response = await instance.post(`/replies`, {
    inquiryId,
    content,
  });
  return response.data;
};