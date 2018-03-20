package com.emmetgray.wrpn;

import javax.swing.JButton;

// A Graphical Button that includes the "original" x-y coordinates for
// a 512 x 320 layout.
public class GButton extends JButton {

    private int pX, pY, pKeyCode;
    private String pImageUpPath, pImageDownPath;

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

    public String getImageUpPath() {
        return pImageUpPath;
    }

    public void setImageUpPath(String path) {
        pImageUpPath = path;
    }

    public String getImageDownPath() {
        return pImageDownPath;
    }

    public void setImageDownPath(String path) {
        pImageDownPath = path;
    }
}
