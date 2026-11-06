import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    record TaskEvent(String taskId, String previousState, String currentState) { }

    interface TaskObserver {
        void onStateChanged(TaskEvent event);
    }

    static final class ProjectTask {
        private final String id;
        private final CopyOnWriteArrayList<TaskObserver> observers = new CopyOnWriteArrayList<>();
        private String state = "PENDIENTE";

        ProjectTask(String id) {
            this.id = id;
        }

        void subscribe(TaskObserver observer) {
            observers.addIfAbsent(observer);
        }

        void changeState(String newState) {
            if (newState == null || newState.isBlank()) {
                throw new IllegalArgumentException("El estado es obligatorio");
            }
            String previous = state;
            state = newState;
            System.out.printf("Tarea %s: %s -> %s%n", id, previous, state);
            TaskEvent event = new TaskEvent(id, previous, state);
            observers.forEach(observer -> observer.onStateChanged(event));
        }
    }

    static final class EmailNotifications implements TaskObserver {
        @Override
        public void onStateChanged(TaskEvent event) {
            System.out.printf("Correo: tarea %s cambió a %s%n",
                    event.taskId(), event.currentState());
        }
    }

    static final class Statistics implements TaskObserver {
        private int completedTasks;

        @Override
        public void onStateChanged(TaskEvent event) {
            if ("COMPLETADA".equals(event.currentState())) {
                completedTasks++;
            }
            System.out.printf("Estadísticas: tarea %s procesada; completadas=%d%n",
                    event.taskId(), completedTasks);
        }
    }

    public static void main(String[] args) {
        ProjectTask task = new ProjectTask("T-42");
        task.subscribe(new EmailNotifications());
        task.subscribe(new Statistics());
        task.changeState("EN_PROGRESO");
        task.changeState("COMPLETADA");
    }
}
