package com.fossil.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.fossil.driver.DriverFactory;
import com.fossil.reporting.ExtentManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Builds the Extent report from TestNG events. On failure it captures one screenshot,
 * embeds it in the report (base64, so the HTML is self-contained) and saves a PNG
 * copy to target/screenshots.
 */
public class ExtentReportListener implements ITestListener, ISuiteListener {

    private static final String EXTENT_ATTR = "extentTest";

    @Override
    public void onStart(ISuite suite) {
        ExtentManager.getInstance();
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
    }

    @Override
    public void onTestStart(ITestResult result) {
        createTest(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testFor(result).pass(MarkupHelper.createLabel("PASSED", ExtentColor.GREEN));
        ExtentManager.clearCurrent();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = testFor(result);
        test.fail(result.getThrowable());

        String base64 = captureScreenshot(result.getMethod().getMethodName());
        if (base64 != null) {
            test.fail("Screenshot at failure",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64, "Failure screenshot").build());
        }
        ExtentManager.clearCurrent();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = testFor(result);
        String reason = result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "Skipped - depends on a failed/skipped step";
        test.skip(MarkupHelper.createLabel("SKIPPED", ExtentColor.ORANGE));
        test.skip(reason);
        ExtentManager.clearCurrent();
    }

    /** Dependency skips never fire onTestStart, so create the node on demand. */
    private ExtentTest testFor(ITestResult result) {
        Object existing = result.getAttribute(EXTENT_ATTR);
        return existing instanceof ExtentTest ? (ExtentTest) existing : createTest(result);
    }

    private ExtentTest createTest(ITestResult result) {
        String description = result.getMethod().getDescription();
        ExtentTest test = ExtentManager.getInstance()
                .createTest(result.getMethod().getMethodName(), description == null ? "" : description)
                .assignCategory(result.getTestClass().getRealClass().getSimpleName());
        result.setAttribute(EXTENT_ATTR, test);
        ExtentManager.setCurrent(test);
        return test;
    }

    private String captureScreenshot(String name) {
        WebDriver driver = DriverFactory.getDriver();
        if (driver == null) {
            return null;
        }
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            Path file = dir.resolve(name + "_" + System.currentTimeMillis() + ".png");
            Files.write(file, png);
            System.out.println("Screenshot saved: " + file.toAbsolutePath());
            return Base64.getEncoder().encodeToString(png);
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not capture screenshot: " + e.getMessage());
            return null;
        }
    }
}
