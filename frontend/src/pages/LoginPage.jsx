import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { isEmail } from "../utils/validation";
import AuthShell from "./AuthShell";

const DEMO = [
  ["Student", "student@college.edu", "Student@123"],
  ["Coordinator", "arjun.coord@college.edu", "Coord@123"],
  ["Admin", "admin@college.edu", "Admin@123"],
];

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    if (!isEmail(form.email)) return setError("Enter a valid email address.");
    if (!form.password) return setError("Enter your password.");
    setBusy(true);
    setError("");
    try {
      await login(form.email.trim(), form.password);
      navigate(location.state?.from || "/", { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthShell>
      <form className="stack-form" onSubmit={submit} noValidate>
        <h1 className="auth-title">Log in</h1>
        {error && <div className="alert alert-error">{error}</div>}
        <label className="field">
          <span>College email</span>
          <input
            type="email"
            autoComplete="email"
            value={form.email}
            placeholder="you@college.edu"
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
        </label>
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? "Logging in…" : "Log in"}
        </button>
        <p className="auth-switch">
          New student? <Link to="/register">Create an account</Link>
        </p>
        <div className="demo">
          <span>Try a demo account</span>
          <div>
            {DEMO.map(([label, email, password]) => (
              <button
                key={label}
                type="button"
                className="chip"
                onClick={() => {
                  setForm({ email, password });
                  setError("");
                }}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
      </form>
    </AuthShell>
  );
}
