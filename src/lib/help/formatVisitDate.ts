/** `2026-04-19` → `4월 19일` (타임존 없이 문자열만 파싱) */
export function formatVisitDateHeading(isoDate: string): string {
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(isoDate.trim());
  if (!m) return isoDate;
  const month = Number(m[2]);
  const day = Number(m[3]);
  if (month < 1 || month > 12 || day < 1 || day > 31) return isoDate;
  return `${month}월 ${day}일`;
}

/** `2026-04-19` → `2026.04.19` */
export function formatVisitDateDisplay(isoDate: string): string {
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(isoDate.trim());
  if (!m) return isoDate;
  const y = m[1];
  const mo = m[2].padStart(2, "0");
  const d = m[3].padStart(2, "0");
  return `${y}.${mo}.${d}`;
}

const WEEKDAYS_KO = ["일", "월", "화", "수", "목", "금", "토"] as const;

/** `2026-04-19` → `수` (로컬 자정 기준 요일) */
export function formatWeekdayShortKo(isoDate: string): string {
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(isoDate.trim());
  if (!m) return "";
  const y = Number(m[1]);
  const monthIndex = Number(m[2]) - 1;
  const d = Number(m[3]);
  if (monthIndex < 0 || monthIndex > 11 || d < 1 || d > 31) return "";
  const dt = new Date(y, monthIndex, d, 12, 0, 0, 0);
  return WEEKDAYS_KO[dt.getDay()] ?? "";
}
