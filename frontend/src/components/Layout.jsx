import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ROLE_LABELS, initials } from "../utils/format";

export function BrandMark({ size = 30 }) {
  return <img src="/favicon.svg" alt="" width={size} height={size} />;
}

export default function Layout() {
  const { user, logout, isAdmin, isStudent } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const links = [
    { to: "/", label: "Home", end: true },
    { to: "/clubs", label: "Explore clubs" },
    { to: "/events", label: "Events" },
    ...(isStudent ? [{ to: "/my-clubs", label: "My clubs" }] : []),
    ...(isAdmin ? [{ to: "/admin", label: "Admin" }] : []),
  ];

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand" onClick={() => setOpen(false)}>
            <BrandMark />
            <span>
              Club<b>Portal</b>
            </span>
          </NavLink>

          <button className="nav-toggle" aria-label="Menu" aria-expanded={open} onClick={() => setOpen(!open)}>
            <span />
            <span />
            <span />
          </button>

          <nav className={`nav${open ? " open" : ""}`} onClick={() => setOpen(false)}>
            {links.map((l) => (
              <NavLink key={l.to} to={l.to} end={l.end} className="nav-link">
                {l.label}
              </NavLink>
            ))}
            <div className="nav-user">
              <span className="avatar" aria-hidden="true">
                {initials(user.name)}
              </span>
              <span className="nav-user-text">
                <strong>{user.name}</strong>
                <small>{ROLE_LABELS[user.role]}</small>
              </span>
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  logout();
                  navigate("/login");
                }}
              >
                Log out
              </button>
            </div>
          </nav>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
      <footer className="footer">Club Registration Portal · React · Spring Boot · MySQL</footer>
    </div>
  );
}
