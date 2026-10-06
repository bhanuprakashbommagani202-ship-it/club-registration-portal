export const CATEGORIES = {
  TECHNICAL: { label: "Technical", icon: "⌘" },
  CULTURAL: { label: "Cultural", icon: "♪" },
  SPORTS: { label: "Sports", icon: "⚑" },
  LITERARY: { label: "Literary", icon: "✎" },
  ARTS: { label: "Arts", icon: "✦" },
  SOCIAL_SERVICE: { label: "Social Service", icon: "❤" },
};

export const MEMBERSHIP_LABELS = {
  PENDING: "Pending",
  APPROVED: "Member",
  REJECTED: "Rejected",
  WITHDRAWN: "Withdrawn",
  LEFT: "Left",
  REMOVED: "Removed",
};

export const ROLE_LABELS = { STUDENT: "Student", COORDINATOR: "Coordinator", ADMIN: "Admin" };

export const categoryClass = (category) => `cat-${(category || "").toLowerCase().replace("_", "-")}`;

export function monogram(name = "") {
  const words = name.split(" ").filter((w) => w && !["club", "the", "of", "and", "&"].includes(w.toLowerCase()));
  const parts = words.length ? words : [name];
  if (parts.length === 1) return parts[0].slice(0, 2).replace(/^./, (c) => c.toUpperCase()); // "Coding Club" -> "Co"
  return parts
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join("");
}

export function initials(name = "") {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");
}

export function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export function formatTime(iso) {
  return new Date(iso).toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit" });
}

/** "Fri, 10 Oct · 10:00 am – 1:00 pm" */
export function eventWhen(start, end) {
  const s = new Date(start);
  const day = s.toLocaleDateString("en-IN", { weekday: "short", day: "numeric", month: "short" });
  return `${day} · ${formatTime(start)} – ${formatTime(end)}`;
}

export function dayBlock(iso) {
  const d = new Date(iso);
  return {
    month: d.toLocaleDateString("en-IN", { month: "short" }).toUpperCase(),
    day: d.getDate(),
  };
}

export function timeAgo(iso) {
  if (!iso) return "";
  const seconds = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
  const future = seconds < 0;
  const s = Math.abs(seconds);
  const units = [
    ["year", 31536000],
    ["month", 2592000],
    ["day", 86400],
    ["hour", 3600],
    ["minute", 60],
  ];
  for (const [name, size] of units) {
    const n = Math.floor(s / size);
    if (n >= 1) {
      const text = `${n} ${name}${n > 1 ? "s" : ""}`;
      return future ? `in ${text}` : `${text} ago`;
    }
  }
  return "just now";
}

export function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}

/** Value for <input type="datetime-local"> → ISO string in UTC for the API. */
export const toIso = (local) => (local ? new Date(local).toISOString() : null);
