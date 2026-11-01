package com.adrianpmdev.platform.status;

/**
 * Implemented by a module with a state worth showing in the admin dashboard. Core collects every provider, so it never
 * names a game. It is asked each time an admin opens the dashboard, so it must answer from memory.
 *
 * @author adrianpmdev
 */
public interface ModuleStatusProvider {

    /**
     * The current state of the module.
     *
     * @return block for the dashboard
     */
    ModuleStatus moduleStatus();
}
