package com.xdsdata.taskflow;

import org.springframework.boot.SpringApplication;

public class TestTaskFlowApplication {

	public static void main(String[] args) {
		SpringApplication.from(TaskFlowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
