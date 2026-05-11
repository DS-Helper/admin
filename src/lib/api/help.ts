import { instance } from "./axios";
import { HelpListResponse } from "@/types/help";

export const getHelpList = async () => {
  const response = await instance.get<HelpListResponse>(
    "/reservations/requested-reservations"
  );
  return response.data;
};

export const getHelpDetail = async (id: string) => {
  const response = await instance.get(`/help/${id}`);
  return response;
};
