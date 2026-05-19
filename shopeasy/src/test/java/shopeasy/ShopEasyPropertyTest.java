package shopeasy;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 4 – Property-Based Testing (Chapter 5)
 *
 * Properties tested:
 *   1. Boundedness   – PriceCalculator result is always >= 0
 *   2. Identity      – 0% discount + 0% tax returns exactly the base price
 *   3. Monotonicity  – Higher discount rate never increases the final price
 *   4. Cart commutativity – Adding A then B equals adding B then A (same total)
 *   5. Discount monotonicity on cart – Applying a bigger discount gives a lower or equal result
 */
class ShopEasyPropertyTest {

    // -----------------------------------------------------------------------
    // Property 1: Boundedness
    //
    // Plain English: For any valid inputs, the calculated price is never negative.
    // Bug class caught: sign errors in the formula, negative intermediate values,
    //                   or reversed subtraction (price - discount applied incorrectly).
    // -----------------------------------------------------------------------

    @Property
    void finalPriceIsNeverNegative(
            @ForAll @DoubleRange(min = 0, max = 10_000) double base,
            @ForAll @DoubleRange(min = 0, max = 100)   double discount,
            @ForAll @DoubleRange(min = 0, max = 100)   double tax) {

        PriceCalculator calc = new PriceCalculator();
        double result = calc.calculate(base, discount, tax);
        assertThat(result).isGreaterThanOrEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // Property 2: Identity
    //
    // Plain English: With no discount and no tax, the output equals the input price.
    //               The calculator should be a perfect pass-through in that case.
    // Bug class caught: off-by-one errors in percentage math (e.g., 1 - 0/100 != 1.0
    //                   due to floating-point mistakes), unintended default rates.
    // -----------------------------------------------------------------------

    @Property
    void zeroDiscountAndZeroTaxReturnsBasePrice(
            @ForAll @DoubleRange(min = 0, max = 10_000) double base) {

        PriceCalculator calc = new PriceCalculator();
        double result = calc.calculate(base, 0, 0);
        assertThat(result).isCloseTo(base, within(0.001));
    }

    // -----------------------------------------------------------------------
    // Property 3: Monotonicity of discount
    //
    // Plain English: If you increase the discount rate (keeping everything else fixed),
    //               the price can only stay the same or go lower — never higher.
    // Bug class caught: discount applied in the wrong direction (adding instead of subtracting),
    //                   or percentage sign errors causing the price to rise with more discount.
    // -----------------------------------------------------------------------

    @Property
    void higherDiscountNeverIncreasesPrice(
            @ForAll @DoubleRange(min = 0,  max = 10_000) double base,
            @ForAll @DoubleRange(min = 0,  max = 99)    double lowerDiscount,
            @ForAll @DoubleRange(min = 0,  max = 100)   double tax) {

        // Ensure higherDiscount > lowerDiscount by construction
        double higherDiscount = lowerDiscount + 1.0; // at most 100
        if (higherDiscount > 100) higherDiscount = 100;

        PriceCalculator calc = new PriceCalculator();
        double priceWithLess = calc.calculate(base, lowerDiscount, tax);
        double priceWithMore = calc.calculate(base, higherDiscount, tax);

        assertThat(priceWithMore).isLessThanOrEqualTo(priceWithLess + 0.0001);
    }

    // -----------------------------------------------------------------------
    // Property 4: Cart commutativity
    //
    // Plain English: The order in which you add two different products to a cart
    //               does not affect the total — addition is commutative.
    // Bug class caught: order-dependent state bugs (e.g., a linked list whose
    //                   iteration order affects the total calculation).
    // -----------------------------------------------------------------------

    @Property
    void cartTotalIsIndependentOfAddOrder(
            @ForAll("validProducts") Product p1,
            @ForAll("validProducts") Product p2,
            @ForAll @IntRange(min = 1, max = 20) int qty1,
            @ForAll @IntRange(min = 1, max = 20) int qty2) {

        // Ensure distinct product IDs so no merging occurs
        Product a = new Product("PA", p1.getName(), p1.getPrice(), 100);
        Product b = new Product("PB", p2.getName(), p2.getPrice(), 100);

        ShoppingCart cartAB = new ShoppingCart();
        cartAB.addItem(a, qty1);
        cartAB.addItem(b, qty2);

        ShoppingCart cartBA = new ShoppingCart();
        cartBA.addItem(b, qty2);
        cartBA.addItem(a, qty1);

        assertThat(cartAB.total()).isCloseTo(cartBA.total(), within(0.001));
    }

    // -----------------------------------------------------------------------
    // Property 5: Discount monotonicity on ShoppingCart.applyDiscount
    //
    // Plain English: A higher discount rate on the same cart total always yields
    //               an equal or smaller discounted price.
    // Bug class caught: incorrect discount formula in ShoppingCart (e.g., rate not
    //                   converted from percentage, or subtracted after multiplication).
    // -----------------------------------------------------------------------

    @Property
    void largerDiscountRateGivesLowerOrEqualCartPrice(
            @ForAll("validProducts") Product p,
            @ForAll @IntRange(min = 1, max = 10)  int qty,
            @ForAll @IntRange(min = 0, max = 49)  int smallRate,
            @ForAll @IntRange(min = 50, max = 100) int largeRate) {

        // smallRate < largeRate guaranteed by ranges
        ShoppingCart cartSmall = new ShoppingCart();
        cartSmall.addItem(new Product("P1", p.getName(), p.getPrice(), 100), qty);

        ShoppingCart cartLarge = new ShoppingCart();
        cartLarge.addItem(new Product("P1", p.getName(), p.getPrice(), 100), qty);

        double resultSmall = cartSmall.applyDiscount(smallRate);
        double resultLarge = cartLarge.applyDiscount(largeRate);

        assertThat(resultLarge).isLessThanOrEqualTo(resultSmall + 0.0001);
    }

    // -----------------------------------------------------------------------
    // Custom provider: generates valid Product instances
    // -----------------------------------------------------------------------

    /**
     * Provides arbitrary valid Product instances with non-null name and positive price.
     */
    @Provide
    Arbitrary<Product> validProducts() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8),
                Arbitraries.doubles().between(0.01, 500.0)
        ).as((name, price) -> new Product("P-" + name, name, price, 100));
    }
}
