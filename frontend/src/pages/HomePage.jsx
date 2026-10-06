import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { dashboardApi, membershipApi } from "../api";
import ApplicationRow from "../components/ApplicationRow";
import ClubCard from "../components/ClubCard";
import EmptyState from "../components/EmptyState";
import ErrorBox from "../components/ErrorBox";
import EventCard from "../components/EventCard";
import Spinner from "../components/Spinner";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { CATEGORIES, greeting, timeAgo } from "../utils/format";
import useRsvp from "../utils/useRsvp";

const INTRO = {
  STUDENT: "Your clubs, applications and the events you're going to.",
  COORDINATOR: "Your clubs, new applications and upcoming events.",
  ADMIN: "Everything happening across campus clubs.",
};

export default function HomePage() {
  const { user } = useAuth();
  const toast = useToast();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [events, setEvents] = useState([]);
  const { toggle, busyId } = useRsvp(setEvents);

  const load = useCallback(() => {
    dashboardApi
      .get()
      .then((d) => {
        setData(d);
        setEvents(d.upcomingEvents);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, []);
  useEffect(load, [load]);

  const decide = async (m, status, remark) => {
    try {
      await membershipApi.decide(m.id, status, remark);
      toast(status === "APPROVED" ? `${m.studentName} joined ${m.clubName}.` : `Application ${status.toLowerCase()}.`);
      load();
      return true;
    } catch (err) {
      toast(err.message, "error");
      return false;
    }
  };

  const role = user.role;
  const maxCategory = data ? Math.max(1, ...data.membersByCategory.map((c) => c.count)) : 1;

  return (
    <div className="page">
      <section className="hello">
        <div>
          <p className="eyebrow">{greeting()}</p>
          <h1>{user.name.split(" ")[0]} 👋</h1>
          <p className="muted">{INTRO[role]}</p>
        </div>
        {role === "STUDENT" && (
          <Link to="/clubs" className="btn btn-primary">
            Explore clubs →
          </Link>
        )}
      </section>

      <ErrorBox message={error} onRetry={load} />
      {!data && !error && <Spinner />}

      {data && (
        <>
          <section className="stats">
            {data.stats.map((s, i) => (
              <div key={s.label} className={`stat s${i}`}>
                <span className="stat-label">{s.label}</span>
                <strong>{s.value}</strong>
                {s.hint && <small>{s.hint}</small>}
              </div>
            ))}
          </section>

          {data.pendingApplications.length > 0 && (
            <section className="panel">
              <div className="panel-head">
                <h2>{role === "STUDENT" ? "Waiting for approval" : "Applications to review"}</h2>
              </div>
              {role === "STUDENT" ? (
                <ul className="simple-list">
                  {data.pendingApplications.map((m) => (
                    <li key={m.id}>
                      <Link to={`/clubs/${m.clubId}`} className="link">
                        {m.clubName}
                      </Link>
                      <span className="muted small">applied {timeAgo(m.appliedAt)}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <ul className="app-list">
                  {data.pendingApplications.map((m) => (
                    <ApplicationRow key={m.id} m={m} onDecide={decide} showClub />
                  ))}
                </ul>
              )}
            </section>
          )}

          <section className="panel">
            <div className="panel-head">
              <h2>{role === "STUDENT" ? "My clubs" : role === "COORDINATOR" ? "Clubs you run" : "Most popular clubs"}</h2>
              <Link to="/clubs" className="link">
                All clubs →
              </Link>
            </div>
            {data.clubs.length ? (
              <div className="club-grid">
                {data.clubs.map((c) => (
                  <ClubCard key={c.id} club={c} />
                ))}
              </div>
            ) : (
              <EmptyState
                title={role === "STUDENT" ? "You haven't joined a club yet" : "No clubs yet"}
                text={role === "STUDENT" ? "Browse clubs and apply to the ones you like." : null}
                action={
                  role === "STUDENT" && (
                    <Link to="/clubs" className="btn btn-primary btn-sm">
                      Explore clubs
                    </Link>
                  )
                }
              />
            )}
          </section>

          <div className={role === "ADMIN" ? "two-col" : ""}>
            <section className="panel">
              <div className="panel-head">
                <h2>{role === "STUDENT" ? "Events you're going to" : "Upcoming events"}</h2>
                <Link to="/events" className="link">
                  All events →
                </Link>
              </div>
              {events.length ? (
                <div className="stack">
                  {events.map((e) => (
                    <EventCard key={e.id} event={e} onToggle={role === "STUDENT" ? toggle : null} busy={busyId === e.id} />
                  ))}
                </div>
              ) : (
                <p className="muted">Nothing scheduled yet.</p>
              )}
            </section>

            {role === "ADMIN" && (
              <section className="panel">
                <h2>Members by category</h2>
                <ul className="bars">
                  {data.membersByCategory.map((c) => (
                    <li key={c.label}>
                      <span>{CATEGORIES[c.label]?.label}</span>
                      <div className="bar-track">
                        <div className={`bar-fill cat-${c.label.toLowerCase().replace("_", "-")}`} style={{ width: `${(c.count / maxCategory) * 100}%` }} />
                      </div>
                      <strong>{c.count}</strong>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </div>
        </>
      )}
    </div>
  );
}
