"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { IoChevronDown } from "react-icons/io5";
import { postCustomer } from "@/lib/api/customer";
import { useCustomerInquiryStore } from "@/lib/store/customerInquiryStore";
import styles from "./page.module.scss";

const SHEET_TRANSITION_MS = 330;

function formatCreatedDate(createdAt: string): string {
  const head = createdAt.trim().split(/\s+/)[0] ?? "";
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(head);
  if (!m) return createdAt;
  return `${m[1]}.${m[2].padStart(2, "0")}.${m[3].padStart(2, "0")}`;
}

export default function CustomerDetailPage() {
  const params = useParams();
  const router = useRouter();
  const rawId = params?.id;
  const id = typeof rawId === "string" ? decodeURIComponent(rawId) : "";

  const payload = useCustomerInquiryStore((s) => s.payload);

  const [sheetOpen, setSheetOpen] = useState(false);
  const [backdropVisible, setBackdropVisible] = useState(false);
  const [replyDraft, setReplyDraft] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const inquiry = useMemo(() => {
    if (!id) return undefined;
    return payload?.inquiries.find((row) => row.inquiryId === id);
  }, [id, payload?.inquiries]);

  const isEmpty = !inquiry;

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

  const closeToPreviousPage = () => {
    closeSheetThen(() => {
      router.back();
    });
  };

  const handleSubmitReply = async () => {
    if (!inquiry) return;
    const content = replyText.trim();
    if (!content) return;

    const confirmed = window.confirm("문의에 대한 답변 작성을 하시겠습니까?");
    if (!confirmed) return;

    try {
      setIsSubmitting(true);
      await postCustomer(inquiry.inquiryId, content);
      alert("답변이 등록되었습니다.");
      closeToPreviousPage();
    } catch {
      alert("답변 등록에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const replyText = replyDraft ?? inquiry?.reply ?? "";

  const dialogContent = (
    <div
      className={styles.root}
      role="dialog"
      aria-modal="true"
      aria-labelledby={isEmpty ? "customer-detail-empty-title" : "customer-detail-title"}
    >
      <div
        className={`${styles.backdrop} ${backdropVisible ? styles.backdropVisible : ""}`}
        aria-label="이전 화면으로 돌아가기"
        onClick={closeToPreviousPage}
      />
      <div className={`${styles.sheet} ${sheetOpen ? styles.sheetOpen : ""}`}>
        <div className={styles.handle}>
          <div className={styles.handleRow}>
            <p className={styles.handleText}>{inquiry?.status ?? "고객 문의 상세"}</p>
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
            <h1 id="customer-detail-empty-title" className={styles.emptyTitle}>
              문의 정보를 찾을 수 없습니다
            </h1>
            <p className={styles.emptyText}>
              문의 목록에서 항목을 다시 선택하거나 목록 데이터를 새로 불러온 뒤 시도해 주세요.
            </p>
            <Link href="/customer" className={styles.emptyLink}>
              고객 문의 목록으로
            </Link>
          </div>
        ) : (
          <div className={styles.content}>
            <div className={styles.scroll}>
              <h1 id="customer-detail-title" className={styles.visuallyHidden}>
                고객 문의 상세
              </h1>

              <section className={styles.section}>
                <p className={`${styles.sectionLabel} ${styles.sectionLabelFirst}`}>문의자 정보</p>
                <p className={styles.rowMuted}>ID : {inquiry.user.userId}</p>
                <p className={styles.rowMuted}>{formatCreatedDate(inquiry.createdAt)}</p>
              </section>

              <section className={styles.section}>
                <p className={styles.sectionLabel}>도움 요청 내용</p>
                <p className={styles.bodyText}>{inquiry.content}</p>
              </section>

              <section className={styles.section}>
                <p className={styles.sectionLabel}>문의 유형</p>
                <p className={styles.bodyText}>{inquiry.type}</p>
              </section>

              {inquiry.imageUrls.length > 0 && (
                <section className={styles.section}>
                  <p className={styles.sectionLabel}>관련 이미지</p>
                  <div className={styles.imageGrid}>
                    {inquiry.imageUrls.map((url) => (
                      <a
                        key={url}
                        href={url}
                        className={styles.imageLink}
                        target="_blank"
                        rel="noreferrer noopener"
                      >
                        <Image
                          src={url}
                          alt="문의 관련 이미지"
                          className={styles.imageItem}
                          width={240}
                          height={128}
                          unoptimized
                        />
                      </a>
                    ))}
                  </div>
                </section>
              )}

              <section className={styles.section}>
                <p className={styles.sectionLabel}>답변</p>
                <textarea
                  className={styles.replyInput}
                  value={replyText}
                  onChange={(event) => setReplyDraft(event.target.value)}
                  placeholder=""
                  spellCheck={false}
                />
              </section>

              <footer className={styles.footer}>
                <button
                  type="button"
                  className={styles.submitButton}
                  onClick={handleSubmitReply}
                  disabled={!replyText.trim() || isSubmitting}
                >
                  답변 전송하기
                </button>
              </footer>
            </div>
          </div>
        )}
      </div>
    </div>
  );

  if (typeof document === "undefined") return null;
  return createPortal(dialogContent, document.body);
}
