package com.xdsdata.taskflow.users;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.xdsdata.taskflow.common.Emails;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.error.ConflictException;
import com.xdsdata.taskflow.common.error.NotFoundException;
import com.xdsdata.taskflow.users.internal.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the users module. Authorization of role and status changes is the caller's
 * responsibility (the management module).
 */
@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository users;

	UserService(UserRepository users) {
		this.users = users;
	}

	public Optional<User> findById(UUID id) {
		return users.findById(id);
	}

	public User getById(UUID id) {
		return users.findById(id).orElseThrow(() -> new NotFoundException("User"));
	}

	public Optional<User> findByEmail(String email) {
		return users.findByEmail(Emails.normalize(email));
	}

	public Optional<User> findActiveByEmail(String email) {
		return findByEmail(email).filter(User::isActive);
	}

	public List<User> findAllById(Collection<UUID> ids) {
		return users.findAllById(ids);
	}

	public ProfileDto getProfile(UUID id) {
		return ProfileDto.from(getById(id));
	}

	@Transactional
	public User create(String name, String email, String passwordHash, Role role, String jobTitle) {
		if (users.existsByEmail(Emails.normalize(email))) {
			throw new ConflictException("EMAIL_TAKEN", "An account with this email already exists.", "email");
		}
		return users.save(new User(name, email, passwordHash, role, jobTitle));
	}

	@Transactional
	public ProfileDto updateProfile(UUID userId, String name, String jobTitle) {
		User user = getById(userId);
		user.updateProfile(name, jobTitle);
		return ProfileDto.from(user);
	}

	@Transactional
	public void changePasswordHash(UUID userId, String passwordHash) {
		getById(userId).changePasswordHash(passwordHash);
	}

	@Transactional
	public User changeRole(UUID userId, Role role) {
		User user = getById(userId);
		user.changeRole(role);
		return user;
	}

	@Transactional
	public User changeStatus(UUID userId, UserStatus status) {
		User user = getById(userId);
		user.changeStatus(status);
		return user;
	}

	/** Locks the active IT Manager rows for the current transaction and returns them. */
	@Transactional
	public List<User> lockActiveItManagers() {
		return users.lockByRoleAndStatus(Role.IT_MANAGER, UserStatus.ACTIVE);
	}

	public boolean anyItManagerExists() {
		return users.existsByRoleAndStatus(Role.IT_MANAGER, UserStatus.ACTIVE);
	}

}
