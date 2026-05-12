"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import styles from "./page.module.scss";
import { getMyIdentifier } from "@/lib/api/account";
import {
  LOGIN_REQUIRED_NOTICE_KEY,
  LOGIN_REQUIRED_NOTICE_VALUE,
  persistAdminAccessToken,
} from "@/lib/auth/adminSession";

function LoginPageInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [accessToken, setAccessToken] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (searchParams.get(LOGIN_REQUIRED_NOTICE_KEY) !== LOGIN_REQUIRED_NOTICE_VALUE) {
      return;
    }
    window.alert("관리자 로그인이 필요합니다.");
    router.replace("/login");
  }, [searchParams, router]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const trimmed = accessToken.trim();
    if (!trimmed || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const identifier = await getMyIdentifier(trimmed);
      if (identifier?.userRole === "ADMIN") {
        persistAdminAccessToken(trimmed);
        router.replace("/");
        return;
      }
      if (identifier) {
        window.alert("관리자 계정이 아닙니다.");
        return;
      }
      window.alert("관리자 계정으로 로그인을 하세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className={styles.loginPage}>
      <section className={styles.loginCard}>
        <h1 className={styles.loginTitle}>관리자 로그인</h1>
        <form className={styles.loginForm} onSubmit={handleSubmit} noValidate>
          <div className={styles.tokenField}>
            <label className={styles.tokenLabel} htmlFor="login-access-token">
              Access token
            </label>
            <input
              id="login-access-token"
              name="accessToken"
              type="password"
              autoComplete="off"
              className={styles.tokenInput}
              value={accessToken}
              onChange={(e) => setAccessToken(e.target.value)}
              placeholder="토큰을 입력하세요"
              disabled={isSubmitting}
              spellCheck={false}
            />
          </div>
          <div className={styles.loginActions}>
            <button
              type="submit"
              className={styles.loginSubmitButton}
              disabled={isSubmitting || !accessToken.trim()}
            >
              {isSubmitting ? "확인 중…" : "로그인"}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <main className={styles.loginPage}>
          <section className={styles.loginCard}>
            <h1 className={styles.loginTitle}>관리자 로그인</h1>
            <p className={styles.loginDescription}>불러오는 중입니다.</p>
          </section>
        </main>
      }
    >
      <LoginPageInner />
    </Suspense>
  );
}
