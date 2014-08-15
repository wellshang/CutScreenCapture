package com.xs.image;

import java.util.ArrayList;
import java.util.List;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;  
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Properties;
import java.io.FileInputStream;

public class PicCapture {
    private static Robot robot;
    private static BufferedImage pickedImage;
    private static ArrayList<BufferedImage> charImageList = new ArrayList<BufferedImage>();
    public static Map<String, BufferedImage> standardImageMap = null;
    private static int x_offset = 1190;
    private static int y_offset = 615;
    private static int wight = 120;
    private static int heigth = 30;
    private static int threshold = 200;
    private static int black = 0;
    private static int white = 16777215;
    private static int charWidth = 11;
    private static int charHeight = 30;
    private static int charNum = 6;
    private static int offset_x = 19;
    private static int offset_y = 0;
    private static int gap_x = 3;
    private static int patternNum = 10;
    private static Boolean bNoiseWipe = true;
    private static Boolean bSaveResult = false;
    private static Boolean bPrint = false;
    private static Boolean bCopy = false;
    private static Properties pro;

    private PicCapture() {
        standardImageMap = new HashMap<String, BufferedImage>();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println((new StringBuilder()).append("Internal Error: ")
                    .append(e).toString());
        }

        try {
            pro = new Properties();
            FileInputStream inputFile = new FileInputStream("properties");
            pro.load(inputFile);
            inputFile.close();
        } catch (FileNotFoundException e) {
            System.out.println("The properties file not found!");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Fail to load the properties file!");
            e.printStackTrace();
        }
    }

    public static BufferedImage capture(Rectangle rect)
            throws IOException {
        pickedImage = robot.createScreenCapture(rect);
        
        return pickedImage;
    }

    public static BufferedImage getPickedImage() {
        return pickedImage;
    }

    public static BufferedImage getCharImage(int index) {
        return (BufferedImage)charImageList.get(index);
    }

    public static BufferedImage getPicture()
            throws IOException {
        int green = 0;
        int red = 0;
        int blue = 0;
        int rgb;
        Object data = null;

        for (int n = 0; n < charNum; n++) {
            BufferedImage charImage = new BufferedImage(charWidth, charHeight,
                    pickedImage.getType());
            for (int i = charImage.getMinX(); i < charWidth; i++) {
                for (int j = charImage.getMinY(); j < charHeight; j++) {
                    data = pickedImage.getRaster().getDataElements(
                            i + offset_x + (gap_x + charWidth) * n,
                            j + offset_y, null);
                    red = pickedImage.getColorModel().getRed(data);
                    blue = pickedImage.getColorModel().getBlue(data);
                    green = pickedImage.getColorModel().getGreen(data);
                    rgb = (red * 3 + green * 6 + blue * 1) / 10;
                    if (rgb > threshold) {
                        rgb = white;
                    } else {
                        rgb = black;
                    }
                    charImage.setRGB(i, j, rgb);
                }
            }
            if (bNoiseWipe) {
                charImageList.add(handlePic(charImage));
            } else {
                charImageList.add(charImage);
            }
            saveImage(charImage, "charImage_" + n + ".bmp");
        }

        return pickedImage;
    }

    public static int getChar(int index)
            throws IOException {
        class Score{
            char id;
            int value; 
        }
        List <Score> scoreList = new ArrayList<Score>();

        if (null != charImageList) {
            for (int n = 0; n < patternNum; n++) {
                BufferedImage charImage = getCharImage(index);
                Score score = new Score();
                score.id = (char)n;
                score.value = getDiffValues(charImage, standardImageMap.get(String.valueOf(n)));
                if (bPrint) {
                    System.out.println("Pattern[" + n + "] score is " + score.value);
                }
                scoreList.add(score);
            }
        }

        Collections.sort(scoreList, new Comparator<Score>() {
            @Override
            public int compare(Score s1, Score s2) {
                return s1.value - s2.value;
            }
        });

        return scoreList.get(0).id;
    }

    public static BufferedImage handlePic(BufferedImage iputImage) throws IOException {
        BufferedImage outImage;
        String noistType = getNoiseType(pickedImage);
        if("CrossLine".equals(noistType)){
            List<Integer> noiseList1 = NoiseHandleFactory.getBaseNoiseHandlerFactory()
            .getNoiseHandle(NoisePattern.TRANSVERSE.value).getNoiseLines(iputImage);
            BufferedImage firstResBi = NoiseHandleFactory.getBaseNoiseHandlerFactory()
                    .getNoiseHandle(NoisePattern.TRANSVERSE.value).removeNoise(iputImage);
            List<Integer> noiseList2 = NoiseHandleFactory.getBaseNoiseHandlerFactory()
            .getNoiseHandle(NoisePattern.VERTICAL.value).getNoiseLines(firstResBi);
            BufferedImage secResBi =  NoiseHandleFactory.getBaseNoiseHandlerFactory()
                    .getNoiseHandle(NoisePattern.VERTICAL.value).removeNoise(firstResBi);
            BaseNoiseHandle bn = NoiseHandleFactory.getBaseNoiseHandlerFactory()
                    .getNoiseHandle(NoisePattern.CROSS.value);
            bn.setTransverseNoiseList(noiseList1);
            bn.setVerticalNoiseList(noiseList2);
            outImage = bn.removeNoise(secResBi);
        }else{
            outImage = NoiseHandleFactory.getBaseNoiseHandlerFactory()
                .getNoiseHandle(getNoiseType(iputImage)).removeNoise(iputImage);
        }
        
        return outImage;
    }

    public static void saveImage(BufferedImage image, String imagePath)
            throws IOException {
        if (bSaveResult) {
            return;
        }
        File file = new File(imagePath);
        String suffix = imagePath.substring(imagePath.lastIndexOf('.') + 1);
        ImageIO.write(image, suffix, file);
    }

    public static BufferedImage loadImage(String imagePath)
            throws IOException {
        File file = new File(imagePath);
        return (BufferedImage)ImageIO.read(file);
    }

    public static void loadPattern() throws IOException {
        for (int n = 0; n < 10; n++) {
            File inputFile = new File("D:\\Workspace\\Ge201401\\char_" + n + "_0.bmp");
            BufferedImage charImage = ImageIO.read(inputFile);
            standardImageMap.put(String.valueOf(n), charImage);
        }
    }

    public static int getDiffValues(BufferedImage inputBi, BufferedImage standardBi)
            throws IOException {
        int diffValues = 0;

        for (int i = 0; i < charWidth; i++) {
            for (int j = 0; j < charHeight; j++) {
                if (inputBi.getRGB(i, j) != standardBi.getRGB(i, j)) {
                    diffValues++;
                }
            }
        }
        return diffValues;
    }

    private static String getNoiseType(BufferedImage bi){
        List<Integer> transverseNoiseList = new ArrayList<Integer>();
        List<Integer> verticalNoiseList = new ArrayList<Integer>();
        for (int h = 0; h < bi.getHeight(); ++h) {
            boolean lineNoiseFlag = true;
            for (int w = 0; w < bi.getWidth(); ++w) {
                if (bi.getRGB(w, h) == -1) {
                    lineNoiseFlag = false;
                }

            }
            if (lineNoiseFlag) {
                transverseNoiseList.add(h);
            }
        }
       
        for (int w = 0; w < bi.getWidth(); ++w) {
            boolean lineNoiseFlag = true;
            for (int h = 0; h < bi.getHeight(); ++h) {
                if (bi.getRGB(w, h) == -1) {
                    lineNoiseFlag = false;
                }

            }
            if (lineNoiseFlag) {
                verticalNoiseList.add(w);
            }
        }

        if(transverseNoiseList.size()>1 && verticalNoiseList.size()>1){
            return NoisePattern.CROSS.value;
        }
        if(transverseNoiseList.size()>1){
            return NoisePattern.TRANSVERSE.value;
        }
        if(verticalNoiseList.size()>1){
            return NoisePattern.VERTICAL.value;
        }
        return NoisePattern.DIAGONAL.value;
    }

    private static String getProperty(String key)
            throws IOException {
        String value = pro.getProperty(key);
        if (bPrint) {
            System.out.println("The value of variable " + key + " is " + value);
        }

        return value;
    }

    private static void loadProperty()
            throws IOException {
        x_offset = Integer.valueOf(getProperty("x_offset"));
        y_offset = Integer.valueOf(getProperty("y_offset"));
        wight = Integer.valueOf(getProperty("wight"));
        heigth = Integer.valueOf(getProperty("heigth"));
        threshold = Integer.valueOf(getProperty("threshold"));
        black = Integer.valueOf(getProperty("black"));
        white = Integer.valueOf(getProperty("white"));
        charWidth = Integer.valueOf(getProperty("charWidth"));
        charHeight = Integer.valueOf(getProperty("charHeight"));
        charNum = Integer.valueOf(getProperty("charNum"));
        offset_x = Integer.valueOf(getProperty("offset_x"));
        offset_y = Integer.valueOf(getProperty("offset_y"));
        gap_x = Integer.valueOf(getProperty("gap_x"));
        patternNum = Integer.valueOf(getProperty("patternNum"));
        bNoiseWipe = Boolean.valueOf(getProperty("bNoiseWipe"));
        bSaveResult = Boolean.valueOf(getProperty("bSaveResult"));
        bPrint = Boolean.valueOf(getProperty("bPrint"));
        bCopy = Boolean.valueOf(getProperty("bCopy"));
    }

    public static void main(String args[]) throws Exception {
        PicCapture capture = new PicCapture();
        loadProperty();

        int screenWidth = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeigth = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height;
        
        System.out.println("The screen size is " + screenWidth + " * " + screenHeigth);

        Rectangle rect = new Rectangle(x_offset, y_offset, wight, heigth);
        long startTime = System.currentTimeMillis();
        capture(rect);
        loadPattern();
        getPicture();
        for (int i = 0; i < 6; i++) {
            System.out.println("The charactar[" + i + "] is " + getChar(i));
        }
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
        if (bCopy) {
            ClipboardCopyAction.setClipboardImage(getPicture());
        }
    }
}