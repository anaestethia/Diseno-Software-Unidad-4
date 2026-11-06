import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Scoreboard scoreboard = new Scoreboard();
        ScoreListener display = score -> System.out.println("Pantalla del juego: " + score + " puntos");
        ScoreListener records = score -> System.out.println("Récord provisional actualizado: " + score);

        scoreboard.subscribe(display);
        scoreboard.subscribe(records);
        scoreboard.addPoints(100);
        scoreboard.unsubscribe(records);
        scoreboard.addPoints(50);
        System.out.println("Récord al cerrar el nivel: " + scoreboard.getScore());
    }
}

interface ScoreListener {
    void onScoreChanged(int score);
}

final class Scoreboard {
    private final List<ScoreListener> listeners = new ArrayList<>();
    private int score;

    public void subscribe(ScoreListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(ScoreListener listener) {
        listeners.remove(listener);
    }

    public void addPoints(int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Los puntos deben ser positivos");
        }
        score += points;
        System.out.println("Puntuación acumulada: " + score);
        for (ScoreListener listener : new ArrayList<>(listeners)) {
            listener.onScoreChanged(score);
        }
    }

    public int getScore() {
        return score;
    }
}
