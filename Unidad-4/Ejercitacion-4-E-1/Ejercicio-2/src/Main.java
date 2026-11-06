import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    enum PlaybackStatus { PLAYING, PAUSED, STOPPED }

    record PlaybackState(String title, PlaybackStatus status) { }

    interface PlaybackObserver {
        void onPlaybackChanged(PlaybackState state);
    }

    static final class Player {
        private final List<PlaybackObserver> observers = new CopyOnWriteArrayList<>();
        private PlaybackState state = new PlaybackState("Sin canción", PlaybackStatus.STOPPED);

        void subscribe(PlaybackObserver observer) {
            observers.add(observer);
        }

        void unsubscribe(PlaybackObserver observer) {
            observers.remove(observer);
        }

        void play(String title) {
            change(title, PlaybackStatus.PLAYING);
        }

        void pause() {
            change(state.title(), PlaybackStatus.PAUSED);
        }

        void stop() {
            change(state.title(), PlaybackStatus.STOPPED);
        }

        private void change(String title, PlaybackStatus status) {
            state = new PlaybackState(title, status);
            System.out.printf("Reproductor: %s — %s%n", title, status);
            observers.forEach(observer -> observer.onPlaybackChanged(state));
        }
    }

    static final class PlaybackView implements PlaybackObserver {
        private final String name;

        PlaybackView(String name) {
            this.name = name;
        }

        @Override
        public void onPlaybackChanged(PlaybackState state) {
            System.out.printf("%s: %s — %s%n", name, state.title(), state.status());
        }
    }

    public static void main(String[] args) {
        Player player = new Player();
        player.subscribe(new PlaybackView("Barra inferior"));
        player.subscribe(new PlaybackView("Miniatura flotante"));
        player.subscribe(new PlaybackView("Widget"));
        player.play("Viento del sur");
        player.pause();
        player.stop();
    }
}
