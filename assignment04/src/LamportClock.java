public class LamportClock {
    private int time = 0;

    public LamportClock() {
    }

    public LamportClock(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    // Local event increment by 1 - E.g. sending a message
    public void increment() {
        time++;
    }

    // Update the time to the maximum of the current time and the received time then
    // increment by 1
    public void receiveAction(int srcTime) {
        time = Math.max(time, srcTime);
        increment();
    }
}