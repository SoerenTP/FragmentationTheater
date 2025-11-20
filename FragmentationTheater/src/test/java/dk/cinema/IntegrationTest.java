package dk.cinema;

import dk.cinema.model.*;
import dk.cinema.service.BookingService;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration Tests - Full Booking Scenarios")
public class IntegrationTest {

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService();
    }

    @Test
    @DisplayName("Scenario: Family of 4 books, then couple books adjacent")
    void testFamilyThenCoupleScenario() {
        // Family books 4 seats
        BookingRequest family = new BookingRequest(
                Arrays.asList("5-5", "5-6", "5-7", "5-8"),
                "Family"
        );
        Map<String, Object> result1 = service.bookSeats(family);
        assertTrue((Boolean) result1.get("success"));

        // Couple tries to book right next to them
        BookingRequest couple = new BookingRequest(
                Arrays.asList("5-9", "5-10"),
                "Couple"
        );
        Map<String, Object> result2 = service.bookSeats(couple);
        assertTrue((Boolean) result2.get("success"));

        assertEquals(0.0, service.calculateFragmentation(), 0.01);
    }

    @Test
    @DisplayName("Scenario: Multiple small groups fill row efficiently")
    void testMultipleGroupsFillRow() {
        // Group 1: 3 seats
        service.bookSeats(new BookingRequest(
                Arrays.asList("3-0", "3-1", "3-2"), "Group1"));

        // Group 2: 3 seats
        service.bookSeats(new BookingRequest(
                Arrays.asList("3-3", "3-4", "3-5"), "Group2"));

        // Group 3: 2 seats
        service.bookSeats(new BookingRequest(
                Arrays.asList("3-6", "3-7"), "Group3"));

        // Group 4: 3 seats
        service.bookSeats(new BookingRequest(
                Arrays.asList("3-8", "3-9", "3-10"), "Group4"));

        // Should have no fragmentation
        assertEquals(0.0, service.calculateFragmentation(), 0.01);

        // Check utilization
        double expectedUtil = (11.0 / 150.0) * 100;
        assertEquals(expectedUtil, service.calculateUtilization(), 0.5);
    }

    @Test
    @DisplayName("Scenario: User learns from rejection and books correctly")
    void testUserLearnsFromRejection() {
        // Setup: Book seat at position 5
        service.bookSeats(new BookingRequest(Arrays.asList("0-5"), "Initial"));

        // User tries bad booking
        BookingRequest badAttempt = new BookingRequest(
                Arrays.asList("0-1", "0-2", "0-3"), "User");
        Map<String, Object> result1 = service.bookSeats(badAttempt);

        assertFalse((Boolean) result1.get("success"));
        int rejectedBefore = (int) service.getStatistics().get("rejectedBookings");

        // User books correctly this time
        BookingRequest goodAttempt = new BookingRequest(
                Arrays.asList("0-6", "0-7", "0-8"), "User");
        Map<String, Object> result2 = service.bookSeats(goodAttempt);

        assertTrue((Boolean) result2.get("success"));

        // Rejected count should not increase on success
        int rejectedAfter = (int) service.getStatistics().get("rejectedBookings");
        assertEquals(rejectedBefore, rejectedAfter);
    }

    @Test
    @DisplayName("Scenario: Cinema fills to 80% without fragmentation")
    void testHighUtilizationWithoutFragmentation() {
        // Fill ~80% of seats (120 out of 150) in good patterns
        int seatsBooked = 0;
        int targetSeats = 120;
        int row = 0;
        int startSeat = 0;

        while (seatsBooked < targetSeats) {
            int seatsToBook = Math.min(3, targetSeats - seatsBooked);
            if (startSeat + seatsToBook > 15) {
                row++;
                startSeat = 0;
            }

            List<String> seats = new ArrayList<>();
            for (int i = 0; i < seatsToBook; i++) {
                seats.add(row + "-" + (startSeat + i));
            }

            BookingRequest request = new BookingRequest(seats, "User" + seatsBooked);
            Map<String, Object> result = service.bookSeats(request);

            if ((Boolean) result.get("success")) {
                seatsBooked += seatsToBook;
                startSeat += seatsToBook;
            } else {
                // Skip to next row if blocked
                row++;
                startSeat = 0;
            }
        }

        assertTrue(service.calculateUtilization() >= 75.0,
                "Should achieve at least 75% utilization");
        assertEquals(0.0, service.calculateFragmentation(), 0.01,
                "Should maintain 0% fragmentation");
    }

    @Test
    @DisplayName("Scenario: Reset clears all bookings and stats")
    void testResetClearsEverything() {
        // Make several bookings
        service.bookSeats(new BookingRequest(Arrays.asList("0-0", "0-1"), "User1"));
        service.bookSeats(new BookingRequest(Arrays.asList("1-0", "1-1"), "User2"));
        service.bookSeats(new BookingRequest(Arrays.asList("2-0", "2-1"), "User3"));

        // Verify bookings exist
        assertTrue(service.calculateUtilization() > 0);
        assertTrue((int) service.getStatistics().get("totalBookings") > 0);

        // Reset
        service.reset();

        // Verify everything is cleared
        assertEquals(0.0, service.calculateUtilization(), 0.01);
        assertEquals(0.0, service.calculateFragmentation(), 0.01);
        assertEquals(0, service.getStatistics().get("totalBookings"));
        assertEquals(0, service.getStatistics().get("rejectedBookings"));
        assertEquals(0, service.getStatistics().get("occupiedSeats"));
    }

    @Test
    @DisplayName("Performance: Handle 50 sequential bookings quickly")
    void testPerformance50Bookings() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 50; i++) {
            int row = i / 5;
            int seat = (i % 5) * 3;

            if (seat + 2 < 15) {
                BookingRequest request = new BookingRequest(
                        Arrays.asList(
                                row + "-" + seat,
                                row + "-" + (seat + 1),
                                row + "-" + (seat + 2)
                        ),
                        "User" + i
                );
                service.bookSeats(request);
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 1000,
                "50 bookings should complete in under 1 second, took: " + duration + "ms");
    }
}