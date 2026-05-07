import type { Metadata } from "next";

import { Geist, Geist_Mono } from "next/font/google";

import { Sidebar } from "@/components/sidebar";

import shellStyles from "./adminShell.module.scss";
import "./globals.scss";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Factory Admin",
  description: "관리자 전용 Admin 페이지",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <div className={shellStyles.shell}>
          <Sidebar />
          <main className={shellStyles.mainContent}>{children}</main>
        </div>
      </body>
    </html>
  );
}
