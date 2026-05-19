package shopeasy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Task 5 – Mocks & Stubs (Chapter 6)
 *
 * Tests for {@link OrderProcessor#process(String, ShoppingCart)} in complete isolation.
 *
 * Dependencies mocked:
 *   - {@link InventoryService} – controls stock availability
 *   - {@link PaymentGateway}  – controls charge success/failure
 *
 * Scenarios:
 *   1. Happy path     – inventory OK, payment OK  → Order returned
 *   2. Inventory fail – one item unavailable       → null returned, charge never called
 *   3. Payment fail   – inventory OK, charge fails → null returned
 *   4. Partial qty    – only first item available  → null returned (all must pass)
 *   5. Empty cart     – no items at all            → null returned, nothing charged
 *   6. Multiple items – all available, payment OK  → Order with correct total
 *
 * Reflection (for report):
 *   WHAT MOCKING ALLOWS:
 *     Mocking allows testing OrderProcessor's logic in complete isolation from
 *     real infrastructure (databases, payment APIs). You can simulate rare or
 *     hard-to-reproduce scenarios like payment gateway timeouts, partial inventory,
 *     or network failures without any real external dependency being present.
 *     It also makes tests deterministic and fast.
 *
 *   WHAT MOCKING PREVENTS:
 *     Mocks cannot catch integration bugs — e.g., if the real InventoryService
 *     returns a subtly different object type, or the real PaymentGateway uses a
 *     different charge signature. Contract drift between the mock behaviour and
 *     the real implementation can go undetected.
 *
 *   WHEN MOCKING IS A BAD IDEA:
 *     - When mocking the class under test itself (testing the mock, not the code)
 *     - When the "dependency" is a simple value object with no side effects
 *     - When there is no meaningful behaviour to isolate (over-mocking)
 *     - When integration correctness is what actually matters (prefer integration tests)
 */
@ExtendWith(MockitoExtension.class)
class OrderProcessorMockTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private OrderProcessor orderProcessor;

    private ShoppingCart cart;
    private Product widget;
    private Product gadget;

    @BeforeEach
    void setUp() {
        cart   = new ShoppingCart();
        widget = new Product("P001", "Widget", 25.0, 100);
        gadget = new Product("P002", "Gadget", 40.0, 50);
    }

    // -----------------------------------------------------------------------
    // Scenario 1: Happy path — inventory available, payment succeeds
    // -----------------------------------------------------------------------

    @Test
    void process_inventoryOkAndPaymentOk_returnsOrder() {
        cart.addItem(widget, 2);

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(paymentGateway.charge("customer-1", 50.0)).thenReturn(true);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo("customer-1");
        assertThat(order.getTotal()).isEqualTo(50.0);
        verify(paymentGateway).charge("customer-1", 50.0);
    }

    // -----------------------------------------------------------------------
    // Scenario 2: Inventory failure — isAvailable returns false
    //             → null returned AND charge() is NEVER called
    // -----------------------------------------------------------------------

    @Test
    void process_inventoryUnavailable_returnsNullAndNeverCharges() {
        cart.addItem(widget, 5);

        when(inventoryService.isAvailable(widget, 5)).thenReturn(false);

        Order order = orderProcessor.process("customer-1", cart);

        assertThat(order).isNull();
        // Critical: payment must never be attempted when stock is missing
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    // -----------------------------------------------------------------------
    // Scenario 3: Payment failure — inventory OK but charge returns false
    // -----------------------------------------------------------------------

    @Test
    void process_inventoryOkButPaymentFails_returnsNull() {
        cart.addItem(widget, 1);

        when(inventoryService.isAvailable(widget, 1)).thenReturn(true);
        when(paymentGateway.charge("customer-2", 25.0)).thenReturn(false);

        Order order = orderProcessor.process("customer-2", cart);

        assertThat(order).isNull();
        // charge was still attempted (it's inventory that's fine)
        verify(paymentGateway).charge("customer-2", 25.0);
    }

    // -----------------------------------------------------------------------
    // Scenario 4: Partial quantity — only one of two items passes inventory
    //             Expected behaviour: whole order rejected (all-or-nothing)
    // -----------------------------------------------------------------------

    @Test
    void process_partialInventoryAvailability_returnsNull() {
        cart.addItem(widget, 3);
        cart.addItem(gadget, 2);

        when(inventoryService.isAvailable(widget, 3)).thenReturn(true);
        when(inventoryService.isAvailable(gadget, 2)).thenReturn(false); // gadget out of stock

        Order order = orderProcessor.process("customer-3", cart);

        // The entire order must be rejected if any item is unavailable
        assertThat(order).isNull();
        // Payment should never be triggered
        verify(paymentGateway, never()).charge(anyString(), anyDouble());
    }

    // -----------------------------------------------------------------------
    // Scenario 5: Empty cart — no items to process
    // -----------------------------------------------------------------------

    @Test
    void process_emptyCart_returnsNull() {
        // cart has no items
        Order order = orderProcessor.process("customer-4", cart);

        assertThat(order).isNull();
        verifyNoInteractions(inventoryService);
        verifyNoInteractions(paymentGateway);
    }

    // -----------------------------------------------------------------------
    // Scenario 6: Multiple items — all available, payment succeeds
    //             Order total must equal the sum of all cart items
    // -----------------------------------------------------------------------

    @Test
    void process_multipleItemsAllAvailablePaymentOk_returnsOrderWithCorrectTotal() {
        cart.addItem(widget, 2); // 2 * 25.0 = 50.0
        cart.addItem(gadget, 1); // 1 * 40.0 = 40.0
        // total = 90.0

        when(inventoryService.isAvailable(widget, 2)).thenReturn(true);
        when(inventoryService.isAvailable(gadget, 1)).thenReturn(true);
        when(paymentGateway.charge("customer-5", 90.0)).thenReturn(true);

        Order order = orderProcessor.process("customer-5", cart);

        assertThat(order).isNotNull();
        assertThat(order.getTotal()).isEqualTo(90.0);
        assertThat(order.getCustomerId()).isEqualTo("customer-5");

        verify(inventoryService).isAvailable(widget, 2);
        verify(inventoryService).isAvailable(gadget, 1);
        verify(paymentGateway).charge("customer-5", 90.0);
    }
}
