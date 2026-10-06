import { useEffect, useState } from "react";
import { userApi } from "../api";
import { CATEGORIES } from "../utils/format";
import { fieldErrors } from "../utils/validation";


/** Create / edit form. Admins see every field; coordinators can't change name, category or coordinator. */
export default function ClubForm({ initial, isAdmin, onSubmit, submitLabel, onCancel }) {
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [coordinators, setCoordinators] = useState([]);

  useEffect(() => {
    if (isAdmin) userApi.list("COORDINATOR").then((l) => setCoordinators(l.filter((u) => u.active))).catch(() => {});
  }, [isAdmin]);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.type === "checkbox" ? e.target.checked : e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const found = {};
    if (!form.name.trim()) found.name = "Club name is required.";
    if (form.description.trim().length < 20) found.description = "Write at least 20 characters.";
    const cap = Number(form.capacity);
    if (!Number.isInteger(cap) || cap < 1 || cap > 500) found.capacity = "Capacity must be 1-500.";
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    setError("");
    try {
      await onSubmit({
        ...form,
        name: form.name.trim(),
        description: form.description.trim(),
        capacity: cap,
        coordinatorId: form.coordinatorId ? Number(form.coordinatorId) : null,
      });
    } catch (err) {
      setErrors(fieldErrors(err.details));
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const err = (k) => errors[k] && <small className="field-error">{errors[k]}</small>;

  return (
    <form className="stack-form" onSubmit={submit} noValidate>
      {error && <div className="alert alert-error">{error}</div>}
      <div className="grid-2">
        <label className={`field${errors.name ? " has-error" : ""}`}>
          <span>Club name</span>
          <input value={form.name} maxLength={100} onChange={set("name")} disabled={!isAdmin} />
          {err("name")}
        </label>
        <label className="field">
          <span>Category</span>
          <select value={form.category} onChange={set("category")} disabled={!isAdmin}>
            {Object.entries(CATEGORIES).map(([k, c]) => (
              <option key={k} value={k}>
                {c.label}
              </option>
            ))}
          </select>
        </label>
      </div>
      <label className="field">
        <span>Tagline</span>
        <input value={form.tagline || ""} maxLength={150} onChange={set("tagline")} placeholder="One line that sells the club" />
      </label>
      <label className={`field${errors.description ? " has-error" : ""}`}>
        <span>Description</span>
        <textarea rows={4} value={form.description} maxLength={2000} onChange={set("description")} />
        {err("description")}
      </label>
      <div className="grid-2">
        <label className="field">
          <span>Meeting schedule</span>
          <input value={form.meetingSchedule || ""} maxLength={150} onChange={set("meetingSchedule")} placeholder="Every Friday, 4-6 PM" />
        </label>
        <label className="field">
          <span>Venue</span>
          <input value={form.venue || ""} maxLength={150} onChange={set("venue")} placeholder="Seminar Hall 2" />
        </label>
      </div>
      <div className="grid-2">
        <label className={`field${errors.capacity ? " has-error" : ""}`}>
          <span>Capacity (max members)</span>
          <input type="number" min={1} max={500} value={form.capacity} onChange={set("capacity")} />
          {err("capacity")}
        </label>
        {isAdmin && (
          <label className="field">
            <span>Coordinator</span>
            <select value={form.coordinatorId || ""} onChange={set("coordinatorId")}>
              <option value="">No coordinator yet</option>
              {coordinators.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>
      <label className="check">
        <input type="checkbox" checked={form.registrationOpen} onChange={set("registrationOpen")} />
        Accepting new members
      </label>
      <div className="form-actions">
        {onCancel && (
          <button type="button" className="btn btn-ghost" onClick={onCancel}>
            Cancel
          </button>
        )}
        <button className="btn btn-primary" disabled={busy}>
          {busy ? "Saving…" : submitLabel}
        </button>
      </div>
    </form>
  );
}
