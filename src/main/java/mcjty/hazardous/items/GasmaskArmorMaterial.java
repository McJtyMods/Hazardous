package mcjty.hazardous.items;

import mcjty.hazardous.Hazardous;
import mcjty.lib.items.GenericArmorMaterial;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.crafting.Ingredient;

public class GasmaskArmorMaterial implements GenericArmorMaterial {

    public static final GasmaskArmorMaterial INSTANCE = new GasmaskArmorMaterial();

    private static final int HELMET_DURABILITY = 800;

    private GasmaskArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? HELMET_DURABILITY : 0;
    }

    @Override
    public int getDefenseForType(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? 1 : 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 5;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return Hazardous.MODID + ":gasmask";
    }

    @Override
    public float getToughness() {
        return 0.0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0f;
    }
}
