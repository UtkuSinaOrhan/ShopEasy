package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 2 – Structural Testing & Code Coverage (Chapter 3)
 *
 * Target class: {@link ShoppingCart}
 *
 * Branch coverage strategy:
 *   addItem      → new product (first add) vs existing product (quantity update)
 *   removeItem   → product found vs product not in cart
 *   updateQuantity → product found + valid qty | product found + invalid qty (<=0) | not found
 *   applyDiscount → discountRate = 0 | discountRate > 0
 *   total        → empty cart vs non-empty cart
 */
class ShoppingCartStructuralTest {

    private ShoppingCart cart;
    private Product apple;
    private Product banana;

    @BeforeEach
    void setUp() {
        cart   = new ShoppingCart();
        apple  = new Product("P001", "Apple",  1.50, 100);
        banana = new Product("P002", "Banana", 0.80, 50);
    }

    // -----------------------------------------------------------------------
    // total() — empty vs non-empty cart
    // -----------------------------------------------------------------------

    /** Branch: total() on empty cart → 0.0 */
    @Test
    void total_emptyCart_returnsZero() {
        assertThat(cart.total()).isEqualTo(0.0);
    }

    /** Branch: total() after adding items → sum of price * quantity */
    @Test
    void total_afterAddingItems_returnsSumOfPriceTimesQuantity() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 5);
        // 2*1.50 + 5*0.80 = 3.00 + 4.00 = 7.00
        assertThat(cart.total()).isCloseTo(7.0, within(0.001));
    }

    // -----------------------------------------------------------------------
    // addItem() — new product vs existing product
    // -----------------------------------------------------------------------

    /** Branch: addItem with a new product → itemCount increases by 1 */
    @Test
    void addItem_newProduct_increasesItemCount() {
        cart.addItem(apple, 3);
        assertThat(cart.itemCount()).isEqualTo(1);
    }

    /** Branch: addItem with same product twice → itemCount stays 1, quantity accumulates */
    @Test
    void addItem_existingProduct_updatesQuantityNotItemCount() {
        cart.addItem(apple, 2);
        cart.addItem(apple, 3);
        assertThat(cart.itemCount()).isEqualTo(1);
        // total should reflect 5 apples: 5 * 1.50 = 7.50
        assertThat(cart.total()).isCloseTo(7.5, within(0.001));
    }

    /** Branch: addItem two different products → itemCount is 2 */
    @Test
    void addItem_twoDifferentProducts_itemCountIsTwo() {
        cart.addItem(apple, 1);
        cart.addItem(banana, 1);
        assertThat(cart.itemCount()).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // removeItem() — found vs not found
    // -----------------------------------------------------------------------

    /** Branch: removeItem on existing product → item removed, total decreases */
    @Test
    void removeItem_existingProduct_removesItFromCart() {
        cart.addItem(apple, 2);
        cart.addItem(banana, 1);
        cart.removeItem(apple);
        assertThat(cart.itemCount()).isEqualTo(1);
        assertThat(cart.total()).isCloseTo(0.80, within(0.001));
    }

    /** Branch: removeItem on product not in cart → no exception, cart unchanged */
    @Test
    void removeItem_productNotInCart_doesNothing() {
        cart.addItem(apple, 2);
        assertThatCode(() -> cart.removeItem(banana)).doesNotThrowAnyException();
        assertThat(cart.itemCount()).isEqualTo(1);
    }

    /** Branch: removeItem on empty cart → no exception */
    @Test
    void removeItem_emptyCart_doesNothing() {
        assertThatCode(() -> cart.removeItem(apple)).doesNotThrowAnyException();
        assertThat(cart.itemCount()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // updateQuantity() — found+valid | found+invalid | not found
    // -----------------------------------------------------------------------

    /** Branch: updateQuantity on existing product with valid qty → quantity updated */
    @Test
    void updateQuantity_existingProductValidQuantity_updatesTotal() {
        cart.addItem(apple, 1);
        cart.updateQuantity(apple, 5);
        // 5 * 1.50 = 7.50
        assertThat(cart.total()).isCloseTo(7.5, within(0.001));
    }

    /** Branch: updateQuantity with quantity <= 0 → item removed from cart */
    @Test
    void updateQuantity_quantityZeroOrNegative_removesItem() {
        cart.addItem(apple, 3);
        cart.updateQuantity(apple, 0);
        assertThat(cart.itemCount()).isEqualTo(0);
    }

    /** Branch: updateQuantity on product not in cart → no exception, cart unchanged */
    @Test
    void updateQuantity_productNotInCart_doesNothing() {
        cart.addItem(apple, 2);
        assertThatCode(() -> cart.updateQuantity(banana, 3)).doesNotThrowAnyException();
        assertThat(cart.itemCount()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // applyDiscount() — zero vs positive discount
    // -----------------------------------------------------------------------

    /** Branch: applyDiscount with 0% → total unchanged */
    @Test
    void applyDiscount_zeroRate_returnsTotalUnchanged() {
        cart.addItem(apple, 4);
        // 4 * 1.50 = 6.00; 0% discount → 6.00
        double discounted = cart.applyDiscount(0);
        assertThat(discounted).isCloseTo(6.0, within(0.001));
    }

    /** Branch: applyDiscount with positive rate → total reduced proportionally */
    @Test
    void applyDiscount_twentyPercent_reducesTotalByTwentyPercent() {
        cart.addItem(apple, 4);
        // total = 6.00; 20% off → 4.80
        double discounted = cart.applyDiscount(20);
        assertThat(discounted).isCloseTo(4.8, within(0.001));
    }

    /** Branch: applyDiscount 100% → total is 0 */
    @Test
    void applyDiscount_hundredPercent_returnsZero() {
        cart.addItem(banana, 5);
        double discounted = cart.applyDiscount(100);
        assertThat(discounted).isCloseTo(0.0, within(0.001));
    }

    /** Branch: applyDiscount on empty cart → returns 0 */
    @Test
    void applyDiscount_emptyCart_returnsZero() {
        double discounted = cart.applyDiscount(10);
        assertThat(discounted).isEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // itemCount() helper
    // -----------------------------------------------------------------------

    /** Sanity: itemCount starts at 0 */
    @Test
    void itemCount_initiallyZero() {
        assertThat(cart.itemCount()).isEqualTo(0);
    }

    /** itemCount reflects multiple additions correctly */
    @Test
    void itemCount_afterAddingThreeDistinctProducts_isThree() {
        Product cherry = new Product("P003", "Cherry", 2.0, 20);
        cart.addItem(apple, 1);
        cart.addItem(banana, 1);
        cart.addItem(cherry, 1);
        assertThat(cart.itemCount()).isEqualTo(3);
    }
}
