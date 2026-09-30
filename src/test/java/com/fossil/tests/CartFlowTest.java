package com.fossil.tests;

import com.fossil.config.ConfigReader;
import com.fossil.models.CartLine;
import com.fossil.pages.CartPage;
import com.fossil.pages.HomePage;
import com.fossil.pages.LoginPage;
import com.fossil.pages.ProductPage;
import com.fossil.pages.SearchResultsPage;
import com.fossil.reporting.ReportLog;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.util.List;

/**
 * End-to-end: launch → (login) → search → verify price → add to bag → verify cart maths.
 *
 * Assertion strategy
 *  - Hard Assert  ("assert"): gatekeepers. If they fail, later steps are meaningless, so stop now.
 *  - SoftAssert   ("verify"): detail checks inside a step. Every mismatch is collected and
 *                             reported together via assertAll(), so one run shows all defects.
 */
public class CartFlowTest extends BaseTest {

    private final String searchTerm = ConfigReader.get("search.term");
    private final String productCode = ConfigReader.get("product.code");
    private final String productName = ConfigReader.get("product.name");
    private final BigDecimal expectedPrice = new BigDecimal(ConfigReader.get("product.expected.price")).setScale(2);

    // State carried between ordered steps
    private HomePage homePage;
    private ProductPage productPage;
    private BigDecimal listingPrice;
    private int cartCountBeforeAdd;

    @Test(priority = 1, description = "Launch browser and navigate to the store")
    public void launchAndNavigate() {
        homePage = new HomePage(driver).open();
        ReportLog.info("Opened " + homePage.getCurrentUrl() + " — title: " + homePage.getTitle());

        Assert.assertTrue(homePage.getTitle().contains("Fossil"),
                "Home page title should contain 'Fossil' but was: " + homePage.getTitle());
        Assert.assertTrue(homePage.getCurrentUrl().contains("fossil.in"),
                "Unexpected landing URL: " + homePage.getCurrentUrl());
        ReportLog.info("Browser launched and home page navigation verified.");
    }

    @Test(priority = 2, dependsOnMethods = "launchAndNavigate",
            description = "Sign in with mobile number + OTP (OTP entered manually in the browser)")
    public void login() {
        if (!ConfigReader.getBoolean("login.enabled")) {
            ReportLog.info("login.enabled=false - continuing as guest");
            throw new SkipException("login.enabled=false - continuing as guest");
        }
        String phone = ConfigReader.get("login.phone", "");
        Assert.assertTrue(phone.matches("\\d{10}"), "login.phone must be a 10-digit mobile number");

        LoginPage loginPage = homePage.header().openSignIn();
        Assert.assertTrue(loginPage.isPhoneInputDisplayed(), "Sign-in modal did not open");

        loginPage.enterPhoneNumber(phone).submitPhoneNumber();
        ReportLog.info("Phone number submitted - waiting for OTP to be entered manually");
        boolean loggedIn = loginPage.waitForManualOtpCompletion(ConfigReader.getInt("login.otp.timeout.seconds"));

        Assert.assertTrue(loggedIn, "Login was not completed within the OTP timeout");
        Assert.assertFalse(homePage.header().isSignInButtonShown(), "'Sign in' is still shown after login");
        ReportLog.info("Successfully signed in with phone number: " + phone);
    }

    @Test(priority = 3, dependsOnMethods = "launchAndNavigate",
            description = "Search for the product and verify its price on listing and detail pages")
    public void searchAndVerifyPrice() {
        SearchResultsPage results = homePage.header().searchFor(searchTerm);

        Assert.assertTrue(results.getResultsCount() > 0, "Search for '" + searchTerm + "' returned no results");
        Assert.assertTrue(results.isProductListed(productCode),
                "Product " + productCode + " not found in results for '" + searchTerm + "'");

        listingPrice = results.getProductPrice(productCode);
        ReportLog.info("Search '" + searchTerm + "' → " + results.getResultsCount() + " results; "
                + productCode + " listed at ₹" + listingPrice + " (expected ₹" + expectedPrice + ")");

        SoftAssert verify = new SoftAssert();
        verify.assertEquals(results.getProductName(productCode), productName, "Listing: product name");
        verify.assertEquals(listingPrice, expectedPrice, "Listing: product price");

        productPage = results.openProduct(productCode);
        ReportLog.info("Product page: '" + productPage.getProductName() + "' at ₹" + productPage.getPrice());

        verify.assertEquals(productPage.getProductName(), productName, "PDP: product name");
        verify.assertEquals(productPage.getPrice(), listingPrice, "PDP price should match listing price");
        verify.assertEquals(productPage.getPrice(), expectedPrice, "PDP: product price");
        verify.assertAll();
        ReportLog.info("Product pricing and details verified on search listing and product detail page.");
    }

