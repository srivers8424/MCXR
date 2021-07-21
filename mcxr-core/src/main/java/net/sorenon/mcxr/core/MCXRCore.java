package net.sorenon.mcxr.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sorenon.mcxr.core.accessor.PlayerEntityAcc;
import net.sorenon.mcxr.core.mixin.ServerLoginNetworkHandlerAcc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MCXRCore implements ModInitializer {

    public static final Identifier S2C_CONFIG = new Identifier("mcxr", "config");

    public static final Identifier POSES = new Identifier("mcxr", "poses");
    public static MCXRCore INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger("MCXR Core");

    @Override
    public void onInitialize() {
        INSTANCE = this;

        ServerLoginNetworking.registerGlobalReceiver(S2C_CONFIG, (server, handler, understood, buf, synchronizer, responseSender) -> {
            if (understood) {
                boolean xr = buf.readBoolean();
                var profile = ((ServerLoginNetworkHandlerAcc)handler).getProfile();
                if (xr) {
                    LOGGER.info("Received XR login packet from " + profile);
                } else {
                    LOGGER.info("Received login packet from " + profile);
                }
            }
        });

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            LOGGER.debug("Sending login packet to " + handler.getConnectionInfo());
            var buf = PacketByteBufs.create();
            sender.sendPacket(S2C_CONFIG, buf);
        });

        ServerPlayNetworking.registerGlobalReceiver(POSES,
                (server, player, handler, buf, responseSender) -> {
                    Vector3f vec = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
                    Quaternionf quat = new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
                    server.execute(() -> {
                        Pose pose = new Pose();
                        pose.pos.set(vec);
                        pose.orientation.set(quat);
                        playerPose(player, pose);
                    });
                });
    }

    public void playerPose(PlayerEntity player, Pose pose) {
        PlayerEntityAcc acc = (PlayerEntityAcc) player;
        if (acc.getHeadPose() == null) {
            acc.markVR();
        }
        acc.getHeadPose().set(pose);

        if (player instanceof ClientPlayerEntity) {
            PacketByteBuf buf = PacketByteBufs.create();
            Vector3f pos = pose.pos;
            Quaternionf quat = pose.orientation;
            buf.writeFloat(pos.x).writeFloat(pos.y).writeFloat(pos.z);
            buf.writeFloat(quat.x).writeFloat(quat.y).writeFloat(quat.z).writeFloat(quat.w);

            ClientPlayNetworking.send(POSES, buf);
        }
    }
}