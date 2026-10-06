import { Link } from "react-router-dom";
import { categoryClass, dayBlock, eventWhen } from "../utils/format";
import { Pill } from "./Badges";

/** onToggle(event) registers / unregisters; omit it to hide the button. */
export default function EventCard({ event: e, onToggle, busy, showClub = true, extra }) {
  const d = dayBlock(e.startsAt);
  const seatsLeft = e.capacity - e.registeredCount;
  // eslint-disable-next-line react/purity -- comparing with "now" on each render is intended
  const past = new Date(e.startsAt).getTime() <= Date.now();

  let action = null;
  if (onToggle && !past && !e.cancelled) {
    if (e.registeredByMe) {
      action = (
        <button className="btn btn-ghost btn-sm" disabled={busy} onClick={() => onToggle(e)}>
          Cancel my seat
        </button>
      );
    } else {
      action = (
        <button className="btn btn-primary btn-sm" disabled={busy || seatsLeft <= 0} onClick={() => onToggle(e)}>
          {seatsLeft <= 0 ? "Full" : "Register"}
        </button>
      );
    }
  }

  return (
    <article className={`event-card ${categoryClass(e.clubCategory)}${e.cancelled ? " cancelled" : ""}`}>
      <div className="date-block" aria-hidden="true">
        <span>{d.month}</span>
        <strong>{d.day}</strong>
      </div>
      <div className="event-main">
        <div className="event-top">
          {showClub && (
            <Link to={`/clubs/${e.clubId}`} className="event-club">
              {e.clubName}
            </Link>
          )}
          {e.membersOnly && <Pill tone="neutral">Members only</Pill>}
          {e.cancelled && <Pill tone="closed">Cancelled</Pill>}
          {e.registeredByMe && !e.cancelled && <Pill tone="approved">You're going</Pill>}
        </div>
        <h3>{e.title}</h3>
        <p className="event-meta">
          {eventWhen(e.startsAt, e.endsAt)} · {e.venue}
        </p>
        {e.description && <p className="muted small">{e.description}</p>}
        {extra}
      </div>
      <div className="event-side">
        <span className="seats">
          <span>
            <strong>{e.registeredCount}</strong> / {e.capacity}
          </span>
          <small>registered</small>
        </span>
        {action}
      </div>
    </article>
  );
}
