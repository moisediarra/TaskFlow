package com.xdsdata.taskflow.users.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserStatus;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByRoleAndStatus(Role role, UserStatus status);

	/**
	 * Locks one row with a full {@code FOR UPDATE}, which (unlike JPA's pessimistic write lock on PostgreSQL)
	 * also blocks new rows referencing the user, such as a project they create while being deleted.
	 */
	@Query(value = "select * from users where id = :id for update", nativeQuery = true)
	Optional<User> lockById(UUID id);

	/**
	 * Increments in the database, so two overlapping sign-outs both count; pending changes (a new password
	 * hash) are written first.
	 */
	@Modifying(flushAutomatically = true)
	@Query("update User u set u.sessionVersion = u.sessionVersion + 1 where u.id = :id")
	int incrementSessionVersion(UUID id);

	/** Locks the matching rows so concurrent role/status changes cannot remove the last IT Manager. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.role = :role and u.status = :status")
	List<User> lockByRoleAndStatus(Role role, UserStatus status);

}
