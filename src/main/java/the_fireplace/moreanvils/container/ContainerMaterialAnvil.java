package the_fireplace.moreanvils.container;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;
import the_fireplace.moreanvils.blocks.MaterialAnvil;

import javax.annotation.Nonnull;
import java.util.Map;

public class ContainerMaterialAnvil extends Container {
    /**
     * Here comes out item you merged and/or renamed.
     */
    private IInventory outputSlot;
    /**
     * The 2slots where you put your items in that you want to merge and/or rename.
     */
    private IInventory inputSlots;
    private World theWorld;
    private BlockPos selfPosition;
    /**
     * The maximum cost of repairing/renaming in the anvil.
     */
    public int maximumCost;
    /**
     * The cap for the maximum cost, varies by material.
     */
    public int maximumCap;
    public ItemArmor.ArmorMaterial material;
    /**
     * determined by damage of input item and stackSize of repair materials
     */
    public int materialCost;
    public String repairedItemName;
    /**
     * The player that has this container open.
     */
    private final EntityPlayer thePlayer;

    private String name = "";

    @SideOnly(Side.CLIENT)
    public ContainerMaterialAnvil(InventoryPlayer playerInventory, World worldIn, EntityPlayer player, MaterialAnvil block)
    {
        this(playerInventory, worldIn, BlockPos.ORIGIN, player, block.getArmorMaterial());
        this.name=block.getName();
    }

