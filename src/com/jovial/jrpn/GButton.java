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

package com.jovial.jrpn;

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

    protected ScaleInfo scaleInfo;

    // Color taken to match the G key.  This is a lighter
    // blue than Emmet's original, but I think it's more readable.
    // Originally it was 0,100,255
    private final static Color blueColor = new Color(0, 156, 255);

    private final static Color whiteColor = new Color(255,255,255);

    public void setScaleInfo(ScaleInfo info) {
        scaleInfo = info;
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

    public void alignText() {
        int w = getWidth();
        int h = getHeight();
        whiteX = (w - scaleInfo.whiteFontMetrics.stringWidth(whiteLabel)) / 2;
        whiteY = scaleInfo.scaleY(20);
        blueX = (w - scaleInfo.blueFontMetrics.stringWidth(blueLabel)) / 2;
        blueY = h - scaleInfo.scaleY(3);
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
        int oneX = scaleInfo.scaleX(1);
        int oneY = scaleInfo.scaleY(1);
        int offsetX = pressed ? oneX : 0;
        int offsetY = pressed ? oneY : 0;

        super.paint(g);

        g.setColor(blueColor);
        g.setFont(scaleInfo.blueFont);
        drawBlueLabel(g, offsetX, offsetY);

        g.setColor(whiteColor);
        g.setFont(scaleInfo.whiteFont);
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
        public void alignText() {
            super.alignText();
            if (letters == null) {
                letters = new String[whiteLabel.length()];
                for (int i = 0; i < whiteLabel.length(); i++) {
                    letters[i] = "" + whiteLabel.charAt(i);
                }
                whiteX = new int[letters.length];
            }
            int w = getWidth();
            for (int i = 0; i < letters.length; i++) {
                whiteX[i] = (w - scaleInfo.whiteFontMetrics.stringWidth(letters[i])) / 2;
            }
            whiteHeight = scaleInfo.whiteFontMetrics.getAscent();
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
        public void alignText() {
            super.alignText();
            descent = scaleInfo.whiteFontMetrics.getDescent();
        }

        @Override
        protected void drawWhiteLabel(Graphics2D g, int offsetX, int offsetY) {
            g.setColor(Color.BLACK);
            g.drawString(whiteLabel, whiteX + offsetX, whiteY + offsetY);
        }
    }

    // I fudge the sqrt symbol in the blue text on the OCT key so the bar over the x
    // joins the sqrt symbol.
    public static class Sqrt extends GButton {
        int sqrtWidth;
        int overlineWidth;
        int xWidth;
        int blueHeight;
        private final static String overline = "\u203E\u203E";
        @Override
        public void alignText() {
            super.alignText();
            FontMetrics fm = scaleInfo.blueFontMetrics;
            sqrtWidth = fm.stringWidth("\u221A");  // √
            overlineWidth = fm.stringWidth(overline);
            xWidth = fm.stringWidth("x");
            blueHeight = scaleInfo.blueFontMetrics.getAscent();
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
            int shiftLeft = sqrtWidth / 6;
            if (shiftLeft == 0) {
                shiftLeft = 1;
            }
            // TODO:  Make the shiftLeft factor and minimum be items that can be configured.
            x += sqrtWidth - shiftLeft;
            y -= shiftUp;
            g.drawString("x", x + (overlineWidth - xWidth)/2, y);
            g.drawString("\u203E\u203E", x, y);
            // I saw an issue on OSX with combining overline (\u0305)
        }
    }
}
