package com.adrianpmdev.platform.asset;

import java.util.List;

/**
 * Declares the public pictures a module needs. Core collects every provider to build the admin screen of pictures,
 * and only accepts uploads for declared keys.
 *
 * @author adrianpmdev
 */
public interface ImageSlotProvider {

    /**
     * Pictures the module needs right now (the list may depend on data, for example one per talent tree).
     *
     * @return image slots
     */
    List<ImageSlot> imageSlots();
}
