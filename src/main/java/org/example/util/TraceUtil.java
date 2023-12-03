package org.example.util;

import org.example.bean.XAndYAndTimestampJavaBean;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class TraceUtil {

    // 计算两点间的距离
    public static double calculateDistance(double startX, double startY, double endX, double endY) {
        return Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
    }

    public static List<XAndYAndTimestampJavaBean> insertPoints(double x1, double y1, long timestamp1, double x2, double y2, double speed, long interval) {
        List<XAndYAndTimestampJavaBean> points = new ArrayList<>();

        Point2D.Double start = new Point2D.Double(x1, y1);
        Point2D.Double end = new Point2D.Double(x2, y2);

        // 计算两点间的距离
        double distance = start.distance(end);

        // 计算每步的距离
        double stepDistance = speed * interval / 1000.0;

        if (distance <= stepDistance) {
            points.add(new XAndYAndTimestampJavaBean(x1, y1, timestamp1));
            points.add(new XAndYAndTimestampJavaBean(x2, y2, timestamp1 + interval));
        } else {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double angle = Math.atan2(dy, dx);

            int steps = (int) (distance / stepDistance);
            for (int i = 0; i <= steps; i++) {
                double intermediateX = x1 + i * stepDistance * Math.cos(angle);
                double intermediateY = y1 + i * stepDistance * Math.sin(angle);
                long intermediateTimestamp = timestamp1 + i * interval;
                points.add(new XAndYAndTimestampJavaBean(intermediateX, intermediateY, intermediateTimestamp));
            }

            // 更新结束点的时间戳
            long finalTimestamp = points.get(points.size() - 1).getTimestamp() + interval;
            points.add(new XAndYAndTimestampJavaBean(x2, y2, finalTimestamp));
        }

        return points;
    }

    public static void main(String[] args) {
        double x1 = 12.3, y1 = 32.2;
        long timestamp1 = 1701531485000L;
        double x2 = 12.5, y2 = 34.2;
        double speed = 1.4; // 米/秒
        int interval = 200; // 毫秒

        List<XAndYAndTimestampJavaBean> result = insertPoints(x1, y1, timestamp1, x2, y2, speed, interval);
        for (XAndYAndTimestampJavaBean point : result) {
            System.out.println(point);
        }
    }

    public static boolean isStringEmpty(String text) {
        return text == null || text.trim().equals("");
    }
}
