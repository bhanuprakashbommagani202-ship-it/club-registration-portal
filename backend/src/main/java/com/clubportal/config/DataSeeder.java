package com.clubportal.config;

import com.clubportal.model.*;
import com.clubportal.repository.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fills an empty database with demo users, clubs, memberships and events (runs only when there are no users). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final ClubRepository clubs;
    private final MembershipRepository memberships;
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final PasswordEncoder encoder;
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);

    public DataSeeder(UserRepository users, ClubRepository clubs, MembershipRepository memberships,
                      EventRepository events, EventRegistrationRepository registrations, PasswordEncoder encoder) {
        this.users = users;
        this.clubs = clubs;
        this.memberships = memberships;
        this.events = events;
        this.registrations = registrations;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        User admin = user("Portal Admin", "admin@college.edu", "Admin@123", Role.ADMIN, "Student Affairs", null);
        User arjun = user("Arjun Mehta", "arjun.coord@college.edu", "Coord@123", Role.COORDINATOR, "CSE", null);
        User meera = user("Meera Iyer", "meera.coord@college.edu", "Coord@123", Role.COORDINATOR, "Humanities", null);
        User vikram = user("Vikram Singh", "vikram.coord@college.edu", "Coord@123", Role.COORDINATOR,
                "Physical Education", null);
        User sneha = user("Sneha Patel", "student@college.edu", "Student@123", Role.STUDENT, "CSE", "22CS1045");
        User kiran = user("Kiran Rao", "kiran@college.edu", "Student@123", Role.STUDENT, "ECE", "22EC2031");
        User aisha = user("Aisha Khan", "aisha@college.edu", "Student@123", Role.STUDENT, "IT", "23IT1102");

        Club coding = club("Coding Club", ClubCategory.TECHNICAL, "Build, break and ship code together",
                "Weekly problem-solving sessions, hackathons and open-source sprints. Beginners welcome - "
                        + "seniors mentor first-years on DSA, web development and competitive programming.",
                "Every Wednesday, 4:00 - 6:00 PM", "Lab 3, CSE Block", 60, true, arjun);
        Club robotics = club("Robotics Club", ClubCategory.TECHNICAL, "From line followers to drones",
                "Hands-on workshops on Arduino, sensors and motor control. Teams build robots for "
                        + "inter-college competitions every semester.",
                "Saturdays, 10:00 AM - 1:00 PM", "Innovation Lab", 30, true, arjun);
        Club music = club("Music Club", ClubCategory.CULTURAL, "Jam sessions, open mics and fest performances",
                "Vocalists, guitarists, drummers and keyboardists jam every week and perform at the annual fest "
                        + "and open-mic nights.",
                "Tuesdays & Thursdays, 5:00 - 7:00 PM", "Music Room, Auditorium", 40, true, meera);
        Club dance = club("Dance Crew", ClubCategory.CULTURAL, "Hip-hop, classical and everything between",
                "Choreography practice for college events and competitions. No prior training needed.",
                "Mondays & Fridays, 5:30 - 7:30 PM", "Seminar Hall 2", 35, true, meera);
        Club drama = club("Drama Society", ClubCategory.ARTS, "Street plays, stage plays and improv",
                "We write, direct and perform plays - from short street plays on social issues to full stage "
                        + "productions for the annual day.",
                "Thursdays, 4:00 - 6:00 PM", "Open Air Theatre", 25, true, meera);
        Club cricket = club("Cricket Club", ClubCategory.SPORTS, "Nets every evening, tournaments every term",
                "Practice sessions with the college coach and selection for inter-department and university "
                        + "tournaments.",
                "Mon - Fri, 6:00 - 7:30 AM", "Main Ground", 22, true, vikram);
        Club chess = club("Chess Club", ClubCategory.SPORTS, "Think three moves ahead",
                "Casual games, puzzle nights and rated tournaments. Registrations reopen next semester.",
                "Fridays, 3:00 - 5:00 PM", "Library Seminar Room", 20, false, vikram);
        club("Literary Circle", ClubCategory.LITERARY, "Debate, quiz, poetry and the college magazine",
                "Debates, quizzes, poetry slams and the editorial team of the college magazine.",
                "Wednesdays, 3:00 - 4:30 PM", "Central Library", 30, true, null);
        Club nss = club("NSS Volunteers", ClubCategory.SOCIAL_SERVICE, "Serve the community, one drive at a time",
                "Blood donation camps, village visits, cleanliness drives and teaching programmes with the "
                        + "National Service Scheme.",
                "Sundays, 8:00 AM - 12:00 PM", "NSS Office, Admin Block", 100, true, vikram);

        membership(sneha, coding, MembershipStatus.APPROVED, arjun, null, 30);
        membership(sneha, music, MembershipStatus.PENDING, null, null, 2);
        membership(kiran, coding, MembershipStatus.APPROVED, arjun, null, 25);
        membership(kiran, robotics, MembershipStatus.APPROVED, arjun, null, 20);
        membership(kiran, cricket, MembershipStatus.PENDING, null, null, 1);
        membership(aisha, drama, MembershipStatus.APPROVED, meera, null, 15);
        membership(aisha, coding, MembershipStatus.PENDING, null, null, 3);
        membership(aisha, chess, MembershipStatus.REJECTED, vikram,
                "The club is full for this semester - please apply again next term.", 40);

        Event hackathon = event(coding, arjun, "Hackathon Kickoff",
                "Form teams, meet mentors and hear the problem statements for the 24-hour hackathon.",
                at(3, 10), 3, "Main Auditorium", 50, false);
        event(coding, arjun, "DSA Practice Session", "Graphs and dynamic programming - bring your laptop.",
                at(7, 16), 2, "Lab 3, CSE Block", 30, true);
        Event openMic = event(music, meera, "Open Mic Night", "Sing, play or recite - everyone gets five minutes on stage.",
                at(5, 18), 3, "Amphitheatre", 80, false);
        event(cricket, vikram, "Inter-department Trials", "Selections for the inter-department cricket cup.",
                at(2, 7), 3, "Main Ground", 40, false);
        Event workshop = event(robotics, arjun, "Line Follower Workshop",
                "Build and race your own line-following robot. Kits provided.",
                at(10, 10), 4, "Innovation Lab", 20, true);
        event(nss, vikram, "Blood Donation Camp", "In partnership with the city blood bank. Volunteers needed.",
                at(12, 9), 5, "College Gymnasium", 150, false);
        event(coding, arjun, "Orientation Meetup", "Welcome session for new members.",
                at(-5, 16), 2, "Lab 3, CSE Block", 60, false);

        registrations.save(new EventRegistration(hackathon, sneha));
        registrations.save(new EventRegistration(openMic, sneha));
        registrations.save(new EventRegistration(hackathon, kiran));
        registrations.save(new EventRegistration(workshop, kiran));

        log.info("Demo data created. Logins: admin@college.edu / Admin@123, arjun.coord@college.edu / Coord@123, "
                + "student@college.edu / Student@123");
    }

    /** n days from today at the given hour, in the server's time zone. */
    private static Instant at(long daysFromToday, int hour) {
        return LocalDate.now().plusDays(daysFromToday).atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant();
    }

    private User user(String name, String email, String password, Role role, String dept, String roll) {
        return users.save(new User(name, email, encoder.encode(password), role, dept, roll));
    }

    private Club club(String name, ClubCategory category, String tagline, String description, String schedule,
                      String venue, int capacity, boolean open, User coordinator) {
        Club c = new Club();
        c.setName(name);
        c.setCategory(category);
        c.setTagline(tagline);
        c.setDescription(description);
        c.setMeetingSchedule(schedule);
        c.setVenue(venue);
        c.setCapacity(capacity);
        c.setRegistrationOpen(open);
        c.setCoordinator(coordinator);
        return clubs.save(c);
    }

    private void membership(User student, Club club, MembershipStatus status, User decidedBy, String remark,
                            long daysAgo) {
        Membership m = new Membership(student, club);
        Instant applied = now.minus(Duration.ofDays(daysAgo));
        m.apply("I'd love to be part of " + club.getName() + " and learn from the seniors here.", applied);
        if (status != MembershipStatus.PENDING) {
            m.decide(status, decidedBy, remark, applied.plus(Duration.ofDays(1)));
        }
        memberships.save(m);
    }

    private Event event(Club club, User by, String title, String description, Instant start, long hours, String venue,
                        int capacity, boolean membersOnly) {
        Event e = new Event();
        e.setClub(club);
        e.setTitle(title);
        e.setDescription(description);
        e.setStartsAt(start);
        e.setEndsAt(start.plus(Duration.ofHours(hours)));
        e.setVenue(venue);
        e.setCapacity(capacity);
        e.setMembersOnly(membersOnly);
        e.setCreatedBy(by);
        return events.save(e);
    }
}
