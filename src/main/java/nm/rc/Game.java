package nm.rc;

public class Game {
    private String word;
    private String currentPlayer;
    private String currentPlayerID;
    private String chatID;
    private boolean isActive;
    private String prevMenuMsgID;
    private int countAnswers;

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
    }

    public void setIsActive(boolean isActive){
        this.isActive = isActive;
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

    public boolean getIsActive(){
        return this.isActive;
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

    public String getPlayer() {
        return currentPlayer;
    }

    public String getChatId() {
        return chatID;
    }

    public int getCountAnswers(){
        return this.countAnswers;
    }
}