package com.xdsdata.taskflow.common.config;

import java.time.Clock;
import java.time.ZoneId;

import com.xdsdata.taskflow.common.AppProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TimeConfig {

	/** Clock in the application time zone; "today" for deadlines is {@code LocalDate.now(clock)}. */
	@Bean
	Clock clock(AppProperties properties) {
		return Clock.system(ZoneId.of(properties.timezone()));
	}

}
