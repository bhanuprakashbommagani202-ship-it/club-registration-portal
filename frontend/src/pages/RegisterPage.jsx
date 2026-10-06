import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { PASSWORD_RULE, fieldErrors, isEmail, isStrongPassword } from "../utils/validation";
import AuthShell from "./AuthShell";

const EMPTY = { name: "", email: "", rollNumber: "", department: "", password: "", confirm: "" };

function validate(f) {
  const e = {};
  if (!f.name.trim()) e.name = "Name is required.";
  if (!isEmail(f.email)) e.email = "Enter a valid email address.";
  if (!isStrongPassword(f.password)) e.password = `Password must be ${PASSWORD_RULE}`;
  if (f.confirm !== f.password) e.confirm = "Passwords don't match.";
  return e;
}

export default function RegisterPage() {
  const { user, register } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    const found = validate(form);
    setErrors(found);
    setError("");
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      const u = await register({
        name: form.name.trim(),
        email: form.email.trim(),
        rollNumber: form.rollNumber.trim(),
        department: form.department.trim(),
        password: form.password,
      });
      toast(`Welcome, ${u.name.split(" ")[0]}! Start by exploring clubs.`);
      navigate("/clubs", { replace: true });
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
      <input type={type} value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} {...extra} />
      {errors[key] && <small className="field-error">{errors[key]}</small>}
    </label>
  );

  return (
    <AuthShell>
      <form className="stack-form" onSubmit={submit} noValidate>
        <h1 className="auth-title">Create your account</h1>
        {error && <div className="alert alert-error">{error}</div>}
        {field("name", "Full name", "text", { autoComplete: "name", placeholder: "Sneha Patel" })}
        {field("email", "College email", "email", { autoComplete: "email", placeholder: "you@college.edu" })}
        <div className="grid-2">
          {field("rollNumber", "Roll number", "text", { placeholder: "22CS1045" })}
          {field("department", "Department", "text", { placeholder: "CSE" })}
        </div>
        <div className="grid-2">
          {field("password", "Password", "password", { autoComplete: "new-password" })}
          {field("confirm", "Confirm password", "password", { autoComplete: "new-password" })}
        </div>
        {!errors.password && <small className="muted">Password: {PASSWORD_RULE}</small>}
        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? "Creating account…" : "Create account"}
        </button>
        <p className="auth-switch">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </AuthShell>
  );
}
