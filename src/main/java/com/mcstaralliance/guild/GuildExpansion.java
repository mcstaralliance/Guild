package com.mcstaralliance.guild;

import com.mcstaralliance.guild.utils.GuildUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/* loaded from: Guild-1.2.3-b200614.jar:me/asnxthaony/guild/GuildExpansion.class */
public class GuildExpansion extends PlaceholderExpansion {
    public String getAuthor() {
        return "liuhanwen";
    }

    public String getIdentifier() {
        return "yxguild";
    }

    public String getVersion() {
        return Guild.plugin.getDescription().getVersion();
    }

    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) {
            return "";
        }
        if (identifier.equals("name")) {
            int gid = GuildUtils.getPlayerGuild(player.getName());
            if (gid != 0) {
                return GuildUtils.getGuildName(gid);
            }
            return "无";
        } else if (identifier.equals("owner")) {
            int gid2 = GuildUtils.getPlayerGuild(player.getName());
            if (gid2 != 0) {
                return GuildUtils.getGuildOwner(gid2);
            }
            return "无";
        } else {
            return null;
        }
    }
}
