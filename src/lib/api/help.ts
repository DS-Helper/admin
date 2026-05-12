import { instance } from "./axios";
import { HelpListResponse } from "@/types/help";

export const getHelpList = async () => {
  const response = await instance.get<HelpListResponse>(
    "/admin/reservations/requested-reservations",
  );
  return response.data;
};

export const patchPersonalHelp = async (personalReservationId: string, status: string) => {
  const response = await instance.patch("/admin/personal-reservation/status", {
    personalReservationId,
    status,
  });
  return response.data;
};

export const patchOrganizationHelp = async (organizationReservationId: string, status: string) => {
  const response = await instance.patch("/admin/organization-reservation/status", {
    organizationReservationId,
    status,
  });
  return response.data;
};