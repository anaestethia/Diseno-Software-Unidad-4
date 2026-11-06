import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) {
        ExchangeMonitor monitor = new ExchangeMonitor();
        monitor.addListener(new TrendChart());
        monitor.addListener(new ConversionTable());
        monitor.addListener(new LimitAlarm());
        SelfRemovingPanel temporary = new SelfRemovingPanel(monitor);
        monitor.addListener(temporary);

        monitor.update("USD", new BigDecimal("1.10"));
        monitor.update("EUR", new BigDecimal("0.96"));
        monitor.update("USD", new BigDecimal("1.12"));
    }
}

interface QuoteListener {
    void onQuote(Quote quote);
}

final class Quote {
    private final String currency;
    private final BigDecimal previousRate;
    private final BigDecimal rate;
    private final BigDecimal changePercent;

    Quote(String currency, BigDecimal previousRate, BigDecimal rate) {
        this.currency = currency;
        this.previousRate = previousRate;
        this.rate = rate;
        this.changePercent = rate.subtract(previousRate)
                .multiply(new BigDecimal("100"))
                .divide(previousRate, 2, RoundingMode.HALF_UP);
    }

    public String getCurrency() { return currency; }
    public BigDecimal getPreviousRate() { return previousRate; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getChangePercent() { return changePercent; }
}

final class ExchangeMonitor {
    private final List<QuoteListener> listeners = new CopyOnWriteArrayList<>();
    private final java.util.Map<String, BigDecimal> rates = new java.util.HashMap<>();

    ExchangeMonitor() {
        rates.put("USD", new BigDecimal("1.00"));
        rates.put("EUR", new BigDecimal("0.95"));
    }

    public void addListener(QuoteListener listener) { listeners.add(listener); }
    public void removeListener(QuoteListener listener) { listeners.remove(listener); }

    public synchronized void update(String currency, BigDecimal newRate) {
        BigDecimal previous = rates.get(currency);
        if (previous == null || newRate == null || newRate.signum() <= 0) {
            throw new IllegalArgumentException("Divisa desconocida o cotización inválida");
        }
        rates.put(currency, newRate);
        Quote quote = new Quote(currency, previous, newRate);
        System.out.println("Cotización " + currency + ": " + previous + " -> " + newRate
                + " (" + quote.getChangePercent() + "%)");
        for (QuoteListener listener : listeners) {
            listener.onQuote(quote);
        }
    }
}

final class TrendChart implements QuoteListener {
    @Override
    public void onQuote(Quote quote) {
        System.out.println("Gráfico: punto " + quote.getCurrency() + " = " + quote.getRate());
    }
}

final class ConversionTable implements QuoteListener {
    @Override
    public void onQuote(Quote quote) {
        System.out.println("Tabla de conversión: " + quote.getCurrency() + " "
                + quote.getRate() + " (cambio " + quote.getChangePercent() + "%)");
    }
}

final class LimitAlarm implements QuoteListener {
    @Override
    public void onQuote(Quote quote) {
        if (quote.getRate().compareTo(new BigDecimal("1.11")) > 0) {
            System.out.println("Alarma: " + quote.getCurrency() + " superó el límite");
        }
    }
}

final class SelfRemovingPanel implements QuoteListener {
    private final ExchangeMonitor monitor;
    SelfRemovingPanel(ExchangeMonitor monitor) { this.monitor = monitor; }

    @Override
    public void onQuote(Quote quote) {
        System.out.println("Panel temporal: " + quote.getCurrency() + " recibido; se desuscribe");
        monitor.removeListener(this);
    }
}
