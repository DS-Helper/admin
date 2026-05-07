"use client";

import type { JSX } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";

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
  { href: "/help-requests", label: "도움 요청", Icon: makeNavImage("/icons/leafIcon.svg") },
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

  return (
    <aside className={styles.sidebar}>
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
