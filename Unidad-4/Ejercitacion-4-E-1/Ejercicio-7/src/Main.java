import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    enum Category { TECNOLOGIA, DEPORTES, ECONOMIA }

    record NewsArticle(String title, Category category, String author) { }

    interface NewsObserver {
        void onArticle(NewsArticle article);
    }

    static final class NewsPortal {
        private final Map<Category, CopyOnWriteArrayList<NewsObserver>> channels =
                new ConcurrentHashMap<>();

        void subscribe(Category category, NewsObserver reader) {
            channels.computeIfAbsent(category, ignored -> new CopyOnWriteArrayList<>()).addIfAbsent(reader);
        }

        void unsubscribe(Category category, NewsObserver reader) {
            List<NewsObserver> readers = channels.get(category);
            if (readers != null) {
                readers.remove(reader);
            }
        }

        void publish(NewsArticle article) {
            System.out.printf("Publicado [%s]: %s%n", article.category(), article.title());
            List<NewsObserver> readers = channels.get(article.category());
            if (readers != null) {
                readers.forEach(reader -> reader.onArticle(article));
            }
        }
    }

    static final class Reader implements NewsObserver {
        private final String name;

        Reader(String name) {
            this.name = name;
        }

        @Override
        public void onArticle(NewsArticle article) {
            System.out.printf("Aviso para %s: %s (%s)%n",
                    name, article.title(), article.category());
        }
    }

    public static void main(String[] args) {
        NewsPortal portal = new NewsPortal();
        Reader ana = new Reader("Ana");
        Reader bruno = new Reader("Bruno");
        Reader carla = new Reader("Carla");
        portal.subscribe(Category.TECNOLOGIA, ana);
        portal.subscribe(Category.ECONOMIA, ana);
        portal.subscribe(Category.DEPORTES, bruno);
        portal.subscribe(Category.TECNOLOGIA, carla);
        portal.publish(new NewsArticle("Nueva red de fibra", Category.TECNOLOGIA, "Redacción"));
        portal.unsubscribe(Category.TECNOLOGIA, carla);
        portal.publish(new NewsArticle("Mercados al cierre", Category.ECONOMIA, "Redacción"));
    }
}
