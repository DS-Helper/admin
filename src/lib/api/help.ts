import { instance } from "./axios";
import { HelpListResponse } from "@/types/help";

export const getHelpList = async () => {
  const response = await instance.get<HelpListResponse>(
    "/reservations/requested-reservations"
  );
  return response.data;
};

export const postPersonalHelp = async (personalReservationId: string, status: string) => {
  const response = await instance.post(`/personal-reservation/status`, {
    personalReservationId,
    status,
  });
  return response.data;
};

export const postOrganizationHelp = async (organizationReservationId: string, status: string) => {
  const response = await instance.post(`/organization-reservation/status`, {
    organizationReservationId,
    status,
  });
  return response.data;
};