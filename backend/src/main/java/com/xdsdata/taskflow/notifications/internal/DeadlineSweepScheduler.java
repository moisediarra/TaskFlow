package com.xdsdata.taskflow.notifications.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the deadline sweep shortly after startup and then periodically (APP_DEADLINE_SWEEP_ENABLED). */
@Component
@ConditionalOnProperty(name = "app.notifications.sweep-enabled", havingValue = "true", matchIfMissing = true)
class DeadlineSweepScheduler {

	private static final Logger log = LoggerFactory.getLogger(DeadlineSweepScheduler.class);

	private final DeadlineNotifier deadlines;

	DeadlineSweepScheduler(DeadlineNotifier deadlines) {
		this.deadlines = deadlines;
	}

	@Scheduled(initialDelayString = "PT20S", fixedDelayString = "${app.notifications.deadline-sweep-interval}")
	void run() {
		try {
			int sent = deadlines.sweep();
			if (sent > 0) {
				log.info("Sent {} deadline reminder(s)", sent);
			}
		}
		catch (RuntimeException ex) {
			log.error("Deadline sweep failed", ex);
		}
	}

}
