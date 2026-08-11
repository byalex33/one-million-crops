package com.onemillioncrops.service;

import com.onemillioncrops.model.CropDefinition;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HarvestActionBarServiceTest {
    @Test
    void combinesRapidHarvestsOfTheSameCrop() {
        CropDefinition wheat = crop("wheat", Material.WHEAT);
        HarvestActionBarService.HarvestBatch batch = new HarvestActionBarService.HarvestBatch();

        batch.add(wheat, 3);
        batch.add(wheat, 7);

        assertEquals(1, batch.entries().size());
        assertEquals(10, batch.entries().getFirst().amount());
    }

    @Test
    void retainsDifferentCropsInPickupOrder() {
        CropDefinition wheat = crop("wheat", Material.WHEAT);
        CropDefinition carrot = crop("carrot", Material.CARROT);
        HarvestActionBarService.HarvestBatch batch = new HarvestActionBarService.HarvestBatch();

        batch.add(wheat, 4);
        batch.add(carrot, 2);

        assertEquals("wheat", batch.entries().get(0).crop().id());
        assertEquals("carrot", batch.entries().get(1).crop().id());
    }

    private static CropDefinition crop(String id, Material material) {
        return new CropDefinition(id, material, Set.of(material), "<green>" + id + "</green>");
    }
}
