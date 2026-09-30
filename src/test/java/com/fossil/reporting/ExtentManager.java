package com.fossil.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.fossil.config.ConfigReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Owns the single ExtentReports instance for the run and the ExtentTest of the
 * test currently executing on each thread.
 *
 * Reports are written to reports/ (not target/) so "mvn clean" does not wipe history.
 */
public final class ExtentManager {

    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();
    private static ExtentReports extent;
    private static Path reportPath;

    private ExtentManager() {
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            reportPath = Paths.get("reports", "ExtentReport_" + stamp + ".html").toAbsolutePath();

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath.toString());
            spark.config().setDocumentTitle("Fossil Cart Automation");
            spark.config().setReportName("Fossil.in - Cart Flow");
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Application", ConfigReader.get("base.url"));
            extent.setSystemInfo("Browser", ConfigReader.get("browser"));
            extent.setSystemInfo("Headless", ConfigReader.get("headless"));
            extent.setSystemInfo("Login", ConfigReader.getBoolean("login.enabled") ? "Enabled" : "Guest");
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java", System.getProperty("java.version"));
        }
        return extent;
    }

    public static void setCurrent(ExtentTest test) {
        CURRENT.set(test);
    }

    public static ExtentTest getCurrent() {
        return CURRENT.get();
    }

    public static void clearCurrent() {
        CURRENT.remove();
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
            System.out.println("Extent report: " + reportPath);
        }
    }
}
