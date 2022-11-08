package me.fivevl.stb;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class CommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        switch (e.getName()) {
            case "ticket" -> ticketCommand(e);
            default -> e.reply("Unknown command!").setEphemeral(true).queue();
        }
    }

    private void ticketCommand(SlashCommandInteractionEvent e) {
        if (e.getSubcommandName() == null) {
            e.reply("Unknown subcommand!").setEphemeral(true).queue();
            return;
        }
        switch (e.getSubcommandName()) {
            case "delete" -> ticketDeleteCommand(e);
            case "archive" -> ticketArchiveCommand(e);
            case "info" -> ticketInfoCommand(e);
            case "add" -> ticketAddCommand(e);
            case "remove" -> ticketRemoveCommand(e);
            case "repost" -> ticketRepostCommand(e);
        }
    }

    private void ticketDeleteCommand(SlashCommandInteractionEvent e) {
        if (!isTicket(e.getChannel().asTextChannel())) {
            e.replyEmbeds(new EmbedBuilder().setTitle("This is not a ticket!").build()).setEphemeral(true).queue();
            return;
        }
        e.replyEmbeds(new EmbedBuilder().setTitle("Deleting this ticket...").build()).queue();
        e.getChannel().delete().queue();
    }

    private void ticketArchiveCommand(SlashCommandInteractionEvent e) {
        if (!isTicket(e.getChannel().asTextChannel())) {
            e.replyEmbeds(new EmbedBuilder().setTitle("This is not a ticket!").build()).setEphemeral(true).queue();
            return;
        }
        e.replyEmbeds(new EmbedBuilder().setTitle("Archiving this ticket...").build()).queue();
        e.getChannel().asTextChannel().getManager().setParent(Main.jda.getCategoryById(Main.config.ticketArchiveCategoryId)).queue();
    }

    private void ticketInfoCommand(SlashCommandInteractionEvent e) {
        if (!isTicket(e.getChannel().asTextChannel())) {
            e.replyEmbeds(new EmbedBuilder().setTitle("This is not a ticket!").build()).setEphemeral(true).queue();
            return;
        }
        Document document = Mongo.get(e.getChannel().getId());
        if (document == null) {
            e.replyEmbeds(new EmbedBuilder().setTitle("An error has occurred!").build()).setEphemeral(true).queue();
            return;
        }
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Ticket Info");
        embed.setDescription((String) document.get("description"));
        embed.addField("Budget", (String) document.get("budget"), true);
        embed.addField("Timeframe", (String) document.get("timeframe"), true);
        embed.addField("Department", (String) document.get("department"), true);
        embed.addField("Creator", (String) document.get("user"), true);
        embed.setFooter("Ticket ID: " + e.getChannel().getId());
        String additional = (String) document.get("additional");
        if (additional != null && !additional.isEmpty()) {
            embed.addField("Additional information", additional, false);
        }
        e.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void ticketAddCommand(SlashCommandInteractionEvent e) {
        if (!isTicket(e.getChannel().asTextChannel())) {
            e.replyEmbeds(new EmbedBuilder().setTitle("This is not a ticket!").build()).setEphemeral(true).queue();
            return;
        }
        User user = e.getOption("user").getAsUser();
        List<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.MESSAGE_HISTORY);
        permissions.add(Permission.MESSAGE_SEND);
        permissions.add(Permission.VIEW_CHANNEL);
        e.getChannel().asTextChannel().getManager().putMemberPermissionOverride(user.getIdLong(), permissions, new ArrayList<>()).queue();
        e.replyEmbeds(new EmbedBuilder().setTitle("Added " + user.getName() + " to this ticket.").build()).queue();
    }

    private void ticketRemoveCommand(SlashCommandInteractionEvent e) {
        if (!isTicket(e.getChannel().asTextChannel())) {
            e.replyEmbeds(new EmbedBuilder().setTitle("This is not a ticket!").build()).setEphemeral(true).queue();
            return;
        }
        User user = e.getOption("user").getAsUser();
        List<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.MESSAGE_HISTORY);
        permissions.add(Permission.MESSAGE_SEND);
        permissions.add(Permission.VIEW_CHANNEL);
        e.getChannel().asTextChannel().getManager().putMemberPermissionOverride(user.getIdLong(), new ArrayList<>(), permissions).queue();
        e.replyEmbeds(new EmbedBuilder().setTitle("Removed " + user.getName() + " from this ticket.").build()).queue();
    }

    private void ticketRepostCommand(SlashCommandInteractionEvent e) {
        if (!isTicket(e.getChannel().asTextChannel())) {
            e.replyEmbeds(new EmbedBuilder().setTitle("This is not a ticket!").build()).setEphemeral(true).queue();
            return;
        }
        Document document = Mongo.get(e.getChannel().getId());
        if (document == null) {
            e.replyEmbeds(new EmbedBuilder().setTitle("An error has occurred!").build()).setEphemeral(true).queue();
            return;
        }
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Reposted commission");
        embed.setDescription((String) document.get("description"));
        embed.addField("Budget", (String) document.get("budget"), true);
        embed.addField("Timeframe", (String) document.get("timeframe"), true);
        embed.setFooter("Ticket ID: " + e.getChannel().getId());
        String additional = (String) document.get("additional");
        if (additional != null && !additional.isEmpty()) {
            embed.addField("Additional information", additional, false);
        }
        String roleId;
        switch ((String) document.get("department")) {
            case "plugin" -> roleId = Main.config.pluginDeveloperRoleId;
            case "writer" -> roleId = Main.config.writerRoleId;
            case "graphic" -> roleId = Main.config.graphicDesignerRoleId;
            default -> {
                e.replyEmbeds(new EmbedBuilder().setTitle("An error has occurred!").build()).setEphemeral(true).queue();
                return;
            }
        }
        Main.jda.getTextChannelById(Main.config.commissionChannelId).sendMessage(Main.jda.getGuildById(Main.config.guildId).getRoleById(roleId).getAsMention() + " " + e.getChannel().getAsMention()).addEmbeds(embed.build()).addActionRow(
                Button.success("claim_ticket_" + e.getChannel().getId(), "Claim Commission")
        ).queue();
        e.replyEmbeds(new EmbedBuilder().setTitle("Reposted this ticket!").build()).queue();
    }

    private boolean isTicket(TextChannel channel) {
        return channel.getParentCategory().getId().equals(Main.config.ticketCategoryId) || channel.getParentCategory().getId().equals(Main.config.ticketArchiveCategoryId);
    }
}
