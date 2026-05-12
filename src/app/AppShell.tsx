"use client";

import { usePathname } from "next/navigation";

import { Sidebar } from "@/components/sidebar";

import shellStyles from "./adminShell.module.scss";

type AppShellProps = {
  children: React.ReactNode;
};

export default function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();
  const isLoginPage = pathname === "/login";
  const isHelpStorySubPage = pathname?.startsWith("/helpStory/") ?? false;

  if (isLoginPage) {
    return (
      <div className={shellStyles.shell}>
        <main
          className={`${shellStyles.mainContent} ${shellStyles.mainContentNoSidebar}`}
        >
          {children}
        </main>
      </div>
    );
  }

  if (isHelpStorySubPage) {
    return (
      <div className={shellStyles.shell}>
        <main
          className={`${shellStyles.mainContent} ${shellStyles.mainContentHelpStoryWhite}`}
        >
          {children}
        </main>
      </div>
    );
  }

  return (
    <div className={shellStyles.shell}>
      <Sidebar />
      <main className={shellStyles.mainContent}>{children}</main>
    </div>
  );
}

