package nm.rc;

public class Game {
    private String word;
    private String currentPlayer;
    private String currentPlayerID;
    private String chatID;
    private boolean isActive;

    public Game(String currentPlayer, String currentPlayerID, String chatID, String word){
        this.word = word;
        this.currentPlayer = currentPlayer;
        this.currentPlayerID = currentPlayerID;
        this.chatID = chatID;
        this.isActive = false;
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

    public void swapGameInfo(String newPlayer, String newWord){
        this.currentPlayer = newPlayer;
        this.word = newWord;
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

    public void setIsActive(boolean isActive){
        this.isActive = isActive;
    }
}