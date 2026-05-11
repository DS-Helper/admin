export interface CustomerInquiryUser {
  userId: string;
  email: string | null;
  role: string;
  name: string;
  gender: string;
}

export interface CustomerInquiry {
  inquiryId: string;
  content: string;
  type: string;
  status: string;
  user: CustomerInquiryUser;
  imageUrls: string[];
  createdAt: string;
  updatedAt: string;
  reply: string | null;
}

export interface CustomerInquiryPageSort {
  sorted: boolean;
  empty: boolean;
  unsorted: boolean;
}

export interface CustomerInquiryPage {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  sort: CustomerInquiryPageSort;
}

export interface CustomerInquiryListResponse {
  inquiries: CustomerInquiry[];
  page: CustomerInquiryPage;
}
