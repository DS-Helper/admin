import { instance } from "./axios";
import { useUserStore } from "../store/userStore";

export const KAKAO_OAUTH_AUTHORIZE_ENDPOINT =
  "https://kauth.kakao.com/oauth/authorize";

/** 카카오 개발자 콘솔에 등록할 redirect 경로(프론트 콜백) */
export const KAKAO_OAUTH_REDIRECT_PATH = "/kakao/callback";

export const GOOGLE_OAUTH_REDIRECT_PATH = "/google/callback";
export const NAVER_OAUTH_REDIRECT_PATH = "/naver/callback";

function getAppOrigin(): string {
  if (typeof window === "undefined") return "";
  const appOrigin = process.env.NEXT_PUBLIC_APP_ORIGIN;
  return appOrigin?.replace(/\/$/, "") || window.location.origin;
}

function resolveOAuthRedirectUri(
  urlFromEnv: string | undefined,
  pathname: string
): string {
  if (urlFromEnv) return urlFromEnv;
  if (typeof window === "undefined") return "";
  return `${getAppOrigin()}${pathname}`;
}

export function getKakaoOAuthRedirectUri(): string {
  return resolveOAuthRedirectUri(
    process.env.NEXT_PUBLIC_KAKAO_OAUTH_REDIRECT_URI,
    KAKAO_OAUTH_REDIRECT_PATH
  );
}

export function getKakaoOAuthCallbackPathname(): string {
  const fromEnv = process.env.NEXT_PUBLIC_KAKAO_OAUTH_REDIRECT_URI;
  if (fromEnv) {
    try {
      return new URL(fromEnv).pathname || KAKAO_OAUTH_REDIRECT_PATH;
    } catch {
      return KAKAO_OAUTH_REDIRECT_PATH;
    }
  }
  return KAKAO_OAUTH_REDIRECT_PATH;
}

export function buildKakaoAuthorizeUrl(): string {
  const clientId = process.env.NEXT_PUBLIC_KAKAO_REST_API_KEY;
  if (!clientId) {
    throw new Error(
      ".env에 NEXT_PUBLIC_KAKAO_REST_API_KEY(카카오 REST API 키)를 설정하세요."
    );
  }
  const redirectUri = getKakaoOAuthRedirectUri();
  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: "code",
  });
  return `${KAKAO_OAUTH_AUTHORIZE_ENDPOINT}?${params.toString()}`;
}

export const getLogin = async (code: string) => {
  try {
    const res = await instance.post("/oauth/kakao/login", {
      code,
    });
    console.log("[oauth/kakao/login] response.data", res.data);
    return res;
  } catch (e) {
    console.error(e);
    return null;
  }
};

export function pickOAuthLoginUrl(data: unknown): string | null {
  if (data == null) return null;
  if (typeof data === "string") {
    const s = data.trim();
    return s || null;
  }
  if (typeof data !== "object") return null;
  const o = data as Record<string, unknown>;
  for (const key of ["url", "loginUrl", "redirectUrl"] as const) {
    const v = o[key];
    if (typeof v === "string" && v.trim()) return v.trim();
  }
  return null;
}

/** 백엔드가 내려준 authorize URL의 redirect_uri를 프론트 콜백으로 맞춥니다. */
export function replaceOAuthAuthorizeRedirectUri(
  authorizeUrl: string,
  redirectUri: string
): string {
  try {
    const u = new URL(authorizeUrl);
    u.searchParams.set("redirect_uri", redirectUri);
    return u.toString();
  } catch {
    return authorizeUrl;
  }
}

export const getCheckAuth = async () => {
  try {
    const { refreshToken } = useUserStore.getState();
    const normalizedRefreshToken =
      typeof refreshToken === "string" && refreshToken.trim()
        ? refreshToken.trim()
        : undefined;
    if (!normalizedRefreshToken) return null;

    const res = await instance.get("/auth/check-logged-in", {
      headers: { refreshToken: normalizedRefreshToken },
    });
    return res;
  } catch {
    return null;
  }
};