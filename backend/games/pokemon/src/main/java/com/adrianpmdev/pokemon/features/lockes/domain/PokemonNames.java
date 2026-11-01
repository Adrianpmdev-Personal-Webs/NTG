package com.adrianpmdev.pokemon.features.lockes.domain;

/**
 * Writes a Pokémon name the same way however a player typed it: without extra spaces and with a capital at the start
 * of each word, including the parts of a hyphenated name.
 *
 * @author adrianpmdev
 */
public final class PokemonNames {

    private PokemonNames() {
    }

    /**
     * Normalized form of a name.
     *
     * @param name name as typed
     * @return the name to store, for example {@code Ho-Oh} for {@code "  ho-oh "}
     */
    public static String normalize(String name) {
        String clean = name.trim().replaceAll("\\s+", " ");
        StringBuilder normalized = new StringBuilder(clean.length());
        boolean startOfWord = true;

        for (int character : clean.codePoints().toArray()) {
            normalized.appendCodePoint(startOfWord ? Character.toUpperCase(character) : Character.toLowerCase(character));
            startOfWord = character == ' ' || character == '-';
        }

        return normalized.toString();
    }
}
