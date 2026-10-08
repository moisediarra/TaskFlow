package com.xdsdata.taskflow.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class AuthIntegrationTests extends IntegrationTest {

	private static final Pattern RESET_TOKEN = Pattern.compile("reset-password\\?token=([A-Za-z0-9_-]+)");

	@Test
	void registrationCreatesProjectOwnerAndLoginStartsASession() throws Exception {
		String email = uniqueEmail("Jane Owner");
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("name", "Jane Owner", "email", email.toUpperCase(), "password", PASSWORD,
					"confirmPassword", PASSWORD))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.role").value("PROJECT_OWNER"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());

		MvcResult login = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(toJson(body("email", email, "password", PASSWORD))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andReturn();
		Cookie refresh = login.getResponse().getCookie(REFRESH_COOKIE);
		assertThat(refresh).isNotNull();
		assertThat(refresh.isHttpOnly()).isTrue();
		assertThat(refresh.getPath()).isEqualTo("/api/auth");
		assertThat(login.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");

		Session session = login(email, PASSWORD);
		mvc.perform(getAs(session, "/api/auth/me")).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void registrationValidatesFieldsAndRejectsDuplicates() throws Exception {
		Session existing = register("Taken Name");
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("name", "Someone", "email", existing.email(), "password", PASSWORD, "confirmPassword",
					PASSWORD))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
			.andExpect(jsonPath("$.fieldErrors.email").exists());

		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("name", "Mismatch", "email", uniqueEmail("mismatch"), "password", PASSWORD,
					"confirmPassword", "different-password"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.confirmPassword").value("Passwords do not match."));

		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("name", "", "email", "not-an-email", "password", "short", "confirmPassword", "short"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.name").exists())
			.andExpect(jsonPath("$.fieldErrors.email").exists());
	}

	@Test
	void invalidLoginGetsTheSameGenericAnswer() throws Exception {
		Session user = register("Login Tester");
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("email", user.email(), "password", "wrong-password"))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.detail").value("Invalid email or password."));
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("email", uniqueEmail("nobody"), "password", "whatever-password"))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void refreshRotatesTheCookieAndRevokedTokensStopWorking() throws Exception {
		Session session = register("Refresh Tester");

		MvcResult rotated = mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.user.email").value(session.email()))
			.andReturn();
		String next = rotated.getResponse().getCookie(REFRESH_COOKIE).getValue();
		assertThat(next).isNotEqualTo(session.refreshToken());

		// A second tab presenting the just-rotated token within the grace period still gets an access token.
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
			.andExpect(status().isOk())
			.andExpect(header().doesNotExist("Set-Cookie"));

		mvc.perform(post("/api/auth/logout").cookie(new Cookie(REFRESH_COOKIE, next))).andExpect(status().isNoContent());
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, next)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("SESSION_EXPIRED"))
			.andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, "not-a-real-token")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void foreignOriginsCannotUseTheCookieEndpoints() throws Exception {
		Session session = register("Origin Tester");
		mvc.perform(post("/api/auth/refresh").header("Origin", "https://evil.example")
			.cookie(new Cookie(REFRESH_COOKIE, session.refreshToken()))).andExpect(status().isForbidden());
	}

	@Test
	void passwordResetIsSingleUseAndEndsExistingSessions(CapturedOutput output) throws Exception {
		Session session = register("Reset Tester");
		mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("email", session.email())))).andExpect(status().isAccepted());
		String token = awaitResetToken(output, session.email());

		String newPassword = "Brand-new-pass1";
		mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("token", token, "password", newPassword, "confirmPassword", newPassword))))
			.andExpect(status().isNoContent());

		mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("token", token, "password", newPassword, "confirmPassword", newPassword))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, session.refreshToken())))
			.andExpect(status().isUnauthorized());
		login(session.email(), newPassword);
	}

	@Test
	void forgotPasswordDoesNotRevealWhetherAnAccountExists() throws Exception {
		mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("email", uniqueEmail("ghost")))))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("If an account exists")));
	}

	@Test
	void changingPasswordRequiresTheCurrentOneAndSignsOutOtherSessions() throws Exception {
		Session session = register("Password Changer");
		Session otherDevice = login(session.email(), PASSWORD);
		mvc.perform(postAs(session, "/api/users/me/password",
				body("currentPassword", "wrong", "newPassword", "Another-pass1", "confirmPassword", "Another-pass1")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
		mvc.perform(postAs(session, "/api/users/me/password",
				body("currentPassword", PASSWORD, "newPassword", "Another-pass1", "confirmPassword", "Another-pass1")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty());
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, otherDevice.refreshToken())))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void deactivatedAccountsAreBlockedImmediately() throws Exception {
		Session itManager = createUser("Admin Deactivator", Role.IT_MANAGER);
		Session member = register("Leaving Member");
		mvc.perform(getAs(member, "/api/auth/me")).andExpect(status().isOk());

		mvc.perform(patchAs(itManager, "/api/management/users/{id}/status",
				body("status", "DEACTIVATED", "currentPassword", PASSWORD), member.userId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("DEACTIVATED"));

		mvc.perform(getAs(member, "/api/auth/me")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("email", member.email(), "password", PASSWORD))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"));
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, member.refreshToken())))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void requestsWithoutTokenGetAProblemDocument() throws Exception {
		mvc.perform(get("/api/projects"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
			.andExpect(jsonPath("$.requestId").isNotEmpty());
	}

	/** The reset link is sent asynchronously and logged in the test profile. */
	private String awaitResetToken(CapturedOutput output, String email) throws InterruptedException {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
		while (Instant.now().isBefore(deadline)) {
			for (String line : output.getAll().split("\\R")) {
				if (line.contains(email)) {
					Matcher matcher = RESET_TOKEN.matcher(line);
					if (matcher.find()) {
						return matcher.group(1);
					}
				}
			}
			Thread.sleep(100);
		}
		throw new AssertionError("No password reset link was logged for " + email);
	}

}
