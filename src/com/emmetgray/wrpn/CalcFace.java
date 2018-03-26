package com.emmetgray.wrpn;


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

import java.awt.*;
import javax.swing.JLabel;


/**
 * The calculator face
 */

public class CalcFace extends JLabel {

    /**
     * This represents the bit of yellow text above the buttons
     */
    public static class YellowText {

        protected GButton over;
        protected String text;
        protected int textX;
        protected int textY;

        public YellowText(GButton over, String text) {
            this.over = over;
            this.text = text;
        }

        protected int getRightX() {
            return over.getX() + over.getWidth() - 1;
        }

        public int alignText(ScaleInfo info, int xOffset, int yOffset) {
            int stringWidth = info.yellowFontMetrics.stringWidth(text);
            textX = (over.getX() + getRightX() - stringWidth) / 2 - xOffset;
            textY = over.getY() - info.scaleY(2) - yOffset;
            return stringWidth;
        }

        public void paint(Graphics2D g) {
            g.drawString(text, textX, textY);
        }
    }

    public static class YellowMultiText extends YellowText {

        protected GButton right;
        protected int lineY;
        protected int linesUp;
        protected int pixelsUp;
        protected int stringWidth;
        protected final ScaleInfo scaleInfo;

        public YellowMultiText(ScaleInfo scaleInfo, GButton left, GButton right, int linesUp, String text) {
            super(left, text);
            this.scaleInfo = scaleInfo;
            this.right = right;
            this.linesUp =linesUp;
        }

        protected int getRightX() {
            return right.getX() + right.getWidth() - 1;
        }

        public int alignText(ScaleInfo info, int xOffset, int yOffset) {
            pixelsUp = info.yellowFontMetrics.getAscent() * linesUp;
            stringWidth = super.alignText(info, xOffset, yOffset + pixelsUp);
            lineY = textY - info.yellowFontMetrics.getAscent() / 2;
            return stringWidth;
        }

        public void paint(Graphics2D g) {
            int x1 = over.getX()- scaleInfo.scaleX(2);
            g.drawLine(x1, over.getY() - pixelsUp - scaleInfo.scaleY(1), x1, lineY);
            int x2 = textX - scaleInfo.scaleX(4);
            g.drawLine(x1, lineY, x2, lineY);
            x1 = textX + stringWidth + scaleInfo.scaleX(4);
            x2 = getRightX() + scaleInfo.scaleX(2);
            g.drawLine(x1, lineY, x2, lineY);
            g.drawLine(x2, lineY, x2, right.getY() - pixelsUp - scaleInfo.scaleY(1));
            super.paint(g);
        }
    }


    // Color taken to match the F key.  This is a less intense
    // yellow than Emmet's original, but I think it's more readable.
    // Originally it was Color.YELLOW
    private static Color yellowTextColor = new Color(255, 231, 66);
    private static Color greyFaceColor = new Color(231, 231, 231);
    private static Color bgFaceColor = new Color(66, 66, 66);

    public YellowText[] yellowText;
    private Font yellowFont;
    private Stroke lineStroke = new BasicStroke(1.5f);
    private String faceText = "E M M E T - G R A Y / J O V I A L";
    private Font faceFont;
    private int faceTextWidth;


    void resize(ScaleInfo info) {
        yellowFont = info.yellowFont;
        int y = getY();
        int x = getX();
        for (YellowText yt : yellowText) {
            yt.alignText(info, x, y);
        }

        faceFont = info.faceFont;
        this.faceTextWidth = info.faceFontMetrics.stringWidth(faceText);

        lineStroke = new BasicStroke(1.5f * ((float) info.drawScaleNumerator)
                                             / ((float) info.drawScaleDenominator));
    }

    @Override
    public void paint(Graphics legacyG) {
        Graphics2D g = (Graphics2D) legacyG;
        super.paint(g);
        int w = getWidth();
        int h = getHeight();
        int cw = fmMain.CALC_WIDTH;
        int ch = fmMain.CALC_HEIGHT;

        g.setColor(bgFaceColor);
        g.fillRect(35 * w / cw, 290 * h / ch,
                   10 * w / cw + faceTextWidth, 25 * h / ch);
        g.setColor(greyFaceColor);
        g.setFont(faceFont);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g.drawString(faceText, 40 * w / cw, 310 * h / ch);

        g.setStroke(lineStroke);
        g.setFont(yellowFont);
        g.setColor(yellowTextColor);
        for (YellowText yt : yellowText) {
            yt.paint(g);
        }
    }
}
