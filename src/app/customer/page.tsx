"use client";

import { useEffect, useMemo, useState } from "react";
import { SearchSelectBar } from "@/components/searchSelectBar";
import type { CustomerInquiry } from "@/types/customer";
import { useCustomerInquiriesQuery } from "@/lib/query/useCustomerInquiriesQuery";
import { useCustomerInquiryStore } from "@/lib/store/customerInquiryStore";
import styles from "./page.module.scss";

/** `2026-05-11 14:34` → 날짜 키 `2026-05-11` */
function createdAtDateKey(createdAt: string): string {
  const head = createdAt.trim().split(/\s+/)[0] ?? "";
  return /^\d{4}-\d{1,2}-\d{1,2}$/.test(head) ? head : createdAt.trim();
}

/** `2026-05-11` → `5월 11일` */
function formatDateHeading(isoDate: string): string {
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(isoDate.trim());
  if (!m) return isoDate;
  const month = Number(m[2]);
  const day = Number(m[3]);
  if (month < 1 || month > 12 || day < 1 || day > 31) return isoDate;
  return `${month}월 ${day}일`;
}

/** `2026-05-11` → `2026.05.11` */
function formatDateDot(isoDate: string): string {
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(isoDate.trim());
  if (!m) return isoDate;
  return `${m[1]}.${m[2].padStart(2, "0")}.${m[3].padStart(2, "0")}`;
}

function matchesIdSearch(item: CustomerInquiry, keyword: string): boolean {
  if (!keyword) return true;
  const q = keyword.trim().toLowerCase();
  if (!q) return true;
  return (
    item.inquiryId.toLowerCase().includes(q) ||
    item.user.userId.toLowerCase().includes(q) ||
    item.user.name.toLowerCase().includes(q) ||
    (item.user.email ?? "").toLowerCase().includes(q)
  );
}

export default function CustomerPage() {
  const { data, isLoading, isError } = useCustomerInquiriesQuery();
  const payload = useCustomerInquiryStore((s) => s.payload);
  const setCustomerInquiryList = useCustomerInquiryStore((s) => s.setCustomerInquiryList);

  const [searchValue, setSearchValue] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [dateFilter, setDateFilter] = useState("");

  useEffect(() => {
    if (data) setCustomerInquiryList(data);
  }, [data, setCustomerInquiryList]);

  const inquiries = useMemo(
    () => payload?.inquiries ?? [],
    [payload?.inquiries],
  );

  const filteredItems = useMemo(() => {
    return inquiries
      .filter((item) => {
        const dateKey = createdAtDateKey(item.createdAt);
        const isSearch = matchesIdSearch(item, searchValue);
        const isStatus = !statusFilter || item.status === statusFilter;
        const isType = !typeFilter || item.type === typeFilter;
        const isDate = !dateFilter || dateKey === dateFilter;
        return isSearch && isStatus && isType && isDate;
      })
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }, [dateFilter, inquiries, searchValue, statusFilter, typeFilter]);

  const groupedItems = useMemo(() => {
    return filteredItems.reduce<Record<string, CustomerInquiry[]>>((acc, item) => {
      const key = createdAtDateKey(item.createdAt);
      if (!acc[key]) acc[key] = [];
      acc[key].push(item);
      return acc;
    }, {});
  }, [filteredItems]);

  const statusOptions = useMemo(() => {
    const set = new Set<string>();
    inquiries.forEach((i) => set.add(i.status));
    return Array.from(set).map((status) => ({ value: status, label: status }));
  }, [inquiries]);

  const typeOptions = useMemo(() => {
    const set = new Set<string>();
    inquiries.forEach((i) => set.add(i.type));
    return Array.from(set).sort().map((t) => ({ value: t, label: t }));
  }, [inquiries]);

  const dateOptions = useMemo(() => {
    const set = new Set<string>();
    inquiries.forEach((i) => set.add(createdAtDateKey(i.createdAt)));
    return Array.from(set)
      .sort()
      .reverse()
      .map((d) => ({ value: d, label: formatDateDot(d) }));
  }, [inquiries]);

  const dateGroupEntries = Object.entries(groupedItems).sort(([a], [b]) =>
    b.localeCompare(a),
  );

  const statusBarClass = (status: string) =>
    status === "답변 대기" ? styles.inquiryStatusWaiting : styles.inquiryStatusAnswered;

  return (
    <main className={styles.customerMain}>
      <section className={styles.filterSection}>
        <SearchSelectBar
          searchPlaceholder="ID를 입력하세요"
          searchValue={searchValue}
          onSearchChange={setSearchValue}
          searchButtonLabel="검색하기"
          selects={[
            {
              id: "status",
              placeholder: "처리 상태",
              value: statusFilter,
              onChange: setStatusFilter,
              options: statusOptions,
            },
            {
              id: "type",
              placeholder: "문의 유형",
              value: typeFilter,
              onChange: setTypeFilter,
              options: typeOptions,
            },
            {
              id: "date",
              placeholder: "날짜",
              value: dateFilter,
              onChange: setDateFilter,
              options: dateOptions,
            },
          ]}
        />
      </section>

      <section className={styles.inquiryGroupSection}>
        {isError && (
          <p className={styles.errorText} role="alert">
            고객 문의 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
          </p>
        )}
        {dateGroupEntries.map(([dateKey, items]) => (
          <section key={dateKey} className={styles.dateGroup}>
            <h2 className={styles.dateTitle}>{formatDateHeading(dateKey)}</h2>
            <div className={styles.inquiryList}>
              {items.map((item) => (
                <article key={item.inquiryId} className={styles.inquiryItem}>
                  <div className={styles.inquiryBody}>
                    <p className={styles.inquiryUserName}>{item.user.name}</p>
                    <p className={styles.inquiryTypeText}>{item.type}</p>
                  </div>
                  <div
                    className={`${styles.inquiryStatusBar} ${statusBarClass(item.status)}`}
                  >
                    {item.status}
                  </div>
                </article>
              ))}
            </div>
          </section>
        ))}
        {!isLoading && !isError && dateGroupEntries.length === 0 && (
          <p className={styles.emptyText}>조건에 맞는 문의가 없습니다.</p>
        )}
      </section>
    </main>
  );
}
