"use client";

import { IoChevronDown, IoSearchOutline } from "react-icons/io5";
import styles from "./SearchSelectBar.module.scss";
import {
  SearchSelectBarProps,
  SearchSelectConfig,
} from "./searchSelectBar.types";

function SelectField({ select }: { select: SearchSelectConfig }) {
  const isPlaceholderSelected = !select.value;

  return (
    <div className={styles.selectFieldWrap}>
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
  const [firstSelect, ...restSelects] = selects;

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

        <div className={styles.selectGroup}>
          {useTripleSelectLayout ? (
            <>
              <SelectField select={firstSelect} />
              <div className={styles.selectRowPair}>
                {restSelects.map((select) => (
                  <SelectField key={select.id} select={select} />
                ))}
              </div>
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
