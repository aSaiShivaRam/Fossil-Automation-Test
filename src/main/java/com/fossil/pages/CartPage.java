package com.fossil.pages;

import com.fossil.models.CartLine;
import com.fossil.utils.PriceParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CartPage extends BasePage {

    private static final By CART_TITLE = By.cssSelector("h1.cart-title");
    private static final By CART_TITLE_COUNT = By.cssSelector("h1.cart-title > span");
    /** Scoped to the main column so the header mini-cart flyout is never matched. */
    private static final By LINE_ITEMS = By.cssSelector(".cart-items-column .card.cart-card");

    // Relative to a line item
    private static final By LINE_NAME = By.cssSelector(".product-title a");
    private static final By LINE_SKU = By.cssSelector(".sku-value");
    private static final By LINE_UNIT_PRICE = By.cssSelector(".each-price .price-current");
    private static final By LINE_QTY = By.cssSelector(".quantity-selector .qty-display");
    private static final By LINE_QTY_INCREASE = By.xpath(".//div[contains(@class,'quantity-selector')]/button[last()]");

    // Order summary
    private static final By SUBTOTAL = By.cssSelector(".cart-summary-section .summary-subtotal-row > span:last-child");
    private static final By SHIPPING = By.cssSelector(".cart-summary-section .summary-shipping-row > span:last-child");
    private static final By ESTIMATED_TOTAL = By.cssSelector(".cart-summary-section .estimated-total-value");
    private static final By CHECKOUT_BUTTON = By.cssSelector(".cart-summary-section button.checkout-btn");

    private final HeaderComponent header;

    public CartPage(WebDriver driver) {
        super(driver);
        this.header = new HeaderComponent(driver);
    }

    public CartPage waitForLoaded() {
        wait.until(ExpectedConditions.urlContains("/cart"));
        waitForVisible(CART_TITLE);
        waitForAllVisible(LINE_ITEMS);
        waitForVisible(ESTIMATED_TOTAL);
        acceptConsentIfShown();
        return this;
    }

    public HeaderComponent header() {
        return header;
    }

    /** "Shopping Cart(2)" → 2. The site counts units, not distinct lines. */
    public int getTitleItemCount() {
        return Integer.parseInt(textOf(CART_TITLE_COUNT).replaceAll("\\D", ""));
    }

    public List<CartLine> getLines() {
        return driver.findElements(LINE_ITEMS).stream().map(this::toCartLine).collect(Collectors.toList());
    }

    public CartLine getLine(String sku) {
        return toCartLine(lineElement(sku));
    }

    public BigDecimal getSubtotal() {
        return PriceParser.parse(textOf(SUBTOTAL));
    }

    /** "FREE" is returned as 0.00. */
    public BigDecimal getShipping() {
        return PriceParser.parse(textOf(SHIPPING));
    }

    public BigDecimal getEstimatedTotal() {
        return PriceParser.parse(textOf(ESTIMATED_TOTAL));
    }

    public boolean isCheckoutButtonDisplayed() {
        return waitForVisible(CHECKOUT_BUTTON).isDisplayed();
    }

    /**
     * Presses "+" for the given SKU and waits until both the line quantity and the
     * subtotal have been re-rendered by the cart API.
     */
    public CartPage increaseQuantity(String sku) {
        int currentQty = getLine(sku).getQuantity();
        String subtotalBefore = textOf(SUBTOTAL);

        WebElement plus = lineElement(sku).findElement(LINE_QTY_INCREASE);
        scrollIntoView(plus);
        wait.until(ExpectedConditions.elementToBeClickable(plus)).click();

        waitUntil(d -> getLine(sku).getQuantity() == currentQty + 1);
        waitUntil(d -> !d.findElement(SUBTOTAL).getText().trim().equals(subtotalBefore));
        return this;
    }

    private WebElement lineElement(String sku) {
        By bySku = By.xpath("//div[contains(@class,'cart-items-column')]"
                + "//div[contains(@class,'cart-card')][.//span[contains(@class,'sku-value') and normalize-space()='"
                + sku + "']]");
        return waitForVisible(bySku);
    }

    private CartLine toCartLine(WebElement line) {
        // The cart title is uppercased by CSS; textContent gives the real product name, not the styling.
        return new CartLine(
                line.findElement(LINE_NAME).getDomProperty("textContent").trim(),
                line.findElement(LINE_SKU).getText().trim(),
                PriceParser.parse(line.findElement(LINE_UNIT_PRICE).getText()),
                Integer.parseInt(line.findElement(LINE_QTY).getText().trim()));
    }
}
