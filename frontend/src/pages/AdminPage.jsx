import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { clubApi, userApi } from "../api";
import { CategoryTag } from "../components/Badges";
import ErrorBox from "../components/ErrorBox";
import Spinner from "../components/Spinner";
import Tabs from "../components/Tabs";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { ROLE_LABELS, formatDate } from "../utils/format";
import { PASSWORD_RULE, fieldErrors, isEmail, isStrongPassword } from "../utils/validation";
import { EMPTY_CLUB } from "../utils/clubDefaults";
import ClubForm from "./ClubForm";

function ClubsTab() {
  const toast = useToast();
  const [clubs, setClubs] = useState(null);
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);

  const load = useCallback(() => {
    clubApi
      .list()
      .then((l) => {
        setClubs(l);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, []);
  useEffect(load, [load]);

  return (
    <div className="stack">
      {!creating ? (
        <button className="btn btn-primary btn-sm self-start" onClick={() => setCreating(true)}>
          + New club
        </button>
      ) : (
        <section className="panel">
          <h2>New club</h2>
          <ClubForm
            isAdmin
            initial={EMPTY_CLUB}
            submitLabel="Create club"
            onCancel={() => setCreating(false)}
            onSubmit={async (body) => {
              const c = await clubApi.create(body);
              toast(`${c.name} created.`);
              setCreating(false);
              load();
            }}
          />
        </section>
      )}
      <section className="panel">
        <ErrorBox message={error} onRetry={load} />
        {!clubs && !error && <Spinner />}
        {clubs && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Club</th>
                  <th>Coordinator</th>
                  <th>Members</th>
                  <th>Pending</th>
                  <th>Registration</th>
                </tr>
              </thead>
              <tbody>
                {clubs.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <Link to={`/clubs/${c.id}`} className="row-title">
                        {c.name}
                      </Link>
                      <div>
                        <CategoryTag category={c.category} />
                      </div>
                    </td>
                    <td>{c.coordinatorName || <span className="muted">Not assigned</span>}</td>
                    <td>
                      {c.memberCount}/{c.capacity}
                    </td>
                    <td>{c.pendingCount || 0}</td>
                    <td>{c.registrationOpen ? "Open" : <span className="muted">Closed</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

const NEW_USER = { name: "", email: "", password: "", role: "COORDINATOR", department: "" };

function UsersTab() {
  const { user: me } = useAuth();
  const toast = useToast();
  const [users, setUsers] = useState(null);
  const [error, setError] = useState("");
  const [filter, setFilter] = useState("");
  const [form, setForm] = useState(NEW_USER);
  const [errors, setErrors] = useState({});

  const load = useCallback(() => {
    userApi
      .list(filter)
      .then((l) => {
        setUsers(l);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, [filter]);
  useEffect(load, [load]);

  const create = async (e) => {
    e.preventDefault();
    const found = {};
    if (!form.name.trim()) found.name = "Name is required.";
    if (!isEmail(form.email)) found.email = "Enter a valid email address.";
    if (!isStrongPassword(form.password)) found.password = `Password must be ${PASSWORD_RULE}`;
    setErrors(found);
    if (Object.keys(found).length) return;
    try {
      const u = await userApi.create({ ...form, name: form.name.trim(), email: form.email.trim() });
      toast(`${u.name} added as ${ROLE_LABELS[u.role].toLowerCase()}.`);
      setForm(NEW_USER);
      load();
    } catch (err) {
      setErrors(fieldErrors(err.details));
      toast(err.message, "error");
    }
  };

  const toggle = async (u) => {
    try {
      await userApi.setActive(u.id, !u.active);
      toast(`${u.name} ${u.active ? "disabled" : "enabled"}.`);
      load();
    } catch (err) {
      toast(err.message, "error");
    }
  };

  const input = (key, label, type = "text") => (
    <label className={`field${errors[key] ? " has-error" : ""}`}>
      <span>{label}</span>
      <input type={type} value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} />
      {errors[key] && <small className="field-error">{errors[key]}</small>}
    </label>
  );

  return (
    <div className="admin-grid">
      <section className="panel">
        <div className="panel-head">
          <h2>People</h2>
          <select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter by role" className="compact">
            <option value="">Everyone</option>
            <option value="STUDENT">Students</option>
            <option value="COORDINATOR">Coordinators</option>
            <option value="ADMIN">Admins</option>
          </select>
        </div>
        <ErrorBox message={error} onRetry={load} />
        {!users && !error && <Spinner />}
        {users && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Role</th>
                  <th>Department</th>
                  <th>Joined</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className={u.active ? "" : "row-off"}>
                    <td>
                      <strong>{u.name}</strong>
                      <div className="muted small">{u.email}</div>
                    </td>
                    <td>
                      <span className={`role role-${u.role.toLowerCase()}`}>{ROLE_LABELS[u.role]}</span>
                    </td>
                    <td>{u.department || "—"}</td>
                    <td className="nowrap small">{formatDate(u.createdAt)}</td>
                    <td className="right">
                      {u.id !== me.id && (
                        <button className={`btn btn-sm ${u.active ? "btn-danger" : "btn-ghost"}`} onClick={() => toggle(u)}>
                          {u.active ? "Disable" : "Enable"}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <form className="panel stack-form" onSubmit={create} noValidate>
        <h2>Add a coordinator or admin</h2>
        <p className="muted small">Students sign up on their own. Assign a coordinator to a club from the Clubs tab.</p>
        {input("name", "Full name")}
        {input("email", "Email", "email")}
        {input("department", "Department")}
        {input("password", "Temporary password", "password")}
        <label className="field">
          <span>Role</span>
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="COORDINATOR">Coordinator</option>
            <option value="ADMIN">Admin</option>
          </select>
        </label>
        <button className="btn btn-primary btn-block">Create account</button>
      </form>
    </div>
  );
}

export default function AdminPage() {
  const [tab, setTab] = useState("clubs");
  return (
    <div className="page">
      <section className="hello">
        <div>
          <h1>Admin</h1>
          <p className="muted">Create clubs, assign coordinators and manage accounts.</p>
        </div>
      </section>
      <Tabs
        tabs={[
          ["clubs", "Clubs"],
          ["users", "Users"],
        ]}
        value={tab}
        onChange={setTab}
      />
      {tab === "clubs" ? <ClubsTab /> : <UsersTab />}
    </div>
  );
}