    public ContainerMaterialAnvil(InventoryPlayer playerInventory, final World worldIn, final BlockPos blockPosIn, EntityPlayer player, ItemArmor.ArmorMaterial mat) {
        this.maximumCap = 40 / ItemArmor.ArmorMaterial.IRON.getEnchantability() * mat.getEnchantability();
        this.material = mat;
        this.outputSlot = new InventoryCraftResult();
        this.inputSlots = new InventoryBasic("Repair", true, 2) {
            @Override
            public void markDirty() {
                super.markDirty();
                ContainerMaterialAnvil.this.onCraftMatrixChanged(this);
            }
        };
        this.selfPosition = blockPosIn;
        this.theWorld = worldIn;
        this.thePlayer = player;
        this.addSlotToContainer(new Slot(this.inputSlots, 0, 27, 47));
        this.addSlotToContainer(new Slot(this.inputSlots, 1, 76, 47));
        this.addSlotToContainer(new Slot(this.outputSlot, 2, 134, 47) {
            /**
             * Check if the stack is a valid item for this slot. Always true beside for the armor slots.
             */
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            /**
             * Return whether this slot's stack can be taken from this slot.
             */
            @Override
            public boolean canTakeStack(EntityPlayer playerIn) {
                return (playerIn.capabilities.isCreativeMode || playerIn.experienceLevel >= ContainerMaterialAnvil.this.maximumCost) && ContainerMaterialAnvil.this.maximumCost > 0 && this.getHasStack();
            }

            @Override
            @Nonnull
            public ItemStack onTake(EntityPlayer playerIn, @Nonnull ItemStack stack) {
                if (!playerIn.capabilities.isCreativeMode) {
                    playerIn.addExperienceLevel(-ContainerMaterialAnvil.this.maximumCost);
                }

                float breakChance = getBreakChance(playerIn, stack, ContainerMaterialAnvil.this.material);

                ContainerMaterialAnvil.this.inputSlots.setInventorySlotContents(0, ItemStack.EMPTY);

                if (ContainerMaterialAnvil.this.materialCost > 0) {
                    ItemStack itemstack = ContainerMaterialAnvil.this.inputSlots.getStackInSlot(1);

                    if (!itemstack.isEmpty() && itemstack.getCount() > ContainerMaterialAnvil.this.materialCost) {
                        itemstack.shrink(ContainerMaterialAnvil.this.materialCost);
                        ContainerMaterialAnvil.this.inputSlots.setInventorySlotContents(1, itemstack);
                    } else {
                        ContainerMaterialAnvil.this.inputSlots.setInventorySlotContents(1, ItemStack.EMPTY);
                    }
                } else {
                    ContainerMaterialAnvil.this.inputSlots.setInventorySlotContents(1, ItemStack.EMPTY);
                }

                ContainerMaterialAnvil.this.maximumCost = 0;
                IBlockState iblockstate = worldIn.getBlockState(blockPosIn);

                if (!playerIn.capabilities.isCreativeMode && !worldIn.isRemote && iblockstate.getBlock() instanceof MaterialAnvil && playerIn.getRNG().nextFloat() < breakChance) {
                    int l = iblockstate.getValue(MaterialAnvil.DAMAGE);
                    ++l;

                    if (l > 2) {
                        worldIn.setBlockToAir(blockPosIn);
                        worldIn.playEvent(1029, blockPosIn, 0);
                    } else {
                        worldIn.setBlockState(blockPosIn, iblockstate.withProperty(MaterialAnvil.DAMAGE, l), 2);
                        worldIn.playEvent(1030, blockPosIn, 0);
                    }
                } else if (!worldIn.isRemote) {
                    worldIn.playEvent(1030, blockPosIn, 0);
                }
                return stack;
            }

            public float getBreakChance(EntityPlayer playerIn, ItemStack stack, ItemArmor.ArmorMaterial mat){
                return ForgeHooks.onAnvilRepair(playerIn, stack, ContainerMaterialAnvil.this.inputSlots.getStackInSlot(0), ContainerMaterialAnvil.this.inputSlots.getStackInSlot(1)) / ItemArmor.ArmorMaterial.IRON.getDurability(EntityEquipmentSlot.CHEST) * mat.getDurability(EntityEquipmentSlot.CHEST);
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    public String getName(){
        return name;
    }

    /**
     * Callback for when the crafting matrix is changed.
     */
    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        super.onCraftMatrixChanged(inventoryIn);

        if (inventoryIn == this.inputSlots) {
            this.updateRepairOutput();
        }
    }

    /**
     * called when the Anvil Input Slot changes, calculates the new result and puts it in the output slot
     */
    public void updateRepairOutput() {
        ItemStack itemstack = this.inputSlots.getStackInSlot(0);
        this.maximumCost = 1;
        int i = 0;
        int j = 0;
        int k = 0;

        if (itemstack.isEmpty()) {
            this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
            this.maximumCost = 0;
        } else {
            ItemStack itemstack1 = itemstack.copy();
            ItemStack itemstack2 = this.inputSlots.getStackInSlot(1);
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
            j = j + itemstack.getRepairCost() + (itemstack2.isEmpty() ? 0 : itemstack2.getRepairCost());
            this.materialCost = 0;
            boolean flag = false;

            if (!itemstack2.isEmpty()) {
                if (!onAnvilChange(this, itemstack, itemstack2, outputSlot, repairedItemName, j)) return;
                flag = itemstack2.getItem() == Items.ENCHANTED_BOOK && !Items.ENCHANTED_BOOK.getEnchantments(itemstack2).hasNoTags();

                if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(itemstack, itemstack2)) {
                    int j2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);

                    if (j2 <= 0) {
                        this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                        this.maximumCost = 0;
                        return;
                    }

                    int k2;

                    for (k2 = 0; j2 > 0 && k2 < itemstack2.getCount(); ++k2) {
                        int l2 = itemstack1.getItemDamage() - j2;
                        itemstack1.setItemDamage(l2);
                        ++i;
                        j2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
                    }

                    this.materialCost = k2;
                } else {
                    if (!flag && (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isItemStackDamageable())) {
                        this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                        this.maximumCost = 0;
                        return;
                    }

                    if (itemstack1.isItemStackDamageable() && !flag) {
                        int l = itemstack.getMaxDamage() - itemstack.getItemDamage();
                        int i1 = itemstack2.getMaxDamage() - itemstack2.getItemDamage();
                        int j1 = i1 + itemstack1.getMaxDamage() * 12 / 100;
                        int k1 = l + j1;
                        int l1 = itemstack1.getMaxDamage() - k1;

                        if (l1 < 0) {
                            l1 = 0;
                        }

                        if (l1 < itemstack1.getMetadata()) {
                            itemstack1.setItemDamage(l1);
                            i += 2;
                        }
                    }

                    Map<Enchantment, Integer> map1 = EnchantmentHelper.getEnchantments(itemstack2);

                    for (Enchantment enchantment1 : map1.keySet()) {
                        if (enchantment1 != null) {
                            int i3 = map.getOrDefault(enchantment1, 0);
                            int j3 = map1.get(enchantment1);
                            j3 = i3 == j3 ? j3 + 1 : Math.max(j3, i3);
                            boolean flag1 = enchantment1.canApply(itemstack);

                            if (this.thePlayer.capabilities.isCreativeMode || itemstack.getItem() == Items.ENCHANTED_BOOK) {
                                flag1 = true;
                            }

                            for (Enchantment enchantment : map.keySet()) {
                                if (enchantment != null && enchantment != enchantment1 && !enchantment.func_191560_c(enchantment1)) {//func_191560_c checks if ench can apply with ench1 and vice versa
                                    flag1 = false;
                                    ++i;
                                }
                            }

                            if (flag1) {
                                if (j3 > enchantment1.getMaxLevel()) {
                                    j3 = enchantment1.getMaxLevel();
                                }

                                map.put(enchantment1, j3);
                                int k3 = 0;

                                switch (enchantment1.getRarity()) {
                                    case COMMON:
                                        k3 = 1;
                                        break;
                                    case UNCOMMON:
                                        k3 = 2;
                                        break;
                                    case RARE:
                                        k3 = 4;
                                        break;
                                    case VERY_RARE:
                                        k3 = 8;
                                }

                                if (flag) {
                                    k3 = Math.max(1, k3 / 2);
                                }

                                i += k3 * j3;
                            }
                        }
                    }
                }
            }

            if (flag && !itemstack1.getItem().isBookEnchantable(itemstack1, itemstack2)) itemstack1 = null;

            if (StringUtils.isBlank(this.repairedItemName)) {
                if (itemstack.hasDisplayName()) {
                    k = 1;
                    i += k;
                    itemstack1.clearCustomName();
                }
            } else if (!this.repairedItemName.equals(itemstack.getDisplayName())) {
                k = 1;
                i += k;
                itemstack1.setStackDisplayName(this.repairedItemName);
            }

            this.maximumCost = j + i;

            if (i <= 0) {
                itemstack1 = ItemStack.EMPTY;
            }

            if (k == i && k > 0 && this.maximumCost >= this.maximumCap) {
                this.maximumCost = this.maximumCap - 1;
            }

            if (this.maximumCost >= this.maximumCap && !this.thePlayer.capabilities.isCreativeMode) {
                itemstack1 = ItemStack.EMPTY;
            }

            if (!itemstack1.isEmpty()) {
                int i2 = itemstack1.getRepairCost();

                if (!itemstack2.isEmpty() && i2 < itemstack2.getRepairCost()) {
                    i2 = itemstack2.getRepairCost();
                }

                if (k != i) {
                    i2 = i2 * 2 + 1;
                }

                itemstack1.setRepairCost(i2);
                EnchantmentHelper.setEnchantments(map, itemstack1);
            }

            this.outputSlot.setInventorySlotContents(0, itemstack1);
            this.detectAndSendChanges();
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, 0, this.maximumCost);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            this.maximumCost = data;
        }
    }

