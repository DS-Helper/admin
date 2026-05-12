"use client";

import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useId, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { postPosts, putPosts } from "@/lib/api/helpStory";
import {
  helpStoryPostQueryKey,
  useHelpStoryPostQuery,
} from "@/lib/query/useHelpStoryPostQuery";
import { helpStoryPostsQueryKey } from "@/lib/query/useHelpStoryPostsQuery";
import styles from "./page.module.scss";

const IMAGE_SLOT_COUNT = 1;

function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

function SquareImageSlot({
  id,
  ariaLabel,
  file,
  previewUrl,
  remoteUrl,
  onPick,
  onClear,
}: {
  id: string;
  ariaLabel: string;
  file: File | null;
  previewUrl: string | null;
  /** 서버에 이미 있는 이미지 URL (수정 모드, 새 파일 없을 때) */
  remoteUrl?: string | null;
  onPick: (file: File) => void;
  onClear: () => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const picked = event.target.files?.[0];
    event.target.value = "";
    if (picked) onPick(picked);
  };

  const openPicker = () => {
    inputRef.current?.click();
  };

  const handleSlotKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      openPicker();
    }
  };

  const showLocal = Boolean(file && previewUrl);
  const showRemote = Boolean(!file && remoteUrl);

  return (
    <div className={styles.slotWrap}>
      <input
        ref={inputRef}
        id={id}
        type="file"
        accept="image/*"
        className={styles.visuallyHidden}
        onChange={handleChange}
      />
      <div
        className={`${styles.uploadSlot} ${file || remoteUrl ? styles.uploadSlotFilled : ""}`}
        role="button"
        tabIndex={0}
        aria-label={ariaLabel}
        onClick={openPicker}
        onKeyDown={handleSlotKeyDown}
      >
        {showLocal ? (
          <>
            <Image
              src={previewUrl!}
              alt=""
              fill
              className={styles.previewImage}
              sizes="104px"
              unoptimized
            />
            <span className={styles.fileSizeLabel} aria-hidden="true">
              {formatFileSize(file!.size)}
            </span>
          </>
        ) : showRemote ? (
          <Image
            src={remoteUrl!}
            alt=""
            fill
            className={styles.previewImage}
            sizes="104px"
            unoptimized
          />
        ) : (
          <span className={styles.uploadSlotInner}>
            <span className={styles.uploadPlus} aria-hidden="true">
              +
            </span>
          </span>
        )}
      </div>
      {file || remoteUrl ? (
        <button
          type="button"
          className={styles.clearBadge}
          onClick={(event) => {
            event.preventDefault();
            event.stopPropagation();
            onClear();
          }}
          aria-label={`${ariaLabel} 제거`}
        >
          ✕
        </button>
      ) : null}
    </div>
  );
}

