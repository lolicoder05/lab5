package bsu.rfe.java.group8.lab5.Chernov.var5;


import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;


@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel implements MouseMotionListener, MouseListener {

    // Список координат точек для построения графика
    private Double[][] graphicsData;

    // Флаговые переменные, задающие правила отображения графика
    private boolean showAxis = true;
    private boolean showMarkers = true;

    // Границы диапазона пространства, подлежащего отображению
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    // Используемый масштаб отображения
    private double scale;

    // Различные стили черчения линий
    private BasicStroke graphicsStroke;
    private BasicStroke axisStroke;
    private BasicStroke markerStroke;
    private BasicStroke selectionStroke;

    // Различные шрифты отображения надписей
    private Font axisFont;
    private Point2D.Double selectionStart;
    private Point2D.Double selectionEnd;
    private boolean scalingMode = false; // Режим выделения
    private Double[] selectedPoint = null; // Текущая точка под курсором


    public GraphicsDisplay() {
        // Цвет заднего фона области отображения - белый
        setBackground(Color.WHITE);
        // Сконструировать необходимые объекты, используемые в рисовании
        // Перо для рисования графика
        graphicsStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 10.0f, null, 0.0f);
        // Перо для рисования осей координат
        axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        // Перо для рисования контуров маркеров
        markerStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        selectionStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f);
        // Шрифт для подписей осей координат
        axisFont = new Font("Serif", Font.BOLD, 36);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    // Данный метод вызывается из обработчика элемента меню "Открыть файл с графиком"
    // главного окна приложения в случае успешной загрузки данных
    public void showGraphics(Double[][] graphicsData) {
        // Сохранить массив точек во внутреннем поле класса
        this.graphicsData = graphicsData;
        // Запросить перерисовку компонента, т.е. неявно вызвать paintComponent()
        repaint();
    }

    // Методы-модификаторы для изменения параметров отображения графика
    // Изменение любого параметра приводит к перерисовке области
    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    // Метод отображения всего компонента, содержащего график
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Если данные графика отсутствуют, ничего не делать
        if (graphicsData == null || graphicsData.length == 0) return;

        // Определяем масштабы по осям X и Y
        double scaleX = getSize().getWidth() / (maxX - minX);
        double scaleY = getSize().getHeight() / (maxY - minY);

        // Чтобы изображение было неискажённым, масштаб должен быть одинаков
        scale = Math.min(scaleX, scaleY);

        // Корректируем границы отображаемой области
        if (scale == scaleX) {
            double yIncrement = (getSize().getHeight() / scale - (maxY - minY)) / 2;
            maxY += yIncrement;
            minY -= yIncrement;
        }
        if (scale == scaleY) {
            double xIncrement = (getSize().getWidth() / scale - (maxX - minX)) / 2;
            maxX += xIncrement;
            minX -= xIncrement;
        }

        // Сохраняем текущие настройки холста
        Graphics2D canvas = (Graphics2D) g;
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Paint oldPaint = canvas.getPaint();
        Font oldFont = canvas.getFont();

        // Отрисовываем оси, график и маркеры
        if (showAxis) paintAxis(canvas);
        paintGraphics(canvas);
        if (showMarkers) paintMarkers(canvas);

        // Отрисовываем выделение (если включено)
        if (scalingMode && selectionStart != null && selectionEnd != null) {
            paintSelection(canvas);
        }

        // Если курсор находится над точкой, показываем её координаты
        if (selectedPoint != null) {
            paintCoordinates(canvas);
        }

        // Восстанавливаем старые настройки холста
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);
    }

    // Отрисовка графика по прочитанным координатам
    protected void paintGraphics(Graphics2D canvas) {
        // Устанавливаем стиль линии графика (чередование длинных и коротких отрезков)
        float[] dashPattern = {10, 10, 10, 5, 5, 5}; // Длина штрихов: 3 длинных -> 2 коротких
        canvas.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));
        canvas.setColor(Color.red);

        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
            Point2D.Double point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            if (i > 0) {
                graphics.lineTo(point.getX(), point.getY());
            } else {
                graphics.moveTo(point.getX(), point.getY());
            }
        }
        canvas.draw(graphics);
    }


    // Отображение маркеров точек
    protected void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(markerStroke);

        for (Double[] point : graphicsData) {
            Point2D.Double center = xyToPoint(point[0], point[1]);
            double x = point[0];
            double y = point[1];

            // Проверяем, идут ли цифры числа Y по возрастанию
            boolean isAscending = checkAscendingDigits(y);

            // Устанавливаем цвет маркера
            if (isAscending) {
                canvas.setColor(Color.GREEN); // Зеленый для точек, где цифры возрастают
            } else {
                canvas.setColor(Color.RED); // Красный для остальных точек
            }

            // Размер маркера (ромба)
            int markerSize = 11;

            // Центральные координаты маркера
            double cx = center.getX();
            double cy = center.getY();
            double halfSize = markerSize / 2.0;

            // Координаты вершин ромба
            Point2D.Double topLeft = new Point2D.Double(cx - halfSize, cy - halfSize);
            Point2D.Double topRight = new Point2D.Double(cx + halfSize, cy-halfSize);
            Point2D.Double bottomLeft = new Point2D.Double(cx - halfSize, cy + halfSize);
            Point2D.Double bottomRight = new Point2D.Double(cx + halfSize, cy + halfSize);

            //
            canvas.draw(new Line2D.Double(topLeft, topRight));
            canvas.draw(new Line2D.Double(topLeft, bottomLeft));
            canvas.draw(new Line2D.Double(bottomLeft, bottomRight));
            canvas.draw(new Line2D.Double(topRight, bottomRight));

            // Рисуем крест внутри ромба
            canvas.draw(new Line2D.Double(topLeft, bottomRight));
            canvas.draw(new Line2D.Double(bottomLeft, topRight));
        }
    }
    private boolean checkAscendingDigits(double value) {
        // Преобразуем число в строку
        String str = String.valueOf(Math.abs(value));

        // Переменная для хранения предыдущей цифры
        int previousDigit = -1;

        // Проходим по символам строки
        for (int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);

            // Проверяем, является ли текущий символ цифрой
            if (Character.isDigit(currentChar)) {
                int currentDigit = currentChar - '0'; // Преобразуем символ в цифру

                // Если порядок нарушен, возвращаем false
                if (previousDigit != -1 && currentDigit < previousDigit) {
                    return false;
                }

                // Обновляем предыдущую цифру
                previousDigit = currentDigit;
            }
        }

        return true; // Если все цифры идут по возрастанию, возвращаем true
    }


    // Метод, обеспечивающий отображение осей координат
    protected void paintAxis(Graphics2D canvas) {
        canvas.setStroke(axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setPaint(Color.BLACK);
        canvas.setFont(axisFont);

        FontRenderContext context = canvas.getFontRenderContext();

        // Ось Y
        {
            canvas.draw(new Line2D.Double(xyToPoint(0, maxY), xyToPoint(0, minY)));

            GeneralPath arrow = new GeneralPath();
            Point2D.Double lineEnd = xyToPoint(0, maxY);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
            arrow.lineTo(lineEnd.getX() + 5, lineEnd.getY() + 20);
            arrow.lineTo(lineEnd.getX() - 5, lineEnd.getY() + 20);
            arrow.closePath();
            canvas.draw(arrow);
            canvas.fill(arrow);

            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0, maxY);
            canvas.drawString("y", (float) labelPos.getX() + 10,
                    (float) (labelPos.getY() - bounds.getY()));
        }

        // Ось X (всегда видна)
        canvas.draw(new Line2D.Double(xyToPoint(minX, 0), xyToPoint(maxX, 0)));

        GeneralPath arrow = new GeneralPath();
        Point2D.Double lineEnd = xyToPoint(maxX, 0);
        arrow.moveTo(lineEnd.getX(), lineEnd.getY());
        arrow.lineTo(lineEnd.getX() - 20, lineEnd.getY() - 5);
        arrow.lineTo(lineEnd.getX() - 20, lineEnd.getY() + 5);
        arrow.closePath();
        canvas.draw(arrow);
        canvas.fill(arrow);

        Rectangle2D bounds = axisFont.getStringBounds("x", context);
        Point2D.Double labelPos = xyToPoint(maxX, 0);
        canvas.drawString("x", (float) (labelPos.getX() - bounds.getWidth() - 10),
                (float) (labelPos.getY() + bounds.getY()));
    }
    protected void paintSelection(Graphics2D canvas) {
        canvas.setStroke(selectionStroke);
        canvas.setColor(Color.BLACK);

        Rectangle2D.Double selection = new Rectangle2D.Double(
                Math.min(selectionStart.getX(), selectionEnd.getX()),
                Math.min(selectionStart.getY(), selectionEnd.getY()),
                Math.abs(selectionEnd.getX() - selectionStart.getX()),
                Math.abs(selectionEnd.getY() - selectionStart.getY())
        );

        canvas.draw(selection);
    }
    protected void paintCoordinates(Graphics2D canvas) {
        canvas.setFont(new Font("Serif", Font.PLAIN, 12));
        canvas.setColor(Color.BLACK);

        Point2D.Double screenPoint = xyToPoint(selectedPoint[0], selectedPoint[1]);
        String text = String.format("(%.2f, %.2f)", selectedPoint[0], selectedPoint[1]);

        canvas.drawString(text, (float) screenPoint.getX() + 5, (float) screenPoint.getY() - 5);
    }

    protected Point2D.Double xyToPoint(double x, double y) {
        double deltaX = x - minX;
        double deltaY = maxY - y;
        return new Point2D.Double(deltaX * scale, deltaY * scale);
    }

    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
        return new Point2D.Double(src.getX() + deltaX, src.getY() + deltaY);
    }
    public void mouseMoved(MouseEvent e) {
        selectedPoint = null;
        for (Double[] point : graphicsData) {
            Point2D.Double screenPoint = xyToPoint(point[0], point[1]);
            if (screenPoint.distance(e.getPoint()) < 5) {
                selectedPoint = point;
                break;
            }
        }
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            selectionStart = new Point2D.Double(e.getX(), e.getY());
            scalingMode = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && scalingMode) {
            // Конечная точка выделенной области
            selectionEnd = new Point2D.Double(e.getX(), e.getY());

            // Преобразуем координаты выделенной области в значения графика
            Point2D.Double start = pointToXY(selectionStart.getX(), selectionStart.getY());
            Point2D.Double end = pointToXY(selectionEnd.getX(), selectionEnd.getY());

            // Устанавливаем новые границы графика на основе выделенной области
            minX = Math.min(start.getX(), end.getX());
            maxX = Math.max(start.getX(), end.getX());
            minY = Math.min(start.getY(), end.getY());
            maxY = Math.max(start.getY(), end.getY());

            // Завершаем режим масштабирования
            scalingMode = false;
            selectionStart = null;
            selectionEnd = null;

            // Перерисовываем график с новыми границами
            repaint();
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            // Сброс графика в изначальный масштаб
            resetZoom();
            repaint();
        }
    }

    // Вспомогательный метод для сброса масштаба
    private void resetZoom() {
        if (graphicsData != null && graphicsData.length > 0) {
            // Устанавливаем начальные границы
            minX = graphicsData[0][0];
            maxX = graphicsData[graphicsData.length - 1][0];
            minY = graphicsData[0][1];
            maxY = minY;

            // Находим минимальное и максимальное значение Y
            for (Double[] point : graphicsData) {
                if (point[1] < minY) minY = point[1];
                if (point[1] > maxY) maxY = point[1];
            }

            // Включаем оси в видимую область
            if (minY > 0) minY = 0;
            if (maxY < 0) maxY = 0;
            if (minX > 0) minX = 0;
            if (maxX < 0) maxX = 0;
        }
    }

    protected Point2D.Double pointToXY(double x, double y) {
        return new Point2D.Double(x / scale + minX, maxY - y / scale);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (scalingMode) {
            selectionEnd = new Point2D.Double(e.getX(), e.getY());
            repaint();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}

