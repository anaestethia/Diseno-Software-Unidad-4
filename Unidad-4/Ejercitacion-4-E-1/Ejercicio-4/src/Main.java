import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    record StockUpdate(String product, int remaining, int threshold) { }

    interface StockObserver {
        void onLowStock(StockUpdate update);
    }

    static final class Product {
        private final String name;
        private final int lowStockThreshold;
        private final List<StockObserver> observers = new CopyOnWriteArrayList<>();
        private int stock;

        Product(String name, int initialStock, int lowStockThreshold) {
            if (initialStock < 0 || lowStockThreshold < 0) {
                throw new IllegalArgumentException("El stock y el umbral no pueden ser negativos");
            }
            this.name = name;
            this.stock = initialStock;
            this.lowStockThreshold = lowStockThreshold;
        }

        void subscribe(StockObserver observer) {
            observers.add(observer);
        }

        void sell(int quantity) {
            if (quantity <= 0 || quantity > stock) {
                throw new IllegalArgumentException("Cantidad de venta inválida: " + quantity);
            }
            stock -= quantity;
            System.out.printf("Venta de %d unidades; %s queda en %d%n", quantity, name, stock);
            if (stock <= lowStockThreshold) {
                StockUpdate update = new StockUpdate(name, stock, lowStockThreshold);
                observers.forEach(observer -> observer.onLowStock(update));
            }
        }
    }

    static final class PurchasingDepartment implements StockObserver {
        @Override
        public void onLowStock(StockUpdate update) {
            if (update.remaining() <= update.threshold()) {
                System.out.printf("Compras: reponer %s (stock %d)%n",
                        update.product(), update.remaining());
            }
        }
    }

    static final class MarketingDepartment implements StockObserver {
        @Override
        public void onLowStock(StockUpdate update) {
            if (update.remaining() <= update.threshold()) {
                System.out.printf("Marketing: preparar campaña para %s%n", update.product());
            }
        }
    }

    public static void main(String[] args) {
        Product headphones = new Product("Auriculares", 8, 3);
        headphones.subscribe(new PurchasingDepartment());
        headphones.subscribe(new MarketingDepartment());
        headphones.sell(2);
        headphones.sell(3);
    }
}
