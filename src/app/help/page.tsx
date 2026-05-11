"use client";

import { useEffect, useMemo, useState } from "react";
import { IoTimeOutline } from "react-icons/io5";
import { SearchSelectBar } from "@/components/searchSelectBar";
import { ReservationItem } from "@/types/help";
import { useHelpListQuery } from "@/lib/query/useHelpListQuery";
import { useHelpRequestStore } from "@/lib/store/helpRequestStore";
import styles from "./page.module.scss";

function matchesSearch(item: ReservationItem, keyword: string): boolean {
  if (!keyword) return true;
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) return true;

  return (
    item.reservationHolder.toLowerCase().includes(normalized) ||
    item.address.toLowerCase().includes(normalized) ||
    item.reservationPhoneNumber.includes(normalized) ||
    (item.personalReservationId ?? "").toLowerCase().includes(normalized) ||
    (item.organizationReservationId ?? "").toLowerCase().includes(normalized)
  );
}

export default function HelpPage() {
  const { data, isLoading, isError } = useHelpListQuery();
  const { setCountsFromHelpList, resetCounts } = useHelpRequestStore();

  const [searchValue, setSearchValue] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [typeFilter, setTypeFilter] = useState("");
  const [dateFilter, setDateFilter] = useState("");

  useEffect(() => {
    if (!data) return;
    setCountsFromHelpList(data);
  }, [data, setCountsFromHelpList]);

  useEffect(() => {
    if (!isError) return;
    resetCounts();
  }, [isError, resetCounts]);

  const mergedItems = useMemo(() => {
    const personal = (data?.personalReservations.content ?? []).map((item) => ({
      ...item,
      requestType: "personal" as const,
      requestTypeLabel: "개인",
    }));
    const organization = (data?.organizationReservations.content ?? []).map((item) => ({
      ...item,
      requestType: "organization" as const,
      requestTypeLabel: "기관",
    }));
    return [...personal, ...organization];
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
    return filteredItems.reduce<Record<string, typeof filteredItems>>((acc, item) => {
      if (!acc[item.visitDate]) acc[item.visitDate] = [];
      acc[item.visitDate].push(item);
      return acc;
    }, {});
  }, [filteredItems]);

  const statusOptions = useMemo(() => {
    const statuses = new Set<string>();
    data?.personalReservations.content.forEach((item) =>
      statuses.add(item.reservationStatus)
    );
    data?.organizationReservations.content.forEach((item) =>
      statuses.add(item.reservationStatus)
    );

    return Array.from(statuses).map((status) => ({
      value: status,
      label: status,
    }));
  }, [data]);

  const dateOptions = useMemo(() => {
    const dates = new Set<string>();
    data?.personalReservations.content.forEach((item) => dates.add(item.visitDate));
    data?.organizationReservations.content.forEach((item) => dates.add(item.visitDate));

    return Array.from(dates)
      .sort()
      .map((date) => ({
        value: date,
        label: date,
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
                { value: "personal", label: "개인 요청" },
                { value: "organization", label: "기관 요청" },
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
            <h2 className={styles.dateTitle}>{visitDate}</h2>
            <div className={styles.requestList}>
              {items.map((item) => {
                const cardKey =
                  item.personalReservationId ??
                  item.organizationReservationId ??
                  item.reservationHolderId;
                return (
                  <article key={cardKey} className={styles.requestItem}>
                    <div className={styles.requestBody}>
                      <p className={styles.requestName}>
                        {item.reservationHolder} ({item.requestTypeLabel})
                      </p>
                      <p className={styles.requestDateText}>{item.visitDate}</p>
                      <p className={styles.requestTimeText}>
                        <IoTimeOutline className={styles.timeIcon} aria-hidden="true" />
                        오전 {item.startTime} - 오후 {item.endTime}
                      </p>
                    </div>
                    <div
                      className={`${styles.requestActionBar} ${
                        item.reservationStatus === "거절"
                          ? styles.requestActionReject
                          : item.reservationStatus === "완료"
                            ? styles.requestActionComplete
                            : styles.requestActionWaiting
                      }`}
                    >
                      {item.reservationStatus}
                    </div>
                  </article>
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
