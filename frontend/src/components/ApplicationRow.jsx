import { useState } from "react";
import { Link } from "react-router-dom";
import { initials, timeAgo } from "../utils/format";
import { MembershipBadge } from "./Badges";

/**
 * One application / membership in a coordinator's list.
 * onDecide(membership, status, remark) returns a promise; a reason is asked for when rejecting or removing.
 */
export default function ApplicationRow({ m, onDecide, showClub = false }) {
  const [asking, setAsking] = useState(null); // "REJECTED" | "REMOVED" | null
  const [remark, setRemark] = useState("");
  const [busy, setBusy] = useState(false);

  const run = async (status, text) => {
    setBusy(true);
    const ok = await onDecide(m, status, text);
    setBusy(false);
    if (ok) {
      setAsking(null);
      setRemark("");
    }
  };

  return (
    <li className="app-row">
      <div className="app-person">
        <span className="avatar avatar-sm" aria-hidden="true">
          {initials(m.studentName)}
        </span>
        <div>
          <strong>{m.studentName}</strong>
          <div className="muted small">
            {[m.rollNumber, m.department, m.studentEmail].filter(Boolean).join(" · ")}
          </div>
          {showClub && (
            <Link className="link small" to={`/clubs/${m.clubId}`}>
              {m.clubName}
            </Link>
          )}
        </div>
      </div>
      {m.motivation && m.status === "PENDING" && <p className="app-motivation">“{m.motivation}”</p>}
      <div className="app-actions">
        <span className="muted small">
          {m.status === "PENDING" ? `Applied ${timeAgo(m.appliedAt)}` : <MembershipBadge status={m.status} />}
        </span>
        {m.status === "PENDING" && !asking && (
          <>
            <button className="btn btn-ghost btn-sm" disabled={busy} onClick={() => setAsking("REJECTED")}>
              Reject
            </button>
            <button className="btn btn-primary btn-sm" disabled={busy} onClick={() => run("APPROVED", null)}>
              Approve
            </button>
          </>
        )}
        {m.status === "APPROVED" && !asking && (
          <button className="btn btn-danger btn-sm" disabled={busy} onClick={() => setAsking("REMOVED")}>
            Remove
          </button>
        )}
      </div>
      {asking && (
        <form
          className="reason-form"
          onSubmit={(e) => {
            e.preventDefault();
            run(asking, remark.trim());
          }}
        >
          <input
            autoFocus
            value={remark}
            maxLength={300}
            onChange={(e) => setRemark(e.target.value)}
            placeholder={asking === "REJECTED" ? "Reason for rejecting (the student will see this)" : "Reason for removing"}
          />
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setAsking(null)}>
            Cancel
          </button>
          <button className="btn btn-danger btn-sm" disabled={busy || !remark.trim()}>
            {asking === "REJECTED" ? "Reject" : "Remove"}
          </button>
        </form>
      )}
    </li>
  );
}
