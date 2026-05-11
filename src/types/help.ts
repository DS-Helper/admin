export interface ReservationSortInfo {
  sorted: boolean;
  empty: boolean;
  unsorted: boolean;
}

export interface ReservationPageable {
  pageNumber: number;
  pageSize: number;
  sort: ReservationSortInfo;
  offset: number;
  paged: boolean;
  unpaged: boolean;
}

export interface ReservationItem {
  personalReservationId?: string;
  organizationReservationId?: string;
  organizationName?: string;
  reservationHolderId: string;
  reservationHolder: string;
  reservationPhoneNumber: string;
  visitDate: string;
  startTime: string;
  endTime: string;
  address: string;
  requirement: string;
  recipientGender: string;
  recipientNumber: number;
  reservationStatus: string;
  note: string;
}

export interface ReservationPagedResult<TItem> {
  content: TItem[];
  pageable: ReservationPageable;
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  sort: ReservationSortInfo;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export interface HelpListResponse {
  personalReservations: ReservationPagedResult<ReservationItem>;
  organizationReservations: ReservationPagedResult<ReservationItem>;
}
