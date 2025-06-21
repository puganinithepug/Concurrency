import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class q1 {

    // Parameters
    public static int t;
    public static int n;
    public static int width=4096;
    public static int height=4096;

    public static void main(String[] args) {
        try {

            // once we know what size we want we can creat an empty image
            BufferedImage outputimage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);

            // ------------------------------------
            // Your code would go here
            
            // The easiest mechanisms for getting and setting pixels are the
            // BufferedImage.setRGB(x,y,value) and getRGB(x,y) functions.
            // Consult the javadocs for other methods.

            // The getRGB/setRGB functions return/expect the pixel value in ARGB format, one byte per channel.  For example,
            //  int p = img.getRGB(x,y);
            // With the 32-bit pixel value you can extract individual colour channels by shifting and masking:
            //  int red = ((p>>16)&0xff);
            //  int green = ((p>>8)&0xff);
            //  int blue = (p&0xff);
            // If you want the alpha channel value it's stored in the uppermost 8 bits of the 32-bit pixel value
            //  int alpha = ((p>>24)&0xff);
            // Note that an alpha of 0 is transparent, and an alpha of 0xff is fully opaque.
            
            // ------------------------------------
            
            // Write out the image
            File outputfile = new File("outputimage.png");
            ImageIO.write(outputimage, "png", outputfile);

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }
}
