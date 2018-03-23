
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

import java.awt.*;

/**
 * A little data holder for the stuff that changes when we're resized.
 */
public class ScaleInfo {
    int drawScaleNumerator = 1;
    int drawScaleDenominator = 1;
    int drawScaleNumeratorX = 1;
    int drawScaleDenominatorX = 1;
    int drawScaleNumeratorY= 1;
    int drawScaleDenominatorY = 1;

    Font blueFont;  // The blue text on keys
    FontMetrics blueFontMetrics;
    Font whiteFont; // The white text on keys
    FontMetrics whiteFontMetrics;
    Font yellowFont;  // The yellow text above the keys
    FontMetrics yellowFontMetrics;
    Font faceFont;  // The "EMMET-GRAY/JOVIAL" text on the face
    FontMetrics faceFontMetrics;


    public int scale(int num) {
        return num * drawScaleNumerator / drawScaleDenominator;
    }

    public int scaleX(int num) {
        return num * drawScaleNumeratorX / drawScaleDenominatorX;
    }

    public int scaleY(int num) {
        return num * drawScaleNumeratorY / drawScaleDenominatorY;
    }
}
