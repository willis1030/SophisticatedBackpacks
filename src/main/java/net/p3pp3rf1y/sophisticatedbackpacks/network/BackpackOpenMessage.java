package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.IContextAwareContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class BackpackOpenMessage {
	private final int subBackpackSlotIndex;

	public BackpackOpenMessage() {
		this(-1);
	}

	public BackpackOpenMessage(int subBackpackSlotIndex) {
		this.subBackpackSlotIndex = subBackpackSlotIndex;
	}

	public static void encode(BackpackOpenMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeInt(msg.subBackpackSlotIndex);
	}

	public static BackpackOpenMessage decode(FriendlyByteBuf packetBuffer) {
		return new BackpackOpenMessage(packetBuffer.readInt());
	}

	static void onMessage(BackpackOpenMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer player, BackpackOpenMessage msg) {
		if (player == null) {
			return;
		}

		if (player.containerMenu instanceof BackpackContainer backpackContainer) {
			BackpackContext backpackContext = backpackContainer.getBackpackContext();
			if (msg.subBackpackSlotIndex == -1) {
				openBackpack(player, backpackContext.getParentBackpackContext());
			} else {
				openBackpack(player, backpackContext.getSubBackpackContext(msg.subBackpackSlotIndex));
			}
		} else if (player.containerMenu instanceof IContextAwareContainer contextAwareContainer) {
			openBackpack(player, contextAwareContainer.getBackpackContext());
		} else {
			findAndOpenFirstBackpack(player);
		}
	}

	private static void findAndOpenFirstBackpack(ServerPlayer player) {
		PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, slot) -> {
			BackpackContext.Item backpackContext = new BackpackContext.Item(inventoryName, slot);
			NetworkHooks.openGui(player, new SimpleMenuProvider((w, p, pl) -> new BackpackContainer(w, pl, backpackContext), backpack.getHoverName()),
					backpackContext::toBuffer);
			return true;
		});
	}

	private static void openBackpack(ServerPlayer player, BackpackContext backpackContext) {
		NetworkHooks.openGui(player, new SimpleMenuProvider((w, p, pl) -> new BackpackContainer(w, pl, backpackContext), backpackContext.getDisplayName(player)),
				backpackContext::toBuffer);
	}
}