    /**
     * Called when the container is closed.
     */
    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);

        if (!this.theWorld.isRemote) {
            for (int i = 0; i < this.inputSlots.getSizeInventory(); ++i) {
                ItemStack itemstack = this.inputSlots.removeStackFromSlot(i);

                if (!itemstack.isEmpty()) {
                    playerIn.dropItem(itemstack, false);
                }
            }
        }
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer playerIn) {
        return this.theWorld.getBlockState(this.selfPosition).getBlock() instanceof MaterialAnvil && playerIn.getDistanceSq((double) this.selfPosition.getX() + 0.5D, (double) this.selfPosition.getY() + 0.5D, (double) this.selfPosition.getZ() + 0.5D) <= 64.0D;
    }

    /**
     * Take a stack from the specified inventory slot.
     */
    @Override
    @Nonnull
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index == 2) {
                if (!this.mergeItemStack(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onSlotChange(itemstack1, itemstack);
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 39 && !this.mergeItemStack(itemstack1, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    /**
     * used by the Anvil GUI to update the Item Name being typed by the player
     */
    public void updateItemName(String newName) {
        this.repairedItemName = newName;

        if (this.getSlot(2).getHasStack()) {
            ItemStack itemstack = this.getSlot(2).getStack();

            if (StringUtils.isBlank(newName)) {
                itemstack.clearCustomName();
            } else {
                itemstack.setStackDisplayName(this.repairedItemName);
            }
        }

        this.updateRepairOutput();
    }

    public static boolean onAnvilChange(ContainerMaterialAnvil container, ItemStack left, ItemStack right, IInventory outputSlot, String name, int baseCost) {
        AnvilUpdateEvent e = new AnvilUpdateEvent(left, right, name, baseCost);
        if (MinecraftForge.EVENT_BUS.post(e)) return false;
        if (e.getOutput().isEmpty()) return true;

        outputSlot.setInventorySlotContents(0, e.getOutput());
        container.maximumCost = e.getCost();
        container.materialCost = e.getMaterialCost();
        return false;
    }
}