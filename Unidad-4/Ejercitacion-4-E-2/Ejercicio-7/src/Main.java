import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class Main {
    public static void main(String[] args) {
        StreamingPlatform platform = new StreamingPlatform();
        VideoChannel cooking = new VideoChannel("cocina");
        VideoChannel science = new VideoChannel("ciencia");
        platform.register(cooking);
        platform.register(science);

        User ana = new User("Ana");
        User ben = new User("Ben");
        platform.subscribe("cocina", ana);
        platform.subscribe("cocina", ben);
        platform.subscribe("ciencia", ben);
        platform.unsubscribe("cocina", ben);

        platform.publish(new VideoMetadata("cocina", "Pasta fresca"));
        platform.publish(new VideoMetadata("ciencia", "El sistema solar"));
    }
}

interface VideoListener {
    void onVideoPublished(VideoMetadata video);
}

final class VideoMetadata {
    private final String channelId;
    private final String title;

    VideoMetadata(String channelId, String title) {
        if (channelId == null || title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Metadatos de video inválidos");
        }
        this.channelId = channelId;
        this.title = title;
    }

    public String getChannelId() { return channelId; }
    public String getTitle() { return title; }
}

final class VideoChannel {
    private final String id;
    private final Set<VideoListener> followers = new CopyOnWriteArraySet<>();

    VideoChannel(String id) { this.id = id; }
    public String getId() { return id; }
    public void subscribe(VideoListener listener) { followers.add(listener); }
    public void unsubscribe(VideoListener listener) { followers.remove(listener); }

    public void publish(VideoMetadata video) {
        System.out.println("Canal " + id + " publica: " + video.getTitle());
        for (VideoListener follower : followers) {
            follower.onVideoPublished(video);
        }
    }
}

final class StreamingPlatform {
    private final java.util.Map<String, VideoChannel> channels = new ConcurrentHashMap<>();

    public void register(VideoChannel channel) {
        channels.put(channel.getId(), channel);
    }

    public void subscribe(String channelId, VideoListener listener) {
        getChannel(channelId).subscribe(listener);
    }

    public void unsubscribe(String channelId, VideoListener listener) {
        getChannel(channelId).unsubscribe(listener);
    }

    public void publish(VideoMetadata video) {
        getChannel(video.getChannelId()).publish(video);
    }

    private VideoChannel getChannel(String id) {
        VideoChannel channel = channels.get(id);
        if (channel == null) {
            throw new IllegalArgumentException("Canal inexistente: " + id);
        }
        return channel;
    }
}

final class User implements VideoListener {
    private final String name;
    User(String name) { this.name = name; }

    @Override
    public void onVideoPublished(VideoMetadata video) {
        System.out.println("Alerta emergente para " + name + ": nuevo video «"
                + video.getTitle() + "» en " + video.getChannelId());
    }
}
