package com.xdsdata.taskflow.activity.internal;

import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityRepository extends JpaRepository<ActivityLog, UUID>, JpaSpecificationExecutor<ActivityLog> {

}
