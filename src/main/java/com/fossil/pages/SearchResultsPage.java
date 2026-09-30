package com.fossil.pages;

import com.fossil.utils.PriceParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;

public class SearchResultsPage extends BasePage {

    /** Rendered twice (desktop + mobile copy); only the one matching the viewport is visible. */
    private static final By RESULTS_COUNT = By.cssSelector("span.reval-results-count");
    /** Real product tiles only; skeleton placeholders share the base class but have no product code. */
    private static final By PRODUCT_CARDS = By.cssSelector(".plp-product-card[data-plp-product-code]");
    private static final String CARD_BY_CODE = ".plp-product-card[data-plp-product-code='%s']";

    // Relative to a product card
    private static final By CARD_NAME_LINK = By.cssSelector("a.product-name-link");
    private static final By CARD_PRICE = By.cssSelector("[class*='ProductCard-module'][class*='PriceValue']");

    public SearchResultsPage(WebDriver driver) {
        super(driver);
        visibleResultsCount();
        waitForVisible(PRODUCT_CARDS);
    }

    /** Parses "421 Results for Watch" → 421. */
    public int getResultsCount() {
        String text = visibleResultsCount().getText().trim().replaceAll("^(\\d[\\d,]*).*$", "$1").replace(",", "");
        return Integer.parseInt(text);
    }

    private WebElement visibleResultsCount() {
        return waitUntil(d -> d.findElements(RESULTS_COUNT).stream()
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElse(null));
    }

    public int getVisibleProductCount() {
        return driver.findElements(PRODUCT_CARDS).size();
    }

    public boolean isProductListed(String productCode) {
        return !driver.findElements(cardBy(productCode)).isEmpty();
    }

    public String getProductName(String productCode) {
        return card(productCode).findElement(CARD_NAME_LINK).getText().trim();
    }

    public BigDecimal getProductPrice(String productCode) {
        return PriceParser.parse(card(productCode).findElement(CARD_PRICE).getText());
    }

    public ProductPage openProduct(String productCode) {
        WebElement link = card(productCode).findElement(CARD_NAME_LINK);
        scrollIntoView(link);
        driver.navigate().to(link.getDomProperty("href"));
        return new ProductPage(driver);
    }

    private WebElement card(String productCode) {
        WebElement card = waitForVisible(cardBy(productCode));
        scrollIntoView(card);
        return card;
    }

    private static By cardBy(String productCode) {
        return By.cssSelector(String.format(CARD_BY_CODE, productCode));
    }
}
