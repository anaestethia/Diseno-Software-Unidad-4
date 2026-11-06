import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    record Quote(String symbol, double previousPrice, double price, double changePercent) {
        static Quote create(String symbol, double previousPrice, double price) {
            double change = previousPrice == 0 ? 0 : (price - previousPrice) / previousPrice * 100;
            return new Quote(symbol, previousPrice, price, change);
        }
    }

    interface QuoteObserver {
        void onQuote(Quote quote);
    }

    static final class Market {
        private final List<QuoteObserver> subscribers = new CopyOnWriteArrayList<>();
        private double currentPrice;
        private final String symbol;

        Market(String symbol, double initialPrice) {
            if (initialPrice < 0) {
                throw new IllegalArgumentException("El precio no puede ser negativo");
            }
            this.symbol = symbol;
            this.currentPrice = initialPrice;
        }

        void subscribe(QuoteObserver observer) {
            subscribers.add(observer);
        }

        void unsubscribe(QuoteObserver observer) {
            subscribers.remove(observer);
        }

        void updatePrice(double price) {
            if (price < 0) {
                throw new IllegalArgumentException("El precio no puede ser negativo");
            }
            double previous = currentPrice;
            currentPrice = price;
            Quote quote = Quote.create(symbol, previous, price);
            System.out.printf("Mercado: %s $%.2f -> $%.2f (%+.2f%%)%n",
                    symbol, previous, price, quote.changePercent());
            subscribers.forEach(observer -> observer.onQuote(quote));
        }
    }

    static final class InvestorPanel implements QuoteObserver {
        private final String name;

        InvestorPanel(String name) {
            this.name = name;
        }

        @Override
        public void onQuote(Quote quote) {
            System.out.printf("%s: %s $%.2f (%+.2f%%)%n",
                    name, quote.symbol(), quote.price(), quote.changePercent());
        }
    }

    public static void main(String[] args) {
        Market market = new Market("ACME", 100.00);
        QuoteObserver chart = new InvestorPanel("Gráfico");
        market.subscribe(chart);
        market.subscribe(new InvestorPanel("Resumen"));
        market.subscribe(new InvestorPanel("Alerta de volatilidad"));
        market.updatePrice(103.50);
        market.updatePrice(97.29);
        market.unsubscribe(chart);
        market.updatePrice(98.00);
    }
}
