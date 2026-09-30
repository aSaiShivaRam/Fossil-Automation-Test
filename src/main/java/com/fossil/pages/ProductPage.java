package com.fossil.pages;

import com.fossil.utils.PriceParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;

public class ProductPage extends BasePage {

    private static final By TITLE = By.cssSelector("h1.pdp-title");
    private static final By PRICE = By.cssSelector(".current-price-section .pro-price");
    /** Same class on both states: a button before adding, an <a> "Go To Cart" link once the item is in the bag. */
    private static final By ADD_TO_BAG = By.cssSelector("button.btn-add-bag");
    private static final By GO_TO_CART = By.cssSelector("a.btn-add-bag[href*='/cart']");
    private static final By BAG_ACTION = By.cssSelector(".pdp-action-bar .btn-add-bag");
    private static final By MINI_CART_OPEN = By.cssSelector(".minicart.minicart-open #cartSideView");

    private final HeaderComponent header;

    public ProductPage(WebDriver driver) {
        super(driver);
        this.header = new HeaderComponent(driver);
        waitForVisible(TITLE);
        acceptConsentIfShown();
    }

    public HeaderComponent header() {
        return header;
    }

    public String getProductName() {
        return textOf(TITLE);
    }

    public BigDecimal getPrice() {
        return PriceParser.parse(textOf(PRICE));
    }

    public boolean isAddToBagEnabled() {
        waitForVisible(BAG_ACTION);
        return !driver.findElements(ADD_TO_BAG).isEmpty() && driver.findElement(ADD_TO_BAG).isEnabled();
    }

    /** True when the site shows "Go To Cart" because this product is already in the bag. */
    public boolean isAlreadyInBag() {
        waitForVisible(BAG_ACTION);
        return !driver.findElements(GO_TO_CART).isEmpty();
    }

    /**
     * Clicks "Add To Bag" and waits until the header badge reflects the new item,
     * so callers never race the async cart API.
     */
    public ProductPage addToBag() {
        int before = header.getCartCount();
        click(ADD_TO_BAG);
        header.waitForCartCount(before + 1);
        return this;
    }

    public boolean isMiniCartOpen() {
        return !driver.findElements(MINI_CART_OPEN).isEmpty();
    }
}
