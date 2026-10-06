import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { clubApi, eventApi, membershipApi } from "../api";
import ApplicationRow from "../components/ApplicationRow";
import { CategoryTag, MembershipBadge, Pill } from "../components/Badges";
import CapacityBar from "../components/CapacityBar";
import EmptyState from "../components/EmptyState";
import ErrorBox from "../components/ErrorBox";
import EventCard from "../components/EventCard";
import Spinner from "../components/Spinner";
import Tabs from "../components/Tabs";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { categoryClass, formatDate, monogram, timeAgo } from "../utils/format";
import useRsvp from "../utils/useRsvp";
import ClubForm from "./ClubForm";
import EventForm from "./EventForm";

function JoinPanel({ club, onChanged }) {
  const toast = useToast();
  const { isStudent } = useAuth();
  const [motivation, setMotivation] = useState("");
  const [busy, setBusy] = useState(false);
  const mine = club.myMembership;
  const p = club.permissions;

  const apply = async (e) => {
    e.preventDefault();
    if (motivation.trim().length < 10) return toast("Write at least 10 characters about why you want to join.", "error");
    setBusy(true);
    try {
      await clubApi.apply(club.id, motivation.trim());
      toast("Application sent! The coordinator will review it.");
      setMotivation("");
      onChanged();
    } catch (err) {
      toast(err.message, "error");
    } finally {
      setBusy(false);
    }
  };

  const leave = async () => {
    const pending = mine.status === "PENDING";
    if (!window.confirm(pending ? "Withdraw your application?" : `Leave ${club.name}?`)) return;
    setBusy(true);
    try {
      await membershipApi.leave(mine.id);
      toast(pending ? "Application withdrawn." : `You left ${club.name}.`);
      onChanged();
    } catch (err) {
      toast(err.message, "error");
    } finally {
      setBusy(false);
    }
  };

  if (!isStudent) {
    return (
      <aside className="join-panel">
        {club.permissions.canManage ? (
          <>
            <h3>You manage this club</h3>
            <p className="muted small">
              {club.pendingCount ? `${club.pendingCount} application(s) waiting in the Applications tab.` : "No applications waiting."} Use the
              Events tab to schedule something new.
            </p>
          </>
        ) : (
          <p className="muted small">{p.applyBlockedReason || "Only students can join clubs."}</p>
        )}
      </aside>
    );
  }

  const hasContent = mine || p.canApply || p.applyBlockedReason;
  if (!hasContent) return null;

  return (
    <aside className="join-panel">
      {mine?.status === "APPROVED" && (
        <>
          <MembershipBadge status="APPROVED" />
          <h3>You're a member 🎉</h3>
          <p className="muted small">Joined {formatDate(mine.decidedAt)}. Check the Events tab for what's coming up.</p>
          <button className="btn btn-ghost btn-sm" disabled={busy} onClick={leave}>
            Leave club
          </button>
        </>
      )}
      {mine?.status === "PENDING" && (
        <>
          <MembershipBadge status="PENDING" />
          <h3>Application sent</h3>
          <p className="muted small">You applied {timeAgo(mine.appliedAt)}. The coordinator will approve or reject it.</p>
          {mine.motivation && <p className="quote">“{mine.motivation}”</p>}
          <button className="btn btn-ghost btn-sm" disabled={busy} onClick={leave}>
            Withdraw application
          </button>
        </>
      )}
      {mine && ["REJECTED", "REMOVED"].includes(mine.status) && (
        <div className="notice">
          <MembershipBadge status={mine.status} />
          {mine.remark && <p className="small">Coordinator's note: “{mine.remark}”</p>}
        </div>
      )}
      {p.canApply && (
        <form onSubmit={apply} className="stack-form">
          <h3>{mine ? "Apply again" : "Join this club"}</h3>
          <label className="field">
            <span>Why do you want to join?</span>
            <textarea
              rows={4}
              maxLength={500}
              value={motivation}
              onChange={(e) => setMotivation(e.target.value)}
              placeholder="Your interests, experience, what you'd like to do here…"
            />
            <small className="muted right">{motivation.length}/500</small>
          </label>
          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? "Sending…" : "Send application"}
          </button>
        </form>
      )}
      {!p.canApply && !["APPROVED", "PENDING"].includes(mine?.status) && (
        <p className="muted small">{p.applyBlockedReason}</p>
      )}
    </aside>
  );
}

