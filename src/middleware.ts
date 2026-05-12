import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

import { ADMIN_ACCESS_TOKEN_COOKIE_NAME, LOGIN_REQUIRED_NOTICE_KEY, LOGIN_REQUIRED_NOTICE_VALUE } from "@/lib/auth/adminSession";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname === "/login" || pathname.startsWith("/login/")) {
    return NextResponse.next();
  }

  const raw = request.cookies.get(ADMIN_ACCESS_TOKEN_COOKIE_NAME)?.value;
  const token = raw ? decodeURIComponent(raw).trim() : "";
  if (!token) {
    const loginUrl = new URL("/login", request.url);
    // 루트(`/`)로 처음 들어온 경우에만 알림 없이 로그인으로 보냄
    const isRoot = pathname === "/" || pathname === "";
    if (!isRoot) {
      loginUrl.searchParams.set(LOGIN_REQUIRED_NOTICE_KEY, LOGIN_REQUIRED_NOTICE_VALUE);
    }
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)",
  ],
};
