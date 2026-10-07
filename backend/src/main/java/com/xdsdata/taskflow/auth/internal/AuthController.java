package com.xdsdata.taskflow.auth.internal;

import java.time.Duration;

import com.xdsdata.taskflow.auth.internal.AuthService.Session;
import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.error.GlobalExceptionHandler;
import com.xdsdata.taskflow.common.error.UnauthorizedException;
import com.xdsdata.taskflow.users.ProfileDto;
import com.xdsdata.taskflow.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class AuthController {

	static final String REFRESH_COOKIE = "tf_refresh";

	private static final String COOKIE_PATH = "/api/auth";

	private final AuthService auth;

	private final PasswordResetService passwordReset;

	private final UserService users;

	private final OriginGuard originGuard;

	private final AppProperties properties;

	AuthController(AuthService auth, PasswordResetService passwordReset, UserService users, OriginGuard originGuard,
			AppProperties properties) {
		this.auth = auth;
		this.passwordReset = passwordReset;
		this.users = users;
		this.originGuard = originGuard;
		this.properties = properties;
	}

	@PostMapping("/auth/register")
	ResponseEntity<ProfileDto> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
		originGuard.check(http);
		ProfileDto profile = auth.register(request.name(), request.email(), request.password(), request.confirmPassword());
		return ResponseEntity.status(HttpStatus.CREATED).body(profile);
	}

	@PostMapping("/auth/login")
	ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
		originGuard.check(http);
		return sessionResponse(auth.login(request.email(), request.password(), http.getRemoteAddr()));
	}

	@PostMapping("/auth/refresh")
	ResponseEntity<?> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletRequest http) {
		originGuard.check(http);
		try {
			return sessionResponse(auth.refresh(refreshToken));
		}
		catch (UnauthorizedException ex) {
			// Drop the stale cookie so the browser stops presenting it.
			ProblemDetail problem = GlobalExceptionHandler
				.problem(ex.status(), ex.code(), ex.getMessage(), ex.fieldErrors())
				.getBody();
			return ResponseEntity.status(ex.status())
				.header(HttpHeaders.SET_COOKIE, clearedCookie().toString())
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
		}
	}

	@PostMapping("/auth/logout")
	ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletRequest http) {
		originGuard.check(http);
		auth.logout(refreshToken);
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearedCookie().toString()).build();
	}

	@PostMapping("/auth/forgot-password")
	ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
			HttpServletRequest http) {
		originGuard.check(http);
		passwordReset.requestReset(request.email(), http.getRemoteAddr());
		return ResponseEntity.accepted()
			.body(new MessageResponse("If an account exists for this email, we've sent a link to reset the password."));
	}

	@PostMapping("/auth/reset-password")
	ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
		originGuard.check(http);
		passwordReset.resetPassword(request.token(), request.password(), request.confirmPassword(), http.getRemoteAddr());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/auth/me")
	ProfileDto me(@AuthenticationPrincipal AuthUser user) {
		return users.getProfile(user.id());
	}

	@PostMapping("/users/me/password")
	ResponseEntity<SessionResponse> changePassword(@AuthenticationPrincipal AuthUser user,
			@Valid @RequestBody ChangePasswordRequest request) {
		return sessionResponse(
				auth.changePassword(user, request.currentPassword(), request.newPassword(), request.confirmPassword()));
	}

	private ResponseEntity<SessionResponse> sessionResponse(Session session) {
		ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
		if (session.refreshToken() != null) {
			response.header(HttpHeaders.SET_COOKIE,
					refreshCookie(session.refreshToken().value(), session.refreshToken().maxAge()).toString());
		}
		return response.body(new SessionResponse(session.accessToken(), session.expiresIn(), session.user()));
	}

	private ResponseCookie refreshCookie(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite("Strict")
			.path(COOKIE_PATH)
			.maxAge(maxAge)
			.build();
	}

	private ResponseCookie clearedCookie() {
		return refreshCookie("", Duration.ZERO);
	}

	record RegisterRequest(
			@NotBlank(message = "Full name is required.") @Size(max = 100, message = "Full name must be at most 100 characters.") String name,
			@NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 254, message = "Email is too long.") String email,
			@NotBlank(message = "Password is required.") String password,
			@NotBlank(message = "Please confirm your password.") String confirmPassword) {
	}

	record LoginRequest(@NotBlank(message = "Email is required.") @Size(max = 254) String email,
			@NotBlank(message = "Password is required.") @Size(max = 200) String password) {
	}

	record ForgotPasswordRequest(
			@NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 254) String email) {
	}

	record ResetPasswordRequest(@NotBlank(message = "The reset link is invalid.") @Size(max = 200) String token,
			@NotBlank(message = "Password is required.") String password,
			@NotBlank(message = "Please confirm your password.") String confirmPassword) {
	}

	record ChangePasswordRequest(@NotBlank(message = "Current password is required.") String currentPassword,
			@NotBlank(message = "New password is required.") String newPassword,
			@NotBlank(message = "Please confirm your new password.") String confirmPassword) {
	}

	record SessionResponse(String accessToken, long expiresIn, ProfileDto user) {
	}

	record MessageResponse(String message) {
	}

}
