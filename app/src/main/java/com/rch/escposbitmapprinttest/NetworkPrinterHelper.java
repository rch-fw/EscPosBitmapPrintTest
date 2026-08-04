package com.rch.escposbitmapprinttest;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class NetworkPrinterHelper {

    private static final String TAG = "NetworkPrinter";

    /**
     * Prints an asset image over TCP Socket in a background thread.
     *
     * @param context   Android Context
     * @param ip        Local IP address of the printer (e.g., "192.168.1.100")
     * @param port      Port number (4617)
     * @param assetName Name of the image file in assets (e.g., "logo.png")
     * @param maxWidth  Width in pixels (384 for 58mm, 576 for 80mm)
     */
    public static void printAssetBitmapOverSocket(Context context, String ip, int port, String assetName, int maxWidth) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Socket socket = null;
            OutputStream outputStream = null;

            try {
                // 1. Generate ESC/POS GS v 0 byte payload from Asset
                byte[] imageBytes = EscPosBitmapPrinter.generateGSv0CommandsFromAsset(context, assetName, maxWidth);


                ByteArrayOutputStream printJob = new ByteArrayOutputStream();

                printJob.write("The images are printed with GSv0 esc pos command\n\n\n".getBytes());

                // set double width double height
                printJob.write(new byte[]{27, 33, 56});
                int charWidth=2;

                String line="First Image";
                // only char size modifier is supported so we have to center text by hand
                String lCentered=" ".repeat((48-line.length()*charWidth)/(2*charWidth))+line+"\n";
                printJob.write(lCentered.getBytes());
                printJob.write(imageBytes);

                line= "Another Image";
                charWidth=1;
                lCentered=" ".repeat((48-line.length()*charWidth)/(2*charWidth))+line+"\n";
                printJob.write(lCentered.getBytes());
                printJob.write(imageBytes);// Bitmap command payload


                printJob.write(new byte[]{29, 86, 66, 20}); // paper cut


                byte[] fullPayload = printJob.toByteArray();

                // 3. Open Socket connection with a 4-second connection timeout
                Log.d(TAG, "Connecting to printer at " + ip + ":" + port + "...");
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 4000);
                socket.setSoTimeout(4000); // Read/write timeout

                // 4. Send payload to printer
                outputStream = socket.getOutputStream();
                outputStream.write(fullPayload);
                outputStream.flush();

                Log.d(TAG, "Bitmap sent successfully to printer!");

            } catch (IOException e) {
                Log.e(TAG, "Printing failed: " + e.getMessage(), e);
            } finally {
                // 5. Clean up stream and close socket
                if (outputStream != null) {
                    try { outputStream.close(); } catch (IOException ignored) {}
                }
                if (socket != null && !socket.isClosed()) {
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }
        });
    }
}