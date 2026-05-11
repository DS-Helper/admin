"use client";

import type { JSX } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { IoChevronBackOutline, IoMenuOutline } from "react-icons/io5";

import styles from "./Sidebar.module.scss";

type IconProps = { active?: boolean };

type NavEntry = {
  href: string;
  label: string;
  Icon: ({ active }: IconProps) => JSX.Element;
  matchExact?: boolean;
};

function isNavActive(
  pathname: string,
  href: string,
  matchExact?: boolean,
) {
  if (href === "/" && matchExact !== false)
    return pathname === "/" || pathname === "/dashboard";
  if (matchExact) return pathname === href;
  return pathname === href || pathname.startsWith(`${href}/`);
}

function makeNavImage(src: string) {
  return function NavIconImage({ active }: IconProps) {
    return (
      <Image
        src={src}
        alt=""
        width={26}
        height={26}
        className={styles.menuIconImage}
        data-nav-active={active ? "true" : "false"}
        aria-hidden
      />
    );
  };
}

const navItems: NavEntry[] = [
  { href: "/", label: "대시보드", Icon: IconDashboard, matchExact: true },
  { href: "/help", label: "도움 요청", Icon: makeNavImage("/icons/leafIcon.svg") },
  {
    href: "/customer-inquiries",
    label: "고객 문의",
    Icon: makeNavImage("/icons/inquiryIcon.svg"),
  },
  { href: "/notifications", label: "알림", Icon: makeNavImage("/icons/noticeIcon.svg") },
  {
    href: "/activity-posts",
    label: "활동 게시물",
    Icon: makeNavImage("/icons/boardIcon.svg"),
  },
];

export function Sidebar() {
  const pathname = usePathname() ?? "/";
  const [mobileOpen, setMobileOpen] = useState(false);
  const [isNarrowViewport, setIsNarrowViewport] = useState(false);

  const closeMobile = useCallback(() => setMobileOpen(false), []);
  const openMobile = useCallback(() => setMobileOpen(true), []);

  useEffect(() => {
    closeMobile();
  }, [pathname, closeMobile]);

  useEffect(() => {
    const mq = window.matchMedia("(max-width: 611px)");
    const apply = () => {
      setIsNarrowViewport(mq.matches);
      if (!mq.matches) closeMobile();
    };
    apply();
    mq.addEventListener("change", apply);
    return () => mq.removeEventListener("change", apply);
  }, [closeMobile]);

  useEffect(() => {
    if (!mobileOpen || !isNarrowViewport) return;

    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [mobileOpen, isNarrowViewport]);

  return (
    <>
      <div
        className={`${styles.mobileTopBar} ${mobileOpen ? styles.mobileTopBarDrawerOpen : ""}`}
        data-mobile-only="true"
      >
        {!mobileOpen && (
          <button
            type="button"
            className={styles.iconButton}
            onClick={openMobile}
            aria-expanded={false}
            aria-controls="admin-sidebar-nav"
            aria-label="메뉴 열기"
          >
            <IoMenuOutline className={styles.headerIcon} aria-hidden />
          </button>
        )}
        <Link
          href="/"
          className={styles.mobileTopBrand}
          aria-label="DS Helper 관리 홈"
          onClick={closeMobile}
        >
          <Image
            src="/images/logo.svg"
            alt="DS Helper"
            width={169}
            height={40}
            className={styles.brandImage}
            priority
          />
        </Link>
      </div>

      <div
        className={`${styles.modalOverlay} ${mobileOpen ? styles.modalOverlayVisible : ""}`}
        aria-label="메뉴 닫기"
        onClick={closeMobile}
      />

      <aside
        id="admin-sidebar-nav"
        className={`${styles.sidebar} ${mobileOpen ? styles.sidebarOpen : ""}`}
        aria-hidden={isNarrowViewport && !mobileOpen ? true : undefined}
        inert={isNarrowViewport && !mobileOpen ? true : undefined}
      >
        <header className={styles.sidebarDrawerHeader}>
          <button
            type="button"
            className={styles.sidebarDrawerClose}
            onClick={closeMobile}
            aria-label="메뉴 닫기"
          >
            <IoChevronBackOutline
              className={styles.sidebarDrawerHeaderIcon}
              aria-hidden
            />
          </button>
        </header>

        <div className={styles.desktopBrand}>
          <div className={styles.brand}>
            <Link href="/" className={styles.brandLink} aria-label="DS Helper 관리 홈">
              <Image
                src="/images/logo.svg"
                alt="DS Helper"
                width={169}
                height={40}
                className={styles.brandImage}
                priority
              />
            </Link>
          </div>
        </div>

        <nav className={styles.nav} aria-label="관리 메뉴">
          <ul className={styles.menu}>
            {navItems.map(({ href, label, Icon, matchExact }) => {
              const active = isNavActive(pathname, href, matchExact);
              return (
                <li key={href}>
                  <Link
                    href={href}
                    className={`${styles.menuLink} ${active ? styles.menuLinkActive : ""}`}
                    aria-current={active ? "page" : undefined}
                    onClick={closeMobile}
                  >
                    <span className={styles.menuIcon}>
                      <Icon active={active} />
                    </span>
                    <span className={styles.menuLabel}>{label}</span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>
      </aside>
    </>
  );
}

function IconDashboard({ active }: IconProps) {
  const stroke = active ? "var(--color-semantic-text-brand)" : "var(--color-semantic-text-secondary)";
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M4 17V11M10 17V7M16 17V13M22 17V10"
        stroke={stroke}
        strokeWidth="1.75"
        strokeLinecap="round"
      />
    </svg>
  );
}
