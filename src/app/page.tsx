"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { getMyInfo } from "@/lib/api/account";

import styles from "./page.module.scss";

function extractUserType(payload: unknown): string | null {
  if (!payload || typeof payload !== "object") return null;

  const root = payload as Record<string, unknown>;
  const nested =
    root.data && typeof root.data === "object"
      ? (root.data as Record<string, unknown>)
      : null;

  const userType =
    (nested?.userType as string | undefined) ??
    (nested?.type as string | undefined) ??
    (root.userType as string | undefined);

  return typeof userType === "string" ? userType : null;
}

export default function Home() {
  const router = useRouter();
  const [isAllowedAdmin, setIsAllowedAdmin] = useState(false);

  useEffect(() => {
    const run = async () => {
      const res = await getMyInfo();

      if (!res) {
        alert("관리자로 로그인을 해주세요.");
        router.replace("/login");
        return;
      }

      const userType = extractUserType(res.data);
      if (userType !== "Admin") {
        alert("관리자로 로그인을 해주세요.");
        router.replace("/login");
        return;
      }

      setIsAllowedAdmin(true);
    };

    void run();
  }, [router]);

  if (!isAllowedAdmin) return null;

  return (
    <main className={styles.adminHome}>
      <section className={styles.adminHomeCard}>
        <h1 className={styles.adminHomeTitle}>디에스헬퍼 관리자 페이지</h1>
        <p className={styles.adminHomeDescription}>
          디에스헬퍼 관리자 페이지입니다.
        </p>
      </section>
    </main>
  );
}
