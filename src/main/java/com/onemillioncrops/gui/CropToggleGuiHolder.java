package com.onemillioncrops.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class CropToggleGuiHolder implements InventoryHolder {
    private final int page;
    private Inventory inventory;

    public CropToggleGuiHolder(int page) {
        this.page = page;
    }

    public int page() {
        return page;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Inventory has not been created yet");
        }
        return inventory;
    }
}
