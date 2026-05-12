"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { IoTimeOutline } from "react-icons/io5";
import { SearchSelectBar } from "@/components/searchSelectBar";
import { formatVisitDateDisplay, formatVisitDateHeading } from "@/lib/help/formatVisitDate";
import {
  getHelpReservationCardId,
  mergeHelpListItems,
  type MergedReservationItem,
} from "@/lib/help/mergeHelpListItems";
import { useHelpListQuery } from "@/lib/query/useHelpListQuery";
import { useHelpRequestStore } from "@/lib/store/helpRequestStore";
import type { ReservationItem } from "@/types/help";
import styles from "./page.module.scss";

function matchesSearch(item: MergedReservationItem, keyword: string): boolean {
  if (!keyword) return true;
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) return true;

  return (
    item.reservationHolder.toLowerCase().includes(normalized) ||
    (item.organizationName ?? "").toLowerCase().includes(normalized) ||
    item.address.toLowerCase().includes(normalized) ||
    item.reservationPhoneNumber.includes(normalized) ||
    (item.personalReservationId ?? "").toLowerCase().includes(normalized) ||
    (item.organizationReservationId ?? "").toLowerCase().includes(normalized)
  );
}

function getApplicantTypeLabel(item: MergedReservationItem): "개인" | "기관" {
  if (item.organizationReservationId) return "기관";
  if (item.personalReservationId) return "개인";
  return item.requestTypeLabel;
}

export default function HelpPage() {
  const { data, isLoading, isError } = useHelpListQuery();
  const { setHelpListFromQuery, resetCounts } = useHelpRequestStore();

  const [searchValue, setSearchValue] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [dateFilter, setDateFilter] = useState("");

  useEffect(() => {
    if (!data) return;
    setHelpListFromQuery(data);
  }, [data, setHelpListFromQuery]);

  useEffect(() => {
    if (!isError) return;
    resetCounts();
  }, [isError, resetCounts]);

  const mergedItems = useMemo(() => {
    if (!data) return [];
    return mergeHelpListItems(data);
  }, [data]);

  const filteredItems = useMemo(() => {
    return mergedItems
      .filter((item) => {
        const isMatchedSearch = matchesSearch(item, searchValue);
        const isMatchedStatus = !statusFilter || item.reservationStatus === statusFilter;
        const isMatchedType = !typeFilter || item.requestType === typeFilter;
        const isMatchedDate = !dateFilter || item.visitDate === dateFilter;
        return isMatchedSearch && isMatchedStatus && isMatchedType && isMatchedDate;
      })
      .sort((a, b) => b.visitDate.localeCompare(a.visitDate));
  }, [dateFilter, mergedItems, searchValue, statusFilter, typeFilter]);

  const groupedItems = useMemo(() => {
    return filteredItems.reduce<Record<string, MergedReservationItem[]>>((acc, item) => {
      if (!acc[item.visitDate]) acc[item.visitDate] = [];
      acc[item.visitDate].push(item);
      return acc;
    }, {});
  }, [filteredItems]);

  const statusOptions = useMemo(() => {
    const statuses = new Set<string>();
    data?.personalReservations.content.forEach((row: ReservationItem) =>
      statuses.add(row.reservationStatus),
    );
    data?.organizationReservations.content.forEach((row: ReservationItem) =>
      statuses.add(row.reservationStatus),
    );

    return Array.from(statuses).map((status) => ({
      value: status,
      label: status,
    }));
  }, [data]);

  const dateOptions = useMemo(() => {
    const dates = new Set<string>();
    data?.personalReservations.content.forEach((row: ReservationItem) => dates.add(row.visitDate));
    data?.organizationReservations.content.forEach((row: ReservationItem) => dates.add(row.visitDate));

    return Array.from(dates)
      .sort()
      .map((date) => ({
        value: date,
        label: formatVisitDateDisplay(date),
      }));
  }, [data]);

  const dateGroupEntries = Object.entries(groupedItems);

  return (
    <main className={styles.helpMain}>
      <section className={styles.filterSection}>
        <SearchSelectBar
          searchPlaceholder="요청자의 이름을 입력하세요"
          searchValue={searchValue}
          onSearchChange={setSearchValue}
          searchButtonLabel="검색하기"
          selects={[
            {
              id: "status",
              placeholder: "요청 상태",
              value: statusFilter,
              onChange: setStatusFilter,
              options: statusOptions,
            },
            {
              id: "type",
              placeholder: "신청자 유형",
              value: typeFilter,
              onChange: setTypeFilter,
              options: [
                { value: "personal", label: "개인" },
                { value: "organization", label: "기관" },
              ],
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

      <section className={styles.requestGroupSection}>
        {dateGroupEntries.map(([visitDate, items]) => (
          <section key={visitDate} className={styles.dateGroup}>
            <h2 className={styles.dateTitle}>{formatVisitDateHeading(visitDate)}</h2>
            <div className={styles.requestList}>
              {items.map((item) => {
                const cardKey = getHelpReservationCardId(item);
                return (
                  <Link
                    key={cardKey}
                    href={`/help/${encodeURIComponent(cardKey)}`}
                    className={styles.requestItemLink}
                  >
                    <article className={styles.requestItem}>
                      <div className={styles.requestBody}>
                        <p className={styles.requestName}>
                          {item.reservationHolder} ({getApplicantTypeLabel(item)})
                        </p>
                        <p className={styles.requestDateText}>
                          {formatVisitDateDisplay(item.visitDate)}
                        </p>
                        <p className={styles.requestTimeText}>
                          <IoTimeOutline className={styles.timeIcon} aria-hidden="true" />
                          오전 {item.startTime} ~ 오후 {item.endTime}
                        </p>
                      </div>
                      <div
                        className={`${styles.requestActionBar} ${
                          item.reservationStatus === "거절"
                            ? styles.requestActionReject
                            : item.reservationStatus === "완료"
                              ? styles.requestActionComplete
                              : item.reservationStatus === "수락"
                              ? styles.requestActionAccept
                              : styles.requestActionWaiting
                        }`}
                      >
                        {item.reservationStatus}
                      </div>
                    </article>
                  </Link>
                );
              })}
            </div>
          </section>
        ))}
        {!isLoading && dateGroupEntries.length === 0 && (
          <p className={styles.emptyText}>조건에 맞는 요청이 없습니다.</p>
        )}
      </section>
    </main>
  );
}
