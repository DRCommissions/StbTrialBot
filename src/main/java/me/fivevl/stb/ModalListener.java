package me.fivevl.stb;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class ModalListener extends ListenerAdapter {
    @Override
    public void onModalInteraction(ModalInteractionEvent e) {
        if (e.getModalId().startsWith("create_ticket_")) {
            ticketCreate(e, e.getModalId().split("_")[2]);
        }
    }

    private void ticketCreate(ModalInteractionEvent e, String department) {
        TextChannel commChannel = Main.jda.getTextChannelById(Main.config.commissionChannelId);
        if (commChannel == null) {
            e.reply("Commission channel not found!").setEphemeral(true).queue();
            return;
        }
        String roleId;
        switch (department) {
            case "plugin" -> roleId = Main.config.pluginDeveloperRoleId;
            case "graphic" -> roleId = Main.config.graphicDesignerRoleId;
            case "writer" -> roleId = Main.config.writerRoleId;
            default -> {
                e.reply("Unknown department!").setEphemeral(true).queue();
                return;
            }
        }
        Category tickets = Main.jda.getCategoryById(Main.config.ticketCategoryId);
        if (tickets == null) {
            e.reply("Ticket category not found!").setEphemeral(true).queue();
            return;
        }
        tickets.createTextChannel(e.getUser().getName() + "-" + department).queue(ticket -> {
            List<Permission> permissions = new ArrayList<>();
            permissions.add(Permission.MESSAGE_HISTORY);
            permissions.add(Permission.MESSAGE_SEND);
            permissions.add(Permission.VIEW_CHANNEL);
            ticket.getManager().putMemberPermissionOverride(e.getUser().getIdLong(), permissions, new ArrayList<>()).queue();

            EmbedBuilder createMessage = new EmbedBuilder();
            createMessage.setTitle("Ticket created");
            createMessage.setDescription("Your ticket has been created. " + ticket.getAsMention());

            e.replyEmbeds(createMessage.build()).setEphemeral(true).queue();

            String description = e.getValue("description").getAsString();
            String budget = e.getValue("budget").getAsString();
            String timeframe = e.getValue("timeframe").getAsString();
            String additional = e.getValue("additional").getAsString();

            Document document = new Document();

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("New commission by " + e.getUser().getAsTag());
            embed.setDescription(description);
            embed.addField("Budget", budget, true);
            embed.addField("Timeframe", timeframe, true);
            embed.setFooter("Ticket ID: " + ticket.getId());
            if (!additional.isEmpty()) {
                embed.addField("Additional information", additional, false);
                document.put("additional", additional);
            }

            document.put("id", ticket.getId());
            document.put("department", department);
            document.put("user", e.getUser().getId());
            document.put("description", description);
            document.put("budget", budget);
            document.put("timeframe", timeframe);

            Mongo.set(document);

            commChannel.sendMessage(Main.jda.getGuildById(Main.config.guildId).getRoleById(roleId).getAsMention() + " " + ticket.getAsMention()).addEmbeds(embed.build()).addActionRow(
                    Button.success("claim_ticket_" + ticket.getId(), "Claim Commission")
            ).queue();
        });
    }
}
