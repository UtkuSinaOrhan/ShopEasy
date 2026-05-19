package shopeasy;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 3 – Design by Contract (Chapter 4)
 *
 * Part B – Contract Tests
 *
 * Contracts being tested:
 *
 *   ShoppingCart.addItem:
 *     PRE:  product != null
 *     PRE:  quantity > 0
 *     POST: itemCount() increased OR product quantity updated
 *
 *   ShoppingCart.applyDiscount:
 *     PRE:  0 <= discountRate <= 100
 *     POST: result <= total() when discountRate > 0
 *
 *   PriceCalculator.calculate:
 *     PRE:  basePrice >= 0
 *     PRE:  0 <= discountRate <= 100
 *     PRE:  0 <= taxRate <= 100
 *     POST: result >= 0
 *
 *   ShoppingCart invariant:
 *     total() >= 0 after any mutating operation
 */
class ContractTest {

    private ShoppingCart cart;
    private PriceCalculator calculator;
    private Product product;

    @Before
    public void setUp() {
        cart       = new ShoppingCart();
        calculator = new PriceCalculator();
        product    = new Product("P001", "Widget", 10.0, 50);
    }

    // -----------------------------------------------------------------------
    // ShoppingCart.addItem — PRE-CONDITION tests
    // -----------------------------------------------------------------------

    /** PRE violated: null product → AssertionError */
    @Test
    void addItem_nullProduct_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(null, 1))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE violated: quantity = 0 → AssertionError */
    @Test
    void addItem_zeroQuantity_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(product, 0))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE violated: negative quantity → AssertionError */
    @Test
    void addItem_negativeQuantity_shouldViolatePreCondition() {
        assertThatThrownBy(() -> cart.addItem(product, -5))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE holds: valid product and positive quantity → no exception */
    @Test
    void addItem_validInput_shouldNotThrow() {
        assertThatCode(() -> cart.addItem(product, 3))
                .doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // ShoppingCart.addItem — POST-CONDITION tests
    // -----------------------------------------------------------------------

    /** POST: itemCount increases when a new product is added */
    @Test
    void addItem_newProduct_itemCountIncreases() {
        int before = cart.itemCount();
        cart.addItem(product, 2);
        assertThat(cart.itemCount()).isGreaterThan(before);
    }

    /** POST: total is updated when existing product quantity is added */
    @Test
    void addItem_existingProduct_totalReflectsUpdatedQuantity() {
        cart.addItem(product, 2);
        double totalAfterFirst = cart.total();
        cart.addItem(product, 3);
        // itemCount stays the same, but total grows
        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.total()).isGreaterThan(totalAfterFirst);
    }

    // -----------------------------------------------------------------------
    // ShoppingCart.applyDiscount — PRE-CONDITION tests
    // -----------------------------------------------------------------------

    /** PRE violated: discountRate < 0 → AssertionError */
    @Test
    void applyDiscount_negativeRate_shouldViolatePreCondition() {
        cart.addItem(product, 1);
        assertThatThrownBy(() -> cart.applyDiscount(-1))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE violated: discountRate > 100 → AssertionError */
    @Test
    void applyDiscount_rateAboveHundred_shouldViolatePreCondition() {
        cart.addItem(product, 1);
        assertThatThrownBy(() -> cart.applyDiscount(101))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE holds: discountRate = 0 (lower bound) → no exception */
    @Test
    void applyDiscount_zeroRate_shouldNotThrow() {
        cart.addItem(product, 1);
        assertThatCode(() -> cart.applyDiscount(0))
                .doesNotThrowAnyException();
    }

    /** PRE holds: discountRate = 100 (upper bound) → no exception */
    @Test
    void applyDiscount_hundredRate_shouldNotThrow() {
        cart.addItem(product, 1);
        assertThatCode(() -> cart.applyDiscount(100))
                .doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // ShoppingCart.applyDiscount — POST-CONDITION tests
    // -----------------------------------------------------------------------

    /** POST: result <= total() when discountRate > 0 */
    @Test
    void applyDiscount_positiveRate_resultIsLessOrEqualToTotal() {
        cart.addItem(product, 2);
        double total      = cart.total();
        double discounted = cart.applyDiscount(20);
        assertThat(discounted).isLessThanOrEqualTo(total);
    }

    /** POST: result == total() when discountRate == 0 */
    @Test
    void applyDiscount_zeroRate_resultEqualsTotal() {
        cart.addItem(product, 2);
        double total      = cart.total();
        double discounted = cart.applyDiscount(0);
        assertThat(discounted).isEqualTo(total);
    }

    // -----------------------------------------------------------------------
    // PriceCalculator.calculate — PRE-CONDITION tests
    // -----------------------------------------------------------------------

    /** PRE violated: basePrice < 0 → AssertionError */
    @Test
    void calculate_negativeBasePrice_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(-1, 10, 10))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE violated: discountRate < 0 → AssertionError */
    @Test
    void calculate_negativeDiscountRate_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100, -1, 10))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE violated: discountRate > 100 → AssertionError */
    @Test
    void calculate_discountRateAboveHundred_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100, 101, 10))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE violated: taxRate < 0 → AssertionError */
    @Test
    void calculate_negativeTaxRate_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100, 10, -1))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE violated: taxRate > 100 → AssertionError */
    @Test
    void calculate_taxRateAboveHundred_shouldViolatePreCondition() {
        assertThatThrownBy(() -> calculator.calculate(100, 10, 101))
                .isInstanceOf(AssertionError.class);
    }

    /** PRE holds: all inputs at valid lower bounds → no exception */
    @Test
    void calculate_allZeroRates_shouldNotThrow() {
        assertThatCode(() -> calculator.calculate(0, 0, 0))
                .doesNotThrowAnyException();
    }

    /** PRE holds: all inputs at valid upper bounds → no exception */
    @Test
    void calculate_allMaxRates_shouldNotThrow() {
        assertThatCode(() -> calculator.calculate(1000, 100, 100))
                .doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // PriceCalculator.calculate — POST-CONDITION tests
    // -----------------------------------------------------------------------

    /** POST: result is always >= 0 for valid inputs */
    @Test
    void calculate_validInputs_resultIsNonNegative() {
        double result = calculator.calculate(100, 20, 10);
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    /** POST: result = 0 when basePrice = 0 */
    @Test
    void calculate_basePriceZero_resultIsZero() {
        double result = calculator.calculate(0, 50, 50);
        assertThat(result).isEqualTo(0.0);
    }

    /** POST: result = 0 when discountRate = 100 (regardless of tax) */
    @Test
    void calculate_fullDiscount_resultIsZero() {
        double result = calculator.calculate(500, 100, 20);
        assertThat(result).isEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // ShoppingCart invariant: total() >= 0 after any operation
    // -----------------------------------------------------------------------

    /** Invariant: total >= 0 after addItem */
    @Test
    void invariant_totalNonNegativeAfterAddItem() {
        cart.addItem(product, 1);
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    /** Invariant: total >= 0 after removeItem */
    @Test
    void invariant_totalNonNegativeAfterRemoveItem() {
        cart.addItem(product, 2);
        cart.removeItem(product);
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    /** Invariant: total >= 0 after updateQuantity */
    @Test
    void invariant_totalNonNegativeAfterUpdateQuantity() {
        cart.addItem(product, 5);
        cart.updateQuantity(product, 1);
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }

    /** Invariant: total >= 0 after applyDiscount */
    @Test
    void invariant_totalNonNegativeAfterApplyDiscount() {
        cart.addItem(product, 3);
        cart.applyDiscount(75);
        assertThat(cart.total()).isGreaterThanOrEqualTo(0.0);
    }
}
