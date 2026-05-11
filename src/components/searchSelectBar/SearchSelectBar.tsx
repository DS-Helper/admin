"use client";

import { IoChevronDown, IoSearchOutline } from "react-icons/io5";
import styles from "./SearchSelectBar.module.scss";
import { SearchSelectBarProps } from "./searchSelectBar.types";

export function SearchSelectBar({
  searchPlaceholder,
  searchValue,
  onSearchChange,
  selects,
  onSearchButtonClick,
  searchButtonLabel = "검색하기",
}: SearchSelectBarProps) {
  return (
    <section className={styles.searchSelectBarWrap}>
      <div className={styles.searchSelectBar}>
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

        <div className={styles.selectGroup}>
          {selects.map((select) => {
            const isPlaceholderSelected = !select.value;

            return (
              <div key={select.id} className={styles.selectFieldWrap}>
                <select
                  className={`${styles.selectField} ${
                    isPlaceholderSelected ? styles.selectFieldPlaceholder : ""
                  }`}
                  value={select.value}
                  onChange={(event) => select.onChange(event.target.value)}
                >
                  <option value="" disabled>
                    {select.placeholder}
                  </option>
                  {select.options.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <IoChevronDown className={styles.arrowIcon} aria-hidden="true" />
              </div>
            );
          })}
        </div>
      </div>

      <button
        type="button"
        className={styles.searchButton}
        onClick={onSearchButtonClick}
      >
        {searchButtonLabel}
      </button>
    </section>
  );
}
