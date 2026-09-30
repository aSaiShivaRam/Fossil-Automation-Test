package com.fossil.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Site header shared by every page: search, sign-in trigger and bag badge. */
public class HeaderComponent extends BasePage {

    private static final By SEARCH_TOGGLE = By.cssSelector("button.search-form-trigger[aria-label='Open search']");
    private static final By SEARCH_INPUT = By.cssSelector("input.header-search-input[type='search']");
    private static final By SIGN_IN_BUTTON = By.cssSelector("button.login-trigger-btn");
    private static final By CART_COUNT = By.id("cartCount");
    private static final By VIEW_CART_LINK = By.cssSelector("a#viewcart[href*='/cart']");

    public HeaderComponent(WebDriver driver) {
        super(driver);
    }

    public SearchResultsPage searchFor(String term) {
        click(SEARCH_TOGGLE);
        WebElement input = waitForVisible(SEARCH_INPUT);
        input.clear();
        input.sendKeys(term, Keys.ENTER);
        wait.until(ExpectedConditions.urlContains("/search"));
        return new SearchResultsPage(driver);
    }

    public LoginPage openSignIn() {
        click(SIGN_IN_BUTTON);
        return new LoginPage(driver);
    }

    public boolean isSignInButtonShown() {
        return !driver.findElements(SIGN_IN_BUTTON).isEmpty()
                && driver.findElement(SIGN_IN_BUTTON).isDisplayed();
    }

    /** Bag badge value. Read via textContent because the badge can be visually hidden when 0. */
    public int getCartCount() {
        WebElement badge = wait.until(ExpectedConditions.presenceOfElementLocated(CART_COUNT));
        String text = badge.getDomProperty("textContent").trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    /** Blocks until the badge shows the expected count (fluent wait tolerates re-renders). */
    public void waitForCartCount(int expected) {
        waitUntil(d -> getCartCount() == expected);
    }

    public CartPage openCart() {
        WebElement link = wait.until(ExpectedConditions.presenceOfElementLocated(VIEW_CART_LINK));
        // Navigate by the link's own href: the icon is sometimes covered by the mini-cart flyout.
        driver.navigate().to(link.getDomProperty("href"));
        return new CartPage(driver).waitForLoaded();
    }
}
