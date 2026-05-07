"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SiKakaotalk } from "react-icons/si";

import styles from "./page.module.scss";
import {
  buildKakaoAuthorizeUrl,
  getCheckAuth,
  getLogin,
} from "@/lib/api/authUser";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [isAuthenticating, setIsAuthenticating] = useState(false);

  useEffect(() => {
    const run = async () => {
      const res = await getCheckAuth();
      if (res) {
        router.replace("/");
      }
    };

    void run();
  }, [router]);

  useEffect(() => {
    const code = searchParams.get("code");
    if (!code || isAuthenticating) return;

    const run = async () => {
      try {
        setIsAuthenticating(true);
        await getLogin(code);
        const check = await getCheckAuth();

        if (check) {
          router.replace("/");
        } else {
          alert("로그인에 실패하셨습니다.");
          router.replace("/login");
        }
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
        <h1 className={styles.loginTitle}>디에스헬퍼 관리자 로그인</h1>
        <p className={styles.loginDescription}>
          카카오 계정으로 관리자 페이지에 로그인하세요.
        </p>
        <div className={styles.loginActions}>
          <button
            type="button"
            className={styles.kakaoButton}
            onClick={handleKakaoLoginClick}
            disabled={isAuthenticating}
          >
            <span className={styles.kakaoIcon} aria-hidden="true">
              <SiKakaotalk />
            </span>
            <span className={styles.kakaoLabel}>카카오 로그인하기</span>
          </button>
          <p className={styles.loginHelperText}>
            로그인 완료 후 자동으로 관리자 페이지로 이동합니다.
          </p>
        </div>
      </section>
    </main>
  );
}

