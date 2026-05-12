import type { HelpListResponse, ReservationItem } from "@/types/help";

export type MergedReservationItem = ReservationItem & {
  requestType: "personal" | "organization";
  requestTypeLabel: "개인" | "기관";
};

export function mergeHelpListItems(payload: HelpListResponse): MergedReservationItem[] {
  const personal = payload.personalReservations.content.map((item) => ({
    ...item,
    requestType: "personal" as const,
    requestTypeLabel: "개인" as const,
  }));
  const organization = payload.organizationReservations.content.map((item) => ({
    ...item,
    requestType: "organization" as const,
    requestTypeLabel: "기관" as const,
  }));
  return [...personal, ...organization];
}

export function getHelpReservationCardId(item: ReservationItem): string {
  return item.personalReservationId ?? item.organizationReservationId ?? item.reservationHolderId;
}
