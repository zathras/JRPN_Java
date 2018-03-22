package com.emmetgray.wrpn;

import javax.swing.*;
import java.awt.*;

// A Graphical Button that includes the "original" x-y coordinates for
// a 512 x 320 layout.
public class GButton extends JButton {

    private int pX, pY, pKeyCode;

    protected String whiteLabel;
    protected int whiteX, whiteY;
    private String blueLabel;
    private int blueX, blueY;

    private static Font blueFont;
    private static Font whiteFont;

    // Colors taken from the original button image files:
    private final static Color blueColor = new Color(0, 100, 255);
    private final static Color whiteColor = new Color(255,255,255);
    private final static Color bgColor = new Color(67,67,67);
    private final static Color outlineColor = new Color(0, 0, 0);
    private final static Color topColor = new Color(22, 22, 22);
    private final static Color midColor = new Color(41, 41, 41);
    private final static Color bottomColor = topColor;
    private static int drawScaleNumerator = 1;
    private static int drawScaleDenominator = 1;

    public static void setBlueFont(Font font) {
        blueFont = font;
    }

    public static void setWhiteFont(Font font) {
        whiteFont = font;
    }

    public static void setDrawScale(int numerator, int denominator) {
        drawScaleNumerator = numerator;
        drawScaleDenominator = denominator;
    }

    public static int scale(int num) {
        return num * drawScaleNumerator / drawScaleDenominator;
    }

    public int getOriginalX() {
        return pX;
    }

    public void setOriginalX(int x) {
        pX = x;
    }

    public int getOriginalY() {
        return pY;
    }

    public void setOriginalY(int y) {
        pY = y;
    }

    public int getKeyCode() {
        return pKeyCode;
    }

    public void setKeyCode(int keycode) {
        pKeyCode = keycode;
    }

    public void setWhiteLabel(String str) {
        whiteLabel = str;
    }

    public void setBlueLabel(String str) {
        blueLabel = str;
    }

    public void alignText(FontMetrics buttonWhiteMetrics, FontMetrics buttonBlueMetrics) {
        if (whiteLabel == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        whiteX = (w - buttonWhiteMetrics.stringWidth(whiteLabel)) / 2;
        whiteY = scale(23);
        blueX = (w - buttonBlueMetrics.stringWidth(blueLabel)) / 2;
        blueY = h - scale(3);
    }

    @Override
    public void paint(Graphics legacyG) {
        Graphics2D g = (Graphics2D) legacyG;
        boolean pressed = getModel().isPressed();
        int w = getWidth();
        int h = getHeight();
        int x;
        int y;
        int nextY;
        int one = scale(1);
        int offset = pressed ? one : 0;
        if (whiteLabel != null) {
            g.setColor(bgColor);
            g.fillRect(0, 0, w, h);
            g.setColor(outlineColor);
            g.fillRect(one+offset, one+offset, w-2*one, h-2*one);

            g.setColor(topColor);
            x = scale(2);
            y = scale(2);
            nextY = scale(8);
            g.fill3DRect(x, y, w - 2*x, nextY - y, !pressed);

            g.setColor(midColor);
            y = nextY;
            nextY = scale(27);
            g.fill3DRect(x, y, w - 2*x, nextY - y, !pressed);

            g.setColor(bottomColor);
            y = nextY;
            nextY = h - scale(2);
            g.fill3DRect(x, y, w - 2*x, nextY - y, !pressed);

            g.setColor(blueColor);
            g.setFont(blueFont);
            g.drawString(blueLabel, blueX+offset, blueY+offset);

            g.setColor(whiteColor);
            g.setFont(whiteFont);
            drawWhiteLabel(g, offset);
        } else {
            super.paint(g);
        }
    }

    protected void drawWhiteLabel(Graphics2D g, int offset) {
        g.drawString(whiteLabel, whiteX + offset, whiteY + offset);
    }

    @Override
    public void setIcon(Icon icon) {
        if (whiteLabel == null) {
            super.setIcon(icon);
        }
    }

    @Override
    public void setPressedIcon(Icon icon) {
        if (whiteLabel == null) {
            super.setPressedIcon(icon);
        }
    }

    public static class Enter extends GButton {

        private int whiteHeight;

        @Override
        public void alignText(FontMetrics buttonWhiteMetrics, FontMetrics buttonBlueMetrics) {
            super.alignText(buttonWhiteMetrics, buttonBlueMetrics);
            whiteX = (getWidth() - buttonWhiteMetrics.stringWidth("E")) / 2;
            whiteHeight = buttonWhiteMetrics.getAscent();
        }

        @Override
        protected void drawWhiteLabel(Graphics2D g, int offset) {
            int y = 0;
            for (int i = 0; i < whiteLabel.length(); i++)  {
                char c = whiteLabel.charAt(i);
                g.drawString(""+c, whiteX + offset, whiteY + offset + y);
                y += whiteHeight;
            }
        }
    }
}
