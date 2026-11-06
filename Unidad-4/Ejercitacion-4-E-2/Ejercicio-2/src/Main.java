import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) {
        NetworkMonitor monitor = new NetworkMonitor();
        monitor.addListener(new ToolbarIcon());
        monitor.addListener(new StatusBar());

        monitor.setStatus(NetworkStatus.CONECTADO);
        monitor.setStatus(NetworkStatus.INTERMITENTE);
        monitor.setStatus(NetworkStatus.DESCONECTADO);
    }
}

enum NetworkStatus {
    CONECTADO, DESCONECTADO, INTERMITENTE
}

interface NetworkStatusListener {
    void onStatusChanged(NetworkStatus status);
}

final class NetworkMonitor {
    private final List<NetworkStatusListener> listeners = new CopyOnWriteArrayList<>();
    private NetworkStatus status = NetworkStatus.DESCONECTADO;

    public void addListener(NetworkStatusListener listener) {
        listeners.add(listener);
    }

    public synchronized void setStatus(NetworkStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }
        status = newStatus;
        System.out.println("Monitor: " + status);
        for (NetworkStatusListener listener : listeners) {
            listener.onStatusChanged(status);
        }
    }
}

final class ToolbarIcon implements NetworkStatusListener {
    @Override
    public void onStatusChanged(NetworkStatus status) {
        String icon = status == NetworkStatus.CONECTADO ? "verde"
                : status == NetworkStatus.INTERMITENTE ? "amarillo" : "rojo";
        System.out.println("Icono de red: " + icon);
    }
}

final class StatusBar implements NetworkStatusListener {
    @Override
    public void onStatusChanged(NetworkStatus status) {
        String message = status == NetworkStatus.CONECTADO ? "Conectado"
                : status == NetworkStatus.INTERMITENTE ? "Conexión intermitente" : "Desconectado";
        System.out.println("Barra de estado: " + message);
    }
}
