package nm.rc;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.sql.SQLException;
import java.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RaccoonBot extends TelegramLongPollingBot{

    private final String botUsername = System.getenv("BOT_USERNAME");
    private final String botToken = System.getenv("BOT_TOKEN");
    private final String developer = System.getenv("DEVELOPER_USERNAME");

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final Map<String, Game> activeGames = new HashMap<>();

    private Set<String> words;

    public RaccoonBot(){
        telegramBotInit();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()){
            String chatID = String.valueOf(update.getCallbackQuery().getMessage().getChatId());
            try {
                handleCallback(update, activeGames.get(chatID));
            }
            catch(SQLException e){
                e.printStackTrace();
            }
        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            String text = message.getText();
            String chatID = String.valueOf(message.getChatId());
            String userID = String.valueOf(message.getFrom().getId());

            CompletableFuture.runAsync(() -> handleUserAndGameLogic(userID, chatID, message, text), executorService);
        }
    }

    private void handleUserAndGameLogic(String userID, String chatID, Message message, String text) {
        try {
            try {
                if (!DatabaseControl.userExist(userID)) {
                    String username = message.getFrom().getUserName();
                    String name = message.getFrom().getFirstName();
                    String lastname = message.getFrom().getLastName();
                    int words = 0;
                    int insert_id = DatabaseControl.getUserCount() + 1;

                    boolean userInserted = DatabaseControl.insertUser(insert_id, username, name, lastname, words, userID);
                    if (userInserted) {
                        System.out.println("[RaccoonBot] User with ID: " + userID + " inserted successfully");
                    } else if (DatabaseControl.hasOneDayPassed(userID)) {
                        executorService.submit(() -> {
                            DatabaseControl.updateInfo(userID, username, name, lastname);
                        });
                    }
                }
            } catch (SQLException e) {
                System.out.println("[RaccoonBot] Database error: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("[RaccoonBot] Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }

            String command = text.replace("@RaccoonGameMBot", "").trim();

            switch (command) {
                case "/start": {
                    handleStartCommand(message, chatID);
                    break;
                }

                case "/start_raccoon_game": {
                    handleStartGame(chatID, message);
                    break;
                }

                case "/stop_raccoon_game": {
                    handleStopGame(chatID, message);
                    break;
                }

                case "/top": {
                    sendTopUsers(chatID);
                    break;
                }

                default: {
                    Game game = activeGames.get(chatID);

                    if(handleUserGuess(game, command, String.valueOf(message.getFrom().getId()), message.getMessageId())){
                        String username = message.getFrom().getUserName();
                        String name = message.getFrom().getFirstName();
                        String lastname = message.getFrom().getLastName();
                        if(username == null){
                            sendMsg(chatID, name + " " + lastname + " відгадав слово✅.");
                        }
                        else{
                            sendMsg(chatID, "@" + username + " відгадав слово✅.");
                        }

                        CompletableFuture.runAsync(() -> {
                            if(deletePrevMenuMsg(game)){
                                System.out.println("[RaccoonBot] Message has been successfully deleted.");
                            } else {
                                System.out.println("[RaccoonBot] Error while deleting message.");
                            }
                            String currentPlayerID = game.getCurrentPlayerID();
                            DatabaseControl.increaseWords(currentPlayerID);
                            DatabaseControl.increaseMoney(currentPlayerID,10);
                            game.increaseCountAnswers();
                        }, executorService);

                        game.swapGameInfo(userID,username, getRandomWord());
                        sendGameMenu(username, name, lastname, game);
                    }
                    break;
                }
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendMsg(chatID, "[DATABASE ERROR] ὪBЗверніться до розробника: " + "@" + this.developer);
        }
    }

    private void handleStartCommand(Message message, String chatID) {
        if (isGroupChat(message)) {
            sendMsg(chatID, "Ця команда доступна в особистому чаті з ботом " + "@RaccoonGameMBot");
        } else {
            sendMsg(chatID, "Привіт, я RaccoonBot для гри в крокодила 🦝." +
                    "\nДля того щоб розпочати гру 🎮 тобі необхідно додати мене в групу 👥 з іншими гравцями та ввести команду /start_raccoon_game.");
        }
    }

    private void handleStartGame(String chatID, Message message) throws TelegramApiException {
        if (isPrivateChat(message)) {
            sendMsg(chatID, "Гра в крокодила доступна тільки в групових чатах.\n" +
                    "Для того щоб розпочати гру додай мене у групу з гравцями та введи команду /start_raccoon_game заново✅.");
        } else {
            Game game = activeGames.get(chatID);
            if (game != null) {
                sendMsg(chatID, "Гра вже розпочалася! ▶");
            } else {
                activeGames.put(chatID, new Game(message.getFrom().getUserName(), String.valueOf(message.getFrom().getId()), chatID, getRandomWord()));
                sendMsg(chatID,"Розпочинаю гру ▶. Загальна кількість зареєстрованих слів: " + WordLoader.getWordsCount());
                sendGameMenu(message.getFrom().getUserName(), message.getFrom().getFirstName(), message.getFrom().getLastName(), activeGames.get(chatID));
            }
        }
    }

    private String getRandomWord() {
        if (words == null || words.isEmpty()) {
            return "Немає доступних слів";
        }
        List<String> wordList = new ArrayList<>(words);
        return wordList.get(new Random().nextInt(wordList.size()));
    }

    private void handleStopGame(String chatID, Message message) {
        if(isPrivateChat(message)){
            sendMsg(chatID, "Ця команда доступна лише в груповому чаті.");
        }else {
            Game game = activeGames.get(chatID);
            if (game != null) {
                activeGames.remove(chatID);
                sendMsg(chatID, "Гру завершено\uD83C\uDFC1. Кількість відгаданих слів: " + game.getCountAnswers());
            } else {
                sendMsg(chatID, "Гру не розпочато ⏸");
            }
        }
    }

    private void sendTopUsers(String chatID) {
        CompletableFuture.runAsync(() -> {
            try {
                sendMsg(chatID, DatabaseControl.getTopUsers());
            } catch (SQLException e) {
                sendMsg(chatID, "[DATABASE ERROR] ὪBПомилка при отриманні списку користувачів.");
            }
        }, executorService);
    }

    private void handleCallback(Update update, Game game) throws SQLException {
        if(update.hasCallbackQuery() && game != null){
            String callbackQueryId = update.getCallbackQuery().getId();
            String callbackData = update.getCallbackQuery().getData();
            String text = "";

            String userID = String.valueOf(update.getCallbackQuery().getFrom().getId());

            text = switch (callbackData) {
                case "seeWordButtonCallBack" -> handleSeeWordCallback(game, userID);
                case "newWordButtonCallBack" -> handleNewWordCallback(game, userID);
                case "tipSeeWordButtonCallBackData" -> handleTipSeeWordCallback(userID, game);
                case "tipOpenLetterButtonCallBack" -> handleTipOpenLetterCallback(userID, game);
                default -> "Необроблене запитання";
            };

            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
            answerCallbackQuery.setCallbackQueryId(callbackQueryId);
            answerCallbackQuery.setText(text);
            answerCallbackQuery.setShowAlert(Boolean.TRUE);

            try {
                execute(answerCallbackQuery);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String handleSeeWordCallback(Game game, String userID) {
        if (game.getCurrentPlayerID().equals(userID)) {
            return "\uD83D\uDD0D\uFE0EСлово: " + game.getWord();
        } else {
            return "Слово пояснює інший гравець❌";
        }
    }

    private String handleNewWordCallback(Game game, String userID) {
        if (game.getCurrentPlayerID().equals(userID)) {
            game.setWord(this.getRandomWord());
            return "\uD83C\uDD95Нове слово: " + game.getWord();
        } else {
            return "Слово пояснює інший гравець❌";
        }
    }

    private String handleTipSeeWordCallback(String userID, Game game) throws SQLException {
        if (DatabaseControl.getMoneyByUserId(userID) >= 100) {
            if (game.getCurrentPlayerID().equals(userID)) {
                return "Ти пояснюєш слово";
            } else {
                CompletableFuture.runAsync(() -> {
                    DatabaseControl.decreaseMoney(userID, 100);
                }, executorService);

                return "Слово: " + game.getWord();
            }
        } else {
            return "В тебе недостатньо монет\uD83D\uDCB0";
        }
    }

    private String handleTipOpenLetterCallback(String userID, Game game) throws SQLException {
        if (DatabaseControl.getMoneyByUserId(userID) >= 50) {
            if (game.getCurrentPlayerID().equals(userID)) {
                return "Ти пояснюєш слово";
            } else {

                String word = game.getCurrentTip(userID);

                if(word.contains("_")) {
                    CompletableFuture.runAsync(() -> {
                        DatabaseControl.decreaseMoney(userID, 50);
                    }, executorService);
                }
                return "Букви: " + game.getWordTip(userID);
            }
        } else {
            return "В тебе недостатньо монет\uD83D\uDCB0";
        }
    }


    private boolean handleUserGuess(Game game, String userText, String sender, int messageID){
        if (game == null) {
            return false;
        }

        String wordToGuess = game.getWord();
        boolean isCorrectGuess = userText.equalsIgnoreCase(wordToGuess);
        boolean isNotCurrentPlayer = !sender.equals(game.getCurrentPlayerID());

        if (isCorrectGuess && isNotCurrentPlayer) {
            return true;
        }

        if (isCorrectGuess && sender.equals(game.getCurrentPlayerID())) {
            deleteMsg(game, messageID);
            sendMsg(game.getChatId(), "@" + game.getCurrentPlayer() + " підказувати заборонено. Вам нараховано штраф -10 монет\uD83D\uDCB0 .");

            CompletableFuture.runAsync(() -> {
                DatabaseControl.decreaseMoney(game.getCurrentPlayerID(), 10);
            } , executorService);
            return false;
        }

        return false;
    }

    private boolean deletePrevMenuMsg(Game game) {
        String prevMenuID = game.getPrevMenuMsgID();

        if (prevMenuID != null && game.getChatId() != null) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(game.getChatId());
            deleteMessage.setMessageId(Integer.valueOf(prevMenuID));

            CompletableFuture.runAsync(() -> {
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }, executorService);

            return true;
        }

        return false;
    }

    private boolean deleteMsg(Game game, int messageID){
        if(game != null){
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(game.getChatId());
            deleteMessage.setMessageId(Integer.valueOf(messageID));

            CompletableFuture.runAsync(() -> {
                try {
                    execute(deleteMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }, executorService);
            return true;
        }

        return false;
    }

    private boolean isPrivateChat(Message message) {
        return message.getChat().getType().equals("private");
    }

    private boolean isGroupChat(Message message) {
        return message.getChat().getType().equals("group");
    }

    private void sendGameMenu(String username, String name, String lastname, Game game) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(game.getChatId());
        if(username == null){
            sendMessage.setText("Слово пояснює: " + name + " " + lastname);
        }
        else{
            sendMessage.setText("Слово пояснює: @" + username);
        }

        InlineKeyboardMarkup keyboardMarkup = getInlineKeyboardMarkup();
        sendMessage.setReplyMarkup(keyboardMarkup);

        CompletableFuture.runAsync(() -> {
            try {
                Message sentMessage = execute(sendMessage);
                String messageID = String.valueOf(sentMessage.getMessageId());
                game.setPrevMenuMsgID(messageID);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }, executorService);
    }

    private static InlineKeyboardMarkup getInlineKeyboardMarkup() {
        InlineKeyboardButton seeWordBtn = new InlineKeyboardButton();
        seeWordBtn.setText("Подивитися слово\uD83D\uDD0D\uFE0E");
        seeWordBtn.setCallbackData("seeWordButtonCallBack");

        InlineKeyboardButton newWordBtn = new InlineKeyboardButton();
        newWordBtn.setText("Нове слово\uD83C\uDD95");
        newWordBtn.setCallbackData("newWordButtonCallBack");

        InlineKeyboardButton tipSeeWordBtn = new InlineKeyboardButton();
        tipSeeWordBtn.setText("Підглянути слово\uD83D\uDCDD");
        tipSeeWordBtn.setCallbackData("tipSeeWordButtonCallBackData");

        InlineKeyboardButton tipOpenLetterBtn = new InlineKeyboardButton();
        tipOpenLetterBtn.setText("Відкрити букву\uD83D\uDD20");
        tipOpenLetterBtn.setCallbackData("tipOpenLetterButtonCallBack");


        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(
                Arrays.asList(seeWordBtn, newWordBtn),
                Arrays.asList(tipSeeWordBtn, tipOpenLetterBtn)
        ));
        return keyboardMarkup;
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
    public String getBotToken() {
        return this.botToken;
    }

    public void setBotCommands(){
        List<BotCommand> commandList = new ArrayList<>();

        commandList.add(new BotCommand("/start", "Запуск бота"));
        commandList.add(new BotCommand("/start_raccoon_game", "Розпочати гру"));
        commandList.add(new BotCommand("/stop_raccoon_game", "Завершити гру"));
        commandList.add(new BotCommand("/top", "Топ 10 найкращих гравців"));

        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commandList);

        try {
            execute(setMyCommands);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void telegramBotInit(){
        this.words = WordLoader.loadWords();
        setBotCommands();
    }
}