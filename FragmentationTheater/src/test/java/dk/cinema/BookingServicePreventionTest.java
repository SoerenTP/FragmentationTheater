package dk.cinema;

import dk.cinema.model.*;
import dk.cinema.service.BookingService;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for fragmentation prevention functionality
 */
@DisplayName("Booking Service - Fragmentation Prevention Tests")
public class BookingServicePreventionTest {

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService();
    }

    @Test
    @DisplayName("Should allow booking that creates no fragmentation")
    void testAllowedBooking_NoFragmentation() {
        // Book seats 0-1-2 from the start
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-0", "0-1", "0-2"),
                "Test User"
        );

        Map<String, Object> result = service.bookSeats(request);

        assertTrue((Boolean) result.get("success"),
                "Booking should be allowed");
        assertEquals(0.0, service.calculateFragmentation(), 0.01,
                "Fragmentation should remain 0%");
    }

    @Test
    @DisplayName("Should block booking that would create isolated seat")
    void testBlockedBooking_CreatesIsolatedSeat() {
        // First book seat at position 5
        service.bookSeats(new BookingRequest(Arrays.asList("0-5"), "User1"));

        // Try to book seats 1-2-3 (would isolate seat 0 and 4)
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-1", "0-2", "0-3"),
                "User2"
        );

        Map<String, Object> result = service.bookSeats(request);

        assertFalse((Boolean) result.get("success"),
                "Booking should be blocked");
        assertEquals("FRAGMENTATION_PREVENTION", result.get("reason"),
                "Should indicate fragmentation prevention");

        @SuppressWarnings("unchecked")
        List<String> isolated = (List<String>) result.get("isolatedSeats");
        assertTrue(isolated.size() >= 1,
                "Should identify at least one isolated seat");
    }

    @Test
    @DisplayName("Should block 'donut hole' booking pattern")
    void testBlockedBooking_DonutHole() {
        // Create situation: [X][ ][ ][ ][X]
        service.bookSeats(new BookingRequest(Arrays.asList("0-0"), "User1"));
        service.bookSeats(new BookingRequest(Arrays.asList("0-4"), "User2"));

        // Try to book seats 1 and 3, leaving 2 isolated
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-1", "0-3"),
                "User3"
        );

        Map<String, Object> result = service.bookSeats(request);

        assertFalse((Boolean) result.get("success"),
                "Should block donut hole pattern");

        @SuppressWarnings("unchecked")
        List<String> isolated = (List<String>) result.get("isolatedSeats");
        assertTrue(isolated.contains("0-2"),
                "Seat 0-2 should be identified as isolated");
    }

    @Test
    @DisplayName("Should allow booking that fills a gap perfectly")
    void testAllowedBooking_FillsGap() {
        // Create gap: [X][X][ ][ ][X][X]
        service.bookSeats(new BookingRequest(Arrays.asList("0-0", "0-1"), "User1"));
        service.bookSeats(new BookingRequest(Arrays.asList("0-4", "0-5"), "User2"));

        // Fill the gap with seats 2-3
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-2", "0-3"),
                "User3"
        );

        Map<String, Object> result = service.bookSeats(request);

        assertTrue((Boolean) result.get("success"),
                "Booking that fills gap should be allowed");
        assertEquals(0.0, service.calculateFragmentation(), 0.01,
                "Should maintain 0% fragmentation");
    }

    @Test
    @DisplayName("Should provide alternative seat suggestions")
    void testAlternativeSuggestions() {
        // Book a seat to create constraints
        service.bookSeats(new BookingRequest(Arrays.asList("0-5"), "User1"));

        // Try problematic booking
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-1", "0-2", "0-3"),
                "User2"
        );

        Map<String, Object> result = service.bookSeats(request);

        assertFalse((Boolean) result.get("success"));

        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) result.get("suggestions");
        assertNotNull(suggestions, "Should provide suggestions");
    }

    @Test
    @DisplayName("Should track rejected bookings in statistics")
    void testRejectedBookingsTracking() {
        // Create scenario for rejection
        service.bookSeats(new BookingRequest(Arrays.asList("0-5"), "User1"));

        int initialRejected = (int) service.getStatistics().get("rejectedBookings");

        // Try problematic booking
        service.bookSeats(new BookingRequest(
                Arrays.asList("0-1", "0-2", "0-3"),
                "User2"
        ));

        int afterRejected = (int) service.getStatistics().get("rejectedBookings");

        assertEquals(initialRejected + 1, afterRejected,
                "Rejected bookings count should increase");
    }

    @Test
    @DisplayName("Should allow edge seats bookings")
    void testEdgeSeatsAllowed() {
        // Book first 3 seats
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-0", "0-1", "0-2"),
                "User"
        );

        Map<String, Object> result = service.bookSeats(request);

        assertTrue((Boolean) result.get("success"),
                "Edge seats should be allowed");
    }

    @Test
    @DisplayName("Should allow last seats in row")
    void testLastSeatsAllowed() {
        // Book last 3 seats in row (12-13-14 in a 15-seat row)
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-12", "0-13", "0-14"),
                "User"
        );

        Map<String, Object> result = service.bookSeats(request);

        assertTrue((Boolean) result.get("success"),
                "Last seats should be allowed");
    }

    @Test
    @DisplayName("Should maintain zero fragmentation with prevention")
    void testZeroFragmentationMaintained() {
        // Make multiple allowed bookings
        service.bookSeats(new BookingRequest(Arrays.asList("0-0", "0-1", "0-2"), "User1"));
        service.bookSeats(new BookingRequest(Arrays.asList("0-3", "0-4", "0-5"), "User2"));
        service.bookSeats(new BookingRequest(Arrays.asList("0-6", "0-7"), "User3"));

        assertEquals(0.0, service.calculateFragmentation(), 0.01,
                "Fragmentation should always be 0% with prevention");
    }

    @Test
    @DisplayName("Should handle single seat booking at edges")
    void testSingleSeatAtEdge() {
        // Book single seat at start
        BookingRequest request1 = new BookingRequest(Arrays.asList("0-0"), "User1");
        Map<String, Object> result1 = service.bookSeats(request1);
        assertTrue((Boolean) result1.get("success"));

        // Book single seat at end
        BookingRequest request2 = new BookingRequest(Arrays.asList("0-14"), "User2");
        Map<String, Object> result2 = service.bookSeats(request2);
        assertTrue((Boolean) result2.get("success"));
    }

    @Test
    @DisplayName("Should block alternating seat pattern")
    void testBlockAlternatingPattern() {
        // Book seat 0
        service.bookSeats(new BookingRequest(Arrays.asList("0-0"), "User1"));

        // Try to book seat 2 (would isolate seat 1)
        BookingRequest request = new BookingRequest(Arrays.asList("0-2"), "User2");
        Map<String, Object> result = service.bookSeats(request);

        assertFalse((Boolean) result.get("success"),
                "Should block alternating pattern");
    }

    @Test
    @DisplayName("Should handle multiple rows independently")
    void testMultipleRowsIndependent() {
        // Book in row 0
        service.bookSeats(new BookingRequest(Arrays.asList("0-0", "0-1"), "User1"));

        // Book in row 1 - should be independent
        BookingRequest request = new BookingRequest(
                Arrays.asList("1-0", "1-1", "1-2"),
                "User2"
        );

        Map<String, Object> result = service.bookSeats(request);
        assertTrue((Boolean) result.get("success"),
                "Different rows should be independent");
    }
}