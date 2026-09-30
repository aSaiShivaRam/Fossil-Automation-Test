package com.fossil.driver;

import com.fossil.config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * Creates the browser chosen at runtime (config / -Dbrowser). Driver binaries are
 * resolved automatically by Selenium Manager, so nothing is hardcoded to a path.
 * A ThreadLocal keeps the factory safe if the suite is later run in parallel.
 */
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver initDriver() {
        String browser = ConfigReader.get("browser", "chrome").toLowerCase();
        boolean headless = ConfigReader.getBoolean("headless");

        WebDriver driver;
        switch (browser) {
            case "firefox": {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                driver = new FirefoxDriver(options);
                break;
            }
            case "edge": {
                EdgeOptions options = new EdgeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1920,1080");
                driver = new EdgeDriver(options);
                break;
            }
            case "chrome": {
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1920,1080", "--disable-notifications");
                driver = new ChromeDriver(options);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        // Implicit wait stays at 0 on purpose: mixing it with explicit waits causes unpredictable timeouts.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("page.load.timeout.seconds")));
        if (!headless) {
            driver.manage().window().maximize();
        }
        DRIVER.set(driver);
        return driver;
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
