"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { deletePosts } from "@/lib/api/helpStory";
import { formatVisitDateDisplay, formatWeekdayShortKo } from "@/lib/help/formatVisitDate";
import {
  helpStoryPostQueryKey,
  useHelpStoryPostQuery,
} from "@/lib/query/useHelpStoryPostQuery";
import { helpStoryPostsQueryKey } from "@/lib/query/useHelpStoryPostsQuery";
import styles from "./page.module.scss";

function splitContentBlocks(content: string): string[] {
  const normalized = content.trim();
  if (!normalized) return [];
  const byPara = normalized
    .split(/\n\s*\n/)
    .map((s) => s.trim())
    .filter(Boolean);
  if (byPara.length > 1) return byPara;
  return normalized
    .split("\n")
    .map((s) => s.trim())
    .filter(Boolean);
}

export default function HelpStoryDetailPage() {
  const params = useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const rawId = params?.id;
  const postId = typeof rawId === "string" ? decodeURIComponent(rawId) : "";

  const { data: post, isLoading, isError } = useHelpStoryPostQuery(postId || undefined);

  const deleteMutation = useMutation({
    mutationFn: () => deletePosts(postId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: helpStoryPostsQueryKey });
      await queryClient.removeQueries({ queryKey: helpStoryPostQueryKey(postId) });
      router.push("/helpStory");
    },
  });

  const handleDelete = () => {
    if (!postId) return;
    if (!window.confirm("이 게시물을 삭제할까요?")) return;
    deleteMutation.mutate();
  };

  const showLoading = isLoading && !post;
  const dateLine =
    post &&
    `${formatVisitDateDisplay(post.createdAt)} (${formatWeekdayShortKo(post.createdAt)})`;
  const heroSrc = post?.imageUrls?.[0];
  const blocks = post ? splitContentBlocks(post.content) : [];

  return (
    <main className={styles.detailMain}>
      {showLoading ? (
        <p className={styles.loadingText}>불러오는 중입니다.</p>
      ) : isError && !post ? (
        <p className={styles.errorText}>게시물을 불러오지 못했습니다.</p>
      ) : post ? (
        <>
          <header className={styles.headerBlock}>
            <h1 className={styles.title}>{post.title}</h1>
            {dateLine ? <p className={styles.metaDate}>{dateLine}</p> : null}
          </header>

          {heroSrc ? (
            <div className={styles.heroImageWrap}>
              <Image
                src={heroSrc}
                alt=""
                fill
                className={styles.heroImage}
                sizes="(max-width: 72rem) 100vw, 72rem"
                priority
                unoptimized
              />
            </div>
          ) : null}

          <div className={styles.body}>
            {blocks.map((block, index) => (
              <p key={`${index}-${block.slice(0, 20)}`} className={styles.paragraph}>
                {block}
              </p>
            ))}
          </div>
          <div className={styles.actionsRow}>
            <button
              type="button"
              className={styles.backButton}
              onClick={() => router.push("/helpStory")}
            >
              이전으로
            </button>
            <button
              onClick={() => router.push(`/helpStory/write?postId=${encodeURIComponent(post.postId)}`)}
              className={styles.actionButton}
            >
              수정
            </button>
            <button
              type="button"
              className={`${styles.actionButton} ${styles.actionButtonDanger}`}
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? "삭제 중…" : "삭제"}
            </button>
          </div>
        </>
      ) : (
        <p className={styles.emptyText}>게시물을 찾을 수 없습니다.</p>
      )}
    </main>
  );
}
