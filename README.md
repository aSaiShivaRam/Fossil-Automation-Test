# Fossil.in Cart Automation (Selenium 4 + Java + TestNG)

Automates this manual flow on https://www.fossil.in:
launch browser → (sign in) → search product → verify price → Add To Bag → cart counts and order-summary maths → change quantity and check it all again.

## Run

Requirements: JDK 17+, Maven 3.9+, and Chrome, Edge or Firefox. The driver binary is downloaded automatically by Selenium Manager.

```bash
mvn clean test
```

Override any config key on the command line:

```bash
mvn clean test -Dbrowser=edge -Dheadless=true
```

> **Windows note:** some machines fail before the browser opens with `Unable to establish loopback connection`. The JDK can't create its internal socket in `%TEMP%`. The `windows-socket-fix` profile in `pom.xml` turns on automatically on Windows and points that socket at `C:\Users\Public`.

> **Headless note:** fossil.in's bot protection answers headless Chrome with an "Access Denied" page, so run headed (`headless=false`, the default).

## Reports

- **Extent report:** `reports/ExtentReport_<yyyyMMdd_HHmmss>.html`. Each run gets a new file and `mvn clean` does not delete them. It shows every step with the values it compared (prices, cart lines, subtotal and total), plus pass/fail/skip labels, skip reasons, and the environment (browser, OS, Java). The screenshot of a failed step is embedded in the HTML, so you can share that one file on its own.
- **Screenshots:** a PNG copy of each failure screenshot goes to `target/screenshots/`.
- **TestNG / Surefire:** `target/surefire-reports/`.

The Extent report is built by `listeners/ExtentReportListener`, which is registered in `testng.xml`. Tests add their own step lines with `ReportLog.info(...)`.

## Project layout (Page Object Model)

```
src/main/java/com/fossil
├── config/ConfigReader        properties + -D overrides
├── driver/DriverFactory       picks the browser at runtime, ThreadLocal driver
├── models/CartLine            value object returned by CartPage
├── utils/PriceParser          "₹ 11,995.00" / "FREE" → BigDecimal
└── pages/                     locators + actions only, NO assertions
    ├── BasePage               explicit + fluent waits, safe click, consent banner
    ├── HeaderComponent        search, sign-in trigger, bag badge
    ├── HomePage / LoginPage / SearchResultsPage / ProductPage / CartPage
src/test/java/com/fossil
├── tests/BaseTest             browser lifecycle
├── tests/CartFlowTest         all validations
├── reporting/ExtentManager    Extent report instance + current test per thread
├── reporting/ReportLog        step logging from tests
└── listeners/ExtentReportListener   builds the report, embeds failure screenshots
src/test/resources/config.properties
testng.xml
```

## How it meets the technical expectations

| Expectation | Implementation |
|---|---|
| **POM** | Pages expose actions/state (`getPrice()`, `addToBag()`, `getLines()`). Every `Assert` is in `CartFlowTest`. |
| **No `Thread.sleep()`** | `WebDriverWait` for element state; `FluentWait` (polling, ignores stale/missing elements) for async cart updates such as the bag badge changing and the subtotal re-rendering. Implicit wait is 0 so the two kinds of wait don't mix. |
| **Robust locators** | Stable hooks: `data-plp-product-code`, `#cartCount`, `button.btn-add-bag`, `.estimated-total-value`. The site's hashed CSS-module classes (`ProductCard-module__zQzbga__…`) are matched only by stable fragments (`[class*='PriceValue']`). Relative XPath is used where text or structure is the real anchor, e.g. finding a cart row by SKU. There are no absolute paths. |
| **Hard vs soft assertions** | `Assert` (hard) is used for gatekeepers: product found, Add To Bag enabled, item present in cart. `SoftAssert` (verify) is used for detail checks such as name, prices, counts and totals, so one run reports every mismatch. |

## What gets checked

1. The title and URL of the landing page.
2. On the listing page: the product name and price match the expected values. On the product page: the name and price match the listing.
3. The header bag badge goes up by exactly 1 after Add To Bag.
4. In the cart: the SKU is present, the unit price matches the listing, and the badge, the "Shopping Cart(n)" heading and the total units all agree.
5. **Subtotal = Σ(unit price × qty)** and **Estimated total = subtotal + shipping**. Shipping shown as "FREE" counts as 0.
6. After pressing "+": the quantity, badge and heading go up by 1, the line total equals unit price × new quantity, and the summary maths are checked again.

## Login note

fossil.in signs users in with a **mobile number + OTP**, so the login can't be fully automated. The `login` step is skipped by default and the flow runs as a guest. To include it:

```bash
mvn clean test -Dlogin.enabled=true -Dlogin.phone=<10-digit-number>
```

The script opens the sign-in modal, enters the number and presses Continue. It then waits up to `login.otp.timeout.seconds` for you to type the OTP in the browser. The tests use hard assertions for login success. The cart checks sum over all lines, so they still hold if your account cart already has items. Items added during a logged-in run stay in that account's cart.

## Changing the product

Edit `search.term`, `product.code`, `product.name` and `product.expected.price` in `config.properties`. The listing and cart assertions use the price read from the listing page, so a price change only fails the "expected price" checks.
