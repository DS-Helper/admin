import axios, { AxiosInstance } from "axios";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL;
// 토큰
const MANUAL_ADMIN_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6ImM2NWIyYzBjLTYzZGYtNDlhMC04ZDk3LTZiMjhmN2YyNWVjNyIsInJvbGUiOiJBRE1JTiIsInR5cGUiOiJQRVJTT05BTCIsInRva2VuVHlwZSI6ImFjY2Vzc1Rva2VuIiwiaWF0IjoxNzc4NDc3MDcyLCJleHAiOjE3Nzg0ODA2NzJ9.gp0X9htpTpKA3Jkh9YG4FLaiwUchUtx18Fz7hnJOISM";

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
  baseURL: apiBaseUrl,
  withCredentials: true,
});

instance.interceptors.request.use(
  (config) => {
    const url = config.url || "";
    if (!shouldAttachAuthorization(url)) {
      return config;
    }

    const token = MANUAL_ADMIN_TOKEN.trim();
    if (!token) return config;

    config.headers = config.headers ?? {};
    if (!config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || "";
      if (url.includes("/auth/check-logged-in")) {
        return Promise.reject(error);
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
  }
);