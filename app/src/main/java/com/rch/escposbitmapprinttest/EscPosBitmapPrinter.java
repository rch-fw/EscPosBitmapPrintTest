package com.rch.escposbitmapprinttest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EscPosBitmapPrinter {

    /**
     * Reads a bitmap file from the assets directory and generates complete GS v 0 ESC/POS bytes.
     *
     * @param context   Android Context (Activity or Application Context)
     * @param fileName  File name/path inside assets folder (e.g. "logo.png" or "images/receipt_header.png")
     * @param maxWidth  Maximum printable width in pixels (e.g., 384 for 58mm printer, 576 for 80mm printer)
     * @return Raw byte array ready to send to printer
     */
    public static byte[] generateGSv0CommandsFromAsset(Context context, String fileName, int maxWidth) throws IOException {
        Bitmap originalBitmap = null;
        InputStream inputStream = null;

        try {
            // 1. Open input stream from assets folder
            inputStream = context.getAssets().open(fileName);

            // 2. Decode stream directly into a Bitmap object
            originalBitmap = BitmapFactory.decodeStream(inputStream);

            if (originalBitmap == null) {
                throw new IOException("Failed to decode image from asset: " + fileName);
            }
        } finally {
            // Always close the input stream
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        }

        // 3. Scale bitmap to fit printer's max printable width
        Bitmap scaledBitmap = scaleBitmapToFitWidth(originalBitmap, maxWidth);

        // 4. Convert to ESC/POS GS v 0 command bytes
        return generateGSv0Bytes(scaledBitmap);
    }

    /**
     * Converts an Android Bitmap into ESC/POS GS v 0 byte array payload.
     */
    public static byte[] generateGSv0Bytes(Bitmap bitmap) {
        int widthPixels = bitmap.getWidth();
        int heightPixels = bitmap.getHeight();

        // Calculate width in bytes (must be a multiple of 8)
        int bytesPerLine = (int) Math.ceil(widthPixels / 8.0);

        // Calculate Little-Endian dimensions
        int xL = bytesPerLine % 256;
        int xH = bytesPerLine / 256;
        int yL = heightPixels % 256;
        int yH = heightPixels / 256;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // GS v 0 command header: [0x1D, 0x76, 0x30, mode(0), xL, xH, yL, yH]
        baos.write(0x1D);
        baos.write(0x76);
        baos.write(0x30);
        baos.write(0x00); // Mode 0 = Normal speed/density
        baos.write(xL);
        baos.write(xH);
        baos.write(yL);
        baos.write(yH);

        // Convert pixels to 1-bit monochrome data (MSB first)
        for (int y = 0; y < heightPixels; y++) {
            for (int xByte = 0; xByte < bytesPerLine; xByte++) {
                byte currentByte = 0;

                for (int bit = 0; bit < 8; bit++) {
                    int xPixel = (xByte * 8) + bit;

                    if (xPixel < widthPixels) {
                        int pixelColor = bitmap.getPixel(xPixel, y);

                        // Extract RGB components
                        int r = (pixelColor >> 16) & 0xFF;
                        int g = (pixelColor >> 8) & 0xFF;
                        int b = pixelColor & 0xFF;

                        // Calculate grayscale luminance value
                        int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                        // Threshold check: pixel darker than 128 maps to 1 (Black pixel)
                        if (luminance < 128) {
                            currentByte |= (1 << (7 - bit));
                        }
                    }
                    // Bits beyond widthPixels remain 0 (White padding)
                }

                baos.write(currentByte);
            }
        }

        return baos.toByteArray();
    }

    /**
     * Scales bitmap proportionally to fit maximum printable width.
     */
    private static Bitmap scaleBitmapToFitWidth(Bitmap src, int maxWidth) {
        if (src.getWidth() <= maxWidth) {
            return src;
        }

        float aspectRatio = (float) src.getHeight() / (float) src.getWidth();
        int targetHeight = Math.round(maxWidth * aspectRatio);

        return Bitmap.createScaledBitmap(src, maxWidth, targetHeight, true);
    }
}
