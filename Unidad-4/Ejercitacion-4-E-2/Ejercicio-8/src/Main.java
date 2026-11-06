import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) {
        InventoryItem item = new InventoryItem("SKU-100", 10);
        item.addListener(new BillingModule());
        item.addListener(new StatisticsModule());
        item.receiveStock(5);
        item.sell(3);
    }
}

interface StockListener {
    void onStockChanged(StockChange change);
}

final class StockChange {
    private final String sku;
    private final int previousQuantity;
    private final int currentQuantity;

    StockChange(String sku, int previousQuantity, int currentQuantity) {
        this.sku = sku;
        this.previousQuantity = previousQuantity;
        this.currentQuantity = currentQuantity;
    }

    public String getSku() { return sku; }
    public int getPreviousQuantity() { return previousQuantity; }
    public int getCurrentQuantity() { return currentQuantity; }
}

final class InventoryItem {
    private final String sku;
    private final List<StockListener> listeners = new CopyOnWriteArrayList<>();
    private int quantity;

    InventoryItem(String sku, int initialQuantity) {
        if (sku == null || sku.trim().isEmpty() || initialQuantity < 0) {
            throw new IllegalArgumentException("Artículo o stock inicial inválido");
        }
        this.sku = sku;
        this.quantity = initialQuantity;
    }

    public void addListener(StockListener listener) { listeners.add(listener); }

    public synchronized void receiveStock(int units) {
        if (units <= 0) throw new IllegalArgumentException("Las unidades deben ser positivas");
        changeQuantity(quantity + units);
    }

    public synchronized void sell(int units) {
        if (units <= 0 || units > quantity) {
            throw new IllegalArgumentException("Venta inválida o stock insuficiente");
        }
        changeQuantity(quantity - units);
    }

    private void changeQuantity(int newQuantity) {
        int previous = quantity;
        quantity = newQuantity;
        System.out.println("Inventario " + sku + ": " + previous + " -> " + quantity);
        StockChange event = new StockChange(sku, previous, quantity);
        for (StockListener listener : listeners) {
            listener.onStockChanged(event);
        }
    }
}

final class BillingModule implements StockListener {
    @Override
    public void onStockChanged(StockChange change) {
        System.out.println("Facturación: disponibilidad de " + change.getSku()
                + " = " + change.getCurrentQuantity());
    }
}

final class StatisticsModule implements StockListener {
    @Override
    public void onStockChanged(StockChange change) {
        System.out.println("Estadísticas: variación para " + change.getSku()
                + " = " + (change.getCurrentQuantity() - change.getPreviousQuantity()));
    }
}
