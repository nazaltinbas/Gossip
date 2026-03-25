package com.example.mesajlasmauygulamasi;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javafx.scene.text.TextAlignment;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static com.example.mesajlasmauygulamasi.Controller.getLoggedInUser;

public class MesajSayfasiController implements Initializable {


    @FXML private Label currentUserLabel;
    @FXML private VBox messageContainer;
    @FXML private TextField messageField;
    @FXML private TextField searchField;
    @FXML private TextField chatSearchField;
    @FXML private Button sendButton;
    @FXML private ScrollPane scrollpane;
    @FXML private Button cikisyap;
    @FXML private ListView<String> userListView;
    @FXML private Label chatTitleLabel;
    @FXML private Button ayarlar_buton;
    @FXML private Button sohbeti_temizle;
    @FXML private HBox inputBar;
    @FXML private HBox chatHeader;

    private String currentChatPartner;
    private String currentUser;
    private String lastDisplayedDay = "";
    private ObservableList<String> visibleList;

    private boolean isProgrammaticSelection = false;

    //demo aşamasındaki özellik uyarısı
    @FXML
    private void demoFeatureUyari() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Yapım Aşamasında");
        alert.setHeaderText(null);
        alert.setContentText("Bu özellik şu an demo modundadır.\nYakında eklenecektir! 🛠️");
        alert.showAndWait();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = getLoggedInUser();

        //giriş yapan kullanıcının adını sağ üste yaz
        if (currentUserLabel != null) {
            currentUserLabel.setText(currentUser);
        }

        currentUser = getLoggedInUser();
        lastDisplayedDay = "";

        visibleList = FXCollections.observableArrayList();
        if (userListView != null) {
            userListView.setItems(visibleList);
            //hiç kimseyle sohbet başlatılmamışsa uyarı verir
            Label emptyLabel = new Label("Henüz açık sohbetin yok.\nYukarıdan bir kullanıcı aratıp\nmesajlaşmaya başlayabilirsin. 👋");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 13px; -fx-font-weight: bold;");
            emptyLabel.setWrapText(true);
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setTextAlignment(TextAlignment.CENTER);

            userListView.setPlaceholder(emptyLabel);
        }

        //başlangıçta sadece mesajlaşılan kişileri yükler
        loadUserList();
        setupUserListDesign();

        if (sendButton != null) sendButton.setOnAction(event -> sendMessage());
        if (messageField != null) messageField.setOnAction(event -> sendMessage());

