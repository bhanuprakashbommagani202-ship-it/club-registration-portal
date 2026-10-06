import { useEffect, useState } from "react";
import { eventApi } from "../api";
import EmptyState from "../components/EmptyState";
import ErrorBox from "../components/ErrorBox";
import EventCard from "../components/EventCard";
import Spinner from "../components/Spinner";
import Tabs from "../components/Tabs";
import { useAuth } from "../context/AuthContext";
import useRsvp from "../utils/useRsvp";

export default function EventsPage() {
  const { isStudent } = useAuth();
  const [scope, setScope] = useState("upcoming");
  const [events, setEvents] = useState(null);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const { toggle, busyId } = useRsvp(setEvents);

  useEffect(() => {
    let cancelled = false;
    eventApi
      .list({ scope })
      .then((list) => {
        if (cancelled) return;
        setEvents(list);
        setError("");
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, [scope, reload]);

  const tabs = [["upcoming", "Upcoming"], ...(isStudent ? [["mine", "My registrations"]] : []), ["past", "Past"]];

  // Group by month for a calendar-like feel.
  const groups = [];
  for (const e of events || []) {
    const key = new Date(e.startsAt).toLocaleDateString("en-IN", { month: "long", year: "numeric" });
    const last = groups[groups.length - 1];
    if (last && last.key === key) last.items.push(e);
    else groups.push({ key, items: [e] });
  }

  return (
    <div className="page page-narrow">
      <section className="hello">
        <div>
          <h1>Events</h1>
          <p className="muted">Workshops, meetups and performances from every club on campus.</p>
        </div>
      </section>
      <Tabs
        tabs={tabs}
        value={scope}
        onChange={(s) => {
          setEvents(null);
          setScope(s);
        }}
      />
      <ErrorBox message={error} onRetry={() => setReload((n) => n + 1)} />
      {!events && !error && <Spinner />}
      {events && events.length === 0 && (
        <EmptyState
          title={scope === "mine" ? "You haven't registered for anything yet" : "No events here"}
          text={scope === "mine" ? "Register from the Upcoming tab." : null}
        />
      )}
      {groups.map((g) => (
        <section key={g.key} className="stack">
          <h2 className="month">{g.key}</h2>
          {g.items.map((e) => (
            <EventCard key={e.id} event={e} onToggle={isStudent ? toggle : null} busy={busyId === e.id} />
          ))}
        </section>
      ))}
    </div>
  );
}
