/** 브라우저 localStorage 키 (axios가 Authorization 구성 시 사용) */
export const ADMIN_ACCESS_TOKEN_STORAGE_KEY = "admin_access_token";

/** 미들웨어가 로그인 여부를 판별할 때 사용하는 쿠키 이름 */
export const ADMIN_ACCESS_TOKEN_COOKIE_NAME = "admin_access_token";

const COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;

/** 미들웨어가 비로그인 사용자를 보낼 때 붙이는 쿼리 — 로그인 페이지에서 알림 후 제거 */
export const LOGIN_REQUIRED_NOTICE_KEY = "notice";
export const LOGIN_REQUIRED_NOTICE_VALUE = "require-admin";

function cookieSecureSegment(): string {
  if (typeof window === "undefined") return "";
  return window.location.protocol === "https:" ? "; Secure" : "";
}

export function persistAdminAccessToken(token: string): void {
  const trimmed = token.trim();
  if (typeof window === "undefined" || !trimmed) return;

  window.localStorage.setItem(ADMIN_ACCESS_TOKEN_STORAGE_KEY, trimmed);
  document.cookie = `${ADMIN_ACCESS_TOKEN_COOKIE_NAME}=${encodeURIComponent(trimmed)}; Path=/; Max-Age=${COOKIE_MAX_AGE_SECONDS}; SameSite=Lax${cookieSecureSegment()}`;
}

export function clearAdminAccessToken(): void {
  if (typeof window === "undefined") return;

  window.localStorage.removeItem(ADMIN_ACCESS_TOKEN_STORAGE_KEY);
  document.cookie = `${ADMIN_ACCESS_TOKEN_COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Lax`;
}
