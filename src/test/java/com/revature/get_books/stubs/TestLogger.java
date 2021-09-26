package com.revature.get_books.stubs;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class TestLogger implements LambdaLogger {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";

    private final Writer logFileWriter;

    public TestLogger() {
        String logDirectoryPath = "src/test/resources/logs";
        String fileName = "" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss"));
        String filePath = logDirectoryPath + "/" + fileName + ".log";

        try {
            logFileWriter = new FileWriter(filePath, true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    private void printMessageToConsole(String level, String message) {
        switch (level) {
            case "INFO":
                System.out.println(message);
                break;
            case "ERROR":
                System.out.println(ANSI_RED + message + ANSI_RESET);
                break;
        }
    }

    public void close() {
        try {
            logFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void log(String s) {
        try {
            String formattedMessage = "TEST LOG: " + s;
            logFileWriter.write(formattedMessage + "\n");
            printMessageToConsole("INFO", formattedMessage);
        } catch (IOException e) {
            printMessageToConsole("ERROR", "Could not write message to file");
        }
    }

    @Override
    public void log(byte[] bytes) {
        try {
            String formattedMessage = "TEST LOG: " + Arrays.toString(bytes);
            logFileWriter.write(formattedMessage + "\n");
            printMessageToConsole("INFO", formattedMessage);
        } catch (IOException e) {
            printMessageToConsole("ERROR", "Could not write message to file");
        }
    }

}
