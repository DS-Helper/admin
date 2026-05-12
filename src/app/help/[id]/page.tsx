"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { IoCalendarOutline, IoChevronDown, IoLocationSharp } from "react-icons/io5";
import {
  formatVisitDateDisplay,
  formatWeekdayShortKo,
} from "@/lib/help/formatVisitDate";
import {
  getHelpReservationCardId,
  mergeHelpListItems,
  type MergedReservationItem,
} from "@/lib/help/mergeHelpListItems";
import { useHelpRequestStore } from "@/lib/store/helpRequestStore";
import styles from "./page.module.scss";

const SHEET_TRANSITION_MS = 330;

function handleToneClass(status: string | undefined, empty: boolean): string {
  if (empty || !status) return styles.handleToneWaiting;
  switch (status) {
    case "대기":
      return styles.handleToneWaiting;
    case "수락":
      return styles.handleToneAccepted;
    case "완료":
      return styles.handleToneComplete;
    case "거절":
      return styles.handleToneRejected;
    default:
      return styles.handleToneWaiting;
  }
}

function formatGenderKo(gender: string): string {
  const g = gender.trim().toLowerCase();
  if (g === "male" || g === "m") return "남자";
  if (g === "female" || g === "f") return "여자";
  return gender;
}

export default function HelpDetailPage() {
  const params = useParams();
  const router = useRouter();
  const rawId = params?.id;
  const id = typeof rawId === "string" ? decodeURIComponent(rawId) : "";

  const helpList = useHelpRequestStore((s) => s.helpList);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [backdropVisible, setBackdropVisible] = useState(false);

  const item = useMemo((): MergedReservationItem | undefined => {
    if (!id || !helpList) return undefined;
    return mergeHelpListItems(helpList).find((row) => getHelpReservationCardId(row) === id);
  }, [helpList, id]);

  const isEmpty = !item;
  const isWaitingStatus = item?.reservationStatus === "대기";
  const isOrganizationReservation = Boolean(item?.organizationReservationId);

  useEffect(() => {
    const t = window.requestAnimationFrame(() => {
      setBackdropVisible(true);
      setSheetOpen(true);
    });
    return () => window.cancelAnimationFrame(t);
  }, []);

  useEffect(() => {
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "";
    };
  }, []);

  const closeSheetThen = (navigate: () => void) => {
    setSheetOpen(false);
    setBackdropVisible(false);
    window.setTimeout(navigate, SHEET_TRANSITION_MS);
  };

  const closeToList = () => {
    closeSheetThen(() => {
      router.push("/help");
    });
  };

  const closeToPreviousPage = () => {
    closeSheetThen(() => {
      router.back();
    });
  };

  const dateTimeLine = item
    ? `${formatVisitDateDisplay(item.visitDate)} (${formatWeekdayShortKo(item.visitDate)}) 오전 ${item.startTime} ~ 오후 ${item.endTime}`
    : "";

  const dialogContent = (
    <div
      className={styles.root}
      role="dialog"
      aria-modal="true"
      aria-labelledby={isEmpty ? "help-detail-empty-title" : "help-detail-title"}
    >
      <div
        className={`${styles.backdrop} ${backdropVisible ? styles.backdropVisible : ""}`}
        aria-label="목록으로 돌아가기"
        onClick={closeToList}
      />
      <div className={`${styles.sheet} ${sheetOpen ? styles.sheetOpen : ""}`}>
        <div
          className={`${styles.handle} ${handleToneClass(item?.reservationStatus, isEmpty)}`}
        >
          <div className={styles.handleRow}>
            <p className={styles.handleText}>{item?.reservationStatus}</p>
            <button
              type="button"
              className={styles.handleChevronBtn}
              aria-label="이전 화면으로 닫기"
              onClick={(event) => {
                event.stopPropagation();
                closeToPreviousPage();
              }}
            >
              <IoChevronDown className={styles.handleChevronIcon} aria-hidden />
            </button>
          </div>
        </div>
        {isEmpty ? (
          <div className={styles.empty}>
            <h1 id="help-detail-empty-title" className={styles.emptyTitle}>
              요청 정보를 찾을 수 없습니다
            </h1>
            <p className={styles.emptyText}>
              목록에서 항목을 선택하거나, 도움 요청 목록을 다시 불러온 뒤 열어 주세요.
            </p>
            <Link href="/help" className={styles.emptyLink}>
              도움 요청 목록으로
            </Link>
          </div>
        ) : (
          <div className={styles.content}>
            <div className={styles.scroll}>
              <h1 id="help-detail-title" className={styles.visuallyHidden}>
                도움 요청 상세
              </h1>

              <section className={styles.section}>
                <p className={styles.sectionLabel}>날짜 및 시간</p>
                <p className={styles.row}>
                  <IoCalendarOutline className={styles.iconCalendar} aria-hidden />
                  <span>{dateTimeLine}</span>
                </p>
                <p className={styles.row}>
                  <IoLocationSharp className={styles.iconLocation} aria-hidden />
                  <span>{item.address}</span>
                </p>
              </section>

              <section className={styles.section}>
                <p className={styles.sectionLabel}>신청자 정보</p>
                {isOrganizationReservation && (
                  <p className={`${styles.row} ${styles.rowMuted}`}>
                    기관명 : {item.organizationName || "—"}
                  </p>
                )}
                <p className={`${styles.row} ${styles.rowMuted}`}>이름 : {item.reservationHolder}</p>
                <p className={`${styles.row} ${styles.rowMuted}`}>연락처 : {item.reservationPhoneNumber}</p>
              </section>

              <section className={styles.section}>
                <p className={styles.sectionLabel}>도움 요청 내용</p>
                <p className={styles.bodyText}>{item.requirement}</p>
              </section>

              <section className={styles.section}>
                <p className={styles.sectionLabel}>도움 받는 사람의 성별 / 수</p>
                <p className={styles.bodyText}>
                  {formatGenderKo(item.recipientGender)} / {item.recipientNumber}
                </p>
              </section>

              <section className={styles.section}>
                <p className={styles.sectionLabel}>특이사항</p>
                <p className={styles.bodyText}>{item.note || "—"}</p>
              </section>

              {isWaitingStatus ? (
                <footer className={styles.footer}>
                  <button
                    type="button"
                    className={`${styles.footerBtn} ${styles.acceptBtn}`}
                    onClick={closeToList}
                  >
                    수락하기
                  </button>
                  <button
                    type="button"
                    className={`${styles.footerBtn} ${styles.rejectBtn}`}
                    onClick={closeToList}
                  >
                    거절하기
                  </button>
                </footer>
              ) : null}
            </div>
          </div>
        )}
      </div>
    </div>
  );

  if (typeof document === "undefined") return null;
  return createPortal(dialogContent, document.body);
}
