import { CATEGORIES, MEMBERSHIP_LABELS, categoryClass } from "../utils/format";

export function CategoryTag({ category }) {
  const c = CATEGORIES[category];
  return (
    <span className={`tag ${categoryClass(category)}`}>
      <span aria-hidden="true">{c?.icon}</span> {c?.label || category}
    </span>
  );
}

export function MembershipBadge({ status }) {
  if (!status) return null;
  return <span className={`pill pill-${status.toLowerCase()}`}>{MEMBERSHIP_LABELS[status] || status}</span>;
}

export function Pill({ tone = "neutral", children }) {
  return <span className={`pill pill-${tone}`}>{children}</span>;
}
