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
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	/** Locks the matching rows so concurrent role/status changes cannot remove the last IT Manager. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.role = :role and u.status = :status")
	List<User> lockByRoleAndStatus(Role role, UserStatus status);

}
