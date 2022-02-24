package ac.grim.bettergrimdebug;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.UncertaintyHandler;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.events.CompletePredictionEvent;
import ac.grim.grimac.utils.lists.EvictingList;
import ac.grim.grimac.utils.math.GrimMath;
import club.minnced.discord.webhook.WebhookClient;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class BetterGrimDebug extends JavaPlugin implements Listener {
    private static WebhookClient client;

    HashMap<UUID, List<VectorData>> predicted = new HashMap<>();
    HashMap<UUID, List<Vector>> actually = new HashMap<>();
    HashMap<UUID, List<Vector>> positions = new HashMap<>();
    HashMap<UUID, Long> lastDebugTime = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        client = WebhookClient.withUrl("YOUR WEBHOOK URL HERE");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        predicted.put(event.getPlayer().getUniqueId(), new EvictingList<>(20));
        actually.put(event.getPlayer().getUniqueId(), new EvictingList<>(20));
        positions.put(event.getPlayer().getUniqueId(), new EvictingList<>(20));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        predicted.remove(event.getPlayer().getUniqueId());
        actually.remove(event.getPlayer().getUniqueId());
        positions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFlagEvent(CompletePredictionEvent event) {
        if (!predicted.containsKey(event.getPlayer().playerUUID)) return;

        GrimPlayer player = event.getPlayer();

        predicted.get(player.playerUUID).add(player.predictedVelocity);
        actually.get(player.playerUUID).add(player.actualMovement);
        positions.get(player.playerUUID).add(new Vector(player.x, player.y, player.z));

        if (event.getOffset() > 0.0001) {
            if (false && lastDebugTime.containsKey(player.playerUUID) && System.currentTimeMillis() - lastDebugTime.get(player.playerUUID) < 30 * 1000) { // 30 s cooldown
                return;
            }

            lastDebugTime.put(player.playerUUID, System.currentTimeMillis());

            StringBuilder sb = new StringBuilder();
            sb.append("Player Name: ");
            sb.append(player.bukkitPlayer.getName());
            sb.append("\nPing: ");
            sb.append(player.getTransactionPing() * 0.000001);
            sb.append("ms\n\n");

            for (int i = 0; i < predicted.get(player.playerUUID).size(); i++) {
                VectorData predict = predicted.get(player.playerUUID).get(i);
                Vector actual = actually.get(player.playerUUID).get(i);
                Vector position = positions.get(player.playerUUID).get(i);

                sb.append("Predicted: ");
                sb.append(predict.vector.toString());
                sb.append("\nActually:  ");
                sb.append(actual.toString());
                sb.append("\nOffset:    ");
                sb.append(actual.clone().subtract(predict.vector));
                sb.append("\nPosition:  ");
                sb.append(position.toString());

                sb.append("\nkb: ");
                sb.append(predict.isKnockback());
                sb.append(" explosion: ");
                sb.append(predict.isExplosion());
                sb.append(" trident: ");
                sb.append(predict.isTrident());
                sb.append(" 0.03: ");
                sb.append(predict.isZeroPointZeroThree());
                sb.append(" swimhop: ");
                sb.append(predict.isSwimHop());
                sb.append(" jump: ");
                sb.append(predict.isJump());
                sb.append("\n\n");
            }

            UncertaintyHandler uncertaintyHandler = player.uncertaintyHandler;
            sb.append("XNeg: ");
            sb.append(uncertaintyHandler.xNegativeUncertainty);
            sb.append("\nXPos: ");
            sb.append(uncertaintyHandler.xPositiveUncertainty);
            sb.append("\nYNeg: ");
            sb.append(uncertaintyHandler.yNegativeUncertainty);
            sb.append("\nYPos: ");
            sb.append(uncertaintyHandler.yPositiveUncertainty);
            sb.append("\nZNeg: ");
            sb.append(uncertaintyHandler.zNegativeUncertainty);
            sb.append("\nZPos: ");
            sb.append(uncertaintyHandler.zPositiveUncertainty);
            sb.append("\nStuck: ");
            sb.append(uncertaintyHandler.stuckOnEdge);
            sb.append("\n\n0.03: ");
            sb.append(uncertaintyHandler.lastMovementWasZeroPointZeroThree);
            sb.append("\n0.03 reset:");
            sb.append(uncertaintyHandler.lastMovementWasUnknown003VectorReset);
            sb.append("\n0.03 vertical: ");
            sb.append(uncertaintyHandler.wasZeroPointThreeVertically);

            sb.append("\n\nIs gliding: ");
            sb.append(player.isGliding);
            sb.append("\nIs swimming: ");
            sb.append(player.isSwimming);
            sb.append("\nIs on ground: ");
            sb.append(player.onGround);
            sb.append("\nClient claims ground: ");
            sb.append(player.clientClaimsLastOnGround);
            sb.append("\nLast on ground: ");
            sb.append(player.lastOnGround);
            sb.append("\nWater: ");
            sb.append(player.wasTouchingWater);
            sb.append("\nLava: ");
            sb.append(player.wasTouchingLava);
            sb.append("\nVehicle: ");
            sb.append(player.inVehicle);

            sb.append("\n\n");
            sb.append(player.boundingBox);
            sb.append("\n");

            for (int j = GrimMath.floor(player.boundingBox.minY) - 2; j <= GrimMath.ceil(player.boundingBox.maxY) + 2; j++) {
                for (int i = GrimMath.floor(player.boundingBox.minX) - 2; i <= GrimMath.ceil(player.boundingBox.maxX) + 2; i++) {
                    for (int k = GrimMath.floor(player.boundingBox.minZ) - 2; k <= GrimMath.ceil(player.boundingBox.maxZ) + 2; k++) {
                        WrappedBlockState block = player.compensatedWorld.getWrappedBlockStateAt(i, j, k);
                        sb.append(i);
                        sb.append(",");
                        sb.append(j);
                        sb.append(",");
                        sb.append(k);
                        sb.append(" ");
                        sb.append(block);
                        sb.append("\n");
                    }
                }
            }

            client.send(sb.toString().getBytes(StandardCharsets.UTF_8), "flag.txt");
        }
    }
}
