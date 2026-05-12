import { instance } from "./axios";
import type { AccountIdentifierData, AccountMyInfoResponse } from "@/types/account";

function parseIdentifierPayload(data: unknown): AccountIdentifierData | null {
  if (!data || typeof data !== "object") return null;
  const o = data as Record<string, unknown>;
  if (typeof o.userId === "string" && typeof o.userRole === "string") {
    return { userId: o.userId, userRole: o.userRole };
  }
  if (o.success === true && o.data !== null && typeof o.data === "object") {
    const d = o.data as Record<string, unknown>;
    if (typeof d.userId === "string" && typeof d.userRole === "string") {
      return { userId: d.userId, userRole: d.userRole };
    }
  }
  return null;
}

/**
 * 입력한 accessToken으로 내 정보 조회. 성공 시 본문, 실패 시 `null`.
 */
export const getMyInfo = async (
  accessToken: string,
): Promise<AccountMyInfoResponse | null> => {
  const trimmed = accessToken.trim();
  if (!trimmed) return null;

  try {
    const res = await instance.get<AccountMyInfoResponse>("/user/my-info", {
      headers: { Authorization: `Bearer ${trimmed}` },
    });
    const body = res.data;
    if (body?.success && body.data) {
      return body;
    }
    return null;
  } catch (e) {
    console.error(e);
    return null;
  }
};

/**
 * 입력한 accessToken으로 사용자 식별 정보 조회.
 * 응답이 `{ userId, userRole }` 이거나 `{ success, data: { userId, userRole } }` 형태를 지원한다.
 */
export const getMyIdentifier = async (
  accessToken: string,
): Promise<AccountIdentifierData | null> => {
  const trimmed = accessToken.trim();
  if (!trimmed) return null;

  try {
    const res = await instance.get<unknown>("/user/my-identifier", {
      headers: { Authorization: `Bearer ${trimmed}` },
    });
    return parseIdentifierPayload(res.data);
  } catch (e) {
    console.error(e);
    return null;
  }
};
