package com.adrianpmdev.core.web.health;


import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;

/**
 * Controller used to keep the service active.
 *
 * @author adrianpmdev
 */
@RestController
public class KeepAliveController {

    @GetMapping("/keep-alive")
    public ResponseEntity<String> keepAlive() {
        return ResponseEntity.ok("Service is alive");
    }
}