function HelpStoryWriteForm({ editPostId }: { editPostId: string }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const baseId = useId();

  const isEdit = Boolean(editPostId);

  const { data: editPost } = useHelpStoryPostQuery(isEdit ? editPostId : undefined);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [remoteImageUrl, setRemoteImageUrl] = useState<string | null>(null);
  const [imageSlots, setImageSlots] = useState<(File | null)[]>(() =>
    Array.from({ length: IMAGE_SLOT_COUNT }, () => null),
  );
  const [imagePreviews, setImagePreviews] = useState<(string | null)[]>(() =>
    Array.from({ length: IMAGE_SLOT_COUNT }, () => null),
  );
  const [formError, setFormError] = useState<string | null>(null);

  const hydratedRef = useRef(false);

  useEffect(() => {
    if (!isEdit || !editPost || hydratedRef.current) return;
    hydratedRef.current = true;
    setTitle(editPost.title);
    setContent(editPost.content);
    setRemoteImageUrl(editPost.imageUrls[0] ?? null);
    setImageSlots(Array.from({ length: IMAGE_SLOT_COUNT }, () => null));
    setImagePreviews((prev) => {
      prev.forEach((url) => {
        if (url?.startsWith("blob:")) URL.revokeObjectURL(url);
      });
      return Array.from({ length: IMAGE_SLOT_COUNT }, () => null);
    });
  }, [isEdit, editPost]);

  const setImageAt = useCallback((index: number, file: File | null) => {
    if (file) setRemoteImageUrl(null);
    setImageSlots((prev) => {
      const next = [...prev];
      next[index] = file;
      return next;
    });
    setImagePreviews((prev) => {
      const prevUrl = prev[index];
      if (prevUrl?.startsWith("blob:")) URL.revokeObjectURL(prevUrl);
      const next = [...prev];
      next[index] = file ? URL.createObjectURL(file) : null;
      return next;
    });
  }, []);

  const clearSlot = useCallback(
    (index: number) => {
      setRemoteImageUrl(null);
      setImageAt(index, null);
    },
    [setImageAt],
  );

  const mutation = useMutation({
    mutationFn: async (payload: { title: string; content: string; images: File[] }) => {
      if (isEdit) return putPosts(editPostId, payload);
      return postPosts(payload);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: helpStoryPostsQueryKey });
      if (isEdit) {
        await queryClient.invalidateQueries({ queryKey: helpStoryPostQueryKey(editPostId) });
        router.push(`/helpStory/${encodeURIComponent(editPostId)}`);
      } else {
        router.push("/helpStory");
      }
    },
    onError: () => {
      setFormError(
        isEdit
          ? "수정에 실패했습니다. 잠시 후 다시 시도해 주세요."
          : "등록에 실패했습니다. 잠시 후 다시 시도해 주세요.",
      );
    },
  });

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);

    const trimmedTitle = title.trim();
    const trimmedContent = content.trim();
    if (!trimmedTitle || !trimmedContent) {
      setFormError("제목과 내용을 모두 입력해 주세요.");
      return;
    }

    const extraImages = imageSlots.filter((f): f is File => f !== null);

    mutation.mutate({
      title: trimmedTitle,
      content: trimmedContent,
      images: extraImages,
    });
  };

  const pending = mutation.isPending;
  const showEditLoading = isEdit && !editPost && !formError;

  return (
    <main className={styles.writeMain}>
      <h1 className={styles.pageTitle}>{isEdit ? "게시물 수정" : "게시물 작성"}</h1>

      {showEditLoading ? (
        <p className={styles.loadingHint}>게시물을 불러오는 중입니다.</p>
      ) : null}

      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <div className={styles.field}>
          <label className={styles.fieldLabel} htmlFor={`${baseId}-title`}>
            제목
          </label>
          <input
            id={`${baseId}-title`}
            type="text"
            className={styles.textInput}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder=""
            autoComplete="off"
            disabled={isEdit && !editPost}
          />
        </div>

        <div className={styles.field}>
          <label className={styles.fieldLabel} htmlFor={`${baseId}-content`}>
            내용
          </label>
          <textarea
            id={`${baseId}-content`}
            className={styles.textArea}
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder=""
            rows={2}
            disabled={isEdit && !editPost}
          />
        </div>

        <div className={styles.field}>
          <p className={styles.fieldLabel}>썸네일 이미지</p>
          <div className={styles.uploadRow}>
            {imageSlots.map((file, index) => (
              <SquareImageSlot
                key={`slot-${index}`}
                id={`${baseId}-img-${index}`}
                ariaLabel="이미지"
                file={file}
                previewUrl={imagePreviews[index]}
                remoteUrl={index === 0 ? remoteImageUrl : null}
                onPick={(picked) => setImageAt(index, picked)}
                onClear={() => clearSlot(index)}
              />
            ))}
          </div>
        </div>

        {formError ? <p className={styles.errorText}>{formError}</p> : null}

        <div className={styles.actionsRow}>
          <button
            type="button"
            className={styles.backButton}
            onClick={() => router.push(isEdit ? `/helpStory/${encodeURIComponent(editPostId)}` : "/helpStory")}
          >
            이전으로
          </button>
          <button
            type="submit"
            className={styles.submitButton}
            disabled={pending || (isEdit && !editPost)}
          >
            {pending ? (isEdit ? "수정 중…" : "등록 중…") : isEdit ? "수정하기" : "등록하기"}
          </button>
        </div>
      </form>
    </main>
  );
}

export default function HelpStoryWritePage() {
  return (
    <Suspense
      fallback={
        <main className={styles.writeMain}>
          <h1 className={styles.pageTitle}>게시물 작성</h1>
          <p className={styles.loadingHint}>불러오는 중입니다.</p>
        </main>
      }
    >
      <HelpStoryWriteFormWithKey />
    </Suspense>
  );
}

function HelpStoryWriteFormWithKey() {
  const searchParams = useSearchParams();
  const editPostId = searchParams.get("postId")?.trim() ?? "";
  return <HelpStoryWriteForm key={editPostId || "create"} editPostId={editPostId} />;
}
