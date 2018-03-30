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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// The main form for the calculator
public class fmMain extends javax.swing.JFrame {

    private final static String CONFIG_FILE_NAME = ".JRPN.config";
    private final static String STATE_FILE_NAME = ".JRPN.CalcState.xml";

    static java.util.Properties prop;
    final static int CALC_WIDTH = 512;
    final static int CALC_HEIGHT = 320;
    final static int DISPLAY_WIDTH = 320;
    final static int DISPLAY_HEIGHT = 38;
    final static int BUTTON_WIDTH = 37;
    final static int BUTTON_HEIGHT = 33;
    private static CalcState cs;
    private static Calculator c;

    private Font tbDisplayFontSmall;
    private Font tbDisplayFontLarge;
    private Rectangle lastFaceBounds = null;

    private Image calcFaceImage;
    private ImageIcon jupiterIcon;
    private Image jupiterIconImage;
    private Font jupiterFont;
    private FontMetrics jupiterFontMetrics;
    private int jupiterTextWidth;
    private final static String JUPITER_TEXT = "JRPN";

    private JupiterLabel jupiterLabel;

    private class JupiterLabel extends JLabel {
        @Override
        public void paint(Graphics legacyG) {
            Graphics2D g = (Graphics2D) legacyG;
            super.paint(g);

            int width = getWidth();
            int height = getHeight();
            g.setColor(Color.black);
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(231, 231, 231));
            g.fillRect(scaleInfo.scaleX(1), scaleInfo.scaleY(1),
                       width - scaleInfo.scaleX(2), height - scaleInfo.scaleY(2));
            g.drawImage(jupiterIcon.getImage(),
                        (getWidth() - jupiterIcon.getImage().getWidth(null))/2,
                        scaleInfo.scaleY(2), this);
            g.setColor(Color.black);
            g.setStroke(new BasicStroke(scaleInfo.scale(1)));
            g.drawLine(0, scaleInfo.scaleY(31), width, scaleInfo.scaleY(31));
            g.setFont(jupiterFont);
            g.drawString(JUPITER_TEXT, scaleInfo.scaleX(1) + (width - jupiterTextWidth) / 2,
                         scaleInfo.scaleY(31) + jupiterFontMetrics.getAscent());
        }
    }

    private static class ButtonIcons {
        ImageIcon buttonIcon;
        ImageIcon buttonPressedIcon;
        Image buttonImage;
        Image buttonPressedImage;

        ButtonIcons(String name, String downName, MediaTracker t, int id) {
            buttonIcon = new ImageIcon(getClass().getResource("/com/jovial/jrpn/resources/" + name));
            buttonImage = buttonIcon.getImage();
            t.addImage(buttonImage, id);
            buttonPressedIcon = new ImageIcon(getClass().getResource("/com/jovial/jrpn/resources/" + downName));
            buttonPressedImage = buttonPressedIcon.getImage();
            t.addImage(buttonPressedImage, id+1);
        }

        void scaleTo(int width, int height, MediaTracker t, int id) {
            Image im =  buttonImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            t.addImage(im, id);
            buttonIcon = new ImageIcon(im);
            im = buttonPressedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            t.addImage(im, id+1);
            buttonPressedIcon = new ImageIcon(im);
        }
    }
    private ButtonIcons buttonIcons;
    private ButtonIcons enterButtonIcons;
    private ButtonIcons yellowButtonIcons;
    private ButtonIcons blueButtonIcons;

    private ScaleInfo scaleInfo = new ScaleInfo();

    public fmMain() throws InterruptedException {
        initComponents();

        // create the event listener for the buttons
        for (Component comp : jLayeredPane1.getComponents()) {
            if (comp instanceof GButton) {
                GButton bn = (GButton) comp;

                // add a common event listener to all buttons
                bn.addActionListener(new java.awt.event.ActionListener() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        GButton_Click(evt);
                    }
                });
            }
        }

        this.setIconImage(new ImageIcon(getClass().getResource("/com/jovial/jrpn/resources/JRPN_ico.png")).getImage());
        this.setTitle("JRPN 16c");
        
        // configure a single key event manager
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager(); 
        manager.addKeyEventDispatcher(new MyDispatcher());

        // copy the "prototype" config file if it doesn't already exist
        File config = new File(System.getProperty("user.home"), CONFIG_FILE_NAME);
        if (!config.exists()) {
            BufferedWriter sw = null;
            String line;

            try {
                sw = new BufferedWriter(new FileWriter(config));
                BufferedReader sr = new BufferedReader(new InputStreamReader(
                        fmMain.class.getResourceAsStream("/com/jovial/jrpn/JRPNconfig.xml")));

                // copy the file
                while ((line = sr.readLine()) != null) {
                    sw.write(line + "\n");
                }
                sr.close();
            } catch (Exception ex) {
                Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, ex);
            } finally {
                if (sw != null) {
                    try {
                        sw.flush();
                        sw.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
            
        // set some defaults if there is no config file
        prop = new java.util.Properties();
        prop.setProperty("NumRegisters", "32");
        prop.setProperty("PrgmMemoryLines", "302");
        prop.setProperty("SleepDelay", "1500");
        prop.setProperty("SyncConversions", "true");
        prop.setProperty("HomeURL", "http://jrpn.jovial.com");
        prop.setProperty("Email", "egray1@hot.rr.com");
        prop.setProperty("Version", "6.0.8");
        prop.setProperty("HelpURL", "http://www.wrpn.emmet-gray.com/UsersGuide.html"); // TODO

        // load the config file.  Error are ignored
        try {
            prop.loadFromXML(new FileInputStream(config));
        } catch (FileNotFoundException e) {
            Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
        } catch (IOException e) {
            Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
        }

        // load user's version of the saved calculator state
        cs = new CalcState();
        c = new Calculator(cs);
        LoadState();
        UpdateMenu();

        // process a dummy key to refresh the display
        ProcessPacket(c.ProcessKey(-1));
    }

    /**
     * This method was originally generated by the NetBeans form generator, then
     * modified by hand in IntelliJ.  At the time this was done, I deleted fmMain.form,
     * to make sure the changes didn't get stomped on.
     *
     * For reference, here's what the real calculator looks like:
     * https://www.scss.tcd.ie/SCSSTreasuresCatalog/hardware/temp-photos-Canon-101-03-20160312/HP-16C/IMG_0090.JPG
     */
    private void initComponents() throws InterruptedException {
        jLayeredPane1 = new javax.swing.JLayeredPane();
        tbDisplay = new javax.swing.JTextPane();
        jupiterLabel = new JupiterLabel();
        lbFKey = new javax.swing.JLabel();
        lbGKey = new javax.swing.JLabel();
        lbCarry = new javax.swing.JLabel();
        lbOverflow = new javax.swing.JLabel();
        lbPrgm = new javax.swing.JLabel();
        pnCalcFace = new CalcFace();
        bnA = new GButton();
        bnB = new GButton();
        bnC = new GButton();
        bnD = new GButton();
        bnE = new GButton();
        bnF = new GButton();
        bn7 = new GButton();
        bn8 = new GButton();
        bn9 = new GButton();
        bnDiv = new GButton();
        bnGSB = new GButton();
        bnGTO = new GButton();
        bnHEX = new GButton();
        bnDEC = new GButton();
        bnOCT = new GButton.Sqrt();
        bnBIN = new GButton();
        bn4 = new GButton();
        bn5 = new GButton();
        bn6 = new GButton();
        bnMul = new GButton();
        bnRS = new GButton();
        bnSST = new GButton();
        bnRol = new GButton();
        bnXY = new GButton();
        bnBSP = new GButton();
        bnEnt = new GButton.Enter();
        bn1 = new GButton();
        bn2 = new GButton();
        bn3 = new GButton();
        bnMin = new GButton();
        bnON = new GButton();
        bnFKey = new GButton.Shift();
        bnGKey = new GButton.Shift();
        bnSTO = new GButton();
        bnRCL = new GButton();
        bn0 = new GButton();
        bnDp = new GButton();
        bnCHS = new GButton();
        bnPls = new GButton();
        mbMain = new javax.swing.JMenuBar();
        mFile = new javax.swing.JMenu();
        mFileOpen = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mFileSave = new javax.swing.JMenuItem();
        mFileSaveAs = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mFilePrint = new javax.swing.JMenuItem();
        mFileSetup = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        mFileExit = new javax.swing.JMenuItem();
        mEdit = new javax.swing.JMenu();
        mEditCopy = new javax.swing.JMenuItem();
        mEditPaste = new javax.swing.JMenuItem();
        mMode = new javax.swing.JMenu();
        mModeFloat = new javax.swing.JCheckBoxMenuItem();
        mModeHex = new javax.swing.JCheckBoxMenuItem();
        mModeDec = new javax.swing.JCheckBoxMenuItem();
        mModeOct = new javax.swing.JCheckBoxMenuItem();
        mModeBin = new javax.swing.JCheckBoxMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        mModeSI = new javax.swing.JCheckBoxMenuItem();
        mModePrecision = new javax.swing.JMenu();
        mModeP0 = new javax.swing.JCheckBoxMenuItem();
        mModeP1 = new javax.swing.JCheckBoxMenuItem();
        mModeP2 = new javax.swing.JCheckBoxMenuItem();
        mModeP3 = new javax.swing.JCheckBoxMenuItem();
        mModeP4 = new javax.swing.JCheckBoxMenuItem();
        mModeP5 = new javax.swing.JCheckBoxMenuItem();
        mModeP6 = new javax.swing.JCheckBoxMenuItem();
        mModeP7 = new javax.swing.JCheckBoxMenuItem();
        mModeP8 = new javax.swing.JCheckBoxMenuItem();
        mModeP9 = new javax.swing.JCheckBoxMenuItem();
        mOptions = new javax.swing.JMenu();
        mOptionClear = new javax.swing.JMenuItem();
        mOptionSave = new javax.swing.JCheckBoxMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        mOption8bit = new javax.swing.JCheckBoxMenuItem();
        mOption16bit = new javax.swing.JCheckBoxMenuItem();
        mOption32bit = new javax.swing.JCheckBoxMenuItem();
        mOption64bit = new javax.swing.JCheckBoxMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        mOption1sComp = new javax.swing.JCheckBoxMenuItem();
        mOption2sComp = new javax.swing.JCheckBoxMenuItem();
        mOptionUnsigned = new javax.swing.JCheckBoxMenuItem();
        mFlags = new javax.swing.JMenu();
        mFlagUser0 = new javax.swing.JCheckBoxMenuItem();
        mFlagUser1 = new javax.swing.JCheckBoxMenuItem();
        mFlagUser2 = new javax.swing.JCheckBoxMenuItem();
        mFlagZeros = new javax.swing.JCheckBoxMenuItem();
        mFlagCarry = new javax.swing.JCheckBoxMenuItem();
        mFlagOverflow = new javax.swing.JCheckBoxMenuItem();
        mHelp = new javax.swing.JMenu();
        mHelpContents = new javax.swing.JMenuItem();
        mHelpBackPanel = new javax.swing.JMenuItem();
        mHelpIndex = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        mHelpAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                try {
                    fmMain_Resized(evt);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        });

        jLayeredPane1.setDoubleBuffered(true);

        tbDisplay.setEditable(false);
        tbDisplay.setFont(new java.awt.Font("Lucidia Sans Typewriter", 1, 12)); // NOI18N
        tbDisplay.setText("0.000");
        tbDisplay.setBounds(54, 31, 320, 38);
        jLayeredPane1.add(tbDisplay, javax.swing.JLayeredPane.PALETTE_LAYER);

        jupiterLabel.setBounds(455, 21, 488, 65);
        jLayeredPane1.add(jupiterLabel, JLayeredPane.MODAL_LAYER);

        lbFKey.setFont(new java.awt.Font("Tahoma", 0, 7)); // NOI18N
        lbFKey.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbFKey.setName("100");
        lbFKey.setBounds(100, 55, 30, 12);
        jLayeredPane1.add(lbFKey, javax.swing.JLayeredPane.MODAL_LAYER);

        lbGKey.setFont(new java.awt.Font("Tahoma", 0, 7)); // NOI18N
        lbGKey.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbGKey.setName("155");
        lbGKey.setBounds(155, 55, 30, 12);
        jLayeredPane1.add(lbGKey, javax.swing.JLayeredPane.MODAL_LAYER);

        lbCarry.setFont(new java.awt.Font("Tahoma", 0, 7)); // NOI18N
        lbCarry.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbCarry.setName("210");
        lbCarry.setBounds(210, 55, 30, 12);
        jLayeredPane1.add(lbCarry, javax.swing.JLayeredPane.MODAL_LAYER);

        lbOverflow.setFont(new java.awt.Font("Tahoma", 0, 7)); // NOI18N
        lbOverflow.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbOverflow.setName("265");
        lbOverflow.setBounds(265, 55, 30, 12);
        jLayeredPane1.add(lbOverflow, javax.swing.JLayeredPane.MODAL_LAYER);

        lbPrgm.setFont(new java.awt.Font("Tahoma", 0, 7)); // NOI18N
        lbPrgm.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lbPrgm.setName("320");
        lbPrgm.setBounds(320, 55, 30, 12);
        jLayeredPane1.add(lbPrgm, javax.swing.JLayeredPane.MODAL_LAYER);

        MediaTracker tracker = new MediaTracker(this);
        ImageIcon calcFaceIcon = new ImageIcon(getClass().
                getResource("/com/jovial/jrpn/resources/Background.png"));
        calcFaceImage = calcFaceIcon.getImage();
        tracker.addImage(calcFaceImage, 1);
        pnCalcFace.setIcon(calcFaceIcon);
        pnCalcFace.setVerifyInputWhenFocusTarget(false);
        pnCalcFace.setBounds(0, 0, 512, 320);
        lastFaceBounds = null;  // Just being conservative.
        jLayeredPane1.add(pnCalcFace, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jupiterIcon = new ImageIcon(getClass().getResource("/com/jovial/jrpn/resources/jupiter.png"));
        jupiterIconImage = jupiterIcon.getImage();
        tracker.addImage(jupiterIconImage, 2);

        buttonIcons = new ButtonIcons("Bn.png", "BnDown.png", tracker, 3);
        enterButtonIcons = new ButtonIcons("BnEnt.png", "BnEntDn.png", tracker, 5);
        yellowButtonIcons = new ButtonIcons("BnFkey.png", "BnFkeyDn.png", tracker, 7);
        blueButtonIcons = new ButtonIcons("BnGkey.png", "BnGkeyDn.png", tracker, 9);

        Image scaledJupiter =
                jupiterIconImage.getScaledInstance(scaleInfo.scale(jupiterIconImage.getWidth(null))/4,
                                        scaleInfo.scale(jupiterIconImage.getHeight(null))/4,
                                        Image.SCALE_SMOOTH);
        jupiterIcon.setImage(scaledJupiter);
        tracker.addImage(scaledJupiter, 11);
        tracker.waitForAll();

        bnA.setIcon(buttonIcons.buttonIcon);
        bnA.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnA.setWhiteLabel("A");
        bnA.setBlueLabel("LJ");
        bnA.setKeyCode(10);
        bnA.setName("");
        bnA.setOriginalX(31);
        bnA.setOriginalY(111);
        bnA.setPreferredSize(new java.awt.Dimension(37, 33));
        bnA.setBounds(31, 110, 37, 33);
        jLayeredPane1.add(bnA, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnB.setIcon(buttonIcons.buttonIcon);
        bnB.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnB.setWhiteLabel("B");
        bnB.setBlueLabel("ASR");
        bnB.setKeyCode(11);
        bnB.setName("");
        bnB.setOriginalX(77);
        bnB.setOriginalY(111);
        bnB.setPreferredSize(new java.awt.Dimension(37, 33));
        bnB.setBounds(77, 110, 37, 33);
        jLayeredPane1.add(bnB, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnC.setIcon(buttonIcons.buttonIcon);
        bnC.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnC.setWhiteLabel("C");
        bnC.setBlueLabel("RLC");
        bnC.setKeyCode(12);
        bnC.setName("");
        bnC.setOriginalX(123);
        bnC.setOriginalY(111);
        bnC.setPreferredSize(new java.awt.Dimension(37, 33));
        bnC.setBounds(123, 111, 37, 33);
        jLayeredPane1.add(bnC, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnD.setIcon(buttonIcons.buttonIcon);
        bnD.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnD.setWhiteLabel("D");
        bnD.setBlueLabel("RRC");
        bnD.setKeyCode(13);
        bnD.setName("");
        bnD.setOriginalX(169);
        bnD.setOriginalY(111);
        bnD.setPreferredSize(new java.awt.Dimension(37, 33));
        bnD.setBounds(169, 111, 37, 33);
        jLayeredPane1.add(bnD, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnE.setIcon(buttonIcons.buttonIcon);
        bnE.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnE.setWhiteLabel("E");
        bnE.setBlueLabel("RCLn");
        bnE.setKeyCode(14);
        bnE.setName("");
        bnE.setOriginalX(215);
        bnE.setOriginalY(111);
        bnE.setPreferredSize(new java.awt.Dimension(37, 33));
        bnE.setBounds(215, 111, 37, 33);
        jLayeredPane1.add(bnE, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnF.setIcon(buttonIcons.buttonIcon);
        bnF.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnF.setWhiteLabel("F");
        bnF.setBlueLabel("RRCn");
        bnF.setKeyCode(15);
        bnF.setName("");
        bnF.setOriginalX(261);
        bnF.setOriginalY(111);
        bnF.setPreferredSize(new java.awt.Dimension(37, 33));
        bnF.setBounds(261, 111, 37, 33);
        jLayeredPane1.add(bnF, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn7.setIcon(buttonIcons.buttonIcon);
        bn7.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn7.setWhiteLabel("7");
        bn7.setBlueLabel("#B");
        bn7.setKeyCode(7);
        bn7.setName("");
        bn7.setOriginalX(307);
        bn7.setOriginalY(111);
        bn7.setPreferredSize(new java.awt.Dimension(37, 33));
        bn7.setBounds(307, 111, 37, 33);
        jLayeredPane1.add(bn7, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn8.setIcon(buttonIcons.buttonIcon);
        bn8.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn8.setWhiteLabel("8");
        bn8.setBlueLabel("ABS");
        bn8.setKeyCode(8);
        bn8.setName("");
        bn8.setOriginalX(353);
        bn8.setOriginalY(111);
        bn8.setPreferredSize(new java.awt.Dimension(37, 33));
        bn8.setBounds(353, 111, 37, 33);
        jLayeredPane1.add(bn8, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn9.setIcon(buttonIcons.buttonIcon);
        bn9.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn9.setWhiteLabel("9");
        bn9.setBlueLabel("DBLR");
        bn9.setKeyCode(9);
        bn9.setName("");
        bn9.setOriginalX(399);
        bn9.setOriginalY(111);
        bn9.setPreferredSize(new java.awt.Dimension(37, 33));
        bn9.setBounds(399, 111, 37, 33);
        jLayeredPane1.add(bn9, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnDiv.setIcon(buttonIcons.buttonIcon);
        bnDiv.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnDiv.setWhiteLabel("\u00F7");  // ÷
        bnDiv.setBlueLabel("DBL\u00F7"); // DBL÷
        bnDiv.setKeyCode(16);
        bnDiv.setName("");
        bnDiv.setOriginalX(445);
        bnDiv.setOriginalY(111);
        bnDiv.setPreferredSize(new java.awt.Dimension(37, 33));
        bnDiv.setBounds(445, 111, 37, 33);
        jLayeredPane1.add(bnDiv, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnGSB.setIcon(buttonIcons.buttonIcon);
        bnGSB.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnGSB.setWhiteLabel("GSB");
        bnGSB.setBlueLabel("RTN");
        bnGSB.setKeyCode(33);
        bnGSB.setName("");
        bnGSB.setOriginalX(31);
        bnGSB.setOriginalY(162);
        bnGSB.setPreferredSize(new java.awt.Dimension(37, 33));
        bnGSB.setBounds(31, 162, 37, 33);
        jLayeredPane1.add(bnGSB, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnGTO.setIcon(buttonIcons.buttonIcon);
        bnGTO.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnGTO.setWhiteLabel("GTO");
        bnGTO.setBlueLabel("LBL");
        bnGTO.setKeyCode(34);
        bnGTO.setName("");
        bnGTO.setOriginalX(77);
        bnGTO.setOriginalY(162);
        bnGTO.setPreferredSize(new java.awt.Dimension(37, 33));
        bnGTO.setBounds(77, 162, 37, 33);
        jLayeredPane1.add(bnGTO, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnHEX.setIcon(buttonIcons.buttonIcon);
        bnHEX.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnHEX.setWhiteLabel("HEX");
        bnHEX.setBlueLabel("DSZ");
        bnHEX.setKeyCode(35);
        bnHEX.setName("");
        bnHEX.setOriginalX(123);
        bnHEX.setOriginalY(162);
        bnHEX.setPreferredSize(new java.awt.Dimension(37, 33));
        bnHEX.setBounds(123, 162, 37, 33);
        jLayeredPane1.add(bnHEX, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnDEC.setIcon(buttonIcons.buttonIcon);
        bnDEC.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnDEC.setWhiteLabel("DEC");
        bnDEC.setBlueLabel("ISZ");
        bnDEC.setKeyCode(36);
        bnDEC.setName("");
        bnDEC.setOriginalX(169);
        bnDEC.setOriginalY(162);
        bnDEC.setPreferredSize(new java.awt.Dimension(37, 33));
        bnDEC.setBounds(169, 162, 37, 33);
        jLayeredPane1.add(bnDEC, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnOCT.setIcon(buttonIcons.buttonIcon);
        bnOCT.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnOCT.setWhiteLabel("OCT");
        bnOCT.setBlueLabel("\u221Ax\u0305");   // √x with "combining overline"
        bnOCT.setKeyCode(37);                  // See also GButton.Sqrt's paint method
        bnOCT.setName("");
        bnOCT.setOriginalX(215);
        bnOCT.setOriginalY(162);
        bnOCT.setPreferredSize(new java.awt.Dimension(37, 33));
        bnOCT.setBounds(215, 162, 37, 33);
        jLayeredPane1.add(bnOCT, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnBIN.setIcon(buttonIcons.buttonIcon);
        bnBIN.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnBIN.setWhiteLabel("BIN");
        bnBIN.setBlueLabel("1/x");
        bnBIN.setKeyCode(38);
        bnBIN.setName("");
        bnBIN.setOriginalX(261);
        bnBIN.setOriginalY(162);
        bnBIN.setPreferredSize(new java.awt.Dimension(37, 33));
        bnBIN.setBounds(261, 162, 37, 33);
        jLayeredPane1.add(bnBIN, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn4.setIcon(buttonIcons.buttonIcon);
        bn4.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn4.setWhiteLabel("4");
        bn4.setBlueLabel("SF");
        bn4.setKeyCode(4);
        bn4.setName("");
        bn4.setOriginalX(307);
        bn4.setOriginalY(162);
        bn4.setPreferredSize(new java.awt.Dimension(37, 33));
        bn4.setBounds(307, 162, 37, 33);
        jLayeredPane1.add(bn4, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn5.setIcon(buttonIcons.buttonIcon);
        bn5.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn5.setWhiteLabel("5");
        bn5.setBlueLabel("CF");
        bn5.setKeyCode(5);
        bn5.setName("");
        bn5.setOriginalX(353);
        bn5.setOriginalY(162);
        bn5.setPreferredSize(new java.awt.Dimension(37, 33));
        bn5.setBounds(353, 162, 37, 33);
        jLayeredPane1.add(bn5, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn6.setIcon(buttonIcons.buttonIcon);
        bn6.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn6.setWhiteLabel("6");
        bn6.setBlueLabel("F?");
        bn6.setKeyCode(6);
        bn6.setName("");
        bn6.setOriginalX(399);
        bn6.setOriginalY(162);
        bn6.setPreferredSize(new java.awt.Dimension(37, 33));
        bn6.setBounds(399, 162, 37, 33);
        jLayeredPane1.add(bn6, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnMul.setIcon(buttonIcons.buttonIcon);
        bnMul.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnMul.setWhiteLabel("X");
        bnMul.setBlueLabel("DBL\u00D7");  // DBL×
        bnMul.setKeyCode(32);
        bnMul.setName("");
        bnMul.setOriginalX(445);
        bnMul.setOriginalY(162);
        bnMul.setPreferredSize(new java.awt.Dimension(37, 33));
        bnMul.setBounds(445, 162, 37, 33);
        jLayeredPane1.add(bnMul, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnRS.setIcon(buttonIcons.buttonIcon);
        bnRS.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnRS.setWhiteLabel("R/S");
        bnRS.setBlueLabel("P/R");
        bnRS.setKeyCode(49);
        bnRS.setName("");
        bnRS.setOriginalX(31);
        bnRS.setOriginalY(213);
        bnRS.setPreferredSize(new java.awt.Dimension(37, 33));
        bnRS.setBounds(31, 213, 37, 33);
        jLayeredPane1.add(bnRS, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnSST.setIcon(buttonIcons.buttonIcon);
        bnSST.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnSST.setWhiteLabel("SST");
        bnSST.setBlueLabel("BST");
        bnSST.setKeyCode(50);
        bnSST.setName("");
        bnSST.setOriginalX(77);
        bnSST.setOriginalY(213);
        bnSST.setPreferredSize(new java.awt.Dimension(37, 33));
        bnSST.setBounds(77, 213, 37, 33);
        jLayeredPane1.add(bnSST, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnRol.setIcon(buttonIcons.buttonIcon);
        bnRol.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnRol.setWhiteLabel("R\u2193");  // R↓
        bnRol.setBlueLabel("R\u2191");   // R↑
        bnRol.setKeyCode(51);
        bnRol.setName("");
        bnRol.setOriginalX(123);
        bnRol.setOriginalY(213);
        bnRol.setPreferredSize(new java.awt.Dimension(37, 33));
        bnRol.setBounds(123, 213, 37, 33);
        jLayeredPane1.add(bnRol, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnXY.setIcon(buttonIcons.buttonIcon);
        bnXY.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnXY.setWhiteLabel("x\u2B0Cy");   // x⬌y
        bnXY.setBlueLabel("PSE");
        bnXY.setKeyCode(52);
        bnXY.setName("");
        bnXY.setOriginalX(169);
        bnXY.setOriginalY(213);
        bnXY.setPreferredSize(new java.awt.Dimension(37, 33));
        bnXY.setBounds(169, 213, 37, 33);
        jLayeredPane1.add(bnXY, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnBSP.setIcon(buttonIcons.buttonIcon);
        bnBSP.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnBSP.setWhiteLabel("BSP");
        bnBSP.setBlueLabel("CLx");
        bnBSP.setKeyCode(53);
        bnBSP.setName("");
        bnBSP.setOriginalX(215);
        bnBSP.setOriginalY(213);
        bnBSP.setPreferredSize(new java.awt.Dimension(37, 33));
        bnBSP.setBounds(215, 213, 37, 33);
        jLayeredPane1.add(bnBSP, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnEnt.setIcon(enterButtonIcons.buttonIcon);
        bnEnt.setPressedIcon(enterButtonIcons.buttonPressedIcon);
        bnEnt.setWhiteLabel("ENTER");
        bnEnt.setBlueLabel("LSTx");
        bnEnt.setKeyCode(54);
        bnEnt.setName("");
        bnEnt.setOriginalX(261);
        bnEnt.setOriginalY(213);
        bnEnt.setPreferredSize(new java.awt.Dimension(37, 84));
        bnEnt.setBounds(261, 213, 37, 84);
        jLayeredPane1.add(bnEnt, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn1.setIcon(buttonIcons.buttonIcon);
        bn1.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn1.setWhiteLabel("1");
        bn1.setBlueLabel("x\u2264y");  // x≤y
        bn1.setKeyCode(1);
        bn1.setName("");
        bn1.setOriginalX(307);
        bn1.setOriginalY(213);
        bn1.setPreferredSize(new java.awt.Dimension(37, 33));
        bn1.setBounds(307, 213, 37, 33);
        jLayeredPane1.add(bn1, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn2.setIcon(buttonIcons.buttonIcon);
        bn2.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn2.setWhiteLabel("2");
        bn2.setBlueLabel("x<0");
        bn2.setKeyCode(2);
        bn2.setName("");
        bn2.setOriginalX(353);
        bn2.setOriginalY(213);
        bn2.setPreferredSize(new java.awt.Dimension(37, 33));
        bn2.setBounds(353, 213, 37, 33);
        jLayeredPane1.add(bn2, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn3.setIcon(buttonIcons.buttonIcon);
        bn3.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn3.setWhiteLabel("3");
        bn3.setBlueLabel("x>y");
        bn3.setKeyCode(3);
        bn3.setName("");
        bn3.setOriginalX(399);
        bn3.setOriginalY(213);
        bn3.setPreferredSize(new java.awt.Dimension(37, 33));
        bn3.setBounds(399, 213, 37, 33);
        jLayeredPane1.add(bn3, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnMin.setIcon(buttonIcons.buttonIcon);
        bnMin.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnMin.setWhiteLabel("-");
        bnMin.setBlueLabel("x>0");
        bnMin.setKeyCode(48);
        bnMin.setName("");
        bnMin.setOriginalX(445);
        bnMin.setOriginalY(213);
        bnMin.setPreferredSize(new java.awt.Dimension(37, 33));
        bnMin.setBounds(445, 213, 37, 33);
        jLayeredPane1.add(bnMin, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnON.setIcon(buttonIcons.buttonIcon);
        bnON.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnON.setWhiteLabel("ON");
        bnON.setBlueLabel("");
        bnON.setKeyCode(65);
        bnON.setName("");
        bnON.setOriginalX(31);
        bnON.setOriginalY(264);
        bnON.setPreferredSize(new java.awt.Dimension(37, 33));
        bnON.setBounds(31, 264, 37, 33);
        jLayeredPane1.add(bnON, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnFKey.setIcon(yellowButtonIcons.buttonIcon);
        bnFKey.setPressedIcon(yellowButtonIcons.buttonPressedIcon);
        bnFKey.setWhiteLabel("f");
        bnFKey.setBlueLabel("");
        bnFKey.setKeyCode(66);
        bnFKey.setName("");
        bnFKey.setOriginalX(77);
        bnFKey.setOriginalY(264);
        bnFKey.setPreferredSize(new java.awt.Dimension(37, 33));
        bnFKey.setBounds(77, 264, 37, 33);
        jLayeredPane1.add(bnFKey, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnGKey.setIcon(blueButtonIcons.buttonIcon);
        bnGKey.setPressedIcon(blueButtonIcons.buttonPressedIcon);
        bnGKey.setWhiteLabel("g");
        bnGKey.setBlueLabel("");
        bnGKey.setKeyCode(67);
        bnGKey.setName("");
        bnGKey.setOriginalX(123);
        bnGKey.setOriginalY(264);
        bnGKey.setPreferredSize(new java.awt.Dimension(37, 33));
        bnGKey.setBounds(123, 264, 37, 33);
        jLayeredPane1.add(bnGKey, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnSTO.setIcon(buttonIcons.buttonIcon);
        bnSTO.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnSTO.setWhiteLabel("STO");
        bnSTO.setBlueLabel("\u2B9C"); // ⮜
        bnSTO.setKeyCode(68);
        bnSTO.setName("");
        bnSTO.setOriginalX(169);
        bnSTO.setOriginalY(264);
        bnSTO.setPreferredSize(new java.awt.Dimension(37, 33));
        bnSTO.setBounds(169, 264, 37, 33);
        jLayeredPane1.add(bnSTO, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnRCL.setIcon(buttonIcons.buttonIcon);
        bnRCL.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnRCL.setWhiteLabel("RCL");
        bnRCL.setBlueLabel("\u2B9E"); // ⮞
        bnRCL.setKeyCode(69);
        bnRCL.setName("");
        bnRCL.setOriginalX(215);
        bnRCL.setOriginalY(264);
        bnRCL.setPreferredSize(new java.awt.Dimension(37, 33));
        bnRCL.setBounds(215, 264, 37, 33);
        jLayeredPane1.add(bnRCL, javax.swing.JLayeredPane.PALETTE_LAYER);

        bn0.setIcon(buttonIcons.buttonIcon);
        bn0.setPressedIcon(buttonIcons.buttonPressedIcon);
        bn0.setWhiteLabel("0");
        bn0.setBlueLabel("x\u2260y");  // x≠y
        bn0.setName("");
        bn0.setOriginalX(307);
        bn0.setOriginalY(264);
        bn0.setPreferredSize(new java.awt.Dimension(37, 33));
        bn0.setBounds(307, 264, 37, 33);
        jLayeredPane1.add(bn0, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnDp.setIcon(buttonIcons.buttonIcon);
        bnDp.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnDp.setWhiteLabel("\u2219");   // ∙
        bnDp.setBlueLabel("x\u22600");  // x≠0
        bnDp.setKeyCode(72);
        bnDp.setName("");
        bnDp.setOriginalX(353);
        bnDp.setOriginalY(264);
        bnDp.setPreferredSize(new java.awt.Dimension(37, 33));
        bnDp.setBounds(353, 264, 37, 33);
        jLayeredPane1.add(bnDp, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnCHS.setIcon(buttonIcons.buttonIcon);
        bnCHS.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnCHS.setWhiteLabel("CHS");
        bnCHS.setBlueLabel("x=y");
        bnCHS.setKeyCode(73);
        bnCHS.setName("");
        bnCHS.setOriginalX(399);
        bnCHS.setOriginalY(264);
        bnCHS.setPreferredSize(new java.awt.Dimension(37, 33));
        bnCHS.setBounds(399, 264, 37, 33);
        jLayeredPane1.add(bnCHS, javax.swing.JLayeredPane.PALETTE_LAYER);

        bnPls.setIcon(buttonIcons.buttonIcon);
        bnPls.setPressedIcon(buttonIcons.buttonPressedIcon);
        bnPls.setWhiteLabel("+");
        bnPls.setBlueLabel("x=0");
        bnPls.setKeyCode(64);
        bnPls.setName("");
        bnPls.setOriginalX(445);
        bnPls.setOriginalY(264);
        bnPls.setPreferredSize(new java.awt.Dimension(37, 33));
        bnPls.setBounds(445, 264, 37, 33);
        jLayeredPane1.add(bnPls, javax.swing.JLayeredPane.PALETTE_LAYER);

        pnCalcFace.yellowText = new CalcFace.YellowText[] {
                new CalcFace.YellowText(bnA, "SL"),
                new CalcFace.YellowText(bnB, "SR"),
                new CalcFace.YellowText(bnC, "RL"),
                new CalcFace.YellowText(bnD, "RR"),
                new CalcFace.YellowText(bnE, "RLn"),
                new CalcFace.YellowText(bnF, "RRn"),
                new CalcFace.YellowText(bn7, "MASKL"),
                new CalcFace.YellowText(bn8, "MASKR"),
                new CalcFace.YellowText(bn9, "RMD"),
                new CalcFace.YellowText(bnDiv, "XOR"),
                new CalcFace.YellowText(bnGSB, "x\u2B0C(i)"),   // x⬌(i)
                new CalcFace.YellowText(bnGTO, "x\u2B0CI"),   // x⬌I
                new CalcFace.YellowMultiText(scaleInfo, bnHEX, bnBIN, 0, "SHOW"),
                new CalcFace.YellowText(bn4, "SB"),
                new CalcFace.YellowText(bn5, "CB"),
                new CalcFace.YellowText(bn6, "B?"),
                new CalcFace.YellowText(bnMul, "AND"),
                new CalcFace.YellowText(bnRS, "(i)"),
                new CalcFace.YellowText(bnSST, "I"),
                new CalcFace.YellowMultiText(scaleInfo, bnRol, bnBSP, 1, "CLEAR"),
                new CalcFace.YellowText(bnRol, "PRGM"),
                new CalcFace.YellowText(bnXY, "REG"),
                new CalcFace.YellowText(bnBSP, "PREFIX"),
                new CalcFace.YellowText(bnEnt, "WINDOW"),
                new CalcFace.YellowMultiText(scaleInfo, bn1, bn3, 1, "SET COMPL"),
                new CalcFace.YellowText(bn1, "1'S"),
                new CalcFace.YellowText(bn2, "2'S"),
                new CalcFace.YellowText(bn3, "UNSGN"),
                new CalcFace.YellowText(bnMin, "NOT"),
                new CalcFace.YellowText(bnSTO, "WSIZE"),
                new CalcFace.YellowText(bnRCL, "FLOAT"),
                new CalcFace.YellowText(bn0, "MEM"),
                new CalcFace.YellowText(bnDp, "STATUS"),
                new CalcFace.YellowText(bnCHS, "EEX"),
                new CalcFace.YellowText(bnPls, "OR")
        };

        mFile.setText("File");

        mFileOpen.setText("Open State...");
        mFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFileOpenActionPerformed(evt);
            }
        });
        mFile.add(mFileOpen);
        mFile.add(jSeparator1);

        mFileSave.setText("Save State");
        mFileSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFileSaveActionPerformed(evt);
            }
        });
        mFile.add(mFileSave);

        mFileSaveAs.setText("Save State As...");
        mFileSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFileSaveAsActionPerformed(evt);
            }
        });
        mFile.add(mFileSaveAs);
        mFile.add(jSeparator2);

        mFilePrint.setText("Print Program");
        mFilePrint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFilePrintActionPerformed(evt);
            }
        });
        mFile.add(mFilePrint);

        mFileSetup.setText("Page Setup...");
        mFileSetup.setEnabled(false);
        mFileSetup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFileSetupActionPerformed(evt);
            }
        });
        mFile.add(mFileSetup);
        mFile.add(jSeparator3);

        mFileExit.setText("Exit");
        mFileExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFileExitActionPerformed(evt);
            }
        });
        mFile.add(mFileExit);

        mbMain.add(mFile);

        mEdit.setText("Edit");

        mEditCopy.setText("Copy");
        mEditCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mEditCopyActionPerformed(evt);
            }
        });
        mEdit.add(mEditCopy);

        mEditPaste.setText("Paste");
        mEditPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mEditPasteActionPerformed(evt);
            }
        });
        mEdit.add(mEditPaste);

        mbMain.add(mEdit);

        mMode.setText("Mode");

        mModeFloat.setText("Floating Point");
        mModeFloat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeFloatActionPerformed(evt);
            }
        });
        mMode.add(mModeFloat);

        mModeHex.setText("Hexadecimal");
        mModeHex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeHexActionPerformed(evt);
            }
        });
        mMode.add(mModeHex);

        mModeDec.setText("Decimal");
        mModeDec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeDecActionPerformed(evt);
            }
        });
        mMode.add(mModeDec);

        mModeOct.setText("Octal");
        mModeOct.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeOctActionPerformed(evt);
            }
        });
        mMode.add(mModeOct);

        mModeBin.setText("Binary");
        mModeBin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeBinActionPerformed(evt);
            }
        });
        mMode.add(mModeBin);
        mMode.add(jSeparator4);

        mModeSI.setText("Scientific Notation");
        mModeSI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeSIActionPerformed(evt);
            }
        });
        mMode.add(mModeSI);

        mModePrecision.setText("Floating Point Precision");

        mModeP0.setSelected(true);
        mModeP0.setText("0");
        mModeP0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP0ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP0);

        mModeP1.setSelected(true);
        mModeP1.setText("1");
        mModeP1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP1ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP1);

        mModeP2.setSelected(true);
        mModeP2.setText("2");
        mModeP2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP2ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP2);

        mModeP3.setSelected(true);
        mModeP3.setText("3");
        mModeP3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP3ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP3);

        mModeP4.setSelected(true);
        mModeP4.setText("4");
        mModeP4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP4ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP4);

        mModeP5.setSelected(true);
        mModeP5.setText("5");
        mModeP5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP5ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP5);

        mModeP6.setSelected(true);
        mModeP6.setText("6");
        mModeP6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP6ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP6);

        mModeP7.setSelected(true);
        mModeP7.setText("7");
        mModeP7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP7ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP7);

        mModeP8.setSelected(true);
        mModeP8.setText("8");
        mModeP8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP8ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP8);

        mModeP9.setSelected(true);
        mModeP9.setText("9");
        mModeP9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mModeP9ActionPerformed(evt);
            }
        });
        mModePrecision.add(mModeP9);

        mMode.add(mModePrecision);

        mbMain.add(mMode);

        mOptions.setText("Options");

        mOptionClear.setText("Clear State");
        mOptionClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOptionClearActionPerformed(evt);
            }
        });
        mOptions.add(mOptionClear);

        mOptionSave.setText("Save On Exit");
        mOptionSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOptionSaveActionPerformed(evt);
            }
        });
        mOptions.add(mOptionSave);
        mOptions.add(jSeparator5);

        mOption8bit.setText("8 Bit Word");
        mOption8bit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOption8bitActionPerformed(evt);
            }
        });
        mOptions.add(mOption8bit);

        mOption16bit.setText("16 Bit Word");
        mOption16bit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOption16bitActionPerformed(evt);
            }
        });
        mOptions.add(mOption16bit);

        mOption32bit.setText("32 Bit Word");
        mOption32bit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOption32bitActionPerformed(evt);
            }
        });
        mOptions.add(mOption32bit);

        mOption64bit.setText("64 Bit Word");
        mOption64bit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOption64bitActionPerformed(evt);
            }
        });
        mOptions.add(mOption64bit);
        mOptions.add(jSeparator6);

        mOption1sComp.setText("1's Complement");
        mOption1sComp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOption1sCompActionPerformed(evt);
            }
        });
        mOptions.add(mOption1sComp);

        mOption2sComp.setText("2's Complement");
        mOption2sComp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOption2sCompActionPerformed(evt);
            }
        });
        mOptions.add(mOption2sComp);

        mOptionUnsigned.setText("Unsigned");
        mOptionUnsigned.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mOptionUnsignedActionPerformed(evt);
            }
        });
        mOptions.add(mOptionUnsigned);

        mbMain.add(mOptions);

        mFlags.setText("Flags");

        mFlagUser0.setText("User Flag 0");
        mFlagUser0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFlagUser0ActionPerformed(evt);
            }
        });
        mFlags.add(mFlagUser0);

        mFlagUser1.setText("User Flag 1");
        mFlagUser1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFlagUser1ActionPerformed(evt);
            }
        });
        mFlags.add(mFlagUser1);

        mFlagUser2.setText("User Flag 2");
        mFlagUser2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFlagUser2ActionPerformed(evt);
            }
        });
        mFlags.add(mFlagUser2);

        mFlagZeros.setText("Leading Zeros");
        mFlagZeros.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFlagZerosActionPerformed(evt);
            }
        });
        mFlags.add(mFlagZeros);

        mFlagCarry.setText("Carry Bit");
        mFlagCarry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFlagCarryActionPerformed(evt);
            }
        });
        mFlags.add(mFlagCarry);

        mFlagOverflow.setText("Overflow");
        mFlagOverflow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mFlagOverflowActionPerformed(evt);
            }
        });
        mFlags.add(mFlagOverflow);

        mbMain.add(mFlags);

        mHelp.setText("Help");

        mHelpContents.setText("Online User's Guide...");
        mHelpContents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mHelpContentsActionPerformed(evt);
            }
        });
        mHelp.add(mHelpContents);

        mHelpBackPanel.setText("Back Panel...");
        mHelpBackPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mHelpBackPanelActionPerformed(evt);
            }
        });
        mHelp.add(mHelpBackPanel);

        mHelpIndex.setText("Index");
        mHelpIndex.setEnabled(false);
        mHelp.add(mHelpIndex);
        mHelp.add(jSeparator7);

        mHelpAbout.setText("About...");
        mHelpAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mHelpAboutActionPerformed(evt);
            }
        });
        mHelp.add(mHelpAbout);

        mbMain.add(mHelp);

        setJMenuBar(mbMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLayeredPane1, CALC_WIDTH, CALC_WIDTH, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLayeredPane1, CALC_HEIGHT, CALC_HEIGHT, Short.MAX_VALUE)
        );
        for (Component comp : jLayeredPane1.getComponents()) {
            if (comp instanceof GButton) {
                GButton bn = (GButton) comp;
                bn.setScaleInfo(scaleInfo);
            }
        }

        setFonts();
        pnCalcFace.resize(scaleInfo);
        pack();
        setMinimumSize(getSize());
    }

    // move and resize the controls to match the new size of the form
    private void fmMain_Resized(java.awt.event.ComponentEvent evt)  throws InterruptedException {
        int x, y, width, height;

        Rectangle bounds = getContentPane().getBounds();

        if (bounds.equals(lastFaceBounds)) {
            return;
        }
        lastFaceBounds = bounds;

        // resize the Layered Panel and Label
        jLayeredPane1.setBounds(0, 0, bounds.width, bounds.height);
        pnCalcFace.setBounds(0, 0, bounds.width, bounds.height);

        if (512 * bounds.width / CALC_WIDTH <= 512 * bounds.height / CALC_HEIGHT) {
            scaleInfo.drawScaleNumerator = bounds.width;
            scaleInfo.drawScaleDenominator = CALC_WIDTH;
        } else {
            scaleInfo.drawScaleNumerator = bounds.height;
            scaleInfo.drawScaleDenominator = CALC_HEIGHT;
        }
        scaleInfo.drawScaleNumeratorX = bounds.width;
        scaleInfo.drawScaleDenominatorX = CALC_WIDTH;
        scaleInfo.drawScaleNumeratorY = bounds.height;
        scaleInfo.drawScaleDenominatorY = CALC_HEIGHT;

        MediaTracker tracker = new MediaTracker(this);
        Image image;

        // now resize the background graphics in the Label
        image = calcFaceImage.getScaledInstance(bounds.width, bounds.height, Image.SCALE_SMOOTH);
        tracker.addImage(image, 1);
        pnCalcFace.setIcon(new ImageIcon(image));
        image = jupiterIconImage.getScaledInstance(scaleInfo.scale(jupiterIconImage.getWidth(null))/4,
                                        scaleInfo.scale(jupiterIconImage.getHeight(null))/4,
                                        Image.SCALE_SMOOTH);
        tracker.addImage(image, 2);
        jupiterIcon = new ImageIcon(image);

        width = BUTTON_WIDTH * bounds.width / CALC_WIDTH;
        height = BUTTON_HEIGHT * bounds.height / CALC_HEIGHT;
        buttonIcons.scaleTo(width, height, tracker, 3);
        yellowButtonIcons.scaleTo(width, height, tracker, 5);
        blueButtonIcons.scaleTo(width, height, tracker, 7);

        // resize and move the buttons
        for (Component comp : jLayeredPane1.getComponents()) {
            if (comp instanceof GButton) {
                GButton bn = (GButton) comp;

                // get the new bounds
                int button_height;
                x = bn.getOriginalX() * this.getContentPane().getBounds().width / CALC_WIDTH;
                y = bn.getOriginalY() * this.getContentPane().getBounds().height / CALC_HEIGHT;

                // special case for the Enter key
                if (bn.getKeyCode() == 54) {
                    button_height = 84 * this.getContentPane().getBounds().height / CALC_HEIGHT;
                } else {
                    button_height = height;
                }

                bn.setBounds(x, y, width, button_height);
                if (bn == bnEnt) {
                    enterButtonIcons.scaleTo(width, button_height, tracker, 9);
                    bn.setIcon(enterButtonIcons.buttonIcon);
                    bn.setPressedIcon(enterButtonIcons.buttonPressedIcon);
                } else if (bn == bnFKey) {
                    bn.setIcon(yellowButtonIcons.buttonIcon);
                    bn.setPressedIcon(yellowButtonIcons.buttonPressedIcon);
                } else if (bn == bnGKey) {
                    bn.setIcon(blueButtonIcons.buttonIcon);
                    bn.setPressedIcon(blueButtonIcons.buttonPressedIcon);
                } else {
                    bn.setIcon(buttonIcons.buttonIcon);
                    bn.setPressedIcon(buttonIcons.buttonPressedIcon);
                }
            }
        }
        tracker.waitForAll();
        setFonts();

        // Move and resize the display textbox
        x = 54 * this.getContentPane().getBounds().width / CALC_WIDTH;
        y = 31 * this.getContentPane().getBounds().height / CALC_HEIGHT;
        width = this.getContentPane().getBounds().width * DISPLAY_WIDTH / CALC_WIDTH;
        height = this.getContentPane().getBounds().height * DISPLAY_HEIGHT / CALC_HEIGHT;
        tbDisplay.setBounds(x, y, width, height);

        // Now we adjust the font size to match.  This can be tricky since default DPI settings 
        // may vary.  So, we do a search of sizes that match a test string that will fill 
        // the display fully.
        Font ft = new Font("Lucidia Sans Typewriter", Font.BOLD, 12);
        float fontsize = calculateDisplayFont(ft, "888888888888888888888888", width);
        tbDisplayFontLarge = ft.deriveFont(fontsize);
        fontsize = calculateDisplayFont(ft, "00000000 00000000 00000000 00000000 .b.", width);
        tbDisplayFontSmall = ft.deriveFont(fontsize);
        SetDisplayText(tbDisplay.getText());    // to change the font

        // A slightly smaller font for the Annunciator labels
        Font f = new Font("Tahoma", Font.PLAIN, 7);
        Font temp = f.deriveFont((fontsize * 0.75F));

        // recalculate the location for the labels
        y = (int) (55 * this.getContentPane().getBounds().height / CALC_HEIGHT);
        width = 30 * this.getContentPane().getBounds().width / CALC_WIDTH;
        height = 12 * this.getContentPane().getBounds().height / CALC_HEIGHT;

        // resize and move the labels
        for (Component comp : jLayeredPane1.getComponents()) {
            if (comp instanceof JLabel && comp != jupiterLabel) {
                JLabel lb = (JLabel) comp;
                if (lb.getName() != null) {
                    x = Integer.parseInt(lb.getName()) * this.getContentPane().getBounds().width / CALC_WIDTH;
                    lb.setBounds(x, y, width, height);
                    lb.setFont(temp);
                }
            }
        }
        jupiterLabel.setBounds(scaleInfo.scaleX(455), scaleInfo.scaleY(21),
                               scaleInfo.scaleX(34), scaleInfo.scaleY(45));

        pnCalcFace.resize(scaleInfo);
    }//GEN-LAST:event_fmMain_Resized

    private void setFonts() {
        scaleInfo.whiteFont = new Font("Lucidia Sans", Font.BOLD, scaleInfo.scale(28)/2);
        scaleInfo.whiteFontMetrics = bn0.getFontMetrics(scaleInfo.whiteFont);
        scaleInfo.blueFont = new Font("Lucidia Sans", Font.BOLD, scaleInfo.scale(9));
        scaleInfo.blueFontMetrics = bn0.getFontMetrics(scaleInfo.blueFont);
        for (Component comp : jLayeredPane1.getComponents()) {
            if (comp instanceof GButton) {
                GButton bn = (GButton) comp;
                bn.alignText();
            }
        }
        scaleInfo.yellowFont = scaleInfo.blueFont;
        scaleInfo.yellowFontMetrics = scaleInfo.blueFontMetrics;
        scaleInfo.faceFont = new Font("Lucidia Sans", Font.BOLD, scaleInfo.scale(12));
        scaleInfo.faceFontMetrics = pnCalcFace.getFontMetrics(scaleInfo.faceFont);
        jupiterFont = new Font("Lucidia Sans", Font.BOLD, scaleInfo.scale(11));
        jupiterFontMetrics = jupiterLabel.getFontMetrics(jupiterFont);
        jupiterTextWidth = jupiterFontMetrics.stringWidth(JUPITER_TEXT);
    }

    private float calculateDisplayFont(Font ft, String str, int width) {
        int small = 1;
        int large = scaleInfo.scale(55) * 4;

        while (large - small > 1) {
            int mid = (large + small) / 2;
            float fontsize = mid * 0.25f;
            Font temp = ft.deriveFont(fontsize);
            FontMetrics metrics = tbDisplay.getFontMetrics(temp);
            int w = metrics.stringWidth(str);
            if (w <= width - tbDisplay.getInsets().left - tbDisplay.getInsets().right) {
                small = mid;
            } else {
                large = mid;
            }
        }
        return small * 0.25f;
    }

    // File Open
    private void mFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFileOpenActionPerformed
        JFileChooser fileopen = new JFileChooser();
        FileNameExtensionFilter xml_filter = new FileNameExtensionFilter("XML files", "xml");
        fileopen.addChoosableFileFilter(xml_filter);

        if (fileopen.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            LoadState(fileopen.getSelectedFile().getPath());
            UpdateMenu();
            ProcessPacket(c.ProcessKey(-1));
        }
    }//GEN-LAST:event_mFileOpenActionPerformed

    // File Save State
    private void mFileSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFileSaveActionPerformed
        SaveState();
    }//GEN-LAST:event_mFileSaveActionPerformed

    // File Save State As
    private void mFileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFileSaveAsActionPerformed
        JFileChooser filesave = new JFileChooser();
        FileNameExtensionFilter xml_filter = new FileNameExtensionFilter("XML files", "xml");
        filesave.addChoosableFileFilter(xml_filter);

        if (filesave.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            String path = filesave.getSelectedFile().getPath();
            if (!path.toLowerCase().endsWith(".xml")) {
                path += ".xml";
            }
            SaveState(path);
        }
    }//GEN-LAST:event_mFileSaveAsActionPerformed

    // File Print Program
    private void mFilePrintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFilePrintActionPerformed
        // a quick sanity check...
        if (cs.getPrgmMemory().isEmpty()) {
            JOptionPane.showMessageDialog(null, "There are no Program lines to print", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextArea prgm = new JTextArea();

        // format with line numbers and a newline
        int lc = 1;
        for (String line : cs.getPrgmMemory()) {
            prgm.append(String.format("%1$03d- %2$s\n", lc, line));
            lc += 1;
        }

        try {
            prgm.print();
        } catch (PrinterException ex) {
            Logger.getLogger(fmMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_mFilePrintActionPerformed

    // File Page Setup
    private void mFileSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFileSetupActionPerformed
        // Not needed here... is included in the Print dialog
    }//GEN-LAST:event_mFileSetupActionPerformed

    // File Exit
    private void mFileExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFileExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_mFileExitActionPerformed

    // We are closing!
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (cs.isSaveOnExit()) {
            // stop any running application
            if (cs.isPrgmRunning()) {
                cs.setPrgmRunning(false);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
                }
            }
            SaveState();
        }
    }//GEN-LAST:event_formWindowClosing

    // Copy to the clipboard
    private void mEditCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mEditCopyActionPerformed
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        // Hummm... should I strip the "mode indicators" off the
        // end to make it a bit more normal?
        StringSelection ss = new StringSelection(tbDisplay.getText());
        clipboard.setContents(ss, null);
    }//GEN-LAST:event_mEditCopyActionPerformed

    // Paste from the clipboard
    private void mEditPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mEditPasteActionPerformed
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

        try {
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String buf = (String) t.getTransferData(DataFlavor.stringFlavor);
                // limit to 35 characters
                if (buf.length() > 35) {
                    buf = buf.substring(buf.length() - 35);
                }
                c.ImportRawDisplay(buf);
                // v6.0.6 - 1 Mar 2014
                // process a dummy key to refresh the display
                ProcessPacket(c.ProcessKey(-1));
            }
        } catch (UnsupportedFlavorException e) {
            Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
        } catch (IOException e) {
            Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error Converting Pasted Data\n" + ex.getMessage(), "Paste Error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_mEditPasteActionPerformed

    // Change to Float mode
    private void mModeFloatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeFloatActionPerformed
        cs.setOpMode(CalcState.CalcOpMode.Float);
        mModeFloat.setSelected(true);
        mModeHex.setSelected(false);
        mModeDec.setSelected(false);
        mModeOct.setSelected(false);
        mModeBin.setSelected(false);
        mModeSI.setSelected(false);

        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeFloatActionPerformed

    // Change to Hex mode
    private void mModeHexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeHexActionPerformed
        cs.setOpMode(CalcState.CalcOpMode.Hex);
        mModeFloat.setSelected(false);
        mModeHex.setSelected(true);
        mModeDec.setSelected(false);
        mModeOct.setSelected(false);
        mModeBin.setSelected(false);
        mModeSI.setSelected(false);

        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeHexActionPerformed

    // Change to Dec mode
    private void mModeDecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeDecActionPerformed
        cs.setOpMode(CalcState.CalcOpMode.Dec);
        mModeFloat.setSelected(false);
        mModeHex.setSelected(false);
        mModeDec.setSelected(true);
        mModeOct.setSelected(false);
        mModeBin.setSelected(false);
        mModeSI.setSelected(false);

        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeDecActionPerformed

    // Change to Oct mode
    private void mModeOctActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeOctActionPerformed
        cs.setOpMode(CalcState.CalcOpMode.Oct);
        mModeFloat.setSelected(false);
        mModeHex.setSelected(false);
        mModeDec.setSelected(false);
        mModeOct.setSelected(true);
        mModeBin.setSelected(false);
        mModeSI.setSelected(false);

        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeOctActionPerformed

    // Change to Bin mode
    private void mModeBinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeBinActionPerformed
        cs.setOpMode(CalcState.CalcOpMode.Bin);
        mModeFloat.setSelected(false);
        mModeHex.setSelected(false);
        mModeDec.setSelected(false);
        mModeOct.setSelected(false);
        mModeBin.setSelected(true);
        mModeSI.setSelected(false);

        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeBinActionPerformed

    // Change to Scientific Notation (in Float mode)
    private void mModeSIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeSIActionPerformed
        cs.setOpMode(CalcState.CalcOpMode.Float);
        mModeFloat.setSelected(true);
        mModeHex.setSelected(false);
        mModeDec.setSelected(false);
        mModeOct.setSelected(false);
        mModeBin.setSelected(false);
        if (cs.getFloatPrecision() == Calculator.k.KeyDp.index()) {
            // we store the "old" precision in the menu's Name field!!!
            cs.setFloatPrecision(Integer.parseInt(mModePrecision.getName()));
            mModeSI.setSelected(false);
        } else {
            cs.setFloatPrecision(Calculator.k.KeyDp.index());
            mModeSI.setSelected(true);
        }

        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeSIActionPerformed

    // Set float precision
    private void mModeP0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP0ActionPerformed
        cs.setFloatPrecision(0);
        mModeP0.setSelected(true);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("0");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP0ActionPerformed

    // Set float precision
    private void mModeP1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP1ActionPerformed
        cs.setFloatPrecision(1);
        mModeP0.setSelected(false);
        mModeP1.setSelected(true);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("1");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP1ActionPerformed

    // Set float precision
    private void mModeP2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP2ActionPerformed
        cs.setFloatPrecision(2);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(true);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("2");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP2ActionPerformed

    // Set float precision
    private void mModeP3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP3ActionPerformed
        cs.setFloatPrecision(3);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(true);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("3");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP3ActionPerformed

    // Set float precision
    private void mModeP4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP4ActionPerformed
        cs.setFloatPrecision(4);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(true);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("4");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP4ActionPerformed

    // Set float precision
    private void mModeP5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP5ActionPerformed
        cs.setFloatPrecision(5);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(true);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("5");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP5ActionPerformed

    // Set float precision
    private void mModeP6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP6ActionPerformed
        cs.setFloatPrecision(6);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(true);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("6");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP6ActionPerformed

    // Set float precision
    private void mModeP7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP7ActionPerformed
        cs.setFloatPrecision(7);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(true);
        mModeP8.setSelected(false);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("7");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP7ActionPerformed

    // Set float precision
    private void mModeP8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP8ActionPerformed
        cs.setFloatPrecision(8);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(true);
        mModeP9.setSelected(false);

        // a rather odd approach for storing the precision
        mModePrecision.setName("8");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP8ActionPerformed

    // Set float precision
    private void mModeP9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mModeP9ActionPerformed
        cs.setFloatPrecision(9);
        mModeP0.setSelected(false);
        mModeP1.setSelected(false);
        mModeP2.setSelected(false);
        mModeP3.setSelected(false);
        mModeP4.setSelected(false);
        mModeP5.setSelected(false);
        mModeP6.setSelected(false);
        mModeP7.setSelected(false);
        mModeP8.setSelected(false);
        mModeP9.setSelected(true);

        // a rather odd approach for storing the precision
        mModePrecision.setName("9");
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mModeP9ActionPerformed

    // Reset to the "factory defaults"
    private void mOptionClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOptionClearActionPerformed
        // Load from the resource
        LoadState(fmMain.class.getResourceAsStream("/com/jovial/jrpn/CalcState.xml"));
        // Not STATE_FILE_NAME; that's the external name, and this is the internal
        // file with the defaults.
        UpdateMenu();
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOptionClearActionPerformed

    // Should we automatically save state upon exit?
    private void mOptionSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOptionSaveActionPerformed
        if (cs.isSaveOnExit()) {
            cs.setSaveOnExit(false);
            mOptionSave.setSelected(false);
        } else {
            cs.setSaveOnExit(true);
            mOptionSave.setSelected(true);
        }
    }//GEN-LAST:event_mOptionSaveActionPerformed

    // Set 8 bit word size
    private void mOption8bitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOption8bitActionPerformed
        cs.setWordSize(8);
        mOption8bit.setSelected(true);
        mOption16bit.setSelected(false);
        mOption32bit.setSelected(false);
        mOption64bit.setSelected(false);
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOption8bitActionPerformed

    // Set 16 bit word size
    private void mOption16bitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOption16bitActionPerformed
        cs.setWordSize(16);
        mOption8bit.setSelected(false);
        mOption16bit.setSelected(true);
        mOption32bit.setSelected(false);
        mOption64bit.setSelected(false);
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOption16bitActionPerformed

    // Set 32 bit word size
    private void mOption32bitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOption32bitActionPerformed
        cs.setWordSize(32);
        mOption8bit.setSelected(false);
        mOption16bit.setSelected(false);
        mOption32bit.setSelected(true);
        mOption64bit.setSelected(false);
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOption32bitActionPerformed

    // Set 64 bit word size
    private void mOption64bitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOption64bitActionPerformed
        cs.setWordSize(64);
        mOption8bit.setSelected(false);
        mOption16bit.setSelected(false);
        mOption32bit.setSelected(false);
        mOption64bit.setSelected(true);
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOption64bitActionPerformed

    // Set 1's complement Arithemtic mode
    private void mOption1sCompActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOption1sCompActionPerformed
        cs.setArithMode(CalcState.CalcArithMode.OnesComp);
        mOption1sComp.setSelected(true);
        mOption2sComp.setSelected(false);
        mOptionUnsigned.setSelected(false);
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOption1sCompActionPerformed

    // Set 2's complement Arithemtic mode
    private void mOption2sCompActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOption2sCompActionPerformed
        cs.setArithMode(CalcState.CalcArithMode.TwosComp);
        mOption1sComp.setSelected(false);
        mOption2sComp.setSelected(true);
        mOptionUnsigned.setSelected(false);
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOption2sCompActionPerformed

    // Set Unsigned Arithemtic mode
    private void mOptionUnsignedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOptionUnsignedActionPerformed
        cs.setArithMode(CalcState.CalcArithMode.Unsigned);
        mOption1sComp.setSelected(false);
        mOption2sComp.setSelected(false);
        mOptionUnsigned.setSelected(true);
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mOptionUnsignedActionPerformed

    // User Flag 0
    private void mFlagUser0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFlagUser0ActionPerformed
        if (cs.isFlag(CalcState.CalcFlag.User0)) {
            cs.setFlag(CalcState.CalcFlag.User0, false);
            mFlagUser0.setSelected(false);
        } else {
            cs.setFlag(CalcState.CalcFlag.User0, true);
            mFlagUser0.setSelected(true);
        }
    }//GEN-LAST:event_mFlagUser0ActionPerformed

    // User Flag 1
    private void mFlagUser1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFlagUser1ActionPerformed
        if (cs.isFlag(CalcState.CalcFlag.User1)) {
            cs.setFlag(CalcState.CalcFlag.User1, false);
            mFlagUser1.setSelected(false);
        } else {
            cs.setFlag(CalcState.CalcFlag.User1, true);
            mFlagUser1.setSelected(true);
        }
    }//GEN-LAST:event_mFlagUser1ActionPerformed

    // User Flag 2
    private void mFlagUser2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFlagUser2ActionPerformed
        if (cs.isFlag(CalcState.CalcFlag.User2)) {
            cs.setFlag(CalcState.CalcFlag.User2, false);
            mFlagUser2.setSelected(false);
        } else {
            cs.setFlag(CalcState.CalcFlag.User2, true);
            mFlagUser2.setSelected(true);
        }
    }//GEN-LAST:event_mFlagUser2ActionPerformed

    // Leading Zeros Flag
    private void mFlagZerosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFlagZerosActionPerformed
        if (cs.isFlag(CalcState.CalcFlag.LeadingZero)) {
            cs.setFlag(CalcState.CalcFlag.LeadingZero, false);
            mFlagZeros.setSelected(false);
        } else {
            cs.setFlag(CalcState.CalcFlag.LeadingZero, true);
            mFlagZeros.setSelected(true);
        }
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mFlagZerosActionPerformed

    // Carry bit flag
    private void mFlagCarryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFlagCarryActionPerformed
        if (cs.isFlag(CalcState.CalcFlag.Carry)) {
            cs.setFlag(CalcState.CalcFlag.Carry, false);
            mFlagCarry.setSelected(false);
        } else {
            cs.setFlag(CalcState.CalcFlag.Carry, true);
            mFlagCarry.setSelected(true);
        }
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mFlagCarryActionPerformed

    // Overflow flag
    private void mFlagOverflowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mFlagOverflowActionPerformed
        if (cs.isFlag(CalcState.CalcFlag.Overflow)) {
            cs.setFlag(CalcState.CalcFlag.Overflow, false);
            mFlagOverflow.setSelected(false);
        } else {
            cs.setFlag(CalcState.CalcFlag.Overflow, true);
            mFlagOverflow.setSelected(true);
        }
        ProcessPacket(c.ProcessKey(-1));
    }//GEN-LAST:event_mFlagOverflowActionPerformed

    // launch a browser for on-line help
    private void mHelpContentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mHelpContentsActionPerformed
        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            try {
                // TODO: Use the BrowserLauncher2 library
                desktop.browse(new java.net.URI(prop.getProperty("HelpURL")));
            } catch (Exception e) {
                Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
            }
        }

    }//GEN-LAST:event_mHelpContentsActionPerformed

    // Hey, that's me!
    private void mHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mHelpAboutActionPerformed
        fmAbout about = new fmAbout();
        about.setLocationByPlatform(true);
        about.setVisible(true);
    }//GEN-LAST:event_mHelpAboutActionPerformed

    private void mHelpBackPanelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mHelpBackPanelActionPerformed
        fmBackPanel backpanel = new fmBackPanel();
        backpanel.setLocationByPlatform(true);
        backpanel.setVisible(true);
    }//GEN-LAST:event_mHelpBackPanelActionPerformed

    // Map a few keys to their corresponding buttons
    private class MyDispatcher implements KeyEventDispatcher {

        @Override
        public boolean dispatchKeyEvent(java.awt.event.KeyEvent evt) {
            if (evt.getID() == java.awt.event.KeyEvent.KEY_RELEASED) {
                switch (evt.getKeyCode()) {
                    case java.awt.event.KeyEvent.VK_0:
                    case java.awt.event.KeyEvent.VK_NUMPAD0:
                        bn0.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_1:
                    case java.awt.event.KeyEvent.VK_NUMPAD1:
                        bn1.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_2:
                    case java.awt.event.KeyEvent.VK_NUMPAD2:
                        bn2.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_3:
                    case java.awt.event.KeyEvent.VK_NUMPAD3:
                        bn3.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_4:
                    case java.awt.event.KeyEvent.VK_NUMPAD4:
                        bn4.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_5:
                    case java.awt.event.KeyEvent.VK_NUMPAD5:
                        bn5.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_6:
                    case java.awt.event.KeyEvent.VK_NUMPAD6:
                        bn6.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_7:
                    case java.awt.event.KeyEvent.VK_NUMPAD7:
                        bn7.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_8:
                        if (evt.isShiftDown()) {
                            bnMul.doClick();
                        } else {
                            bn8.doClick();
                        }
                        break;
                    case java.awt.event.KeyEvent.VK_NUMPAD8:
                        bn8.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_9:
                    case java.awt.event.KeyEvent.VK_NUMPAD9:
                        bn9.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_A:
                        bnA.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_B:
                        bnB.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_C:
                        bnC.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_D:
                        bnD.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_E:
                        bnE.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_F:
                        bnF.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_ADD:
                    case java.awt.event.KeyEvent.VK_EQUALS:                       
                        bnPls.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_MULTIPLY:
                        bnMul.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_SUBTRACT:
                    case java.awt.event.KeyEvent.VK_MINUS:                        
                        bnMin.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_SLASH:
                    case java.awt.event.KeyEvent.VK_DIVIDE:
                        bnDiv.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_PERIOD:
                    case java.awt.event.KeyEvent.VK_DECIMAL:
                        bnDp.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_ENTER:
                        bnEnt.doClick();
                        break;
                   case java.awt.event.KeyEvent.VK_BACK_SPACE:
                        bnBSP.doClick();
                        break;
                    case java.awt.event.KeyEvent.VK_NUM_LOCK:
                    case java.awt.event.KeyEvent.VK_SHIFT:
                        // allow these without a beep
                        break;
                    default:
                        Toolkit.getDefaultToolkit().beep();
                        return false;
                }
            }
            return true;
        }
    }
        
    // the button click event
    private void GButton_Click(java.awt.event.ActionEvent evt) {
        GButton bn = (GButton) evt.getSource();
        DisplayPacket pkt;

        // any keystroke will terminate a running program
        cs.setPrgmRunning(false);

        // Send the keystrokes to the calculator engine
        pkt = c.ProcessKey(bn.getKeyCode());

        // show the results in the GUI
        ProcessPacket(pkt);

        // should we start a program?
        if (pkt.getStart() == DisplayPacket.StartType.RunProgram) {
            // fire up a background thread to run our program
            Thread t = new Thread(RunProgram);
            t.start();
        } else if (pkt.getStart() == DisplayPacket.StartType.RunLine) {
            // just run one line at a time
            RunLine();
        }
    }

    // process the return packet
    private void ProcessPacket(DisplayPacket pkt) {
        // Display the Shifted Annunciators
        if (pkt.isF_Annunciator()) {
            lbFKey.setText("f");
        } else {
            lbFKey.setText("");
        }
        if (pkt.isG_Annunciator()) {
            lbGKey.setText("g");
        } else {
            lbGKey.setText("");
        }

        // Display the System Flag Annunciators
        if (pkt.isCarry_Annunciator()) {
            lbCarry.setText("C");
        } else {
            lbCarry.setText("");
        }
        if (pkt.isOverflow_Annunciator()) {
            lbOverflow.setText("G");
        } else {
            lbOverflow.setText("");
        }

        // Display the Program Annunciator
        if (pkt.isPrgm_Annunciator()) {
            lbPrgm.setText("PRGM");
        } else {
            lbPrgm.setText("");
        }

        // Did the engine ask for a beep?
        if (!cs.isPrgmRunning() && pkt.isBeep()) {
            Toolkit.getDefaultToolkit().beep();
        }

        // Process the menu changes (if any)
        if (pkt.isMenuNeedsUpdating()) {
            UpdateMenu();
        }

        // Optionally show an alternate message for a short time
        if (pkt.getDelay() > 0) {
            SetDisplayText(pkt.getAlternateText());
            tbDisplay.update(tbDisplay.getGraphics());
            try {
                Thread.sleep(pkt.getDelay());
            } catch (InterruptedException e) {
                Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
            }
            SetDisplayText(pkt.getDisplayText());
        } else {
            if (pkt.getAlternateText() == null || pkt.getAlternateText().length() == 0) {
                SetDisplayText(pkt.getDisplayText());
            } else {
                SetDisplayText(pkt.getAlternateText());
            }
        }
    }

    // change the calculator display
    private void SetDisplayText(String text) {
        if (text.length() > 24) {
            tbDisplay.setFont(tbDisplayFontSmall);
        } else {
            tbDisplay.setFont(tbDisplayFontLarge);
        }
        tbDisplay.setText(text);
    }
    
    // Run a program starting at the current line number
    Runnable RunProgram = new Runnable() {

        @Override
        public void run() {
            while (cs.getPrgmPosition() < cs.getPrgmMemory().size()) {
                // execute the instructions
                if (RunLine()) {
                    // Some error occurred
                    break;
                }

                // prepare to process the next line
                cs.setPrgmPosition(cs.getPrgmPosition() + 1);

                // stop if somebody pressed a key
                if (cs.isPrgmRunning() == false) {
                    break;
                }
            }
            ProcessPacket(c.ProcessKey(-1));
        }
    };

    // Execute instructions at the current program line
    private boolean RunLine() {
        String line;
        int k1, k2, k3;
        DisplayPacket p;

        // A quick sanity check
        // v6.0.2 - 26 Apr 12
	if (cs.getPrgmPosition() < 0 || cs.getPrgmPosition() >= cs.getPrgmMemory().size()) {
            // if you "step off the edge", then just stop
            return true;
        }
        line = cs.getPrgmMemory().get(cs.getPrgmPosition());
        
        // Go to the current line and process the keys found there.
        // The line will be in 1 of 3 formats
        if (line.startsWith("      ")) {
            try {
                k1 = Integer.parseInt(line.substring(6, 8).trim(), 16);
            } catch (final Exception e) {
                // I'm anticipating that folks might edit the XML by hand
                // to make minor tweaks to a program.  So, we have to be
                // ready for a corrupted file
                JOptionPane.showMessageDialog(null, "Corrupted Program Memory at line " + cs.getPrgmPosition() + "\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return true;
            }
            p = c.ProcessKey(k1);
        } else if (line.startsWith("   ")) {
            try {
                k1 = Integer.parseInt(line.substring(3, 5).trim(), 16);
                k2 = Integer.parseInt(line.substring(6, 8).trim(), 16);
            } catch (final Exception e) {
                JOptionPane.showMessageDialog(null, "Corrupted Program Memory at line " + cs.getPrgmPosition() + "\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return true;
            }
            c.ProcessKey(k1);
            p = c.ProcessKey(k2);
        } else {
            try {
                k1 = Integer.parseInt(line.substring(0, 2).trim(), 16);
                k2 = Integer.parseInt(line.substring(3, 5).trim(), 16);
                k3 = Integer.parseInt(line.substring(6, 8).trim(), 16);
            } catch (final Exception e) {
                JOptionPane.showMessageDialog(null, "Corrupted Program Memory at line " + cs.getPrgmPosition() + "\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return true;
            }
            c.ProcessKey(k1);
            c.ProcessKey(k2);
            p = c.ProcessKey(k3);
        }

        // We only update the display if there is a pause.  No Annunciator
        // flags are updated while a program is running.
        if (p.getDelay() > 0) {
            if (p.getAlternateText() == null || p.getAlternateText().length() == 0) {
                SetDisplayText(p.getDisplayText());
                tbDisplay.update(tbDisplay.getGraphics());
                try {
                    Thread.sleep(p.getDelay());
                } catch (InterruptedException e) {
                    Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
                }
            } else {
                SetDisplayText(p.getAlternateText());
                tbDisplay.update(tbDisplay.getGraphics());
                try {
                    Thread.sleep(p.getDelay());
                } catch (InterruptedException e) {
                    Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
                }
                SetDisplayText(p.getDisplayText());
            }
        }
        return false;
    }

    // Save the Calculator state to the default user's profile
    private void SaveState() {
        File StateFile;

        // save to the user's version
        StateFile = new File(System.getProperty("user.home"), STATE_FILE_NAME);
        SaveState(StateFile.getPath());
    }

    // Save the Calculator state to a named file
    private void SaveState(String FileName) {
        BufferedWriter sw = null;

        try {
            sw = new BufferedWriter(new FileWriter(FileName));
            sw.write(cs.Serialize());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not save the state file\n" + ex.getMessage(), "Error Saving State", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (sw != null) {
                try {
                    sw.flush();
                    sw.close();
                } catch (IOException e) {
                    Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
                }
            }
        }
    }

    // Load the user's default state
    private static void LoadState() {
        File StateFile;

        // get the user's version
        StateFile = new File(System.getProperty("user.home"), STATE_FILE_NAME);

        // if the user doesn't have a state file, then use the default
        if (!StateFile.exists()) {
            StateFile = new File(System.getProperty("user.dir"), STATE_FILE_NAME);

            // if that doesn't exist, then just quit
            if (!StateFile.exists()) {
                return;
            }
        }
        LoadState(StateFile.getPath());
    }

    // Load a saved Calculator state file
    private static void LoadState(String FileName) {
        BufferedReader sr = null;

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            sr = new BufferedReader(new FileReader(FileName));
            while ((line = sr.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            cs.Deserialize(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not open the state file " + FileName + "\n" + ex.getMessage(), "Error Opening State", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (sr != null) {
                try {
                    sr.close();
                } catch (IOException e) {
                    Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
                }
            }
        }
    }

    // Load a saved state from a resource stream
    private static void LoadState(InputStream stream) {
        BufferedReader sr = null;

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            sr = new BufferedReader(new InputStreamReader(stream));

            while ((line = sr.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            cs.Deserialize(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Could not read the state file\n" + ex.getMessage(), "Error Reading State", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (sr != null) {
                try {
                    sr.close();
                } catch (IOException e) {
                    Logger.getLogger(fmMain.class.getName()).log(Level.WARNING, null, e);
                }
            }
        }
    }

    // Update all of the checkbox values in the menu
    private void UpdateMenu() {
        // The Mode Menu
        mModeFloat.setSelected(false);
        mModeHex.setSelected(false);
        mModeDec.setSelected(false);
        mModeOct.setSelected(false);
        mModeBin.setSelected(false);
        mModeSI.setSelected(false);

        switch (cs.getOpMode()) {
            case Float:
                mModeFloat.setSelected(true);
                if (cs.getFloatPrecision() == Calculator.k.KeyDp.index()) {
                    mModeSI.setSelected(true);
                } else {
                    mModeSI.setSelected(false);
                }
                break;
            case Hex:
                mModeHex.setSelected(true);
                break;
            case Dec:
                mModeDec.setSelected(true);
                break;
            case Oct:
                mModeOct.setSelected(true);
                break;
            case Bin:
                mModeBin.setSelected(true);
                break;
        }
        if (cs.getFloatPrecision() != Calculator.k.KeyDp.index()) {
            mModeP0.setSelected(false);
            mModeP1.setSelected(false);
            mModeP2.setSelected(false);
            mModeP3.setSelected(false);
            mModeP4.setSelected(false);
            mModeP5.setSelected(false);
            mModeP6.setSelected(false);
            mModeP7.setSelected(false);
            mModeP8.setSelected(false);
            mModeP9.setSelected(false);
            switch (cs.getFloatPrecision()) {
                case 0:
                    mModeP0.setSelected(true);
                    mModePrecision.setName("0");
                    break;
                case 1:
                    mModeP1.setSelected(true);
                    mModePrecision.setName("1");
                    break;
                case 2:
                    mModeP2.setSelected(true);
                    mModePrecision.setName("2");
                    break;
                case 3:
                    mModeP3.setSelected(true);
                    mModePrecision.setName("3");
                    break;
                case 4:
                    mModeP4.setSelected(true);
                    mModePrecision.setName("4");
                    break;
                case 5:
                    mModeP5.setSelected(true);
                    mModePrecision.setName("5");
                    break;
                case 6:
                    mModeP6.setSelected(true);
                    mModePrecision.setName("6");
                    break;
                case 7:
                    mModeP7.setSelected(true);
                    mModePrecision.setName("7");
                    break;
                case 8:
                    mModeP8.setSelected(true);
                    mModePrecision.setName("8");
                    break;
                case 9:
                    mModeP9.setSelected(true);
                    mModePrecision.setName("9");
                    break;
            }
        }

        // The Options Menu
        mOptionSave.setSelected(cs.isSaveOnExit());
        mOption8bit.setSelected(false);
        mOption16bit.setSelected(false);
        mOption32bit.setSelected(false);
        mOption64bit.setSelected(false);

        switch (cs.getWordSize()) {
            case 8:
                mOption8bit.setSelected(true);
                break;
            case 16:
                mOption16bit.setSelected(true);
                break;
            case 32:
                mOption32bit.setSelected(true);
                break;
            case 64:
                mOption64bit.setSelected(true);
                break;
            // unlike a lot of other menu items, this one may
            // not have anything checked
        }
        mOption1sComp.setSelected(false);
        mOption2sComp.setSelected(false);
        mOptionUnsigned.setSelected(false);

        switch (cs.getArithMode()) {
            case OnesComp:
                mOption1sComp.setSelected(true);
                break;
            case TwosComp:
                mOption2sComp.setSelected(true);
                break; case Unsigned: mOptionUnsigned.setSelected(true); break;
        }

        // The Flags Menu
        mFlagUser0.setSelected(cs.isFlag(CalcState.CalcFlag.User0));
        mFlagUser1.setSelected(cs.isFlag(CalcState.CalcFlag.User1));
        mFlagUser2.setSelected(cs.isFlag(CalcState.CalcFlag.User2));
        mFlagZeros.setSelected(cs.isFlag(CalcState.CalcFlag.LeadingZero));
        mFlagCarry.setSelected(cs.isFlag(CalcState.CalcFlag.Carry));
        mFlagOverflow.setSelected(cs.isFlag(CalcState.CalcFlag.Overflow));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(fmMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(fmMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(fmMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(fmMain.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    final fmMain main_form = new fmMain();
                    main_form.setLocationByPlatform(true);
                    main_form.setVisible(true);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private GButton bn0;
    private GButton bn1;
    private GButton bn2;
    private GButton bn3;
    private GButton bn4;
    private GButton bn5;
    private GButton bn6;
    private GButton bn7;
    private GButton bn8;
    private GButton bn9;
    private GButton bnA;
    private GButton bnB;
    private GButton bnBIN;
    private GButton bnBSP;
    private GButton bnC;
    private GButton bnCHS;
    private GButton bnD;
    private GButton bnDEC;
    private GButton bnDiv;
    private GButton bnDp;
    private GButton bnE;
    private GButton bnEnt;
    private GButton bnF;
    private GButton bnFKey;
    private GButton bnGKey;
    private GButton bnGSB;
    private GButton bnGTO;
    private GButton bnHEX;
    private GButton bnMin;
    private GButton bnMul;
    private GButton bnOCT;
    private GButton bnON;
    private GButton bnPls;
    private GButton bnRCL;
    private GButton bnRS;
    private GButton bnRol;
    private GButton bnSST;
    private GButton bnSTO;
    private GButton bnXY;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JLabel lbCarry;
    private javax.swing.JLabel lbFKey;
    private javax.swing.JLabel lbGKey;
    private javax.swing.JLabel lbOverflow;
    private javax.swing.JLabel lbPrgm;
    private javax.swing.JMenu mEdit;
    private javax.swing.JMenuItem mEditCopy;
    private javax.swing.JMenuItem mEditPaste;
    private javax.swing.JMenu mFile;
    private javax.swing.JMenuItem mFileExit;
    private javax.swing.JMenuItem mFileOpen;
    private javax.swing.JMenuItem mFilePrint;
    private javax.swing.JMenuItem mFileSave;
    private javax.swing.JMenuItem mFileSaveAs;
    private javax.swing.JMenuItem mFileSetup;
    private javax.swing.JCheckBoxMenuItem mFlagCarry;
    private javax.swing.JCheckBoxMenuItem mFlagOverflow;
    private javax.swing.JCheckBoxMenuItem mFlagUser0;
    private javax.swing.JCheckBoxMenuItem mFlagUser1;
    private javax.swing.JCheckBoxMenuItem mFlagUser2;
    private javax.swing.JCheckBoxMenuItem mFlagZeros;
    private javax.swing.JMenu mFlags;
    private javax.swing.JMenu mHelp;
    private javax.swing.JMenuItem mHelpAbout;
    private javax.swing.JMenuItem mHelpBackPanel;
    private javax.swing.JMenuItem mHelpContents;
    private javax.swing.JMenuItem mHelpIndex;
    private javax.swing.JMenu mMode;
    private javax.swing.JCheckBoxMenuItem mModeBin;
    private javax.swing.JCheckBoxMenuItem mModeDec;
    private javax.swing.JCheckBoxMenuItem mModeFloat;
    private javax.swing.JCheckBoxMenuItem mModeHex;
    private javax.swing.JCheckBoxMenuItem mModeOct;
    private javax.swing.JCheckBoxMenuItem mModeP0;
    private javax.swing.JCheckBoxMenuItem mModeP1;
    private javax.swing.JCheckBoxMenuItem mModeP2;
    private javax.swing.JCheckBoxMenuItem mModeP3;
    private javax.swing.JCheckBoxMenuItem mModeP4;
    private javax.swing.JCheckBoxMenuItem mModeP5;
    private javax.swing.JCheckBoxMenuItem mModeP6;
    private javax.swing.JCheckBoxMenuItem mModeP7;
    private javax.swing.JCheckBoxMenuItem mModeP8;
    private javax.swing.JCheckBoxMenuItem mModeP9;
    private javax.swing.JMenu mModePrecision;
    private javax.swing.JCheckBoxMenuItem mModeSI;
    private javax.swing.JCheckBoxMenuItem mOption16bit;
    private javax.swing.JCheckBoxMenuItem mOption1sComp;
    private javax.swing.JCheckBoxMenuItem mOption2sComp;
    private javax.swing.JCheckBoxMenuItem mOption32bit;
    private javax.swing.JCheckBoxMenuItem mOption64bit;
    private javax.swing.JCheckBoxMenuItem mOption8bit;
    private javax.swing.JMenuItem mOptionClear;
    private javax.swing.JCheckBoxMenuItem mOptionSave;
    private javax.swing.JCheckBoxMenuItem mOptionUnsigned;
    private javax.swing.JMenu mOptions;
    private javax.swing.JMenuBar mbMain;
    private CalcFace pnCalcFace;
    private javax.swing.JTextPane tbDisplay;
    // End of variables declaration//GEN-END:variables
}
