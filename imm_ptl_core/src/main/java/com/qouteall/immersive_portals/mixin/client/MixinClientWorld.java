package com.qouteall.immersive_portals.mixin.client;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Supplier;

@Mixin(value = ClientWorld.class)
public abstract class MixinClientWorld implements IEClientWorld {
    @Shadow
    @Final
    @Mutable
    private ClientPlayNetworkHandler netHandler;
    
    @Mutable
    @Shadow
    @Final
    private ClientChunkManager chunkManager;
    
    @Shadow
    public abstract Entity getEntityById(int id);
    
    private List<Portal> globalTrackedPortals;
    
    @Override
    public ClientPlayNetworkHandler getNetHandler() {
        return netHandler;
    }
    
    @Override
    public void setNetHandler(ClientPlayNetworkHandler handler) {
        netHandler = handler;
    }
    
    @Override
    public List<Portal> getGlobalPortals() {
        return globalTrackedPortals;
    }
    
    @Override
    public void setGlobalPortals(List<Portal> arg) {
        globalTrackedPortals = arg;
    }

//    @Redirect(
//        method = "<init>",
//        at = @At(
//            value = "NEW",
//            target = "net/minecraft/client/world/ClientChunkManager"
//        )
//    )
//    private ClientChunkManager replaceChunkManager(ClientWorld world, int loadDistance) {
//        return O_O.createMyClientChunkManager(world, loadDistance);
//    }
    
    //use my client chunk manager
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    void onConstructed(
        ClientPlayNetworkHandler clientPlayNetworkHandler, ClientWorld.Properties properties,
        RegistryKey<World> registryKey, DimensionType dimensionType, int i,
        Supplier<Profiler> supplier, WorldRenderer worldRenderer, boolean bl,
        long l, CallbackInfo ci
    ) {
        ClientWorld clientWorld = (ClientWorld) (Object) this;
        ClientChunkManager myClientChunkManager =
            O_O.createMyClientChunkManager(clientWorld, i);
        chunkManager = myClientChunkManager;
    }
    
    // avoid entity duplicate when an entity travels
    @Inject(
        method = "addEntityPrivate",
        at = @At("TAIL")
    )
    private void onOnEntityAdded(int entityId, Entity entityIn, CallbackInfo ci) {
        if (ClientWorldLoader.getIsInitialized()) {
            for (ClientWorld world : ClientWorldLoader.getClientWorlds()) {
                if (world != (Object) this) {
                    world.removeEntity(entityId);
                }
            }
        }
    }
    
    /**
     * If the player goes into a portal when the other side chunk is not yet loaded
     * freeze the player so the player won't drop
     * {@link ClientPlayerEntity#tick()}
     */
    @Inject(
        method = "Lnet/minecraft/client/world/ClientWorld;isChunkLoaded(II)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsChunkLoaded(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        WorldChunk chunk = chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk instanceof EmptyChunk) {
            cir.setReturnValue(false);
//            Helper.log("chunk not loaded");
//            new Throwable().printStackTrace();
        }
    }
    
    // for debug
    @Inject(method = "toString", at = @At("HEAD"), cancellable = true)
    private void onToString(CallbackInfoReturnable<String> cir) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        cir.setReturnValue("ClientWorld " + this_.getRegistryKey().getValue());
    }
}
