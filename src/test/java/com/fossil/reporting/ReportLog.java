package com.fossil.reporting;

import com.aventstack.extentreports.ExtentTest;

/** Step logging from tests into the Extent report; a no-op when no report test is active. */
public final class ReportLog {

    private ReportLog() {
    }

    public static void info(String message) {
        ExtentTest test = ExtentManager.getCurrent();
        if (test != null) {
            test.info(message);
        }
    }

    public static void pass(String message) {
        ExtentTest test = ExtentManager.getCurrent();
        if (test != null) {
            test.pass(message);
        }
    }
}
