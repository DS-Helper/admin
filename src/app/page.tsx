"use client";

import { useEffect } from "react";
import { useHelpListQuery } from "@/lib/query/useHelpListQuery";
import { useHelpRequestStore } from "@/lib/store/helpRequestStore";
import styles from "./page.module.scss";

export default function Home() {
  const { data, isLoading, isError } = useHelpListQuery();
  const {
    waitingRequestCount,
    acceptedRequestCount,
    setHelpListFromQuery,
    resetCounts,
  } = useHelpRequestStore();

  useEffect(() => {
    if (!data) return;
    setHelpListFromQuery(data);
  }, [data, setHelpListFromQuery]);

  useEffect(() => {
    if (!isError) return;
    resetCounts();
  }, [isError, resetCounts]);

  return (
    <main className={styles.dashboardMain}>
      <section className={styles.dashboardHeader}>
        <h1 className={styles.dashboardTitle}>요청 상태</h1>
      </section>

      <section className={styles.requestGrid}>
        <article className={styles.requestCard}>
          <p className={styles.requestLabel}>대기 중인 요청 수</p>
          <p className={styles.requestCount}>
            {isLoading ? "-" : waitingRequestCount}개
          </p>
        </article>

        <article className={styles.requestCard}>
          <p className={styles.requestLabel}>수락한 요청 수</p>
          <p className={styles.requestCount}>
            {isLoading ? "-" : acceptedRequestCount}
          </p>
        </article>
      </section>
    </main>
  );
}
