import { Link } from "react-router-dom";
import { categoryClass, monogram } from "../utils/format";
import { CategoryTag, MembershipBadge, Pill } from "./Badges";
import CapacityBar from "./CapacityBar";

export default function ClubCard({ club }) {
  return (
    <Link to={`/clubs/${club.id}`} className={`club-card ${categoryClass(club.category)}`}>
      <div className="club-card-banner">
        <span className="monogram">{monogram(club.name)}</span>
        <div className="club-card-flags">
          {club.myStatus && <MembershipBadge status={club.myStatus} />}
          {!club.registrationOpen && <Pill tone="closed">Closed</Pill>}
          {club.registrationOpen && club.full && <Pill tone="closed">Full</Pill>}
          {club.pendingCount > 0 && <Pill tone="attention">{club.pendingCount} to review</Pill>}
        </div>
      </div>
      <div className="club-card-body">
        <CategoryTag category={club.category} />
        <h3>{club.name}</h3>
        {club.tagline && <p className="muted">{club.tagline}</p>}
        <dl className="club-facts">
          {club.meetingSchedule && (
            <div>
              <dt>When</dt>
              <dd>{club.meetingSchedule}</dd>
            </div>
          )}
          {club.coordinatorName && (
            <div>
              <dt>Coordinator</dt>
              <dd>{club.coordinatorName}</dd>
            </div>
          )}
        </dl>
        <CapacityBar used={club.memberCount} total={club.capacity} />
      </div>
    </Link>
  );
}
