package nm.rc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class WordLoader {

    private static Set<String> words = new HashSet<>();

    public static Set<String> loadWords() {
        if (words.isEmpty()) {
            try (InputStream inputStream = WordLoader.class.getClassLoader().getResourceAsStream("WordsList.txt");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] wordArray = line.split(";");
                    for (String word : wordArray) {
                        if (!word.trim().isEmpty()) {
                            words.add(word.trim());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return words;
    }

    public static int getWordsCount() {
        if (words.isEmpty()) {
            loadWords();
        }
        return words.size();
    }
}
