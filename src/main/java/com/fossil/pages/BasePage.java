package com.fossil.pages;

import com.fossil.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Shared synchronisation and interaction helpers. No assertions live here or in any
 * page object; pages only locate, act and report state.
 */
public abstract class BasePage {

    /** Tracking-consent banner that overlays the bottom of every page on first visit. */
    private static final By CONSENT_OK = By.cssSelector("button[class*='tracking-consent'][class*='affirmButton']");

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Wait<WebDriver> fluentWait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("wait.explicit.seconds")));
        this.fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(ConfigReader.getInt("wait.fluent.timeout.seconds")))
                .pollingEvery(Duration.ofMillis(ConfigReader.getInt("wait.fluent.polling.millis")))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /** Fluent wait for any custom condition; stale/missing elements are retried, not thrown. */
    protected <T> T waitUntil(Function<WebDriver, T> condition) {
        return fluentWait.until(condition);
    }

    protected void click(By locator) {
        WebElement element = waitForClickable(locator);
        scrollIntoView(element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // A sticky header or late overlay can sit on top of the target; a JS click still fires the handler.
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    protected void type(By locator, String text) {
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected String textOf(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    protected boolean isPresent(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Dismisses the cookie/tracking banner if it appears; does nothing when it is absent. */
    public void acceptConsentIfShown() {
        try {
            WebElement ok = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(CONSENT_OK));
            ok.click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(CONSENT_OK));
        } catch (TimeoutException ignored) {
            // Banner not shown (already accepted in this session).
        }
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
