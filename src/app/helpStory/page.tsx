"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { SearchSelectBar } from "@/components/searchSelectBar";
import { formatVisitDateHeading } from "@/lib/help/formatVisitDate";
import { useHelpStoryPostsQuery } from "@/lib/query/useHelpStoryPostsQuery";
import { useHelpStoryStore } from "@/lib/store/helpStoryStore";
import type { HelpStoryPost } from "@/types/helpStory";
import styles from "./page.module.scss";

function matchesTitleSearch(post: HelpStoryPost, keyword: string): boolean {
  if (!keyword.trim()) return true;
  const normalized = keyword.trim().toLowerCase();
  return post.title.toLowerCase().includes(normalized);
}

export default function HelpStoryPage() {
  const { data, isLoading, isError } = useHelpStoryPostsQuery();
  const { postsList, setPostsListFromQuery, resetPostsList } = useHelpStoryStore();

  const [searchValue, setSearchValue] = useState("");
  const [visibilityFilter, setVisibilityFilter] = useState("");

  useEffect(() => {
    if (!data) return;
    setPostsListFromQuery(data);
  }, [data, setPostsListFromQuery]);

  useEffect(() => {
    if (!isError) return;
    resetPostsList();
  }, [isError, resetPostsList]);

  const displayPayload = postsList ?? data ?? null;

  const filteredPosts = useMemo(() => {
    if (!displayPayload?.posts) return [];
    return displayPayload.posts
      .filter((post) => matchesTitleSearch(post, searchValue))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  }, [displayPayload, searchValue]);

  const groupedByDate = useMemo(() => {
    return filteredPosts.reduce<Record<string, HelpStoryPost[]>>((acc, post) => {
      const key = post.createdAt;
      if (!acc[key]) acc[key] = [];
      acc[key].push(post);
      return acc;
    }, {});
  }, [filteredPosts]);

  const dateEntries = useMemo(() => {
    return Object.entries(groupedByDate).sort(([a], [b]) => b.localeCompare(a));
  }, [groupedByDate]);

  const visibilityOptions = useMemo(
    () => [
      { value: "public", label: "공개" },
      { value: "private", label: "비공개" },
    ],
    [],
  );

  return (
    <main className={styles.helpStoryMain}>
      <section className={styles.filterSection}>
        <SearchSelectBar
          searchPlaceholder="게시물의 제목을 입력하세요"
          searchValue={searchValue}
          onSearchChange={setSearchValue}
          searchButtonLabel="검색하기"
          selects={[
            {
              id: "visibility",
              placeholder: "공개 상태",
              value: visibilityFilter,
              onChange: setVisibilityFilter,
              options: visibilityOptions,
            },
          ]}
        />
      </section>

      <section className={styles.postGroupSection}>
        {isLoading && <p className={styles.loadingText}>게시물을 불러오는 중입니다.</p>}
        {isError && !isLoading && (
          <p className={styles.errorText}>게시물을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
        )}
        {!isLoading &&
          !isError &&
          dateEntries.map(([createdAt, posts]) => (
            <section key={createdAt} className={styles.dateGroup}>
              <h2 className={styles.dateTitle}>{formatVisitDateHeading(createdAt)}</h2>
              <div className={styles.postList}>
                {posts.map((post) => {
                  const thumb = post.imageUrls[0];
                  return (
                    <Link
                      key={post.postId}
                      href={`/helpStory/${encodeURIComponent(post.postId)}`}
                      className={styles.postItemLink}
                    >
                      <article className={styles.postCard}>
                        <div className={styles.thumbnailWrap}>
                          {thumb ? (
                            <Image
                              src={thumb}
                              alt=""
                              fill
                              className={styles.thumbnail}
                              sizes="86px, 86px"
                              unoptimized
                            />
                          ) : null}
                        </div>
                        <div className={styles.postBody}>
                          <p className={styles.postTitle}>{post.title}</p>
                        </div>
                      </article>
                    </Link>
                  );
                })}
              </div>
            </section>
          ))}
        {!isLoading && !isError && dateEntries.length === 0 && (
          <p className={styles.emptyText}>조건에 맞는 게시물이 없습니다.</p>
        )}
      </section>

      <Link href="/helpStory/write" className={styles.fab} aria-label="활동 게시물 작성">
        <Image
          src="/icons/boardPencilIcon.svg"
          alt=""
          width={24}
          height={24}
          className={styles.fabIcon}
          aria-hidden
        />
      </Link>
    </main>
  );
}
