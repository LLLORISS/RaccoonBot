package nm.rc;

import java.lang.reflect.Array;
import java.util.*;

public class Game {
    private String word;
    private String currentPlayer;
    private String currentPlayerID;
    private String chatID;
    private boolean isActive;
    private String prevMenuMsgID;
    private int countAnswers;

    private final Map<String, String> usersTips = new HashMap<>();

    public Game(String currentPlayer, String currentPlayerID, String chatID, String word){
        this.word = word;
        this.currentPlayer = currentPlayer;
        this.currentPlayerID = currentPlayerID;
        this.chatID = chatID;
        this.isActive = false;
        this.prevMenuMsgID = null;
        this.countAnswers = 0;
    }

    public void swapGameInfo(String userID,String newPlayer, String newWord){
        this.currentPlayer = newPlayer;
        this.word = newWord;
        this.currentPlayerID = userID;

        this.usersTips.clear();
    }

    public void setWord(String word){
        this.word = word;
    }

    public void setPrevMenuMsgID(String prevMenuMsgID){
        this.prevMenuMsgID = prevMenuMsgID;
    }

    public void increaseCountAnswers(){
        this.countAnswers++;
    }

    public String getPrevMenuMsgID(){
        return this.prevMenuMsgID;
    }

    public String getCurrentPlayer(){
        return this.currentPlayer;
    }

    public String getCurrentPlayerID(){
        return this.currentPlayerID;
    }

    public String getWord() {
        return word;
    }

    public String getChatId() {
        return chatID;
    }

    public int getCountAnswers(){
        return this.countAnswers;
    }

    String getWordTip(String userID) {
        int currentWordLength = this.word.length();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < currentWordLength; i++) {
            result.append("_");
        }

        String resultString;

        Random rand = new Random();

        if (usersTips.get(userID) == null) {
            int randomNumber = rand.nextInt(currentWordLength);

            result.setCharAt(randomNumber, this.word.charAt(randomNumber));

            resultString = result.toString();

            usersTips.put(userID, resultString);
        } else {
            String curWord = usersTips.get(userID);

            Set<Integer> indexes = new HashSet<>();

            for(int i = 0; i < currentWordLength; i++){
                if(curWord.charAt(i) != '_'){
                    indexes.add(i);
                }
            }
            int randomIndex;

            do {
                randomIndex = rand.nextInt(currentWordLength);
            } while (indexes.contains(randomIndex));

            result.setCharAt(randomIndex, this.word.charAt(randomIndex));
            resultString = result.toString();
            usersTips.put(userID, resultString);
        }

        return resultString;
    }
}