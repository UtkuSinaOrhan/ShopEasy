package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Task 1 – Specification-Based Testing (Chapter 2)
 *
 * Equivalence Partitions:
 *   basePrice:    [zero] | [positive normal] | [very large]
 *   discountRate: [0] | (0,100) | [100]
 *   taxRate:      [0] | (0,100) | [100]
 *
 * Formula: result = (basePrice * (1 - discountRate/100)) * (1 + taxRate/100)
 */
class PriceCalculatorSpecTest {

    private PriceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PriceCalculator();
    }

    // -----------------------------------------------------------------------
    // PARTITION: zero base price
    // -----------------------------------------------------------------------

    /** Partition: basePrice = 0 → result must be 0 regardless of rates */
    @Test
    void zeroPriceAlwaysReturnsZero() {
        assertThat(calculator.calculate(0, 20, 10)).isEqualTo(0.0);
    }

    /** Partition: basePrice = 0, both rates at max → still 0 */
    @Test
    void zeroPriceWithMaxRatesReturnsZero() {
        assertThat(calculator.calculate(0, 100, 100)).isEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // BOUNDARY: discountRate boundaries
    // -----------------------------------------------------------------------

    /** Boundary: discountRate = 0 (lower bound) → no discount applied */
    @Test
    void discountRateZeroMeansNoDiscount() {
        double result = calculator.calculate(100, 0, 0);
        assertThat(result).isEqualTo(100.0);
    }

    /** Boundary: discountRate = 100 (upper bound) → price wiped to 0 before tax */
    @Test
    void discountRateHundredMeansFullDiscount() {
        double result = calculator.calculate(100, 100, 0);
        assertThat(result).isEqualTo(0.0);
    }

    /** Boundary: discountRate = 100 with non-zero tax → still 0 (0 * anything = 0) */
    @Test
    void discountRateHundredWithTaxStillZero() {
        double result = calculator.calculate(200, 100, 50);
        assertThat(result).isEqualTo(0.0);
    }

    // -----------------------------------------------------------------------
    // BOUNDARY: taxRate boundaries
    // -----------------------------------------------------------------------

    /** Boundary: taxRate = 0 (lower bound) → no tax added */
    @Test
    void taxRateZeroMeansNoTax() {
        double result = calculator.calculate(100, 0, 0);
        assertThat(result).isEqualTo(100.0);
    }

    /** Boundary: taxRate = 100 → price doubled after discount */
    @Test
    void taxRateHundredDoublesPrice() {
        // base=100, disc=0, tax=100 → 100 * (1+1.0) = 200
        double result = calculator.calculate(100, 0, 100);
        assertThat(result).isCloseTo(200.0, within(0.001));
    }

    // -----------------------------------------------------------------------
    // PARTITION: typical positive values — formula correctness
    // -----------------------------------------------------------------------

    /**
     * Partition: typical values.
     * Formula: base * (1 - disc/100) * (1 + tax/100)
     *
     * Row explanations:
     *   100, 10, 20 → 100*0.9*1.2 = 108.0
     *   200,  0, 10 → 200*1.0*1.1 = 220.0
     *   150, 50,  0 → 150*0.5*1.0 =  75.0
     *   500, 20, 10 → 500*0.8*1.1 = 440.0
     *    50, 25, 25 →  50*0.75*1.25= 46.875
     */
    @ParameterizedTest(name = "base={0}, disc={1}%, tax={2}% => {3}")
    @CsvSource({
        "100.0,  10.0, 20.0, 108.0",
        "200.0,   0.0, 10.0, 220.0",
        "150.0,  50.0,  0.0,  75.0",
        "500.0,  20.0, 10.0, 440.0",
        " 50.0,  25.0, 25.0,  46.875"
    })
    void typicalValues(double base, double disc, double tax, double expected) {
        assertThat(calculator.calculate(base, disc, tax))
                .isCloseTo(expected, within(0.001));
    }

    // -----------------------------------------------------------------------
    // PARTITION: very large base price
    // -----------------------------------------------------------------------

    /** Partition: very large basePrice → result stays proportional, no overflow issues */
    @Test
    void veryLargeBasePriceWithNoRates() {
        double result = calculator.calculate(1_000_000, 0, 0);
        assertThat(result).isCloseTo(1_000_000.0, within(0.001));
    }

    /** Partition: very large basePrice with discount and tax */
    @Test
    void veryLargeBasePriceWithRates() {
        // 1_000_000 * 0.9 * 1.1 = 990_000
        double result = calculator.calculate(1_000_000, 10, 10);
        assertThat(result).isCloseTo(990_000.0, within(0.1));
    }

    // -----------------------------------------------------------------------
    // BOUNDARY: on-point / off-point for discountRate
    // -----------------------------------------------------------------------

    /** Boundary: discountRate just above 0 (off-point) → small but real reduction */
    @Test
    void discountRateJustAboveZeroAppliesSmallReduction() {
        double result = calculator.calculate(100, 1, 0);
        assertThat(result).isCloseTo(99.0, within(0.001));
    }

    /** Boundary: discountRate = 99 (on-point, one below max) → 1% of price remains */
    @Test
    void discountRateNinetyNineLeavesOnePercent() {
        // 100 * 0.01 * 1.0 = 1.0
        double result = calculator.calculate(100, 99, 0);
        assertThat(result).isCloseTo(1.0, within(0.001));
    }
}
