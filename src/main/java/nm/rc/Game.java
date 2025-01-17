package nm.rc;

public class Game {
    private String word;
    private String currentPlayer;
    private String currentPlayerID;
    private String chatID;
    private boolean isActive;

    public Game(String currentPlayer, String chatID){
        this.word = "testWord";
        this.currentPlayer = currentPlayer;
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

    public void swapGameInfo(String currentPlayer){
        this.currentPlayer = currentPlayer;
        //RandomWord
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