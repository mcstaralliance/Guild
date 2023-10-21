package com.mcstaralliance.guild;

import com.mcstaralliance.guild.utils.GuildUtils;
import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.MySQL;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;






public class Guild
        extends JavaPlugin
        implements Listener
{
    public static Plugin plugin;
    public static Database mysql;
    public static Map<String, Integer> invitations = new HashMap<>();


    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        reloadConfig();




        mysql = new MySQL(Logger.getLogger("Minecraft"), "[Guild]", getConfig().getString("mysql.host"), getConfig().getInt("mysql.port"), getConfig().getString("mysql.dbname") + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false", getConfig().getString("mysql.dbuser"), getConfig().getString("mysql.dbpass"));
        mysql.open();

        if (!mysql.isOpen()) {
            log("§c无法连接至 MySQL 数据库，插件已自动停用");
            setEnabled(false);

            return;
        }
        GuildUtils.setupDatabase();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, Guild.this::mysqlCheck,  72000L, 72000L);

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            (new GuildExpansion()).register();
        }

        Objects.requireNonNull(getCommand("guild")).setTabCompleter(this);
    }

    public static Connection getConnection() {
        return mysql.getConnection();
    }

    public void mysqlCheck() {
        log("MySQL 续期...");

        try {
            Statement statement = mysql.getConnection().createStatement();
            statement.executeQuery("select database()");
            statement.close();
        } catch (SQLException e) {
            log("MySQL 续期失败，已刷新 MySQL Connection");
        }
    }


    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("guild")) {
            if (sender instanceof Player) {
                Player player = (Player)sender;
                switch (args.length) {
                    case 0:
                        sendHelp(sender);
                        return true;
                    case 1:
                        switch (args[0].toLowerCase()) {
                            case "help":
                                if (checkPermission(sender, "guild.player.help")) {
                                    return true;
                                }

                                sendHelp(sender);

                                return true;
                            case "list":
                                if (checkPermission(sender, "guild.player.list")) {
                                    return true;
                                }

                                GuildUtils.showGuildList(player, 1);

                                return true;
                            case "info":
                                if (checkPermission(sender, "guild.player.info")) {
                                    return true;
                                }

                                GuildUtils.showGuildInfo(player);

                                return true;
                            case "disband":
                                if (checkPermission(sender, "guild.player.disband")) {
                                    return true;
                                }
                                GuildUtils.disband(player);

                                return true;
                            case "leave":
                                if (checkPermission(sender, "guild.player.leave")) {
                                    return true;
                                }

                                GuildUtils.leave(player);

                                return true;
                        }
                        return false;

                    case 2:
                        switch (args[0].toLowerCase()) {
                            case "list":
                                if (checkPermission(sender, "guild.player.list")) {
                                    return true;
                                }

                                try {
                                    int page = Integer.parseInt(args[1]);
                                    GuildUtils.showGuildList(player, page);
                                } catch (NumberFormatException e) {
                                    player.sendMessage("§c请输入有效的页码");
                                }

                                return true;
                            case "create":
                                if (checkPermission(sender, "guild.player.create")) {
                                    return true;
                                }

                                GuildUtils.create(player, args[1]);

                                return true;
                            case "invite":
                                if (checkPermission(sender, "guild.player.invite")) {
                                    return true;
                                }

                                GuildUtils.invite(player, args[1]);

                                return true;
                            case "join":
                                if (checkPermission(sender, "guild.player.join")) {
                                    return true;
                                }

                                try {
                                    int gid = Integer.parseInt(args[1]);
                                    GuildUtils.join(player, gid);
                                } catch (NumberFormatException e) {
                                    player.sendMessage("§c请输入有效的帮派 ID");
                                }

                                return true;
                            case "kick":
                                if (checkPermission(sender, "guild.player.kick")) {
                                    return true;
                                }

                                GuildUtils.kick(player, args[1]);

                                return true;
                        }
                        return false;
                }

                sender.sendMessage("§cInvalid arguments.");
                return false;
            }


            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        if (command.getName().equalsIgnoreCase("g")) {
            if (sender instanceof Player) {
                if (checkPermission(sender, "guild.player.chat")) {
                    return true;
                }

                if (args.length == 0) {
                    return false;
                }

                GuildUtils.addChat((Player)sender, args);

                return true;
            }
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        String name = event.getPlayer().getName();
        int gid = GuildUtils.getPlayerGuild(name);

        if (gid != 0) {
            String guildName = GuildUtils.getGuildName(gid);
            event.setFormat(String.format("§7[§b%s§7]§r%s", guildName, event.getFormat()));
        }
    }

    public boolean checkPermission(CommandSender sender, String permission) {
        if (sender.isOp() || sender.hasPermission(permission)) {
            return false;
        }
        sender.sendMessage("§c你没有使用该命令的权限");
        return true;
    }


    public void sendHelp(CommandSender sender) {
        String[] message = { "§4/g §c[message] §8- §6帮派聊天", "§4/guild help §8- §6查看插件帮助", "§4/guild list §c[page] §8- §6查看帮派列表", "§4/guild info §8- §6查看帮派信息", "§4/guild create §c<name> §8- §6创建帮派", "§4/guild disband §8- §6解散帮派", "§4/guild invite §c<player> §8- §6邀请入帮", "§4/guild join §c<guild> §8- §6加入帮派", "§4/guild kick §c<player> §8- §6踢出帮派", "§4/guild leave §8- §6退出帮派" };





        sender.sendMessage(message);
    }


    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("guild")) {
            List<String> completionList = new ArrayList<>();

            if (args.length == 1) {
                StringUtil.copyPartialMatches(args[0],
                        Arrays.asList("help", "list", "info", "create", "invite", "join", "kick"), completionList);
            } else if (args.length == 2) {
                StringUtil.copyPartialMatches(args[1],
                        Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList()), completionList);
            }


            Collections.sort(completionList);

            return completionList;
        }

        return null;
    }

    public static void log(String msg) {
        Bukkit.getConsoleSender().sendMessage("§c[帮派日志]§f " + msg);
    }
}