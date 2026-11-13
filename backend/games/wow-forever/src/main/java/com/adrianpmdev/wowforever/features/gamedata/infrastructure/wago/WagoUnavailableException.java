package com.adrianpmdev.wowforever.features.gamedata.infrastructure.wago;

import java.io.IOException;

/**
 * wago.tools could not be reached, answered a server error or cut an answer half way. It passes on its own, unlike a
 * table that lacks a column the export reads, so a read that fails with it is simply tried again later.
 *
 * @author adrianpmdev
 */
public class WagoUnavailableException extends IOException {

    /**
     * Builds the exception.
     *
     * @param message what failed
     * @param cause   the error underneath, or null
     */
    public WagoUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
