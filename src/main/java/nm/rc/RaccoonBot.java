package nm.rc;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RaccoonBot extends TelegramLongPollingBot{

    private String botUsername;
    private String botToken;
    private String developer;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public RaccoonBot(){
        loadConfig();
    }

    private final Set<Game> activeGames = new HashSet<>();

    @Override
    public void onUpdateReceived(Update update){
        if(update.hasMessage()){
            Message message = update.getMessage();
            String text = message.getText();
            String chatID = String.valueOf(message.getChatId());
            String userID = String.valueOf(message.getFrom().getId());

            executorService.submit(() -> {
                try {
                    if(!DatabaseControl.userExist(userID)){
                        String username = message.getFrom().getUserName();
                        String name = message.getFrom().getFirstName();
                        String lastname = message.getFrom().getLastName();
                        int words = 0;
                        int insert_id = DatabaseControl.getUserCount() + 1;

                        if(DatabaseControl.insertUser(insert_id, username, name, lastname, words, userID)){
                            System.out.println("[RaccoonBot] User with ID: " + userID + " inserted successfully");
                        };
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendMsg(chatID, "[DATABASE ERROR] Зверніться до розробника: " + "@" + this.developer);
                }
            });

            if (text.equals("/start_raccoon_game")) {
                if (findGameByChatID(chatID) != null) {
                    sendMsg(chatID, "Гра вже розпочалася!");
                } else {
                    activeGames.add(new Game(message.getFrom().getUserName(),String.valueOf(chatID)));

                    sendGameMenu(chatID, message.getFrom().getUserName());
                }
            }
            if(text.equals("/stop_raccoon_game")){
                if(findGameByChatID(chatID) != null){
                    activeGames.remove(findGameByChatID(chatID));
                    sendMsg(chatID, "Гру завершено");

                }
                else{
                    sendMsg(chatID, "Гру не розпочато");
                }
            }
        }
    }

    private void sendGameMenu(String chatID, String username){
        String sendMsg = new String("Слово пояснює: " + username);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatID));
        sendMessage.setText(sendMsg);

        InlineKeyboardButton seeWordBtn = new InlineKeyboardButton();
        seeWordBtn.setText("Подивитися слово");
        seeWordBtn.setCallbackData("seeWordButtonCallBack");

        InlineKeyboardButton newWordBtn = new InlineKeyboardButton();
        newWordBtn.setText("Нове слово");
        newWordBtn.setCallbackData("newWordButtonCallBack");


        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(seeWordBtn);
        row.add(newWordBtn);

        keyboardMarkup.setKeyboard(List.of(row));

        sendMessage.setReplyMarkup(keyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void sendMsg(String chatID, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    @Override
    public String getBotToken(){
        return this.botToken;
    }

    private Game findGameByChatID(String chatID) {
        return activeGames.stream()
                .filter(game -> game.getChatId().equals(chatID))
                .findFirst()
                .orElse(null);
    }

    private void loadConfig(){
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("RaccoonConfig.properties");) {
            properties.load(inputStream);
            this.botToken = properties.getProperty("bot.token");
            this.botUsername = properties.getProperty("bot.username");
            this.developer = properties.getProperty("developer.username");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}