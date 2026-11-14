package xyz.bluspring.kilt.compat.fabric.architectury

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.EntityEvent
import net.fabricmc.api.ModInitializer
import net.minecraft.world.InteractionResult
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.eventbus.api.Event
import xyz.bluspring.kilt.helpers.LivingDeathBridge

object KiltArchitecturyApiCompat {
    fun initCommon() {
        EntityEvent.ANIMAL_TAME.register { animal, player ->
            if (ForgeEventFactory.onAnimalTame(animal, player))
                EventResult.interruptDefault()
            else
                EventResult.pass()
        }

        // Bridge: when a Forge mod fires LivingDeathEvent without going through LivingEntity.die()
        // (e.g. Cataclysm bosses override die() without super.die()), forward to Architectury
        // so Fabric mods listening to EntityEvent.LIVING_DEATH (like FTB Quests) still get notified.
        MinecraftForge.EVENT_BUS.addListener { event: LivingDeathEvent ->
            try {
                if (!LivingDeathBridge.DIE_HANDLED.get()) {
                    EntityEvent.LIVING_DEATH.invoker().die(event.entity, event.source)
                }
            } finally {
                LivingDeathBridge.DIE_HANDLED.set(false)
            }
        }
    }

    fun eventBusToArchitectury(result: Event.Result): EventResult {
        return when (result) {
            Event.Result.ALLOW -> EventResult.interruptTrue()
            Event.Result.DEFAULT -> EventResult.pass()
            Event.Result.DENY -> EventResult.interruptFalse()
            else -> EventResult.pass()
        }
    }

    fun vanillaToArchitectury(result: InteractionResult): EventResult {
        return when (result) {
            InteractionResult.PASS -> EventResult.pass()
            InteractionResult.FAIL -> EventResult.interruptFalse()
            InteractionResult.SUCCESS -> EventResult.interruptTrue()
            else -> EventResult.interruptDefault()
        }
    }
}