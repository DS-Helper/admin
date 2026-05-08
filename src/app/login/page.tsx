"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { RiKakaoTalkFill } from "react-icons/ri";

import styles from "./page.module.scss";
import {
  buildKakaoAuthorizeUrl,
  getLogin,
} from "@/lib/api/authUser";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [isAuthenticating, setIsAuthenticating] = useState(false);

  useEffect(() => {
    const code = searchParams.get("code");
    if (!code || isAuthenticating) return;

    const run = async () => {
      try {
        setIsAuthenticating(true);
        await getLogin(code);
        router.replace("/");
      } finally {
        setIsAuthenticating(false);
      }
    };

    void run();
  }, [searchParams, isAuthenticating, router]);

  const handleKakaoLoginClick = () => {
    try {
      const url = buildKakaoAuthorizeUrl();
      if (typeof window !== "undefined") {
        window.location.href = url;
      }
    } catch {
      alert("카카오 로그인 초기화 중 오류가 발생했습니다.");
    }
  };

  return (
    <main className={styles.loginPage}>
      <section className={styles.loginCard}>
        <h1 className={styles.loginTitle}>관리자 로그인</h1>
        <div className={styles.loginActions}>
          <button
            type="button"
            className={styles.kakaoButton}
            onClick={handleKakaoLoginClick}
            disabled={isAuthenticating}
          >
            <span className={styles.kakaoIcon} aria-hidden="true">
              <RiKakaoTalkFill />
            </span>
            <span className={styles.kakaoLabel}>카카오 로그인하기</span>
          </button>
        </div>
      </section>
    </main>
  );
}

