export default function CapacityBar({ used, total, label = "members" }) {
  const pct = total ? Math.min(100, Math.round((used / total) * 100)) : 0;
  const tone = pct >= 100 ? "full" : pct >= 80 ? "almost" : "";
  return (
    <div className="capacity">
      <div className="capacity-text">
        <span>
          <strong>{used}</strong> / {total} {label}
        </span>
        <span className="muted">{pct >= 100 ? "Full" : `${total - used} left`}</span>
      </div>
      <div className="capacity-track" role="progressbar" aria-valuenow={used} aria-valuemin={0} aria-valuemax={total}>
        <div className={`capacity-fill ${tone}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
