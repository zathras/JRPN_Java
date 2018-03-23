/*
   Portions of this file copyright 2018 Bill Foote

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.emmetgray.wrpn;

import javax.swing.*;
import java.awt.*;

// A Graphical Button that includes the "original" x-y coordinates for
// a 512 x 320 layout.
public class GButton extends JButton {

    private int pX, pY, pKeyCode;

    protected String whiteLabel;
    protected int whiteX, whiteY;
    protected String blueLabel;
    protected int blueX, blueY;

    private static Font blueFont;
    private static Font whiteFont;

    // Colors taken from the original button image files:
    private final static Color blueColor = new Color(0, 100, 255);
    private final static Color whiteColor = new Color(255,255,255);
    private static int drawScaleNumerator = 1;
    private static int drawScaleDenominator = 1;
    private static int drawScaleNumeratorX = 1;
    private static int drawScaleDenominatorX = 1;
    private static int drawScaleNumeratorY= 1;
    private static int drawScaleDenominatorY = 1;

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

    public static void setDrawScaleX(int numerator, int denominator) {
        drawScaleNumeratorX = numerator;
        drawScaleDenominatorX = denominator;
    }

    public static void setDrawScaleY(int numerator, int denominator) {
        drawScaleNumeratorY = numerator;
        drawScaleDenominatorY = denominator;
    }

    public static int scale(int num) {
        return num * drawScaleNumerator / drawScaleDenominator;
    }

    public static int scaleX(int num) {
        return num * drawScaleNumeratorX / drawScaleDenominatorX;
    }

    public static int scaleY(int num) {
        return num * drawScaleNumeratorY / drawScaleDenominatorY;
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
        int w = getWidth();
        int h = getHeight();
        whiteX = (w - buttonWhiteMetrics.stringWidth(whiteLabel)) / 2;
        whiteY = scaleY(20);
        blueX = (w - buttonBlueMetrics.stringWidth(blueLabel)) / 2;
        blueY = h - scaleY(3);
    }

    @Override
    public void paint(Graphics legacyG) {
        Graphics2D g = (Graphics2D) legacyG;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        boolean pressed = getModel().isPressed();
        int w = getWidth();
        int h = getHeight();
        int x;
        int y;
        int nextY;
        int oneX = scaleX(1);
        int oneY = scaleY(1);
        int offsetX = pressed ? oneX : 0;
        int offsetY = pressed ? oneY : 0;

        super.paint(g);

        g.setColor(blueColor);
        g.setFont(blueFont);
        drawBlueLabel(g, offsetX, offsetY);

        g.setColor(whiteColor);
        g.setFont(whiteFont);
        drawWhiteLabel(g, offsetX, offsetY);
    }

    protected void drawBlueLabel(Graphics2D g, int offsetX, int offsetY) {
        g.drawString(blueLabel, blueX + offsetX, blueY + offsetY);
    }

    protected void drawWhiteLabel(Graphics2D g, int offsetX, int offsetY) {
        g.drawString(whiteLabel, whiteX + offsetX, whiteY + offsetY);
    }

    public static class Enter extends GButton {

        private int whiteHeight;
        private String[] letters = null;
        private int[] whiteX;

        @Override
        public void alignText(FontMetrics buttonWhiteMetrics, FontMetrics buttonBlueMetrics) {
            super.alignText(buttonWhiteMetrics, buttonBlueMetrics);
            if (letters == null) {
                letters = new String[whiteLabel.length()];
                for (int i = 0; i < whiteLabel.length(); i++) {
                    letters[i] = "" + whiteLabel.charAt(i);
                }
                whiteX = new int[letters.length];
            }
            int w = getWidth();
            for (int i = 0; i < letters.length; i++) {
                whiteX[i] = (w - buttonWhiteMetrics.stringWidth(letters[i])) / 2;
            }
            whiteHeight = buttonWhiteMetrics.getAscent();
        }

        @Override
        protected void drawWhiteLabel(Graphics2D g, int offsetX, int offsetY) {
            int y = 0;
            for (int i = 0; i < letters.length; i++)  {
                g.drawString(letters[i], whiteX[i] + offsetX, whiteY + offsetY + y);
                y += whiteHeight;
            }
        }
    }

    // For the red and blue keys.  They display the foreground
    // ("white") text as black.
    public static class Shift extends GButton {

        private int descent;

        @Override
        public void alignText(FontMetrics buttonWhiteMetrics, FontMetrics buttonBlueMetrics) {
            super.alignText(buttonWhiteMetrics, buttonBlueMetrics);
            descent = buttonWhiteMetrics.getDescent();
        }

        @Override
        protected void drawWhiteLabel(Graphics2D g, int offsetX, int offsetY) {
            g.setColor(Color.BLACK);
            g.drawString(whiteLabel, whiteX + offsetX - descent, whiteY + offsetY);
        }
    }

    // I fudge the sqrt symbol in the blue text on the OCT key so the bar over the x
    // joins the sqrt symbol.
    public static class Sqrt extends GButton {
        int sqrtWidth;
        int blueHeight;
        @Override
        public void alignText(FontMetrics buttonWhiteMetrics, FontMetrics buttonBlueMetrics) {
            super.alignText(buttonWhiteMetrics, buttonBlueMetrics);
            sqrtWidth = buttonBlueMetrics.stringWidth("\u221A");  // √
            blueHeight = buttonBlueMetrics.getAscent();
        }

        @Override
        protected void drawBlueLabel(Graphics2D g, int offsetX, int offsetY) {
            // Draw a combining overline, shifted over to the right by the better part
            // of the width of the square root sign.  We also scoot the x up a bit, so the
            // line matches the horizontal part of the square root symbol.
            // This isn't perfect, and it might even make it worse with some fonts,
            // but on balance I think there's a better chance it will look good this
            // way.
            int x = blueX + offsetX;
            int y = blueY + offsetY;
            g.drawString("\u221A", x, y);   // √
            int shiftUp = blueHeight / 10;
            if (shiftUp == 0) {
                shiftUp = 1;
            }
            y -= shiftUp;
            x += sqrtWidth;
            g.drawString(" \u0305", x, y);  // I'm not sure why, but this makes the bar wider
            g.drawString("x\u0305", x, y);
        }
    }
}
