package org.example.bean;

public class XAndYAndTimestampJavaBean {
    private double x;
    private double y;
    private long timestamp;

    public XAndYAndTimestampJavaBean(double x, double y, long timestamp) {
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "XAndYAndTimestampJavaBean{" +
                "x=" + x +
                ", y=" + y +
                ", timestamp=" + timestamp +
                '}';
    }
}

