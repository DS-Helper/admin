export interface AccountCode {
  code: string;
  message: string;
  httpStatus: string;
}

export interface AccountMyInfoData {
  name: string;
  email: string | null;
  birthyear: string;
  gender: string;
  phoneNumber: string;
  profileImageUrl: string;
}

export interface AccountMyInfoResponse {
  success: boolean;
  code: AccountCode;
  message: string;
  data: AccountMyInfoData;
}

/** `/user/my-identifier` 본문 (또는 `data` 안에 동일 필드) */
export interface AccountIdentifierData {
  userId: string;
  userRole: string;
}