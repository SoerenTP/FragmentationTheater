package dk.cinema;

import dk.cinema.model.*;
import dk.cinema.service.BookingService;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Edge Case Tests")
public class EdgeCaseTest {

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService();
    }

    @Test
    @DisplayName("Edge: Single seat booking in empty row")
    void testSingleSeatInEmptyRow() {
        BookingRequest request = new BookingRequest(Arrays.asList("0-7"), "User");
        Map<String, Object> result = service.bookSeats(request);

        assertTrue((Boolean) result.get("success"),
                "Single seat in middle of empty row should be allowed");
    }

    @Test
    @DisplayName("Edge: Booking all seats in a row sequentially")
    void testBookingFullRowSequentially() {
        for (int i = 0; i < 15; i += 3) {
            int endSeat = Math.min(i + 2, 14);
            List<String> seats = new ArrayList<>();
            for (int j = i; j <= endSeat; j++) {
                seats.add("0-" + j);
            }

            BookingRequest request = new BookingRequest(seats, "User" + i);
            Map<String, Object> result = service.bookSeats(request);

            assertTrue((Boolean) result.get("success"),
                    "Sequential booking should succeed at position " + i);
        }
    }

    @Test
    @DisplayName("Edge: Invalid seat ID format")
    void testInvalidSeatIdFormat() {
        BookingRequest request = new BookingRequest(
                Arrays.asList("invalid-seat-id"), "User");

        assertThrows(Exception.class, () -> {
            service.bookSeats(request);
        }, "Should throw exception for invalid seat ID");
    }

    @Test
    @DisplayName("Edge: Booking same seat twice in same request")
    void testDuplicateSeatInRequest() {
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-5", "0-5", "0-6"), "User");

        Map<String, Object> result = service.bookSeats(request);
    }

    @Test
    @DisplayName("Edge: Empty booking request")
    void testEmptyBookingRequest() {
        BookingRequest request = new BookingRequest(new ArrayList<>(), "User");

        assertDoesNotThrow(() -> {
            service.bookSeats(request);
        });
    }

    @Test
    @DisplayName("Edge: Booking more than 3 seats (if allowed)")
    void testBookingMoreThan3Seats() {
        BookingRequest request = new BookingRequest(
                Arrays.asList("0-0", "0-1", "0-2", "0-3", "0-4"), "User");

        Map<String, Object> result = service.bookSeats(request);

        assertNotNull(result.get("success"));
    }
}