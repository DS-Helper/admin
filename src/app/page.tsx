import styles from "./page.module.scss";

export default function Home() {
  return (
    <main className={styles.adminHome}>
      <section className={styles.adminHomeCard}>
        <h1 className={styles.adminHomeTitle}>Factory Admin</h1>
        <p className={styles.adminHomeDescription}>
          관리자 전용 기능을 이곳에서 단계적으로 구현합니다.
        </p>
      </section>
    </main>
  );
}