function MembersTab({ clubId, status, onChanged }) {
  const toast = useToast();
  const [list, setList] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    clubApi
      .members(clubId, status)
      .then((l) => {
        setList(l);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, [clubId, status]);
  useEffect(load, [load]);

  const decide = async (m, next, remark) => {
    try {
      await membershipApi.decide(m.id, next, remark);
      toast(next === "APPROVED" ? `${m.studentName} is now a member.` : `${m.studentName}: ${next.toLowerCase()}.`);
      load();
      onChanged();
      return true;
    } catch (err) {
      toast(err.message, "error");
      return false;
    }
  };

  if (error) return <ErrorBox message={error} onRetry={load} />;
  if (!list) return <Spinner />;
  if (!list.length)
    return <EmptyState title={status === "PENDING" ? "No applications waiting" : "No members yet"} text="New ones will appear here." />;
  return (
    <ul className="app-list">
      {list.map((m) => (
        <ApplicationRow key={m.id} m={m} onDecide={decide} />
      ))}
    </ul>
  );
}

function EventsTab({ club, onChanged }) {
  const toast = useToast();
  const { isStudent } = useAuth();
  const [events, setEvents] = useState(club.upcomingEvents);
  const [creating, setCreating] = useState(false);
  const [attendees, setAttendees] = useState({}); // eventId -> list
  const { toggle, busyId } = useRsvp(setEvents);
  const manage = club.permissions.canManage;

  const create = async (body) => {
    const created = await eventApi.create(body);
    setEvents((l) => [...l, created].sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt)));
    setCreating(false);
    toast("Event created.");
    onChanged();
  };

  const cancel = async (e) => {
    if (!window.confirm(`Cancel "${e.title}"? Registered students will see it as cancelled.`)) return;
    try {
      const updated = await eventApi.cancel(e.id);
      setEvents((l) => l.map((x) => (x.id === e.id ? updated : x)));
      toast("Event cancelled.");
    } catch (err) {
      toast(err.message, "error");
    }
  };

  const showAttendees = async (e) => {
    if (attendees[e.id]) return setAttendees({ ...attendees, [e.id]: null });
    try {
      setAttendees({ ...attendees, [e.id]: await eventApi.attendees(e.id) });
    } catch (err) {
      toast(err.message, "error");
    }
  };

  return (
    <div className="stack">
      {manage && !creating && (
        <button className="btn btn-primary btn-sm self-start" onClick={() => setCreating(true)}>
          + New event
        </button>
      )}
      {creating && <EventForm clubId={club.id} onCreate={create} onCancel={() => setCreating(false)} />}
      {events.length === 0 && !creating && <EmptyState title="No upcoming events" text="Check back soon." />}
      {events.map((e) => (
        <EventCard
          key={e.id}
          event={e}
          showClub={false}
          onToggle={isStudent ? toggle : null}
          busy={busyId === e.id}
          extra={
            manage && (
              <div className="manage-row">
                <button className="link-btn" onClick={() => showAttendees(e)}>
                  {attendees[e.id] ? "Hide attendees" : "Show attendees"}
                </button>
                {!e.cancelled && (
                  <button className="link-btn danger" onClick={() => cancel(e)}>
                    Cancel event
                  </button>
                )}
                {attendees[e.id] && (
                  <ul className="attendees">
                    {attendees[e.id].length === 0 && <li className="muted">No one has registered yet.</li>}
                    {attendees[e.id].map((a) => (
                      <li key={a.userId}>
                        {a.name} <span className="muted">{[a.rollNumber, a.department].filter(Boolean).join(" · ")}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )
          }
        />
      ))}
    </div>
  );
}

export default function ClubDetailPage() {
  const { id } = useParams();
  const toast = useToast();
  const { isAdmin } = useAuth();
  const [club, setClub] = useState(null);
  const [error, setError] = useState("");
  const [tab, setTab] = useState("about");

  const load = useCallback(() => {
    clubApi
      .get(id)
      .then((c) => {
        setClub(c);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, [id]);
  useEffect(load, [load]);

  if (error)
    return (
      <div className="page">
        <Link to="/clubs" className="link">
          ← All clubs
        </Link>
        <ErrorBox message={error} onRetry={load} />
      </div>
    );
  if (!club) return <Spinner />;

  const manage = club.permissions.canManage;
  const tabs = [
    ["about", "About"],
    ["events", `Events (${club.upcomingEvents.length})`],
    ...(manage
      ? [
          ["applications", `Applications${club.pendingCount ? ` (${club.pendingCount})` : ""}`],
          ["members", `Members (${club.memberCount})`],
          ["edit", "Edit club"],
        ]
      : []),
  ];

  return (
    <div className="page">
      <Link to="/clubs" className="link">
        ← All clubs
      </Link>

      <section className={`club-hero ${categoryClass(club.category)}`}>
        <span className="monogram big">{monogram(club.name)}</span>
        <div className="club-hero-text">
          <div className="row-wrap">
            <CategoryTag category={club.category} />
            {!club.registrationOpen && <Pill tone="closed">Registrations closed</Pill>}
            {club.registrationOpen && club.full && <Pill tone="closed">Full</Pill>}
          </div>
          <h1>{club.name}</h1>
          {club.tagline && <p className="tagline">{club.tagline}</p>}
          <ul className="hero-facts">
            {club.meetingSchedule && <li>🗓 {club.meetingSchedule}</li>}
            {club.venue && <li>📍 {club.venue}</li>}
            <li>👤 {club.coordinator ? club.coordinator.name : "Coordinator to be assigned"}</li>
          </ul>
        </div>
      </section>

      <div className="detail-layout">
        <div className="stack">
          <Tabs tabs={tabs} value={tab} onChange={setTab} />
          <section className="panel">
            {tab === "about" && (
              <>
                <p className="description">{club.description}</p>
                <CapacityBar used={club.memberCount} total={club.capacity} />
                {club.coordinator && (
                  <p className="muted small">
                    Questions? Contact {club.coordinator.name} at <a href={`mailto:${club.coordinator.email}`}>{club.coordinator.email}</a>
                  </p>
                )}
              </>
            )}
            {tab === "events" && <EventsTab club={club} onChanged={load} />}
            {tab === "applications" && <MembersTab clubId={club.id} status="PENDING" onChanged={load} />}
            {tab === "members" && <MembersTab clubId={club.id} status="APPROVED" onChanged={load} />}
            {tab === "edit" && (
              <ClubForm
                isAdmin={isAdmin}
                submitLabel="Save changes"
                initial={{ ...club, coordinatorId: club.coordinator?.id || "" }}
                onSubmit={async (body) => {
                  setClub(await clubApi.update(club.id, body));
                  toast("Club updated.");
                  setTab("about");
                }}
              />
            )}
          </section>
        </div>
        <JoinPanel club={club} onChanged={load} />
      </div>
    </div>
  );
}
