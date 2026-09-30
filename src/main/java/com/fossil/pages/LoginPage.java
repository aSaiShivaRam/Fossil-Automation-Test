package com.fossil.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Sign-in modal. fossil.in authenticates by mobile number + one-time password, so the
 * OTP step cannot be scripted; the page enters the number and then waits for a human
 * to finish in the browser.
 */
public class LoginPage extends BasePage {

    private static final By PHONE_INPUT = By.id("phoneInput");
    private static final By CONTINUE_BUTTON =
            By.xpath("//form[.//input[@id='phoneInput']]//button[@type='submit']");
    private static final By SIGN_IN_BUTTON = By.cssSelector("button.login-trigger-btn");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public boolean isPhoneInputDisplayed() {
        return waitForVisible(PHONE_INPUT).isDisplayed();
    }

    public LoginPage enterPhoneNumber(String phone) {
        type(PHONE_INPUT, phone);
        return this;
    }

    public LoginPage submitPhoneNumber() {
        // The button is disabled until a valid 10-digit number is entered.
        click(CONTINUE_BUTTON);
        return this;
    }

    /**
     * Waits for the user to type the OTP. Success is detected by the header "Sign in"
     * trigger disappearing.
     */
    public boolean waitForManualOtpCompletion(int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.invisibilityOfElementLocated(SIGN_IN_BUTTON));
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }
}
