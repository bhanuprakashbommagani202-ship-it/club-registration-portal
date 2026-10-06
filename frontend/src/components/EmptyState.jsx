export default function EmptyState({ title, text, action }) {
  return (
    <div className="empty">
      <div className="empty-dots" aria-hidden="true">
        <span />
        <span />
        <span />
        <span />
      </div>
      <h3>{title}</h3>
      {text && <p className="muted">{text}</p>}
      {action}
    </div>
  );
}
