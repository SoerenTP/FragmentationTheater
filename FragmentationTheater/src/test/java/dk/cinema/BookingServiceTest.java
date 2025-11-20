package dk.cinema.service;

import dk.cinema.model.BookingRequest;
import dk.cinema.model.CinemaHall;
import dk.cinema.model.Seat;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BookingServiceTest {

    BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(5, 8); // 5 rows × 8 seats
    }

    @Test
    void testSingleSeatBookingAlwaysAllowed() {
        BookingRequest req = new BookingRequest(
                List.of("0-0"),
                "Alice"
        );

        Map<String, Object> result = service.bookSeats(req);

        assertTrue((Boolean) result.get("success"));
        assertEquals("0-0", service.getCinemaHall().getSeat(0, 0).getId());
        assertTrue(service.getCinemaHall().getSeat(0, 0).isOccupied());
    }

    @Test
    void testBookingContiguousSeats() {
        BookingRequest req = new BookingRequest(
                List.of("1-2", "1-3", "1-4"),
                "Bob"
        );

        Map<String, Object> result = service.bookSeats(req);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testRejectNonContiguousGroup() {
        BookingRequest req = new BookingRequest(
                List.of("1-2", "1-4"), // gap at seat 3
                "Team"
        );

        Map<String, Object> result = service.bookSeats(req);

        assertFalse((Boolean) result.get("success"));
        assertEquals("NOT_CONTIGUOUS", result.get("reason"));
    }

    @Test
    void testRejectMultipleRows() {
        BookingRequest req = new BookingRequest(
                List.of("0-1", "1-1"),
                "Eve"
        );

        Map<String, Object> result = service.bookSeats(req);

        assertFalse((Boolean) result.get("success"));
        assertEquals("DIFFERENT_ROWS", result.get("reason"));
    }

    @Test
    void testRejectIfFragmentationOccurs() {
        // Use a row with indices 0..6 (ensure service was constructed with >=7 seats per row)
        // Occupy the ends (0 and 6), so booking the middle block 2-4 would leave seats 1 and 5 isolated.
        service.bookSeats(new BookingRequest(List.of("0-0"), "X"));
        service.bookSeats(new BookingRequest(List.of("0-6"), "Y"));

        // Try to book contiguous block 0-2..0-4 -> should be rejected because it would isolate seats 0-1 and 0-5
        BookingRequest problematic = new BookingRequest(
                List.of("0-2", "0-3", "0-4"),
                "Group"
        );

        Map<String, Object> result = service.bookSeats(problematic);

        assertFalse((Boolean) result.get("success"));
        assertEquals("FRAGMENTATION_PREVENTION", result.get("reason"));
    }

    @Test
    void testLastResortBookingAllowed() {
        // Fully occupy except 3 seats (last-resort rule applies)
        for (int row = 0; row < 5; row++) {
            for (int seat = 0; seat < 8; seat++) {
                if (!(row == 0 && (seat == 0 || seat == 1 || seat == 2))) {
                    service.bookSeats(new BookingRequest(List.of(row + "-" + seat), "Auto"));
                }
            }
        }

        // Only 3 left, group of 2 should be allowed even if fragmented
        BookingRequest req = new BookingRequest(
                List.of("0-0", "0-1"),
                "LastResort"
        );

        Map<String, Object> result = service.bookSeats(req);

        assertTrue((Boolean) result.get("success"));
    }

    @Test
    void testAvailableSeatsForGroup() {
        // Occupy some seats to force fragmentation logic
        service.bookSeats(new BookingRequest(List.of("2-2", "2-3"), "X"));

        List<String> available = service.getAvailableSeatsForBooking(2);

        // A valid pair e.g. "2-0" & "2-1" **should** be present
        assertTrue(available.contains("2-0"));
    }

    @Test
    void testFragmentationCalculation() {
        // Create isolated seat 0-1 by booking 0-0 and 0-2 as separate single bookings
        service.bookSeats(new BookingRequest(List.of("0-0"), "X"));
        service.bookSeats(new BookingRequest(List.of("0-2"), "Y"));

        double frag = service.calculateFragmentation();

        // There should be at least one isolated seat (0-1)
        assertTrue(frag > 0, "Expected fragmentation > 0 after creating an isolated seat (0-1)");
    }

    @Test
    void testReset() {
        service.bookSeats(new BookingRequest(List.of("3-3"), "Z"));

        service.reset();

        assertEquals(0, service.getStatistics().get("occupiedSeats"));
        assertEquals(0, service.getStatistics().get("totalBookings"));
        assertEquals(0, service.getStatistics().get("rejectedBookings"));
    }
}
