import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { membershipApi } from "../api";
import { CategoryTag, MembershipBadge } from "../components/Badges";
import EmptyState from "../components/EmptyState";
import ErrorBox from "../components/ErrorBox";
import Spinner from "../components/Spinner";
import { useToast } from "../context/ToastContext";
import { categoryClass, formatDate, monogram } from "../utils/format";

export default function MyClubsPage() {
  const toast = useToast();
  const [list, setList] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    membershipApi
      .mine()
      .then((l) => {
        setList(l);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, []);
  useEffect(load, [load]);

  const leave = async (m) => {
    const pending = m.status === "PENDING";
    if (!window.confirm(pending ? `Withdraw your application to ${m.clubName}?` : `Leave ${m.clubName}?`)) return;
    try {
      await membershipApi.leave(m.id);
      toast(pending ? "Application withdrawn." : `You left ${m.clubName}.`);
      load();
    } catch (err) {
      toast(err.message, "error");
    }
  };

  const active = (list || []).filter((m) => ["APPROVED", "PENDING"].includes(m.status));
  const history = (list || []).filter((m) => !["APPROVED", "PENDING"].includes(m.status));

  const row = (m) => (
    <li key={m.id} className={`membership-row ${categoryClass(m.clubCategory)}`}>
      <span className="monogram small">{monogram(m.clubName)}</span>
      <div className="grow">
        <Link to={`/clubs/${m.clubId}`} className="row-title">
          {m.clubName}
        </Link>
        <div className="row-wrap">
          <CategoryTag category={m.clubCategory} />
          <span className="muted small">
            Applied {formatDate(m.appliedAt)}
            {m.decidedAt && ` · updated ${formatDate(m.decidedAt)}`}
          </span>
        </div>
        {m.remark && <p className="small muted">Coordinator's note: “{m.remark}”</p>}
      </div>
      <MembershipBadge status={m.status} />
      {["APPROVED", "PENDING"].includes(m.status) && (
        <button className="btn btn-ghost btn-sm" onClick={() => leave(m)}>
          {m.status === "PENDING" ? "Withdraw" : "Leave"}
        </button>
      )}
    </li>
  );

  return (
    <div className="page page-narrow">
      <section className="hello">
        <div>
          <h1>My clubs</h1>
          <p className="muted">Your memberships and applications. You can be part of up to 3 clubs at a time.</p>
        </div>
        <Link to="/clubs" className="btn btn-primary">
          Find more clubs
        </Link>
      </section>
      <ErrorBox message={error} onRetry={load} />
      {!list && !error && <Spinner />}
      {list && list.length === 0 && (
        <EmptyState
          title="No clubs yet"
          text="Apply to a club and it will show up here."
          action={
            <Link to="/clubs" className="btn btn-primary btn-sm">
              Explore clubs
            </Link>
          }
        />
      )}
      {active.length > 0 && (
        <section className="panel">
          <h2>Current</h2>
          <ul className="membership-list">{active.map(row)}</ul>
        </section>
      )}
      {history.length > 0 && (
        <section className="panel">
          <h2>History</h2>
          <ul className="membership-list">{history.map(row)}</ul>
        </section>
      )}
    </div>
  );
}
