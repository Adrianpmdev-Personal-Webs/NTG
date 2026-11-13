package com.adrianpmdev.wowforever.features.gamedata.infrastructure.storage;

/**
 * A build waiting to be read on a clean start, as GameDataStore keeps its note.
 *
 * @param build     version of the build
 * @param exporter  fingerprint of the export that has to read it
 * @param deploy    the deploy that has to read it (the commit on Render, empty elsewhere)
 * @param attempts  reads of it that were started and never ended well
 * @param lastError why the last of them failed, or null when it stopped the backend (or none started yet)
 * @author adrianpmdev
 */
public record PendingBuild(String build, String exporter, String deploy, int attempts, String lastError) {
}
