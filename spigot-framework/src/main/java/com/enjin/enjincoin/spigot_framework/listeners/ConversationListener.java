package com.enjin.enjincoin.spigot_framework.listeners;

import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.event.Listener;

public class ConversationListener implements Listener {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    public ConversationListener(BasePlugin main) { this.main = main; }
}
