import { BrandMark } from "../components/Layout";

const BUBBLES = [
  ["Coding", "cat-technical"],
  ["Music", "cat-cultural"],
  ["Cricket", "cat-sports"],
  ["Debate", "cat-literary"],
  ["Drama", "cat-arts"],
  ["NSS", "cat-social-service"],
  ["Robotics", "cat-technical"],
  ["Dance", "cat-cultural"],
];

export default function AuthShell({ children }) {
  return (
    <div className="auth">
      <div className="auth-bubbles" aria-hidden="true">
        {BUBBLES.map(([label, cls], i) => (
          <span key={label} className={`bubble ${cls} b${i}`}>
            {label}
          </span>
        ))}
      </div>
      <div className="auth-card">
        <div className="auth-brand">
          <BrandMark size={40} />
          <div>
            <strong>
              Club<b>Portal</b>
            </strong>
            <span>Find your people on campus</span>
          </div>
        </div>
        {children}
      </div>
    </div>
  );
}
