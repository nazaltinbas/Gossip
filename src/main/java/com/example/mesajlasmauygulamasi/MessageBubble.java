package com.example.mesajlasmauygulamasi;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//mesaj görünümü için css ayarları
public class MessageBubble extends HBox {

    public MessageBubble(String message, boolean isFromMe, String timestamp, boolean isRead) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setFont(javafx.scene.text.Font.font("Segoe UI", 14));

        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(3.0);
        dropShadow.setOffsetX(1.0);
        dropShadow.setOffsetY(1.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.1));

        if (isFromMe) {
            messageLabel.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #a886d3, #8e44ad);" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 18 18 2 18;" +
                            "-fx-padding: 10 14 10 14;"
            );
            messageLabel.setEffect(dropShadow);
        } else {
            messageLabel.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-text-fill: #333333;" +
                            "-fx-background-radius: 18 18 18 2;" +
                            "-fx-padding: 10 14 10 14;" +
                            "-fx-border-color: #f0f0f0;" +
                            "-fx-border-width: 1;" +
                            "-fx-border-radius: 18 18 18 2;"
            );
            messageLabel.setEffect(dropShadow);
        }

        String timeText = formatTime(timestamp);
        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px; -fx-padding: 2 0 0 0;");

        Label statusLabel = new Label();
        if (isFromMe) {
            String statusText = isRead ? "✓✓" : "✓";
            statusLabel.setText(statusText);
            if (isRead) {
                statusLabel.setStyle("-fx-text-fill: #8e44ad; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
            }
        }

        this.setAlignment(isFromMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        this.setPadding(new Insets(4, 8, 4, 8));

        HBox bottomInfo = new HBox(4);
        bottomInfo.setAlignment(Pos.CENTER_RIGHT);

        if (isFromMe) {
            bottomInfo.getChildren().addAll(timeLabel, statusLabel);
        } else {
            bottomInfo.getChildren().add(timeLabel);
        }

        VBox container = new VBox(2);
        container.setAlignment(isFromMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.getChildren().addAll(messageLabel, bottomInfo);

        this.getChildren().add(container);
    }

    private String formatTime(String timestamp) {
        try {
            if (timestamp == null || timestamp.isEmpty()) {
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            }
            LocalDateTime dateTime = LocalDateTime.parse(timestamp,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return "??:??";
        }
    }
}