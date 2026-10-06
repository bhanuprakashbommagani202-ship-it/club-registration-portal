import { useState } from "react";
import { toIso } from "../utils/format";
import { fieldErrors } from "../utils/validation";

const EMPTY = { title: "", description: "", startsAt: "", endsAt: "", venue: "", capacity: 50, membersOnly: false };

export default function EventForm({ clubId, onCreate, onCancel }) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.type === "checkbox" ? e.target.checked : e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const found = {};
    if (form.title.trim().length < 3) found.title = "Title must be at least 3 characters.";
    if (!form.startsAt) found.startsAt = "Choose a start time.";
    else if (new Date(form.startsAt) <= new Date()) found.startsAt = "Start time must be in the future.";
    if (!form.endsAt) found.endsAt = "Choose an end time.";
    else if (form.startsAt && new Date(form.endsAt) <= new Date(form.startsAt)) found.endsAt = "End must be after the start.";
    if (!form.venue.trim()) found.venue = "Venue is required.";
    const cap = Number(form.capacity);
    if (!Number.isInteger(cap) || cap < 1) found.capacity = "Capacity must be at least 1.";
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    setError("");
    try {
      await onCreate({
        clubId,
        title: form.title.trim(),
        description: form.description.trim(),
        startsAt: toIso(form.startsAt),
        endsAt: toIso(form.endsAt),
        venue: form.venue.trim(),
        capacity: cap,
        membersOnly: form.membersOnly,
      });
      setForm(EMPTY);
    } catch (err) {
      setErrors(fieldErrors(err.details));
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const field = (key, label, type = "text", extra = {}) => (
    <label className={`field${errors[key] ? " has-error" : ""}`}>
      <span>{label}</span>
      <input type={type} value={form[key]} onChange={set(key)} {...extra} />
      {errors[key] && <small className="field-error">{errors[key]}</small>}
    </label>
  );

  return (
    <form className="stack-form inset" onSubmit={submit} noValidate>
      <h3>New event</h3>
      {error && <div className="alert alert-error">{error}</div>}
      {field("title", "Title", "text", { maxLength: 150, placeholder: "Hackathon kickoff" })}
      <label className="field">
        <span>Description (optional)</span>
        <textarea rows={2} maxLength={1000} value={form.description} onChange={set("description")} />
      </label>
      <div className="grid-2">
        {field("startsAt", "Starts", "datetime-local")}
        {field("endsAt", "Ends", "datetime-local")}
      </div>
      <div className="grid-2">
        {field("venue", "Venue", "text", { maxLength: 150 })}
        {field("capacity", "Seats", "number", { min: 1, max: 2000 })}
      </div>
      <label className="check">
        <input type="checkbox" checked={form.membersOnly} onChange={set("membersOnly")} />
        Only club members can register
      </label>
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>
          Cancel
        </button>
        <button className="btn btn-primary" disabled={busy}>
          {busy ? "Creating…" : "Create event"}
        </button>
      </div>
    </form>
  );
}
