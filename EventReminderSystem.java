import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.SwingUtilities;

class Event {
    private int id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime dateTime;

    public Event(int id, String title, String description, String location, LocalDateTime dateTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.dateTime = dateTime;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDateTime getDateTime() { return dateTime; }

    public String getStatus() {
        return dateTime.isBefore(LocalDateTime.now()) ? "PASSED" : "UPCOMING";
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return id + " | " + dateTime.format(fmt) + " | " + getStatus() + " | " 
               + title + " | " + description + " | " + location;
    }
}

class EventManager {
    private List<Event> events = new ArrayList<>();
    private int nextId = 1;

    public void addEvent(String title, String description, String location, LocalDateTime dateTime) {
        events.add(new Event(nextId++, title, description, location, dateTime));
    }

    public List<Event> getAllEvents() {
        return events;
    }

    public List<Event> getUpcomingEvents() {
        List<Event> res = new ArrayList<>();
        for (Event e : events) {
            if (e.getDateTime().isAfter(LocalDateTime.now())) res.add(e);
        }
        return res;
    }

    public List<Event> getPassedEvents() {
        List<Event> res = new ArrayList<>();
        for (Event e : events) {
            if (e.getDateTime().isBefore(LocalDateTime.now())) res.add(e);
        }
        return res;
    }

    public boolean deleteEvent(int id) {
        return events.removeIf(e -> e.getId() == id);
    }

    public void saveToFile(String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Event e : events) {
                pw.println(e.getId() + ";" + e.getTitle() + ";" + e.getDescription() + ";" +
                        e.getLocation() + ";" + e.getDateTime().format(fmt));
            }
        } catch (IOException ex) {
            System.out.println("Error saving file: " + ex.getMessage());
        }
    }

    public void loadFromFile(String filename) {
        File f = new File(filename);
        if (!f.exists()) return;
        try (Scanner sc = new Scanner(f)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            while (sc.hasNextLine()) {
                String[] parts = sc.nextLine().split(";");
                if (parts.length == 5) {
                    int id = Integer.parseInt(parts[0]);
                    String title = parts[1];
                    String desc = parts[2];
                    String loc = parts[3];
                    LocalDateTime dt = LocalDateTime.parse(parts[4], fmt);
                    events.add(new Event(id, title, desc, loc, dt));
                    if (id >= nextId) nextId = id + 1;
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading file: " + ex.getMessage());
        }
    }
}

public class EventReminderSystem {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        EventManager manager = new EventManager();
        manager.loadFromFile("events.txt");

        // keep track of already reminded events
        Set<Integer> reminded = Collections.synchronizedSet(new HashSet<>());

        // Reminder thread
        new Thread(() -> {
            while (true) {
                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                for (Event e : manager.getAllEvents()) {
                    if (e.getDateTime().withSecond(0).withNano(0).equals(now) 
                        && !reminded.contains(e.getId())) {
                        reminded.add(e.getId()); // mark as reminded

                        System.out.println("\n🔔 Reminder: " + e.getTitle() + " is happening now!");

                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null,
                                    "Reminder: " + e.getTitle() + " is happening now!",
                                    "Event Reminder", JOptionPane.INFORMATION_MESSAGE);
                        });
                    }
                }
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }).start();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1) Add event");
            System.out.println("2) View all events");
            System.out.println("3) View upcoming events");
            System.out.println("4) View passed events");
            System.out.println("5) Delete event");
            System.out.println("6) Exit");
            System.out.print("Choose: ");

            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (Exception ex) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            if (choice == 1) {
                try {
                    System.out.print("Enter title: ");
                    String title = sc.nextLine();
                    System.out.print("Enter description: ");
                    String desc = sc.nextLine();
                    System.out.print("Enter location: ");
                    String loc = sc.nextLine();
                    System.out.print("Enter date and time (yyyy-MM-dd HH:mm): ");
                    String dt = sc.nextLine();
                    LocalDateTime dateTime = LocalDateTime.parse(dt, fmt);
                    manager.addEvent(title, desc, loc, dateTime);
                    manager.saveToFile("events.txt");
                } catch (Exception ex) {
                    System.out.println("Invalid date/time format. Use yyyy-MM-dd HH:mm");
                }
            } else if (choice == 2) {
                for (Event e : manager.getAllEvents()) System.out.println(e);
            } else if (choice == 3) {
                for (Event e : manager.getUpcomingEvents()) System.out.println(e);
            } else if (choice == 4) {
                for (Event e : manager.getPassedEvents()) System.out.println(e);
            } else if (choice == 5) {
                for (Event e : manager.getAllEvents()) System.out.println(e);
                System.out.print("Enter event ID to delete: ");
                try {
                    int id = Integer.parseInt(sc.nextLine());
                    if (manager.deleteEvent(id)) {
                        manager.saveToFile("events.txt");
                        System.out.println("Event deleted.");
                    } else {
                        System.out.println("No event found with that ID.");
                    }
                } catch (Exception ex) {
                    System.out.println("Invalid ID.");
                }
            } else if (choice == 6) {
                manager.saveToFile("events.txt");
                System.out.println("Exiting...");
                break;
            }
        }
    }
}
