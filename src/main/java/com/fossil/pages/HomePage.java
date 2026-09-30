package com.fossil.pages;

import com.fossil.config.ConfigReader;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class HomePage extends BasePage {

    private final HeaderComponent header;

    public HomePage(WebDriver driver) {
        super(driver);
        this.header = new HeaderComponent(driver);
    }

    public HomePage open() {
        driver.get(ConfigReader.get("base.url"));
        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
        wait.until(ExpectedConditions.titleContains("Fossil"));
        acceptConsentIfShown();
        return this;
    }

    public HeaderComponent header() {
        return header;
    }
}