    @Test(priority = 4, dependsOnMethods = "searchAndVerifyPrice", description = "Add the product to the bag")
    public void addToCart() {
        Assert.assertFalse(productPage.isAlreadyInBag(),
                "Product is already in the bag ('Go To Cart' shown) - empty the account's cart and re-run");
        Assert.assertTrue(productPage.isAddToBagEnabled(), "'Add To Bag' is disabled - product may be out of stock");

        cartCountBeforeAdd = productPage.header().getCartCount();
        ReportLog.info("Cart count before add: " + cartCountBeforeAdd);
        productPage.addToBag();
        ReportLog.info("Bag badge: " + cartCountBeforeAdd + " → " + productPage.header().getCartCount());

        Assert.assertEquals(productPage.header().getCartCount(), cartCountBeforeAdd + 1,
                "Header bag count should increase by 1 after Add To Bag");
        ReportLog.info("Successfully added product " + productCode + " to cart.");
    }

    @Test(priority = 5, dependsOnMethods = "addToCart",
            description = "Verify cart item count and order-summary calculations")
    public void verifyCartAndSummary() {
        CartPage cart = productPage.header().openCart();
        List<CartLine> lines = cart.getLines();

        Assert.assertFalse(lines.isEmpty(), "Cart page shows no line items");
        Assert.assertTrue(lines.stream().anyMatch(l -> l.getSku().equals(productCode)),
                "Added product " + productCode + " is missing from the cart: " + lines);

        CartLine line = cart.getLine(productCode);
        int totalUnits = lines.stream().mapToInt(CartLine::getQuantity).sum();
        ReportLog.info("Cart lines: " + lines);

        SoftAssert verify = new SoftAssert();
        verify.assertEquals(line.getName(), productName, "Cart: product name");
        verify.assertEquals(line.getUnitPrice(), listingPrice, "Cart: unit price should match listing price");
        verify.assertEquals(totalUnits, cartCountBeforeAdd + 1, "Cart: total units");
        verify.assertEquals(cart.getTitleItemCount(), totalUnits, "Cart heading count");
        verify.assertEquals(cart.header().getCartCount(), totalUnits, "Header bag badge");
        assertSummaryMaths(cart, verify, "after add");
        verify.assertTrue(cart.isCheckoutButtonDisplayed(), "Checkout button should be displayed");
        verify.assertAll();
        ReportLog.info("Cart contents, badges, and order summary verified successfully.");
    }

    @Test(priority = 6, dependsOnMethods = "verifyCartAndSummary",
            description = "Increase quantity and verify counts and totals recalculate")
    public void updateQuantityAndVerifyRecalculation() {
        CartPage cart = new CartPage(driver).waitForLoaded();
        int qtyBefore = cart.getLine(productCode).getQuantity();
        int unitsBefore = cart.header().getCartCount();

        cart.increaseQuantity(productCode);
        CartLine line = cart.getLine(productCode);
        ReportLog.info("Pressed '+' on " + productCode + ": qty " + qtyBefore + " → " + line.getQuantity()
                + ", line total: ₹" + line.getLineTotal());

        Assert.assertEquals(line.getQuantity(), qtyBefore + 1, "Line quantity did not increase");

        SoftAssert verify = new SoftAssert();
        cart.header().waitForCartCount(unitsBefore + 1);
        verify.assertEquals(line.getQuantity(), qtyBefore + 1, "Line item quantity after update");
        verify.assertEquals(line.getUnitPrice(), listingPrice, "Line unit price after qty change");
        verify.assertEquals(cart.header().getCartCount(), unitsBefore + 1, "Header bag badge after qty change");
        verify.assertEquals(cart.getTitleItemCount(), unitsBefore + 1, "Cart heading count after qty change");
        verify.assertEquals(line.getLineTotal(), listingPrice.multiply(BigDecimal.valueOf(qtyBefore + 1)),
                "Line total = unit price x quantity");
        assertSummaryMaths(cart, verify, "after qty change");
        verify.assertAll();
        ReportLog.info("Quantity updated to " + line.getQuantity() + " and all summary calculations verified successfully.");
    }

    /** Subtotal = Σ(unit × qty); Estimated total = subtotal + shipping. */
    private void assertSummaryMaths(CartPage cart, SoftAssert verify, String when) {
        BigDecimal expectedSubtotal = cart.getLines().stream()
                .map(CartLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal shipping = cart.getShipping();
        BigDecimal estimatedTotal = cart.getEstimatedTotal();
        ReportLog.info("Summary (" + when + "): subtotal ₹" + subtotal + " (expected ₹" + expectedSubtotal
                + "), shipping ₹" + shipping + ", estimated total ₹" + estimatedTotal);

        verify.assertEquals(subtotal, expectedSubtotal, "Subtotal = sum of line totals (" + when + ")");
        verify.assertEquals(estimatedTotal, subtotal.add(shipping),
                "Estimated total = subtotal + shipping (" + when + ")");
        ReportLog.info("Verified order summary maths (" + when + "): Subtotal ₹" + subtotal + ", Total ₹" + estimatedTotal);
    }
}