        if (userListView != null) {
            userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                //eğer kod tarafından (sendMessage sonrası) seçim yapılıyorsa dinleme yapma
                if (isProgrammaticSelection) return;

                if (newVal != null && !newVal.equals(currentUser)) {
                    openChatWithUser(newVal);
                    //kullanıcı seçildikten sonra arama sonuçlarını ve arama kutusunu temizle
                    if (searchField != null && !searchField.getText().isEmpty()) {
                        searchField.clear();
                    }
                }
            });
        }

        if (messageContainer != null) {
            messageContainer.heightProperty().addListener((obs, oldVal, newVal) -> scrollToBottom());
        }

        //başlangıçta gizle
        Platform.runLater(() -> {
            if (inputBar != null) inputBar.getStyleClass().add("hidden-element");
            if (chatHeader != null) chatHeader.getStyleClass().add("hidden-element");
            showWelcomeMessage();
        });

        try {
            if (getClass().getResource("ayarlar.css") != null && userListView != null) {
                String cssPath = getClass().getResource("ayarlar.css").toExternalForm();
                userListView.getStylesheets().add(cssPath);
                if (scrollpane != null) scrollpane.getStylesheets().add(cssPath);
            }
        } catch (Exception e) {
            System.out.println("CSS hatası: " + e.getMessage());
        }

        //arama kutusu dinleyicisi
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> handleUserSearch(newVal));
        }

        //sohbet içi arama
        if (chatSearchField != null) {
            chatSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentChatPartner != null) refreshChatMessages();
            });
        }
    }

    //sadece mesajlaşılan kişileri yükler
    private void loadUserList() {
        try {
            //sadece mesajı olanları getiriyor
            List<String> chatPartners = MessageManager.getChatPartners(currentUser);
            List<String> sortedChatPartners = sortUsersByDate(chatPartners);

            Platform.runLater(() -> {
                visibleList.setAll(sortedChatPartners);
            });
        } catch (Exception e) {
            System.out.println("Liste yüklenirken hata: " + e.getMessage());
        }
    }

    //arama yaparken tüm kullanıcıları tarar ama normalde sadece mesajlaşılanları gösterir
    private void handleUserSearch(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            loadUserList();
            return;
        }
        try {
            String lowerCaseFilter = searchText.toLowerCase();
            List<User> allUsers = UserManager.loadUsers(); //tüm kayıtlı kullanıcıları çek

            //isminde arama metni geçen herkesi bul (ben hariç)
            List<String> searchResults = allUsers.stream()
                    .map(User::getUsername)
                    .filter(username -> !username.equals(currentUser))
                    .filter(username -> username.toLowerCase().contains(lowerCaseFilter))
                    .collect(Collectors.toList());

            visibleList.setAll(searchResults);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        if (currentChatPartner == null) {
            showAlert("Lütfen mesaj göndermek için önce bir kullanıcı seçin!");
            return;
        }

        if (messageField == null) return;
        String message = messageField.getText().trim();

        if (!message.isEmpty()) {
            //mesajı kaydet
            MessageManager.addMessage(currentUser, currentChatPartner, message);

            if (chatSearchField != null) {
                chatSearchField.clear();
            }

            //ekrana ekle (o sırada görünmesi için)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currentDay = getDayFromTimestamp(timestamp);
            if (!currentDay.equals(lastDisplayedDay)) {
                addDayHeader(currentDay);
                lastDisplayedDay = currentDay;
            }
            addMessageWithTime(message, true, timestamp);

            //listeyi güncelle (en son konuşulan en üste çıksın)
            Platform.runLater(() -> {
                //arama yapmıyorsak listeyi güncelle (konuşulan kişi listeye kalıcı eklensin)
                if (searchField != null && (searchField.getText() == null || searchField.getText().isEmpty())) {
                    List<String> chatPartners = MessageManager.getChatPartners(currentUser);
                    List<String> sorted = sortUsersByDate(chatPartners);

                    isProgrammaticSelection = true; //dinleyiciyi geçici olarak sustur
                    visibleList.setAll(sorted);
                    if (userListView != null) {
                        userListView.getSelectionModel().select(currentChatPartner);
                    }
                    isProgrammaticSelection = false; //dinleyiciyi tekrar aç
                }
            });
            messageField.clear();
        }
    }

    private void openChatWithUser(String partner) {
        currentChatPartner = partner;
        if (chatTitleLabel != null) chatTitleLabel.setText(partner);
        lastDisplayedDay = "";

        if (inputBar != null) inputBar.getStyleClass().remove("hidden-element");
        if (chatHeader != null) chatHeader.getStyleClass().remove("hidden-element");
        if (messageField != null) messageField.requestFocus();
        if (chatSearchField != null) chatSearchField.clear();

        //mesajları yükle
        refreshChatMessages();

        //okundu olarak işaretle
        MessageManager.markMessagesAsRead(partner, currentUser);

        // okundu sayılarını silmek için liste görünümünü yeniliyor
        if (userListView != null) userListView.refresh();

        scrollToBottom();
    }

    //liste tasarımı için
    private void setupUserListDesign() {
        if (userListView == null) return;
        userListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setGraphic(null); setText(null); setStyle("-fx-background-color: transparent;");
                } else {
                    HBox rootBox = new HBox(12); //avatar ile metin arası boşluk
                    rootBox.setAlignment(Pos.CENTER_LEFT);
                    rootBox.setPadding(new Insets(5, 5, 5, 5));

                    // avatar
                    Circle avatarCircle = new Circle(24); // Biraz daha büyük
                    avatarCircle.setFill(Color.web("#dfe4ea")); // Gri zemin
                    SVGPath userIcon = new SVGPath();
                    userIcon.setContent("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z");
                    userIcon.setFill(Color.WHITE);
                    userIcon.setScaleX(1.4); userIcon.setScaleY(1.4);
                    StackPane avatarContainer = new StackPane(avatarCircle, userIcon);
                    avatarContainer.setAlignment(Pos.CENTER);

                    // isim ve tarih bilgisi
                    VBox infoBox = new VBox(4);
                    infoBox.setAlignment(Pos.CENTER_LEFT);

                    // isim + tarih yana yaslı
                    HBox topRow = new HBox();
                    topRow.setAlignment(Pos.CENTER_LEFT);

                    // isim
                    Label nameLabel = new Label(username);
                    nameLabel.setStyle("-fx-text-fill: #111b21; -fx-font-weight: bold; -fx-font-size: 15px;");

                    // boşluk bırakıp tarihi sağa it
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    // son mesaj tarihi/zamanı
                    String dateText = getLastMessageDate(username);
                    Label dateLabel = new Label(dateText);
                    dateLabel.setStyle("-fx-text-fill: #667781; -fx-font-size: 11px;");

                    topRow.getChildren().addAll(nameLabel, spacer, dateLabel);

                    // okunmamış mesaj sayısı
                    HBox bottomRow = new HBox();
                    bottomRow.setAlignment(Pos.CENTER_RIGHT);

                    long unreadCount = getUnreadCountFromUser(username);
                    if (unreadCount > 0) { //kişinin gönderdiği okunmayan mesaj sayısınu ekler
                        Label countLabel = new Label(String.valueOf(unreadCount));
                        countLabel.setTextFill(Color.WHITE);
                        countLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
                        countLabel.setStyle("-fx-background-color: #2556d3; -fx-background-radius: 20; -fx-min-width: 20px; -fx-min-height: 20px; -fx-alignment: center; -fx-padding: 0 4 0 4;");
                        bottomRow.getChildren().add(countLabel);
                    }

                    if (getListView() != null) {
                        infoBox.prefWidthProperty().bind(getListView().widthProperty().subtract(75)); // Avatar payı düşüldü
                    }

                    infoBox.getChildren().addAll(topRow, bottomRow);
                    rootBox.getChildren().addAll(avatarContainer, infoBox);

                    setGraphic(rootBox); setText(null);
                }
            }
        });
    }

    //tarih getirmek için
    private String getLastMessageDate(String partner) {
        try {
            List<Message> msgs = MessageManager.getMessagesBetween(currentUser, partner);
            if (msgs != null && !msgs.isEmpty()) {
                String fullDate = msgs.get(msgs.size()-1).getTimestamp();
                return fullDate.substring(11, 16); // sadece saati al (HH:mm)
            }
        } catch (Exception e) {}
        return "";
    }

    //okunmayan mesaj sayısını getirmek için
    private long getUnreadCountFromUser(String senderName) {
        try {
            List<Message> messages = MessageManager.getMessagesBetween(currentUser, senderName);
            return messages.stream().filter(msg -> msg.getSender().equals(senderName) && !msg.isRead()).count();
        } catch (Exception e) { return 0; }
    }

    //chatteki mesajları yeniler
    private void refreshChatMessages() {
        Platform.runLater(() -> {
            if (messageContainer != null) messageContainer.getChildren().clear();
        });
        List<Message> messages = MessageManager.getMessagesBetween(currentUser, currentChatPartner);
        addMessagesWithDayHeaders(messages);
        scrollToBottom();
    }

    //kullanıcıları en yeni tarihte konuşulan en üstte olacak şekilde sıralar
    private List<String> sortUsersByDate(List<String> users) {
        return users.stream().sorted((user1, user2) -> {
            try {
                List<Message> messages1 = MessageManager.getMessagesBetween(currentUser, user1);
                List<Message> messages2 = MessageManager.getMessagesBetween(currentUser, user2);
                if ((messages1 == null || messages1.isEmpty()) && (messages2 == null || messages2.isEmpty())) return user1.compareTo(user2);
                if (messages1 == null || messages1.isEmpty()) return 1;
                if (messages2 == null || messages2.isEmpty()) return -1;

                String lastTime1 = messages1.get(messages1.size() - 1).getTimestamp();
                String lastTime2 = messages2.get(messages2.size() - 1).getTimestamp();
                return lastTime2.compareTo(lastTime1);
            } catch (Exception e) {
                return user1.compareTo(user2);
            }
        }).collect(Collectors.toList());
    }

    private void addMessagesWithDayHeaders(List<Message> messages) {
        String filterText = "";
        if (chatSearchField != null && chatSearchField.getText() != null) {
            filterText = chatSearchField.getText().toLowerCase().trim();
        }
        boolean isFiltering = !filterText.isEmpty();

        if (messages == null || messages.isEmpty()) {
            showEmptyChatMessage();
            return;
        }

        String currentDay = "";
        boolean separatorAdded = false; //ayracı sadece 1 kere koymak için kontrol

        for (Message msg : messages) {
            //arama filtresi varsa mesajları ele
            if (isFiltering && !msg.getContent().toLowerCase().contains(filterText)) {
                continue;
            }

            //eğer filtreleme yoksa, mesaj benden değilse, okunmamışsa ve daha önce ayraç koymadıysak
            if (!isFiltering && !msg.getSender().equals(currentUser) && !msg.isRead() && !separatorAdded) {
                addNewMessagesSeparator(); // Ayracı ekle
                separatorAdded = true;     // Bir daha ekleme
            }

            if (!isFiltering) {
                String messageDay = getDayFromTimestamp(msg.getTimestamp());
                if (!messageDay.equals(currentDay)) {
                    currentDay = messageDay;
                    addDayHeader(currentDay);
                }
            }
            boolean isFromMe = msg.getSender().equals(currentUser);
            addMessageWithTime(msg.getContent(), isFromMe, msg.getTimestamp());
        }

        //eğer filtreleme yapıldıysa ve sonuç yoksa
        if (isFiltering && messageContainer.getChildren().isEmpty()) {
            Platform.runLater(() -> {
                Label noResultLabel = new Label("Eşleşen mesaj bulunamadı.");
                noResultLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14px; -fx-padding: 20;");
                HBox box = new HBox(noResultLabel);
                box.setAlignment(Pos.CENTER);
                if (messageContainer != null) messageContainer.getChildren().add(box);
            });
        }
        lastDisplayedDay = currentDay;
    }

    //yeni mesajlar için ayırıcı
    private void addNewMessagesSeparator() {
        Platform.runLater(() -> {
            Label separatorLabel = new Label("YENİ MESAJLAR");
            separatorLabel.getStyleClass().add("new-messages-label"); // CSS'ten stil alır

            HBox separatorContainer = new HBox(separatorLabel);
            separatorContainer.setAlignment(Pos.CENTER); // Ortalar
            separatorContainer.getStyleClass().add("new-messages-separator");

            if (messageContainer != null) messageContainer.getChildren().add(separatorContainer);
        });
    }

    //mesajların üstüne gün bilgisi başlığı ekler
    private void addDayHeader(String day) {
        Platform.runLater(() -> {
            Label dayLabel = new Label(day);
            dayLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-padding: 4px 12px; -fx-background-color: rgba(200, 200, 200, 0.3); -fx-background-radius: 12px; -fx-border-radius: 12px;");
            HBox dayContainer = new HBox(dayLabel);
            dayContainer.setAlignment(Pos.CENTER);
            dayContainer.setPadding(new Insets(10, 0, 5, 0));
            if (messageContainer != null) messageContainer.getChildren().add(dayContainer);
        });
    }

    private String getDayFromTimestamp(String timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime now = LocalDateTime.now();
            if (dateTime.toLocalDate().equals(now.toLocalDate())) return "Bugün";
            if (dateTime.toLocalDate().equals(now.minusDays(1).toLocalDate())) return "Dün";
            return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception e) { return "Tarih bilinmiyor"; }
    }

    private void addMessageWithTime(String message, boolean isFromMe, String timestamp) {
        Platform.runLater(() -> {
            boolean isRead = false;
            try {
                if (isFromMe && currentChatPartner != null) {
                    List<Message> messages = MessageManager.getMessagesBetween(currentUser, currentChatPartner);
                    for (Message msg : messages) {
                        if (msg.getTimestamp().equals(timestamp) && msg.getSender().equals(currentUser)) {
                            isRead = msg.isRead();
                            break;
                        }
                    }
                }
            } catch (Exception e) { }
            MessageBubble bubble = new MessageBubble(message, isFromMe, timestamp, isRead);
            if (messageContainer != null) messageContainer.getChildren().add(bubble);
        });
    }

    private void showWelcomeMessage() {
        Platform.runLater(() -> {
            Label welcomeLabel = new Label("Hoş geldiniz " + currentUser + "!\n\nSoldan bir kullanıcı seçerek mesajlaşmaya başlayabilirsiniz.");
            welcomeLabel.setWrapText(true);
            welcomeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666; -fx-padding: 20px; -fx-alignment: center; -fx-font-weight: bold;");
            welcomeLabel.setMaxWidth(400);
            HBox welcomeContainer = new HBox(welcomeLabel);
            welcomeContainer.setAlignment(Pos.CENTER);
            welcomeContainer.setPadding(new Insets(50, 0, 0, 0));
            if (messageContainer != null) messageContainer.getChildren().add(welcomeContainer);
        });
    }

    private void showEmptyChatMessage() {
        Platform.runLater(() -> {
            Label emptyLabel = new Label("Henüz mesaj yok. İlk mesajınızı gönderin!");
            emptyLabel.setWrapText(true);
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666; -fx-padding: 20px; -fx-alignment: center;");
            emptyLabel.setMaxWidth(400);
            HBox emptyContainer = new HBox(emptyLabel);
            emptyContainer.setAlignment(Pos.CENTER);
            emptyContainer.setPadding(new Insets(50, 0, 0, 0));
            if (messageContainer != null) messageContainer.getChildren().add(emptyContainer);
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            try {
                if (scrollpane != null) {
                    scrollpane.applyCss();
                    scrollpane.layout();
                    scrollpane.setVvalue(1.0);
                }
            } catch (Exception e) { }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Uyarı");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void giris_sayfasiGec() throws IOException {
        Stage mevcutStage = (Stage) cikisyap.getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("giris_ekrani.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        mevcutStage.setScene(scene);
        mevcutStage.show();
    }

    @FXML
    private void ayarlar_gec() throws IOException {
        Stage mevcutStage = (Stage) ayarlar_buton.getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ayarlar_ekrani.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        mevcutStage.setScene(scene);
        mevcutStage.show();
    }

    @FXML
    private void sohbetiTemizle() {
        if (currentChatPartner == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sohbeti Temizle");
        alert.setHeaderText(currentChatPartner + " ile olan sohbeti temizlemek istiyor musun?");
        alert.setContentText("Bu işlem sohbeti senin ekranından siler ve listeden kaldırır.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                MessageManager.clearChat(currentUser, currentChatPartner);
                if (messageContainer != null) messageContainer.getChildren().clear();
                lastDisplayedDay = "";
                if (inputBar != null) inputBar.getStyleClass().add("hidden-element");
                if (chatHeader != null) chatHeader.getStyleClass().add("hidden-element");
                currentChatPartner = null;

                //temizledikten sonra listeyi yenile
                loadUserList();

                if (userListView != null) userListView.getSelectionModel().clearSelection();
                showWelcomeMessage();
            }
        });
    }
}