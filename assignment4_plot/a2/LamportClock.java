import java.io.*;

public class LamportClock {
    private int time;

    public LamportClock() {
        this.time = 0;
    }

    // Increment Lamport clock on internal events (PUT/GET)
    public synchronized void tick() {
        time++;
    }

     // Get the current time
     public synchronized int getTime() {
        return time;
    }


    public void setValue(int value) {
        time = value;
    }


    public synchronized void saveClock(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(Integer.toString(time));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void loadClock(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();
            if (line != null) {
                time = Integer.parseInt(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

}
