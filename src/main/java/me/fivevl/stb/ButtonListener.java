package me.fivevl.stb;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import java.util.ArrayList;
import java.util.List;

public class ButtonListener extends ListenerAdapter {
    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        if (e.getComponentId().startsWith("create_ticket_")) {
            createTicket(e);
        }
        if (e.getComponentId().startsWith("claim_ticket_")) {
            claimTicket(e);
        }
    }

    private void createTicket(ButtonInteractionEvent e) {
        Modal.Builder modal = Modal.create("create_ticket_" + e.getComponentId().split("_")[2], "Create a ticket");

        TextInput.Builder description = TextInput.create("description", "Description", TextInputStyle.PARAGRAPH);
        description.setPlaceholder("Describe your request");
        description.setMaxLength(1024);

        TextInput.Builder budget = TextInput.create("budget", "Budget", TextInputStyle.SHORT);
        budget.setPlaceholder("Enter your budget");
        budget.setMaxLength(64);

        TextInput.Builder timeframe = TextInput.create("timeframe", "Timeframe", TextInputStyle.SHORT);
        timeframe.setPlaceholder("Enter your desired timeframe");
        timeframe.setMaxLength(64);

        TextInput.Builder additional = TextInput.create("additional", "Additional", TextInputStyle.SHORT);
        additional.setPlaceholder("Enter any additional information");
        additional.setMaxLength(256);
        additional.setRequired(false);

        modal.addActionRows(
                ActionRow.of(description.build()),
                ActionRow.of(budget.build()),
                ActionRow.of(timeframe.build()),
                ActionRow.of(additional.build())
        );
        e.replyModal(modal.build()).queue();
    }

    private void claimTicket(ButtonInteractionEvent e) {
        String channelId = e.getComponentId().split("_")[2];
        TextChannel ticket = Main.jda.getTextChannelById(channelId);
        if (ticket == null) {
            e.reply("This ticket does not exist anymore!").setEphemeral(true).queue();
            e.getMessage().delete().queue();
            return;
        }
        String role = ticket.getName().split("-")[ticket.getName().split("-").length - 1];
        String roleId;
        switch (role) {
            case "plugin" -> roleId = Main.config.pluginDeveloperRoleId;
            case "graphic" -> roleId = Main.config.graphicDesignerRoleId;
            case "writer" -> roleId = Main.config.writerRoleId;
            default -> {
                e.replyEmbeds(new EmbedBuilder().setTitle("Error").setDescription("This ticket does not have a valid department!").build()).setEphemeral(true).queue();
                return;
            }
        }
        if (e.getMember().getRoles().stream().noneMatch(r -> r.getId().equals(roleId))) {
            e.replyEmbeds(new EmbedBuilder().setTitle("Error").setDescription("You do not have the required role to claim this ticket!").build()).setEphemeral(true).queue();
            return;
        }
        List<Permission> permissions = new ArrayList<>();
        permissions.add(Permission.MESSAGE_HISTORY);
        permissions.add(Permission.MESSAGE_SEND);
        permissions.add(Permission.VIEW_CHANNEL);
        ticket.getManager().putMemberPermissionOverride(e.getUser().getIdLong(), permissions, new ArrayList<>()).queue();
        ticket.sendMessageEmbeds(new EmbedBuilder().setTitle("Ticket claimed").setDescription("This ticket has been claimed by " + e.getUser().getName()).build()).queue();
        e.replyEmbeds(new EmbedBuilder().setTitle("Ticket claimed!").build()).setEphemeral(true).queue();
        e.getMessage().delete().queue();
    }
}
