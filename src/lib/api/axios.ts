import axios, { AxiosInstance } from "axios";

import { ADMIN_ACCESS_TOKEN_STORAGE_KEY, clearAdminAccessToken } from "@/lib/auth/adminSession";

export const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL;

function resolveBearerToken(): string {
  if (typeof window !== "undefined") {
    return window.localStorage.getItem(ADMIN_ACCESS_TOKEN_STORAGE_KEY)?.trim() ?? "";
  }
  return "";
}

function shouldAttachAuthorization(url: string): boolean {
  if (!url) return true;
  const skip = [
    "/oauth/kakao/login",
    "/oauth/google/login",
    "/oauth/naver/login",
    "/auth/login/organization",
  ];
  return !skip.some((p) => url.includes(p));
}

export const instance: AxiosInstance = axios.create({
  baseURL: apiBaseUrl || undefined,
  withCredentials: true,
});

instance.interceptors.request.use(
  (config) => {
    const url = config.url || "";
    if (!shouldAttachAuthorization(url)) {
      return config;
    }

    const token = resolveBearerToken();
    if (!token) return config;

    config.headers = config.headers ?? {};
    if (!config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error),
);

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || "";
      if (url.includes("/auth/check-logged-in")) {
        return Promise.reject(error);
      }
      if (typeof window !== "undefined" && window.location.pathname !== "/login") {
        window.alert("관리자 로그인이 필요합니다.");
        clearAdminAccessToken();
        window.location.assign("/login");
      }
    }

    if (error.response?.status === 403) {
      console.error("403 에러: 권한이 없습니다.", error.response?.data);
      const url = error.config?.url || "";
      const isReservationApi =
        url.includes("/personal-reservations") ||
        url.includes("/organization-reservations");
      if (typeof window !== "undefined" && !isReservationApi) {
        alert("해당 기능에 대한 권한이 없습니다. 관리자에게 문의하세요.");
      }
    }

    return Promise.reject(error);
  },
);
