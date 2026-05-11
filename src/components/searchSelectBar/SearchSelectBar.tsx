"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { IoChevronDown, IoSearchOutline } from "react-icons/io5";
import styles from "./SearchSelectBar.module.scss";
import {
  SearchSelectBarProps,
  SearchSelectConfig,
} from "./searchSelectBar.types";

function SelectField({ select }: { select: SearchSelectConfig }) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);
  const listboxId = useId();
  const isPlaceholderSelected = !select.value;
  const selected = select.options.find((o) => o.value === select.value);
  const displayLabel = selected?.label ?? select.placeholder;

  const close = useCallback(() => setOpen(false), []);

  useEffect(() => {
    if (!open) return;
    const onDocPointer = (event: PointerEvent) => {
      const el = wrapRef.current;
      if (el && !el.contains(event.target as Node)) close();
    };
    document.addEventListener("pointerdown", onDocPointer, true);
    return () => document.removeEventListener("pointerdown", onDocPointer, true);
  }, [open, close]);

  useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") close();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, close]);

  const handlePick = (value: string) => {
    select.onChange(value);
    close();
  };

  return (
    <div
      ref={wrapRef}
      className={`${styles.selectFieldWrap} ${open ? styles.selectFieldWrapOpen : ""}`}
    >
      <button
        type="button"
        id={`${select.id}-trigger`}
        className={`${styles.selectField} ${styles.selectTrigger} ${
          isPlaceholderSelected ? styles.selectFieldPlaceholder : ""
        }`}
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={listboxId}
        onClick={() => setOpen((prev) => !prev)}
      >
        <span className={styles.selectTriggerLabel}>{displayLabel}</span>
        <IoChevronDown
          className={`${styles.arrowIcon} ${open ? styles.arrowIconOpen : ""}`}
          aria-hidden="true"
        />
      </button>

      {open && (
        <div
          id={listboxId}
          className={styles.optionWrap}
          role="listbox"
          aria-labelledby={`${select.id}-trigger`}
        >
          {select.options.map((opt) => (
            <button
              key={opt.value}
              type="button"
              role="option"
              aria-selected={select.value === opt.value}
              className={`${styles.option} ${
                select.value === opt.value ? styles.optionSelected : ""
              }`}
              onClick={() => handlePick(opt.value)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export function SearchSelectBar({
  searchPlaceholder,
  searchValue,
  onSearchChange,
  selects,
  onSearchButtonClick,
  searchButtonLabel = "검색하기",
}: SearchSelectBarProps) {
  const useTripleSelectLayout = selects.length === 3;

  return (
    <section className={styles.searchSelectBarWrap}>
      <div className={styles.inputsGroup}>
        <div className={styles.searchBox}>
          <IoSearchOutline className={styles.searchIcon} aria-hidden="true" />
          <input
            type="text"
            className={styles.searchInput}
            value={searchValue}
            placeholder={searchPlaceholder}
            onChange={(event) => onSearchChange(event.target.value)}
          />
        </div>

        <div
          className={`${styles.selectGroup} ${useTripleSelectLayout ? styles.selectGroupTriple : ""}`}
        >
          {useTripleSelectLayout ? (
            <>
              {selects.map((select) => (
                <SelectField key={select.id} select={select} />
              ))}
            </>
          ) : (
            selects.map((select) => (
              <SelectField key={select.id} select={select} />
            ))
          )}
        </div>
      </div>

      <div className={styles.actionsGroup}>
        <button
          type="button"
          className={styles.searchButton}
          onClick={onSearchButtonClick}
        >
          {searchButtonLabel}
        </button>
      </div>
    </section>
  );
}
