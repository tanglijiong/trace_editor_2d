package org.example;

import org.example.bean.PointInfo;
import org.example.bean.TraceLayer;
import org.example.bean.XAndYAndTimestampJavaBean;
import org.example.component.ImagePanel;
import org.example.util.TraceUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class MainJFrame extends JFrame {
    private static final BasicStroke LINE_STROKE = new BasicStroke(1);
    private static final int radius = 5; // 圆点的直径
    private BufferedImage image;
    private final JTextArea textArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JTextField scaleField = new JTextField("16.31");
    private final JTextField currentLayerNameField = new JTextField("default", 6);
    private final Map<String, TraceLayer> pointsMapKeyByLayerName = new HashMap<>();

    private final JTextField currentTimestampField = new JTextField("0");
    // 在timestampPanel中创建一个JCheckBox
    private final JTextField timestampIntervalField = new JTextField("200");
    private final JCheckBox autoUpdateBox = new JCheckBox("自动步长", true);
    private final JComboBox<String> currentLayerColorBox = new JComboBox<>(new String[]{"red", "green", "blue",
            "black", "cyan", "magenta", "orange", "pink"});

    // 将按钮替换为复选框
    private final JCheckBox connectCheckBox = new JCheckBox("连线", true);
    private final JCheckBox showFontCheckBox = new JCheckBox("文字", true);
    private final JTextField searchField = new JTextField("0,0", 10);
    private final JComboBox<String> searchTypeBox = new JComboBox<>(new String[]{"geo", "pixel", "timestamp"});

    private final JTextField markTextField = new JTextField("", 10);

    private final JButton searchAndMarkButton = new JButton("查找并标记");
    private final JButton markClearButton = new JButton("清除标记");
    private final JButton deletePointButton = new JButton("删除点");
    // 类成员变量
    private final JCheckBox uniformSpeedCheckBox = new JCheckBox("匀速插点", true);
    private final JTextField speedField = new JTextField("1.4"); // 默认步行速度 1.4m/s

    private final JTextField importFilePathField = new JTextField("/Users/tanglijiong/test/input.txt", 20);
    private final JTextField exportFilePathField = new JTextField("/Users/tanglijiong/test/output.txt", 20);
    private final JComboBox<String> importTypeJComboBox = new JComboBox<>(new String[]{"geo", "pixel"});
    private final JComboBox<String> exportTypeJComboBox = new JComboBox<>(new String[]{"geo", "pixel"});

    public MainJFrame() {
        super("Trace Editor 2D");
        try {
            // 默认地图
            image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/map.png")));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        ImagePanel panel = new ImagePanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2d = (Graphics2D) graphics;
                super.paintComponent(g2d);
                g2d.drawImage(image, 0, 0, this);

                for (String layerName : pointsMapKeyByLayerName.keySet()) {
                    TraceLayer traceLayer = pointsMapKeyByLayerName.get(layerName);
                    SortedSet<PointInfo> points = traceLayer.pointInfos;

                    // Draw the points
                    g2d.setColor(COLOR_MAP.getOrDefault(traceLayer.colorNane, Color.red));
                    for (PointInfo pointInfo : points) {
                        int x = pointInfo.point.x - radius / 2;
                        int y = pointInfo.point.y - radius / 2;
                        // 绘制圆点
                        g2d.fill(new Ellipse2D.Double(x, y, radius, radius));
                        // 绘制标志点和文字
                        if (pointInfo.hasMark) {
                            int radiusRemark = radius * 2;
                            x = pointInfo.point.x - radiusRemark / 2;
                            y = pointInfo.point.y - radiusRemark / 2;
                            g2d.fill(new Ellipse2D.Double(x, y, radiusRemark, radiusRemark));
                            if(showFontCheckBox.isSelected()){
                                g2d.drawString(pointInfo.markText, x, y);
                            }

                        }
                    }

                    // Draw lines between points
                    if (connectCheckBox.isSelected() && points.size() > 1) {
                        List<PointInfo> pointList = new ArrayList<>(points);
                        for (int i = 0; i < points.size() - 1; i++) {
                            PointInfo p1 = pointList.get(i);
                            PointInfo p2 = pointList.get(i + 1);
                            Stroke stroke = g2d.getStroke();
                            g2d.setStroke(LINE_STROKE);
                            g2d.drawLine(p1.point.x, p1.point.y, p2.point.x, p2.point.y);
                            g2d.setStroke(stroke);
                        }
                    }
                }

            }
        };

        panel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double scaledX = convertPixelToGeo(e.getPoint().x);
                double scaledY = convertPixelToGeo(e.getPoint().y);
                TraceLayer traceLayer = fetchCurrentLayerTrace();
                SortedSet<PointInfo> points = traceLayer.pointInfos;
                if (!points.isEmpty()) {
                    PointInfo last = points.last();
                    textArea.setText("Clicked at: pixel:(" + e.getX() + ", " + e.getY() + "),geo:" + scaledX + ","
                            + scaledY + ",distance:" + TraceUtil.calculateDistance(last.geoX, last.geoY, scaledX, scaledY));
                } else {
                    textArea.setText("Clicked at: pixel:(" + e.getX() + ", " + e.getY() + "),geo:" + scaledX + ","
                            + scaledY);
                }

            }
        });

        // 在鼠标点击事件的处理器中，检查JCheckBox的状态
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TraceLayer traceLayer = fetchCurrentLayerTrace();
                SortedSet<PointInfo> points = traceLayer.pointInfos;
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // 处理自动更新时间戳
                    if (autoUpdateBox.isSelected()) {
                        double timestamp = Double.parseDouble(currentTimestampField.getText());
                        double step = Double.parseDouble(timestampIntervalField.getText());
                        timestamp += step;
                        currentTimestampField.setText(String.valueOf(timestamp));
                    }
                    // 处理匀速模式下的点插入
                    if (uniformSpeedCheckBox.isSelected()) {
                        // 获取最后一个点的坐标
                        PointInfo lastPoint = points.isEmpty() ? null : points.last();
                        // 如果有上一个点，则计算并插入中间点
                        if (lastPoint != null) {
                            Point currentPoint = e.getPoint();
                            double currentX = convertPixelToGeo(currentPoint.x);
                            double currentY = convertPixelToGeo(currentPoint.y);
                            List<XAndYAndTimestampJavaBean> xAndYAndTimestampJavaBeans = TraceUtil.insertPoints(lastPoint.geoX,
                                    lastPoint.geoY, lastPoint.timestamp, currentX, currentY, Double.parseDouble(speedField.getText()),
                                    Long.parseLong(timestampIntervalField.getText()));
                            for (int i = 1; i < xAndYAndTimestampJavaBeans.size(); i++) {
                                XAndYAndTimestampJavaBean it = xAndYAndTimestampJavaBeans.get(i);
                                points.add(new PointInfo(new Point(convertGeoToPixel(it.getX()), convertGeoToPixel(it.getY())),
                                        it.getX(), it.getY(), it.getTimestamp()));
                            }
                        }
                    }

                    // 添加新点击的点
                    Graphics g = panel.getGraphics();
                    g.setColor(Color.RED);
                    double scaledX = convertPixelToGeo(e.getPoint().x);
                    double scaledY = convertPixelToGeo(e.getPoint().y);
                    String logText = "pixel xy:(" + e.getX() + ", " + e.getY() + "),geo xy: " + scaledX + ", " + scaledY + ",timestamp:" + currentTimestampField.getText();
                    logArea.append(logText + "\n");
                    long timestamp = Math.round(Double.parseDouble(currentTimestampField.getText()));
                    points.add(new PointInfo(e.getPoint(), scaledX, scaledY, timestamp));
                    panel.repaint();
                }

                // 右键点击移除点
                if (SwingUtilities.isRightMouseButton(e)) {
                    points.removeIf(pointInfo -> pointInfo.point.distance(e.getPoint()) < radius);
                    panel.repaint();
                }
            }
        });

        JScrollPane imageScroll = new JScrollPane(panel);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel scaleLabel = new JLabel("比例尺");
        scaleLabel.setPreferredSize(new Dimension(50, 20)); // Adjust width and height as per your requirements
        scaleField.setPreferredSize(new Dimension(50, 20)); // Adjust width and height as per your requirements

        JLabel coordinateLabel = new JLabel("geo xy:(x,y): ");
        coordinateLabel.setPreferredSize(new Dimension(100, 20)); // Adjust width and height as per your requirements
        JTextField coordinateField = new JTextField();
        coordinateField.setPreferredSize(new Dimension(100, 20)); // Adjust width and height as per your requirements
        JButton convertButton = new JButton("坐标渲染");

        convertButton.addActionListener(e -> {
            String[] xy = coordinateField.getText().split(",");
            double x = Double.parseDouble(xy[0]);
            double y = Double.parseDouble(xy[1]);
            int scaledX = convertGeoToPixel(x);
            int scaledY = convertGeoToPixel(y);
            String logText = "pixel xy: (" + scaledX + ", " + scaledY + "),geo xy:: " + x + ", " + y + "timestamp:" + currentTimestampField.getText();
            logArea.append(logText + "\n");
            long timestamp = Math.round(Double.parseDouble(currentTimestampField.getText()));
            TraceLayer traceLayer = fetchCurrentLayerTrace();
            SortedSet<PointInfo> points = traceLayer.pointInfos;
            points.add(new PointInfo(new Point(scaledX, scaledY), x, y, timestamp));
            panel.repaint();
        });

        JLabel timestmapLabel = new JLabel("当前点时间戳");
        timestmapLabel.setPreferredSize(new Dimension(80, 20)); // Adjust width and height as per your requirements
        currentTimestampField.setPreferredSize(new Dimension(50, 20));


        timestampIntervalField.setPreferredSize(new Dimension(50, 20));
        JButton addNumButton = new JButton("手动步长");
        addNumButton.addActionListener(e -> {
            double timestamp = Double.parseDouble(currentTimestampField.getText());
            double step = Double.parseDouble(timestampIntervalField.getText());
            timestamp += step;
            currentTimestampField.setText(String.valueOf(timestamp));
        });

        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectPanel.setPreferredSize(new Dimension(60, 30));
        // 设置复选框的初始状态和事件监听器
        connectCheckBox.addItemListener(e -> panel.repaint());
        showFontCheckBox.addItemListener(e-> panel.repaint());
        JButton clearTraceBtn = new JButton("清理当前");
        clearTraceBtn.addActionListener(e -> {
            TraceLayer traceLayer = fetchCurrentLayerTrace();
            SortedSet<PointInfo> points = traceLayer.pointInfos;
            points.clear();
            currentTimestampField.setText("0");
            panel.repaint();
        });
        JButton clearAllTraceBtn = new JButton("清理所有");
        clearAllTraceBtn.addActionListener(e -> {
            currentTimestampField.setText("0");
            pointsMapKeyByLayerName.clear();
            panel.repaint();
        });
        JButton viewAllTraceBtn = new JButton("预览所有轨迹");
        viewAllTraceBtn.addActionListener(e -> {
            logArea.append("轨迹列表概览：\n");
            for (Map.Entry<String, TraceLayer> stringTraceLayerEntry : pointsMapKeyByLayerName.entrySet()) {
                logArea.append("轨迹名称:" + stringTraceLayerEntry.getKey() + ",颜色：" + stringTraceLayerEntry.getValue().colorNane
                        + "，轨迹点数：" + stringTraceLayerEntry.getValue().pointInfos.size() + "\n");
            }

        });
        currentLayerColorBox.addItemListener(e -> {
            fetchCurrentLayerTrace().colorNane = e.getItem().toString();
            panel.repaint();
        });
        textPanel.add(addJPanelLine(scaleLabel, scaleField, clearAllTraceBtn, viewAllTraceBtn));
        textPanel.add(addJPanelLine(new JLabel("当前轨迹"), currentLayerNameField, currentLayerColorBox,
                clearTraceBtn));
        textPanel.add(addJPanelLine(coordinateLabel, coordinateField, convertButton));
        textPanel.add(addJPanelLine(timestmapLabel, currentTimestampField, addNumButton, timestampIntervalField, autoUpdateBox));
        textPanel.add(addJPanelLine(connectPanel, connectCheckBox, showFontCheckBox));

        textPanel.add(addJPanelLine(new JLabel("查找单个点:"), searchField, searchTypeBox, new JLabel("标识文本:"),
                markTextField));
        textPanel.add(addJPanelLine(searchAndMarkButton, markClearButton, deletePointButton));

        searchAndMarkButton.addActionListener(e -> {
            searchPoint().ifPresent(it -> {
                it.hasMark = true;
                it.markText = TraceUtil.isStringEmpty(markTextField.getText()) ? "" : markTextField.getText();
            });
            panel.repaint();
        });
        markClearButton.addActionListener(e -> {
            TraceLayer traceLayer = fetchCurrentLayerTrace();
            for (PointInfo pointInfo : traceLayer.pointInfos) {
                pointInfo.hasMark = false;
                pointInfo.markText = "";
            }
            panel.repaint();
        });
        deletePointButton.addActionListener(e -> {
            TraceLayer traceLayer = fetchCurrentLayerTrace();
            searchPoint().ifPresent(it -> {
                if (it.hasMark) {
                    traceLayer.pointInfos.remove(it);
                    panel.repaint();
                }
            });
        });

        textPanel.add(addJPanelLine(new JLabel("移动速度(m/s):"), speedField, uniformSpeedCheckBox));

        textPanel.add(addJPanelLine(new JLabel("导入类型："), importTypeJComboBox));
        JButton importButton = new JButton("导入");
        textPanel.add(addJPanelLine(new JLabel("导入文件路径:"), importFilePathField, importButton));
        textPanel.add(addJPanelLine(new JLabel("导出类型："), exportTypeJComboBox));
        JButton exportButton = new JButton("导出");
        textPanel.add(addJPanelLine(new JLabel("导出文件路径:"), exportFilePathField, exportButton));
        // 设置导入按钮的事件监听器
        importButton.addActionListener(e -> importPoints(importFilePathField.getText(), panel));
        // 设置导出按钮的事件监听器
        exportButton.addActionListener(e -> exportPoints(exportFilePathField.getText()));


        textPanel.add(Box.createVerticalStrut(20));  // 创建一个高度为10的垂直空白空间
        textPanel.add(textArea);


        JScrollPane logScroll = new JScrollPane(logArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imageScroll, textPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(500);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane, logScroll);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setDividerLocation(700);

        setContentPane(mainSplitPane);
        pack();
        setSize(1200, 800); // 设置窗口的初始大小为 1200x800 像素
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);


    }

    private Optional<PointInfo> searchPoint() {
        String searchCondition = searchField.getText();
        String searchType = searchTypeBox.getSelectedItem().toString();
        TraceLayer traceLayer = fetchCurrentLayerTrace();
        SortedSet<PointInfo> pointInfos = traceLayer.pointInfos;

        if (searchType.equals("geo")) {
            String[] xyStr = searchCondition.split(",");
            double[] xy = convertToDoubleArr(xyStr);
            return searchPointByGeoXy(xy, pointInfos);
        } else if (searchType.equals("pixel")) {
            String[] xyStr = searchCondition.split(",");
            int[] xy = convertToIntArr(xyStr);
            return searchPointByPixelXy(xy, pointInfos);

        } else if (searchType.equals("timestamp")) {
            long timestampSearch = Long.parseLong(searchCondition);
            return searchPointByTimestamp(timestampSearch, pointInfos);
        }
        return Optional.empty();
    }

    private Optional<PointInfo> searchPointByTimestamp(long timestampSearch, SortedSet<PointInfo> pointInfos) {
        PointInfo pointInfoSelected = null;
        long distanceMin = Long.MAX_VALUE;
        for (PointInfo pointInfo : pointInfos) {
            long dis = Math.abs(pointInfo.timestamp - timestampSearch);
            if (dis < distanceMin) {
                distanceMin = dis;
                pointInfoSelected = pointInfo;
            }
        }
        return Optional.ofNullable(pointInfoSelected);
    }

    private Optional<PointInfo> searchPointByPixelXy(int[] xy, SortedSet<PointInfo> pointInfos) {
        PointInfo pointInfoSelected = null;
        double distanceMin = Double.MAX_VALUE;
        for (PointInfo pointInfo : pointInfos) {
            double dis = TraceUtil.calculateDistance(xy[0], xy[1], pointInfo.point.x, pointInfo.point.y);
            if (dis < distanceMin) {
                distanceMin = dis;
                pointInfoSelected = pointInfo;
            }
        }
        return Optional.ofNullable(pointInfoSelected);
    }

    private int[] convertToIntArr(String[] xyStr) {
        return new int[]{Integer.parseInt(xyStr[0]), Integer.parseInt(xyStr[1])};
    }

    private Optional<PointInfo> searchPointByGeoXy(double[] xy, SortedSet<PointInfo> pointInfos) {
        PointInfo pointInfoSelected = null;
        double distanceMin = Double.MAX_VALUE;
        for (PointInfo pointInfo : pointInfos) {
            double dis = TraceUtil.calculateDistance(xy[0], xy[1], pointInfo.geoX, pointInfo.geoY);
            if (dis < distanceMin) {
                distanceMin = dis;
                pointInfoSelected = pointInfo;
            }
        }
        return Optional.ofNullable(pointInfoSelected);
    }

    private double[] convertToDoubleArr(String[] xyStr) {
        return new double[]{
                Double.parseDouble(xyStr[0]), Double.parseDouble(xyStr[1])
        };
    }

    private static Map<String, Color> COLOR_MAP = new HashMap();

    static {
        COLOR_MAP.put("red", Color.red);
        COLOR_MAP.put("green", Color.green);
        COLOR_MAP.put("blue", Color.blue);
        COLOR_MAP.put("black", Color.black);
        COLOR_MAP.put("cyan", Color.cyan);
        COLOR_MAP.put("magenta", Color.magenta);
        COLOR_MAP.put("orange", Color.orange);
        COLOR_MAP.put("pink", Color.pink);
    }

    ;

    private TraceLayer fetchCurrentLayerTrace() {
        String layerName = currentLayerNameField.getText();
        String colorName = Objects.requireNonNull(currentLayerColorBox.getSelectedItem()).toString();
        return pointsMapKeyByLayerName.computeIfAbsent(layerName, key -> new TraceLayer(colorName));
    }

    private JPanel addJPanelLine(Component... swingConstants) {
        JPanel jPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        return addJPanelLine(jPanel, swingConstants);

    }

    private JPanel addJPanelLine(JPanel jPanel, Component... swingConstants) {
        for (Component component : swingConstants) {
            jPanel.add(component);
        }
        return jPanel;

    }

    // 导入点的方法
    private void importPoints(String filePath, ImagePanel panel) {
        // 清空现有点
        TraceLayer traceLayer = fetchCurrentLayerTrace();
        SortedSet<PointInfo> points = traceLayer.pointInfos;
        points.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            logArea.append("导入数据：\n");
            while ((line = reader.readLine()) != null) {
                logArea.append(line + "\n");
                String[] parts = line.split(",");

                PointInfo pointInfo;
                if (importTypeJComboBox.getSelectedItem().equals("geo")) {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    long timestamp = Long.parseLong(parts[2]);

                    pointInfo = new PointInfo(new Point(convertGeoToPixel(x), convertGeoToPixel(y)), x, y, timestamp);
                } else if (importTypeJComboBox.getSelectedItem().equals("pixel")) {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    long timestamp = Long.parseLong(parts[2]);
                    pointInfo = new PointInfo(new Point(x, y), convertPixelToGeo(x), convertPixelToGeo(y), timestamp);
                } else {
                    throw new RuntimeException("导入数据格式出错！");
                }

                points.add(pointInfo);

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // 重绘面板以显示新点
        panel.repaint();
    }

    private int convertGeoToPixel(double geoValue) {
        double scale = Double.parseDouble(scaleField.getText());
        return (int) (geoValue * scale);
    }

    private double convertPixelToGeo(int intValue) {
        double scale = Double.parseDouble(scaleField.getText());
        return intValue / scale;
    }

    // 导出点的方法
    private void exportPoints(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            logArea.append("导出数据：\n");
            TraceLayer traceLayer = fetchCurrentLayerTrace();
            SortedSet<PointInfo> points = traceLayer.pointInfos;
            for (PointInfo pointInfo : points) {
                String line;
                if (exportTypeJComboBox.getSelectedItem().equals("geo")) {
                    line = pointInfo.geoX + "," + pointInfo.geoY + "," + pointInfo.timestamp;
                } else if (exportTypeJComboBox.getSelectedItem().equals("pixel")) {
                    line = pointInfo.point.x + "," + pointInfo.point.y + "," + pointInfo.timestamp;
                } else {
                    throw new RuntimeException("导出类型出错！");
                }
                logArea.append(line + "\n");
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainJFrame::new);
    }
}




