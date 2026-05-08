"use client";

import styles from "./page.module.scss";

export default function Home() {
  const isAllowedAdmin = true;

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
