import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) {
        Product headphones = new Product("AUR-01", new BigDecimal("100.00"));
        Product keyboard = new Product("TEC-02", new BigDecimal("80.00"));
        headphones.addListener(new EmailChannel("ana@example.test"));
        headphones.addListener(new WebNotification("ana"));
        keyboard.addListener(new EmailChannel("luis@example.test"));

        headphones.updatePrice(new BigDecimal("75.00"));
        keyboard.updatePrice(new BigDecimal("85.00"));
    }
}

interface PriceDropListener {
    void onPriceDrop(PriceDrop drop);
}

final class PriceDrop {
    private final String productId;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;
    private final BigDecimal discountPercent;

    PriceDrop(String productId, BigDecimal oldPrice, BigDecimal newPrice) {
        this.productId = productId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.discountPercent = oldPrice.subtract(newPrice)
                .multiply(new BigDecimal("100"))
                .divide(oldPrice, 2, RoundingMode.HALF_UP);
    }

    public String getProductId() { return productId; }
    public BigDecimal getOldPrice() { return oldPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
}

final class Product {
    private final String id;
    private final List<PriceDropListener> listeners = new CopyOnWriteArrayList<>();
    private BigDecimal price;

    Product(String id, BigDecimal initialPrice) {
        if (id == null || id.trim().isEmpty() || initialPrice == null
                || initialPrice.signum() <= 0) {
            throw new IllegalArgumentException("Producto o precio inicial inválido");
        }
        this.id = id;
        this.price = initialPrice;
    }

    public void addListener(PriceDropListener listener) {
        listeners.add(listener);
    }

    public synchronized void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new IllegalArgumentException("El precio debe ser positivo");
        }
        BigDecimal oldPrice = price;
        price = newPrice;
        System.out.println(id + ": precio " + oldPrice + " -> " + newPrice);
        if (newPrice.compareTo(oldPrice) < 0) {
            PriceDrop drop = new PriceDrop(id, oldPrice, newPrice);
            for (PriceDropListener listener : listeners) {
                listener.onPriceDrop(drop);
            }
        }
    }
}

final class EmailChannel implements PriceDropListener {
    private final String address;
    EmailChannel(String address) { this.address = address; }

    @Override
    public void onPriceDrop(PriceDrop drop) {
        System.out.println("Correo simulado a " + address + ": " + drop.getProductId()
                + " bajó " + drop.getDiscountPercent() + "% ("
                + drop.getOldPrice() + " -> " + drop.getNewPrice() + ")");
    }
}

final class WebNotification implements PriceDropListener {
    private final String user;
    WebNotification(String user) { this.user = user; }

    @Override
    public void onPriceDrop(PriceDrop drop) {
        System.out.println("Panel de " + user + ": descuento de "
                + drop.getDiscountPercent() + "% en " + drop.getProductId());
    }
}
