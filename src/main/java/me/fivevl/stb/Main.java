package me.fivevl.stb;

import com.google.gson.Gson;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class Main {
    public static JDA jda;
    public static Config config;

    public static void main(String[] args) throws IOException, InterruptedException {
        File configFile = new File("config.json");
        if (!configFile.exists()) {
            System.out.println("Config file not found! Creating one...");
            InputStream is = Main.class.getClassLoader().getResourceAsStream("config.json");
            if (is == null) {
                System.out.println("Config file not found in resources!");
                System.exit(1);
                return;
            }
            Files.copy(is, configFile.toPath());
            is.close();
            System.out.println("Config file created! Please fill it out and restart the bot.");
            System.exit(0);
            return;
        }
        config = new Gson().fromJson(Files.readString(configFile.toPath()), Config.class);

        JDABuilder builder = JDABuilder.createDefault(config.token);
        builder.addEventListeners(
                new CommandListener(),
                new ModalListener(),
                new ButtonListener()
        );
        jda = builder.build();
        jda.awaitReady();

        Mongo.init();

        Guild guild = jda.getGuildById(config.guildId);
        if (guild == null) {
            System.out.println("Guild not found! Please check your config file.");
            System.exit(1);
        }

        guild.upsertCommand("ticket", "Main ticket command").addSubcommands(
                new SubcommandData("delete", "Delete the ticket"),
                new SubcommandData("archive", "Archive the ticket"),
                new SubcommandData("info", "View ticket info"),
                new SubcommandData("add", "Add a user to the ticket").addOption(
                        OptionType.USER, "user", "The user to add", true
                ),
                new SubcommandData("remove", "Remove a user from the ticket").addOption(
                        OptionType.USER, "user", "The user to add", true
                ),
                new SubcommandData("repost", "Repost the ticket")
        ).queue();

        TextChannel ticketCreateChannel = guild.getTextChannelById(config.ticketCreateChannelId);
        if (ticketCreateChannel == null) {
            System.out.println("Ticket create channel not found! Please check your config file.");
            System.exit(1);
            return;
        }
        ticketCreateChannel.getIterableHistory().forEach(message -> message.delete().queue());
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Create Ticket");
        embedBuilder.setDescription(config.ticketCreateMessage);
        ticketCreateChannel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                Button.success("create_ticket_plugin", "Plugin Developer"),
                Button.success("create_ticket_graphic", "Graphic Designer"),
                Button.success("create_ticket_writer", "Writer")
        ).queue();
    }
}
