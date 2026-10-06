import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { clubApi } from "../api";
import ClubCard from "../components/ClubCard";
import EmptyState from "../components/EmptyState";
import ErrorBox from "../components/ErrorBox";
import Spinner from "../components/Spinner";
import { CATEGORIES, categoryClass } from "../utils/format";

export default function ClubsPage() {
  const [params, setParams] = useSearchParams();
  const [clubs, setClubs] = useState(null);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [search, setSearch] = useState(params.get("q") || "");

  const category = params.get("category") || "";
  const openOnly = params.get("open") === "1";
  const q = params.get("q") || "";

  const update = (changes) => {
    const next = { category, open: openOnly ? "1" : "", q, ...changes };
    setParams(Object.fromEntries(Object.entries(next).filter(([, v]) => v)));
  };

  useEffect(() => {
    let cancelled = false;
    clubApi
      .list({ category, openOnly, q })
      .then((list) => {
        if (cancelled) return;
        setClubs(list);
        setError("");
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, [category, openOnly, q, reload]);

  // Debounced search
  useEffect(() => {
    if (search === q) return;
    const t = setTimeout(() => update({ q: search.trim() }), 350);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  return (
    <div className="page">
      <section className="explore-hero">
        <h1>Explore clubs</h1>
        <p className="muted">Pick what you love, apply in a minute, and the coordinator will get back to you.</p>
        <input
          className="search"
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search clubs — try “music”, “robots” or “debate”"
          aria-label="Search clubs"
        />
        <div className="cat-filter" role="group" aria-label="Category">
          <button className={`cat-chip${!category ? " active" : ""}`} onClick={() => update({ category: "" })}>
            All
          </button>
          {Object.entries(CATEGORIES).map(([key, c]) => (
            <button
              key={key}
              className={`cat-chip ${categoryClass(key)}${category === key ? " active" : ""}`}
              onClick={() => update({ category: category === key ? "" : key })}
            >
              <span aria-hidden="true">{c.icon}</span> {c.label}
            </button>
          ))}
          <label className="check">
            <input type="checkbox" checked={openOnly} onChange={(e) => update({ open: e.target.checked ? "1" : "" })} />
            Open for registration
          </label>
        </div>
      </section>

      <ErrorBox message={error} onRetry={() => setReload((n) => n + 1)} />
      {!clubs && !error && <Spinner />}
      {clubs &&
        (clubs.length ? (
          <>
            <p className="muted small">
              {clubs.length} club{clubs.length === 1 ? "" : "s"}
            </p>
            <div className="club-grid">
              {clubs.map((c) => (
                <ClubCard key={c.id} club={c} />
              ))}
            </div>
          </>
        ) : (
          <EmptyState
            title="No clubs match"
            text="Try another category or a different search."
            action={
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  setSearch("");
                  setParams({});
                }}
              >
                Clear filters
              </button>
            }
          />
        ))}
    </div>
  );
}
