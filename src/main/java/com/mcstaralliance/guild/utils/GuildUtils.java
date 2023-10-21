package com.mcstaralliance.guild.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.mcstaralliance.guild.Guild;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/* loaded from: Guild-1.2.3-b200614.jar:me/asnxthaony/guild/utils/GuildUtils.class */
public class GuildUtils {
    public static final int MAX_MEMBER_LIMIT = 20;
    public static final int ITEM_PER_PAGE = 10;

    public static void setupDatabase() {
        try {
            Statement statement = Guild.getConnection().createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS guild (id int NOT NULL AUTO_INCREMENT,name varchar(32) NOT NULL,owner varchar(32) NOT NULL,PRIMARY KEY (id),UNIQUE KEY (name, owner))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS guild_player (id int NOT NULL AUTO_INCREMENT,userName varchar(32) NOT NULL,gid int NOT NULL DEFAULT '0',PRIMARY KEY (id),UNIQUE KEY (userName))");
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
    }

    public static void showGuildList(Player player, int currentPage) {
        int count = getGuildTotalCount();
        if (count == 0) {
            player.sendMessage("§a暂无帮派");
            return;
        }
        int maxPages = (int) Math.ceil(count / 10.0d);
        if (currentPage > maxPages) {
            currentPage = maxPages;
        } else if (currentPage <= 0) {
            currentPage = 1;
        }
        String sql = "SELECT * FROM guild LIMIT " + ((currentPage - 1) * 10) + "," + ((currentPage * 10) - 1);
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("§6Page: %d/%d\n", Integer.valueOf(currentPage), Integer.valueOf(maxPages)));
        sb.append("\n");
        sb.append("§4帮派 ID §8- §b帮派名 §8- §6帮主\n");
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                sb.append("§4");
                sb.append(rs.getInt("id"));
                sb.append(" §8- §b");
                sb.append(rs.getString("name"));
                sb.append(" §8- §6");
                sb.append(rs.getString("owner"));
                if (!rs.isLast()) {
                    sb.append("\n");
                }
            }
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        player.sendMessage(sb.toString());
    }

    public static void showGuildInfo(Player player) {
        int gid = getPlayerGuild(player.getName());
        if (gid == 0) {
            player.sendMessage(LanguageUtils.NOT_IN_GUILD);
            return;
        }
        String temp = "";
        List<String> members = getGuildMemberList(gid);
        for (String name : members) {
            String prefix = Bukkit.getPlayer(name) != null ? "§a" : "§c";
            temp = temp + prefix + name + " ";
        }
        String[] message = {"§6", String.format("§6§l%s", getGuildName(gid)), String.format("§7帮主: §r%s", getGuildOwner(gid)), String.format("§7帮派成员: §r%s", temp), "§6", String.format("§7人数: §r%d / %d", Integer.valueOf(getGuildMemberTotalCount(gid)), 20)};
        player.sendMessage(message);
    }

    public static void disband(Player player) {
        String name = player.getName();
        int gid = getPlayerGuild(name);
        if (gid == 0) {
            player.sendMessage(LanguageUtils.NOT_IN_GUILD);
        } else if (!isGuildOwner(name, gid)) {
            player.sendMessage("§c你不是该帮派的所有者。");
        } else {
            List<String> members = getGuildMemberList(gid);
            for (String member : members) {
                setPlayerGuild(member, 0);
            }
            Guild.log(String.format("玩家 %s 解散了帮派 %s", name, getGuildName(gid)));
            try {
                Statement statement = Guild.getConnection().createStatement();
                statement.executeUpdate("DELETE FROM guild WHERE id='" + gid + "'");
                statement.close();
            } catch (SQLException e) {
                Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
            }
            player.sendMessage("§a帮派已解散。");
        }
    }

    public static void leave(Player player) {
        String name = player.getName();
        int gid = getPlayerGuild(name);
        if (gid == 0) {
            player.sendMessage(LanguageUtils.NOT_IN_GUILD);
        } else if (isGuildOwner(name, gid)) {
            player.sendMessage("§c帮派所有者无法离开帮派。");
        } else {
            setPlayerGuild(name, 0);
            Guild.log(String.format("玩家 %s 离开了帮派 %s", name, getGuildName(gid)));
            player.sendMessage("§a你退出了帮派。");
        }
    }

    public static void create(Player player, String guildName) {
        String name = player.getName();
        if (guildName.length() > 4) {
            player.sendMessage("§c创建失败，帮派名不允许超过 4 个字。");
        } else if (StringUtils.isAllChinese(guildName)) {
            player.sendMessage("§c创建失败，帮派名只能由中文组成。");
        } else if (isGuildExisted(guildName)) {
            player.sendMessage("§c创建失败，该帮派名已经存在。");
        } else if (getPlayerGuild(name) != 0) {
            player.sendMessage("§c创建失败，你已经加入帮派。");
        } else {
            try {
                Statement statement = Guild.getConnection().createStatement();
                statement.executeUpdate("INSERT INTO guild (name, owner) VALUES ('" + guildName + "', '" + name + "')");
                statement.close();
            } catch (SQLException e) {
                Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
            }
            int gid = getGuildIdByName(guildName);
            setPlayerGuild(name, gid);
            Guild.log(String.format("玩家 %s 创建了帮派 %s", name, guildName));
            player.sendMessage(String.format("§a帮派 §b%s §a创建成功！", guildName));
        }
    }

    public static void invite(Player player, String targetName) {
        String name = player.getName();
        int gid = getPlayerGuild(name);
        if (gid == 0) {
            player.sendMessage(LanguageUtils.NOT_IN_GUILD);
        } else if (!isGuildOwner(name, gid)) {
            player.sendMessage("§c你不是该帮派的所有者。");
        } else if (getGuildMemberTotalCount(gid) >= 20) {
            player.sendMessage("§c本帮派人数已达上限！");
        } else if (name.equals(targetName)) {
            player.sendMessage("§c你不能邀请自己加入帮派。");
        } else {
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                player.sendMessage("§c被邀请人不在线！");
            } else if (getPlayerGuild(targetName) != 0) {
                player.sendMessage("§c被邀请人已加入帮派！");
            } else {
                Guild.invitations.put(targetName, Integer.valueOf(gid));
                target.sendMessage(String.format("§a你收到了来自帮派 §b%s §a的邀请，输入 §b/guild join %d §a同意邀请。", getGuildName(gid), Integer.valueOf(gid)));
                player.sendMessage("§a邀请已发出！");
            }
        }
    }

    public static void join(Player player, int gid) {
        String name = player.getName();
        if (getPlayerGuild(name) != 0) {
            player.sendMessage("§c你已经加入帮派了，如果想加入其它帮派，请先退出本帮派。");
        } else if (!Guild.invitations.containsKey(name)) {
            player.sendMessage("§c你没有收到任何邀请。");
        } else if (gid == Guild.invitations.get(name).intValue()) {
            Guild.invitations.remove(name);
            if (getGuildName(gid) == "") {
                player.sendMessage("§c邀请你的帮派已解散。");
            } else if (getGuildMemberTotalCount(gid) >= 20) {
                player.sendMessage("§c欲加入帮派人数已达上限！");
            } else {
                setPlayerGuild(name, gid);
                Guild.log(String.format("玩家 %s 加入了帮派 %s", name, getGuildName(gid)));
                player.sendMessage("§a加入成功！");
                Player target = Bukkit.getPlayer(getGuildOwner(gid));
                if (target != null) {
                    target.sendMessage(String.format("§b%s §a已同意加入你的帮派。", name));
                }
            }
        } else {
            player.sendMessage("§c你没有收到该帮派的邀请。");
        }
    }

    public static void kick(Player player, String targetName) {
        String name = player.getName();
        int gid = getPlayerGuild(name);
        if (gid == 0) {
            player.sendMessage(LanguageUtils.NOT_IN_GUILD);
        } else if (!isGuildOwner(name, gid)) {
            player.sendMessage("§c你不是该帮派的所有者。");
        } else if (name.equals(targetName)) {
            player.sendMessage("§c你不能踢出自己。");
        } else {
            int targetGid = getPlayerGuild(targetName);
            if (getPlayerGuild(name) != targetGid) {
                player.sendMessage(String.format("§b%s §c不是你的帮派成员。", targetName));
                return;
            }
            setPlayerGuild(targetName, 0);
            Guild.log(String.format("玩家 %s 将 %s 从帮派 %s 中踢出", name, targetName, getGuildName(gid)));
            player.sendMessage(String.format("§a已踢出帮派成员 §b%s", targetName));
        }
    }

    public static void addChat(Player player, String[] message) {
        String name = player.getName();
        int gid = getPlayerGuild(name);
        if (gid == 0) {
            player.sendMessage(LanguageUtils.NOT_IN_GUILD);
            return;
        }
        String msg = StringUtils.join(message);
        for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
            if (gid == getPlayerGuild(onlinePlayers.getName())) {
                onlinePlayers.sendMessage(String.format("§3[%s] §b%s", name, msg));
            }
        }
    }

    public static boolean isGuildExisted(String name) {
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM guild WHERE name='" + name + "' LIMIT 1");
            if (rs.next()) {
                statement.close();
                rs.close();
                return true;
            }
            return false;
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
            return false;
        }
    }

    public static String getGuildName(int gid) {
        String guildName = "";
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT name FROM guild WHERE id='" + gid + "' LIMIT 1");
            if (rs.next()) {
                guildName = rs.getString("name");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        return guildName;
    }

    public static String getGuildOwner(int gid) {
        String guildOwner = "";
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT owner FROM guild WHERE id='" + gid + "' LIMIT 1");
            if (rs.next()) {
                guildOwner = rs.getString("owner");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        return guildOwner;
    }

    public static int getPlayerGuild(String name) {
        int gid = 0;
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT gid FROM guild_player WHERE userName='" + name + "' LIMIT 1");
            if (rs.next()) {
                gid = rs.getInt("gid");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        return gid;
    }

    public static int getGuildTotalCount() {
        int count = 0;
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM guild");
            if (rs.next()) {
                count = rs.getInt("COUNT(*)");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        return count;
    }

    public static int getGuildMemberTotalCount(int gid) {
        int count = 0;
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) from guild_player WHERE gid='" + gid + "'");
            if (rs.next()) {
                count = rs.getInt("COUNT(*)");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        return count;
    }

    public static List<String> getGuildMemberList(int gid) {
        List<String> members = new ArrayList<>();
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM guild_player WHERE gid='" + gid + "'");
            while (rs.next()) {
                members.add(rs.getString("userName"));
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        return members;
    }

    public static int getGuildIdByName(String name) {
        int gid = 0;
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT id FROM guild WHERE name='" + name + "' LIMIT 1");
            if (rs.next()) {
                gid = rs.getInt("id");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
        return gid;
    }

    public static boolean isGuildOwner(String name, int gid) {
        if (name.equals(getGuildOwner(gid))) {
            return true;
        }
        return false;
    }

    public static void setPlayerGuild(String name, int gid) {
        try {
            Statement statement = Guild.getConnection().createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM guild_player WHERE userName='" + name + "' LIMIT 1");
            if (!rs.next()) {
                statement.executeUpdate("INSERT INTO guild_player (userName, gid) VALUES ('" + name + "', '" + gid + "')");
            } else {
                statement.executeUpdate("UPDATE guild_player SET gid='" + gid + "' WHERE userName='" + name + "'");
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            Guild.log(LanguageUtils.MYSQL_ERROR + e.getMessage());
        }
    }
}