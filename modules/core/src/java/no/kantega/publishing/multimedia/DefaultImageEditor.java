package no.kantega.publishing.multimedia;

import no.kantega.commons.log.Log;
import no.kantega.publishing.common.data.Multimedia;
import no.kantega.publishing.common.data.enums.MultimediaType;
import no.kantega.publishing.multimedia.resizers.ImageResizeAlgorithm;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class DefaultImageEditor implements ImageEditor {
    ImageResizeAlgorithm imageResizeAlgorithm;

    private String defaultImageFormat = "png";

    private int jpgOutputQuality = 85;

    public Multimedia resizeMultimedia(Multimedia multimedia, int targetWidth, int targetHeight) throws IOException {
        return resizeAndCropMultimedia(multimedia, targetWidth, targetHeight, -1, -1, -1, -1);
    }
    public Multimedia resizeAndCropMultimedia(Multimedia multimedia, int targetWidth, int targetHeight, int cropx, int cropy, int cropwidth, int cropheight) throws IOException {
        if (multimedia.getType() == MultimediaType.MEDIA) {
            if (multimedia.getMimeType() != null && multimedia.getMimeType().getType().contains("image")) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(multimedia.getData()));

                // Make sure target-size is valid

                int w = image.getWidth();
                int h = image.getHeight();

                if (targetWidth == -1 || targetWidth > w) {
                    targetWidth = w;
                }

                if (targetHeight == -1 || targetHeight > h) {
                    targetHeight = h;
                }

                // make sure target is smaller that src-image
                if (w < targetWidth || h < targetHeight) {
                    if (w < targetWidth) {
                        w = targetWidth;
                    }

                    if (h < targetHeight) {
                        h = targetHeight;
                    }
                }

                // Keep aspect ratio
                double thumbRatio = (double) targetWidth / (double) targetHeight;
                double imageRatio = (double) w / (double) h;
                if (thumbRatio < imageRatio) {
                    targetHeight = (int) (targetWidth / imageRatio);
                } else {
                    targetWidth = (int) (targetHeight * imageRatio);
                }

                // Resize image
                long start = new Date().getTime();
                BufferedImage resizedImage = imageResizeAlgorithm.resizeImage(image, targetWidth, targetHeight);
                long end = new Date().getTime();

                Log.info(this.getClass().getName(), "Resized:" + multimedia.getName() + " in " + (end-start) + " ms", null, null);
                if (cropx != -1 && cropy != -1 && cropwidth != -1 && cropheight != -1) {
                    // Crop image
                    BufferedImage cropImage = new BufferedImage(cropwidth, cropheight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics2D = cropImage.createGraphics();
                    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    graphics2D.drawImage(resizedImage, -cropx, -cropy, resizedImage.getWidth(), resizedImage.getHeight(), null);
                    resizedImage = cropImage;
                }

                // Determine output format
                String imageFormat = getDefaultImageFormat();
                if (multimedia.getMimeType().getType().contains("jpg")) {
                    imageFormat = "jpg";
                }

                // Write image
                ImageWriter writer = null;
                Iterator iter = ImageIO.getImageWritersByFormatName(imageFormat);
                if (iter.hasNext()) {
                    writer = (ImageWriter)iter.next();
                }

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ImageOutputStream ios = ImageIO.createImageOutputStream(bout);
                writer.setOutput(ios);

                ImageWriteParam iwparam = null;
                if (imageFormat.equalsIgnoreCase("jpg")) {
                    iwparam = new JPEGImageWriteParam(Locale.getDefault());
                    iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    float q = ((float) jpgOutputQuality)/((float)100);
                    iwparam.setCompressionQuality(q);
                }
                writer.write(null, new IIOImage(resizedImage, null, null), iwparam);

                ios.flush();
                writer.dispose();

                // Update filename and data
                String filename = multimedia.getFilename();
                if (filename.indexOf(".") != -1) {
                    filename = filename.substring(0, filename.lastIndexOf("."));
                }
                filename += "." + imageFormat;
                multimedia.setWidth(resizedImage.getWidth());
                multimedia.setHeight(resizedImage.getHeight());
                multimedia.setFilename(filename);
                multimedia.setData(bout.toByteArray());
            }
        }
        return multimedia;
    }

    public String getDefaultImageFormat() {
        return defaultImageFormat;
    }

    public void setImageResizeAlgorithm(ImageResizeAlgorithm imageResizeAlgorithm) {
        this.imageResizeAlgorithm = imageResizeAlgorithm;
    }

    public void setDefaultImageFormat(String defaultImageFormat) {
        this.defaultImageFormat = defaultImageFormat;
    }

    public void setJpgOutputQuality(int jpgOutputQuality) {
        this.jpgOutputQuality = jpgOutputQuality;
    }
}
