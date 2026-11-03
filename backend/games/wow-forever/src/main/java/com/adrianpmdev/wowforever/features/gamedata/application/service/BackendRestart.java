package com.adrianpmdev.wowforever.features.gamedata.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Closes the application in order and ends the process with an error code, which Render answers by starting it again.
 * It runs on a thread of its own because closing the application also closes the scheduler that asks for it.
 *
 * @author adrianpmdev
 */
@Component
@RequiredArgsConstructor
class BackendRestart {

    private static final int EXIT_CODE = 1;

    private final ApplicationContext context;

    /** Starts closing the application and returns at once. */
    void run() {
        new Thread(() -> System.exit(SpringApplication.exit(context, () -> EXIT_CODE)), "backend-restart").start();
    }
}
