package com.weatherapp;

public class LamportClock {
    private int time;

    /**
     * Constructor for the LamportClock.
     * Initializes the clock to a starting time of 0.
     */
    public LamportClock() {
        time = 0;
    }

    /**
     * Retrieves the current time of the Lamport clock.
     *
     * @return the current time as an integer.
     */
    public synchronized int getTime() {
        return time;
    }

    /**
     * Increments the Lamport clock by 1.
     * This method is used to increment the logical clock when an internal event
     * occurs.
     *
     * @return the updated time after incrementing.
     */
    public synchronized void incrementTime() {
        time++;
    }

    /**
     * Updates the Lamport clock based on the received time from another process.
     * Compares the current clock time with the received time and sets the clock
     * to the maximum of both values, incrementing it by 1.
     *
     * @param receivedTime the time received from another process (must be
     *                     non-negative).
     * @return the updated time after synchronization.
     */
    public synchronized void updateClock(int receivedTime) {
        time = Math.max(time, receivedTime) + 1;
    }
}
