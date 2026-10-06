package com.clubportal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** End-to-end API tests against an in-memory H2 database filled by DataSeeder. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClubPortalApiTest {

    private static final String MOTIVATION = "{\"motivation\":\"I really want to learn and contribute here.\"}";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // ---- helpers ---------------------------------------------------------------------------

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private String newStudent(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Student\",\"email\":\"" + email + "\",\"password\":\"Hello1234\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private long clubId(String token, String name) throws Exception {
        String body = mvc.perform(get("/api/clubs").header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode c : json.readTree(body)) {
            if (c.get("name").asText().equals(name)) {
                return c.get("id").asLong();
            }
        }
        throw new IllegalStateException("club not found: " + name);
    }

    private ResultActions apply(String token, long clubId) throws Exception {
        return mvc.perform(post("/api/clubs/" + clubId + "/apply").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(MOTIVATION));
    }

    private long applyOk(String token, long clubId) throws Exception {
        String body = apply(token, clubId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private ResultActions decide(String token, long membershipId, String status, String remark) throws Exception {
        String remarkJson = remark == null ? "null" : "\"" + remark + "\"";
        return mvc.perform(patch("/api/memberships/" + membershipId + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\",\"remark\":" + remarkJson + "}"));
    }

    private long eventId(String token, long clubId, String title) throws Exception {
        String body = mvc.perform(get("/api/events?clubId=" + clubId).header("Authorization", token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode e : json.readTree(body)) {
            if (e.get("title").asText().equals(title)) {
                return e.get("id").asLong();
            }
        }
        throw new IllegalStateException("event not found: " + title);
    }

    // ---- tests ----------------------------------------------------------------------------

    @Test
    void healthIsPublicButEverythingElseNeedsLogin() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/clubs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mvc.perform(get("/api/clubs").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() throws Exception {
        for (String email : new String[]{"student@college.edu", "nobody@college.edu"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"Wrong@1234\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        }
    }

    @Test
    void registerValidatesInputAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasSize(3)));
        newStudent("fresh@college.edu");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Again\",\"email\":\"fresh@college.edu\",\"password\":\"Hello1234\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void applyApproveLeaveAndReapply() throws Exception {
        String student = newStudent("joiner@college.edu");
        String meera = login("meera.coord@college.edu", "Coord@123");
        String arjun = login("arjun.coord@college.edu", "Coord@123");
        long dance = clubId(student, "Dance Crew");

        long id = applyOk(student, dance);
        apply(student, dance).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ALREADY_APPLIED"));

        decide(arjun, id, "APPROVED", null).andExpect(status().isForbidden());   // not his club
        decide(meera, id, "APPROVED", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedByName").value("Meera Iyer"));
        decide(meera, id, "APPROVED", null).andExpect(status().isConflict());     // no longer pending

        mvc.perform(get("/api/clubs/" + dance).header("Authorization", student))
                .andExpect(jsonPath("$.myMembership.status").value("APPROVED"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.permissions.canApply").value(false));
        apply(student, dance).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ALREADY_MEMBER"));

        mvc.perform(post("/api/memberships/" + id + "/leave").header("Authorization", student))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LEFT"));
        assertEquals(id, applyOk(student, dance)); // re-applying reuses the same row
    }

    @Test
    void rejectingNeedsAReason() throws Exception {
        String student = newStudent("hopeful@college.edu");
        String meera = login("meera.coord@college.edu", "Coord@123");
        long id = applyOk(student, clubId(student, "Music Club"));

        decide(meera, id, "REJECTED", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REMARK_REQUIRED"));
        decide(meera, id, "REJECTED", "Auditions are over for this term").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.remark").value("Auditions are over for this term"));
    }

    @Test
    void clubRulesAreEnforced() throws Exception {
        // Kiran already has 3 active clubs (2 approved + 1 pending)
        String kiran = login("kiran@college.edu", "Student@123");
        apply(kiran, clubId(kiran, "Dance Crew")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CLUB_LIMIT_REACHED"));

        // Chess Club has registrations closed
        String student = newStudent("chessfan@college.edu");
        apply(student, clubId(student, "Chess Club")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REGISTRATION_CLOSED"));

        // coordinators and admins can't apply
        String admin = login("admin@college.edu", "Admin@123");
        apply(admin, clubId(admin, "Music Club")).andExpect(status().isForbidden());
    }

    @Test
    void approvalsStopAtCapacity() throws Exception {
        String admin = login("admin@college.edu", "Admin@123");
        String body = mvc.perform(post("/api/clubs").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tiny Club\",\"category\":\"ARTS\",\"description\":\"A club with exactly one seat available.\","
                                + "\"capacity\":1,\"registrationOpen\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long tiny = json.readTree(body).get("id").asLong();

        String s1 = newStudent("first@college.edu");
        String s2 = newStudent("second@college.edu");
        long a1 = applyOk(s1, tiny);
        long a2 = applyOk(s2, tiny);
        decide(admin, a1, "APPROVED", null).andExpect(status().isOk());
        decide(admin, a2, "APPROVED", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CLUB_FULL"));
        String s3 = newStudent("third@college.edu");
        apply(s3, tiny).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CLUB_FULL"));

        // capacity can't go below the current member count
        String tinyUpdate = "{\"name\":\"Tiny Club\",\"category\":\"ARTS\","
                + "\"description\":\"A club with exactly one seat available.\",\"capacity\":%d}";
        mvc.perform(put("/api/clubs/" + tiny).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(tinyUpdate.formatted(0)))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/clubs/" + tiny).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(tinyUpdate.formatted(2)))
                .andExpect(status().isOk());
        decide(admin, a2, "APPROVED", null).andExpect(status().isOk());
        mvc.perform(put("/api/clubs/" + tiny).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(tinyUpdate.formatted(1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CAPACITY_TOO_LOW"));
    }

    @Test
    void membersOnlyEventsAndSeatBooking() throws Exception {
        String sneha = login("student@college.edu", "Student@123");   // approved Coding Club member
        String aisha = login("aisha@college.edu", "Student@123");     // only a pending applicant
        long coding = clubId(sneha, "Coding Club");
        long dsa = eventId(sneha, coding, "DSA Practice Session");

        mvc.perform(post("/api/events/" + dsa + "/register").header("Authorization", aisha))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/events/" + dsa + "/register").header("Authorization", sneha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCount").value(1))
                .andExpect(jsonPath("$.registeredByMe").value(true));
        mvc.perform(post("/api/events/" + dsa + "/register").header("Authorization", sneha))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_REGISTERED"));
        mvc.perform(delete("/api/events/" + dsa + "/register").header("Authorization", sneha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredCount").value(0));
    }

    @Test
    void onlyManagersCreateEventsAndSeeAttendees() throws Exception {
        String sneha = login("student@college.edu", "Student@123");
        String meera = login("meera.coord@college.edu", "Coord@123");
        String arjun = login("arjun.coord@college.edu", "Coord@123");
        long coding = clubId(sneha, "Coding Club");
        String event = "{\"clubId\":" + coding + ",\"title\":\"Git Workshop\",\"startsAt\":\"2099-01-10T10:00:00Z\","
                + "\"endsAt\":\"2099-01-10T12:00:00Z\",\"venue\":\"Lab 3\",\"capacity\":25,\"membersOnly\":false}";

        mvc.perform(post("/api/events").header("Authorization", sneha).contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/events").header("Authorization", meera).contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/events").header("Authorization", arjun).contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canManage").value(true));

        long hackathon = eventId(sneha, coding, "Hackathon Kickoff");
        mvc.perform(get("/api/events/" + hackathon + "/attendees").header("Authorization", sneha))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/events/" + hackathon + "/attendees").header("Authorization", arjun))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void studentsCannotUseAdminEndpoints() throws Exception {
        String sneha = login("student@college.edu", "Student@123");
        mvc.perform(get("/api/users").header("Authorization", sneha))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(get("/api/memberships/pending").header("Authorization", sneha))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardsAreRoleSpecific() throws Exception {
        mvc.perform(get("/api/dashboard").header("Authorization", login("admin@college.edu", "Admin@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.pendingApplications", hasSize(3)));
        mvc.perform(get("/api/dashboard").header("Authorization", login("arjun.coord@college.edu", "Coord@123")))
                .andExpect(jsonPath("$.role").value("COORDINATOR"))
                .andExpect(jsonPath("$.clubs", hasSize(2)))
                .andExpect(jsonPath("$.pendingApplications", hasSize(1)));
        mvc.perform(get("/api/dashboard").header("Authorization", login("student@college.edu", "Student@123")))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.clubs", hasSize(1)))
                .andExpect(jsonPath("$.pendingApplications", hasSize(1)))
                .andExpect(jsonPath("$.upcomingEvents", hasSize(2)));
    }

    @Test
    void badInputGivesCleanErrors() throws Exception {
        String sneha = login("student@college.edu", "Student@123");
        mvc.perform(post("/api/clubs/1/apply").header("Authorization", sneha)
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
        mvc.perform(get("/api/clubs?category=BANANA").header("Authorization", sneha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
        mvc.perform(get("/api/clubs/999999").header("Authorization", sneha))
                .andExpect(status().isNotFound());
    }
}
