package org.hucompute.tlgparser;

import java.io.*;

public class IOUtil {

    public static void copyFile(File pSource, File pTarget) throws IOException {
        byte[] lBuffer = new byte[1024*1024];
        int lRead = 0;
        InputStream lInput = new FileInputStream(pSource);
        OutputStream lOutput = new FileOutputStream(pTarget);
        while ((lRead = lInput.read(lBuffer)) > 0) {
            lOutput.write(lBuffer, 0, lRead);
        }
        lOutput.flush();
        lOutput.close();
        lInput.close();
    }

    public static boolean equals(File pFile1, File pFile2) throws IOException {
        if (pFile1.length() != pFile2.length()) return false;
        byte[] lBuffer1 = new byte[1024*1024];
        byte[] lBuffer2 = new byte[1024*1024];
        int lRead1 = 0;
        int lRead2 = 0;
        InputStream lInput1 = new FileInputStream(pFile1);
        InputStream lInput2 = new FileInputStream(pFile2);
        while ((lRead1 = lInput1.read(lBuffer1)) > 0) {
            lRead2 = lInput2.read(lBuffer2);
            if (lRead1 != lRead2) return false;
            for (int i=0; i<lRead1; i++) {
                if (lBuffer1[i] != lBuffer2[i]) return false;
            }
        }
        lInput1.close();
        lInput2.close();
        return true;
    }

    public static synchronized File getTmpFile() throws IOException {
        File lResult = null;
        do {
            lResult = new File("tmp"+File.separator+System.currentTimeMillis());
        } while (lResult.exists());
        lResult.createNewFile();
        return lResult;
    }

    public static synchronized File getTmpDir() throws IOException {
        File lResult = null;
        do {
            lResult = new File("tmp"+File.separator+System.currentTimeMillis());
        } while (lResult.exists());
        lResult.mkdirs();
        return lResult;
    }

    public static void delete(File pFile, boolean pRecursive) {
        if (!pRecursive || pFile.isFile()) {
            pFile.delete();
        }
        else {
            for (File lFile:pFile.listFiles()) {
                delete(lFile, pRecursive);
            }
        }
    }

}
