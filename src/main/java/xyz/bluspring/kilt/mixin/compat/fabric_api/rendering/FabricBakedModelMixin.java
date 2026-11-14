package xyz.bluspring.kilt.mixin.compat.fabric_api.rendering;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.bluspring.kilt.helpers.FRAPIThreadedStorage;

import java.util.function.Supplier;

@Mixin(FabricBakedModel.class)
public interface FabricBakedModelMixin {
    @Inject(method = "emitBlockQuads", at = @At("HEAD"))
    private void kilt$storeVariables(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context, CallbackInfo ci) {
        if (!(blockView instanceof ClientLevel))
            return;

        FRAPIThreadedStorage.LEVEL.set(blockView);
        FRAPIThreadedStorage.POS.set(pos);
    }

    @Inject(method = "emitBlockQuads", at = @At("TAIL"))
    private void kilt$clearVariables(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context, CallbackInfo ci) {
        FRAPIThreadedStorage.LEVEL.remove();
        FRAPIThreadedStorage.POS.remove();
    }

    // Forge models may only provide their item geometry through render passes (e.g. Domum Ornamentum).
    // When such a model is rendered through FRAPI (for example wrapped by Continuity's emissive model),
    // the default implementation only uses the vanilla quads, so emit every render pass instead.
    @Inject(method = "emitItemQuads", at = @At("HEAD"), cancellable = true)
    private void kilt$emitForgeRenderPasses(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context, CallbackInfo ci) {
        if (!((Object) this instanceof BakedModel model))
            return;

        var renderPasses = model.getRenderPasses(stack, true);
        if (renderPasses.size() == 1 && renderPasses.get(0) == model)
            return;

        for (BakedModel renderPass : renderPasses) {
            if (renderPass == model)
                VanillaModelEncoder.emitItemQuads(model, null, randomSupplier, context);
            else
                ((FabricBakedModel) renderPass).emitItemQuads(stack, randomSupplier, context);
        }

        ci.cancel();
    }
}
