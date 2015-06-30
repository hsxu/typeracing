package com.example.hsxu.typeracing;

//http://stackoverflow.com/questions/8255738/is-there-a-stopwatch-in-java
public class Stopwatch {
    private long start;
    private boolean isStopwatchRunning = false;

    public Stopwatch() {
    }

    public void startStopwatch() {
        start = System.currentTimeMillis();
        isStopwatchRunning = true;
    }

    public void stopStopwatch() {
        isStopwatchRunning = false;
    }

    public double elapsedTime() {
        long now = System.currentTimeMillis();
        return (now - start) / 1000.0;
    }

    public boolean getIsStopWatchRunning() {
        return isStopwatchRunning;
    }

    public void setIsStopwatchRunning(boolean bool) {
        isStopwatchRunning = bool;
    }
}
