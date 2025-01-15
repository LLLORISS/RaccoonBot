package nm.rc;

public class Game {
    private String word;
    private String currentPlayer;
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

    public boolean getIsActive(){
        return this.isActive;
    }

    public void setWord(String word){
        this.word = word;
    }

    public void setCurrentPlayer(String currentPlayer){
        this.currentPlayer = currentPlayer;
    }

    public void setChatID(String chatID){
        this.chatID = chatID;
    }

    public void setIsActive(boolean isActive){
        this.isActive = isActive;
    }
}