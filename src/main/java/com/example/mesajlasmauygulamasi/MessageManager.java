package com.example.mesajlasmauygulamasi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageManager {
    private static final String MESSAGES_FILE = "messages.json";
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static String currentUser;

    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    //mesajları dosyadan yükle
    public static List<Message> loadMessages() {
        try {
            File file = new File(MESSAGES_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            String json = new String(Files.readAllBytes(Paths.get(MESSAGES_FILE)));
            Type messageListType = new TypeToken<List<Message>>() {}.getType();
            List<Message> messages = gson.fromJson(json, messageListType);
            return messages != null ? messages : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    //mesajları dosyaya kaydet
    public static void saveMessages(List<Message> messages) {
        try (FileWriter writer = new FileWriter(MESSAGES_FILE)) {
            gson.toJson(messages, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //yeni mesaj ekle
    public static void addMessage(String sender, String receiver, String content) {
        List<Message> messages = loadMessages();
        Message newMessage = new Message(sender, receiver, content);
        messages.add(newMessage);
        saveMessages(messages);
    }

    //iki kullanıcı arasındaki mesajları getir
    public static List<Message> getMessagesBetween(String user1, String user2) {
        List<Message> allMessages = loadMessages();
        return allMessages.stream()
                .filter(msg -> {
                    boolean isSenderUser1 = msg.getSender().equals(user1);
                    if (isSenderUser1 && msg.getReceiver().equals(user2)) {
                        return !msg.isDeletedForSender();
                    }
                    else if (msg.getSender().equals(user2) && msg.getReceiver().equals(user1)) {
                        return !msg.isDeletedForReceiver();
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    //tek bir kişiyle olan sohbeti sil
    public static void clearChat(String me, String partner) {
        List<Message> allMessages = loadMessages();
        boolean changed = false;
        for (Message msg : allMessages) {
            if (msg.getSender().equals(me) && msg.getReceiver().equals(partner)) {
                msg.setDeletedForSender(true);
                changed = true;
            }
            else if (msg.getSender().equals(partner) && msg.getReceiver().equals(me)) {
                msg.setDeletedForReceiver(true);
                changed = true;
            }
        }
        if (changed) {
            saveMessages(allMessages);
        }
    }

    //tüm sohbetleri sil
    public static void clearAllChatsForUser(String username) {
        List<Message> allMessages = loadMessages();
        boolean changed = false;

        for (Message msg : allMessages) {
            //eğer gönderen bensem benden silindi yap
            if (msg.getSender().equals(username)) {
                if (!msg.isDeletedForSender()) {
                    msg.setDeletedForSender(true);
                    changed = true;
                }
            }
            //eğer alıcı bensem benden silindi yap
            if (msg.getReceiver().equals(username)) {
                if (!msg.isDeletedForReceiver()) {
                    msg.setDeletedForReceiver(true);
                    changed = true;
                }
            }
        }

        if (changed) {
            saveMessages(allMessages);
        }
    }

    //sohbet edilen kişileri getir
    public static List<String> getChatPartners(String username) {
        List<Message> allMessages = loadMessages();
        List<String> partners = new ArrayList<>();
        for (Message msg : allMessages) {
            if (msg.getSender().equals(username) && !msg.isDeletedForSender()) {
                if (!partners.contains(msg.getReceiver())) partners.add(msg.getReceiver());
            }
            else if (msg.getReceiver().equals(username) && !msg.isDeletedForReceiver()) {
                if (!partners.contains(msg.getSender())) partners.add(msg.getSender());
            }
        }
        return partners;
    }

    //okundu işaretle
    public static void markMessagesAsRead(String sender, String receiver) {
        List<Message> allMessages = loadMessages();
        boolean changed = false;
        for (Message msg : allMessages) {
            if (msg.getSender().equals(sender) && msg.getReceiver().equals(receiver) && !msg.isRead()) {
                msg.setRead(true);
                changed = true;
            }
        }
        if (changed) saveMessages(allMessages);
    }
}