package nm.rc;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class RaccoonBot extends TelegramLongPollingBot{

    private String botUsername;
    private String botToken;

    public RaccoonBot(){
        loadConfig();
    }

    private Set<Game> activeGames = new HashSet<>();

    @Override
    public void onUpdateReceived(Update update){
        if(update.hasMessage()){
            Message message = update.getMessage();
            String text = message.getText();
            String chatID = String.valueOf(message.getChatId());

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

    private Game findGameByChatID(String chatID){
        for(Game game : activeGames){
            if(game.getChatId().equals(chatID)){
                return game;
            }
        }
        return null;
    }

    private void loadConfig(){
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("RaccoonConfig.properties");) {
            properties.load(inputStream);
            this.botToken = properties.getProperty("bot.token");
            this.botUsername = properties.getProperty("bot.username");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}