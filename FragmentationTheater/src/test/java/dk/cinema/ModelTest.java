package dk.cinema;

import dk.cinema.model.*;
import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Model Classes Tests")
public class ModelTest {

    @Test
    @DisplayName("Seat - Basic functionality")
    void testSeatBasics() {
        Seat seat = new Seat(5, 10);

        assertEquals(5, seat.getRow());
        assertEquals(10, seat.getNumber());
        assertFalse(seat.isOccupied());
        assertEquals("5-10", seat.getId());
    }

    @Test
    @DisplayName("Seat - Booking and release")
    void testSeatBookingAndRelease() {
        Seat seat = new Seat(0, 0);

        assertFalse(seat.isOccupied());
        assertNull(seat.getBookingId());

        seat.book("booking-123");
        assertTrue(seat.isOccupied());
        assertEquals("booking-123", seat.getBookingId());

        seat.release();
        assertFalse(seat.isOccupied());
        assertNull(seat.getBookingId());
    }

    @Test
    @DisplayName("CinemaHall - Initialization")
    void testCinemaHallInitialization() {
        CinemaHall hall = new CinemaHall(10, 15);

        assertEquals(10, hall.getRows());
        assertEquals(15, hall.getSeatsPerRow());

        // All seats should be empty
        for (Seat seat : hall.getAllSeats()) {
            assertFalse(seat.isOccupied());
        }
    }

    @Test
    @DisplayName("CinemaHall - Seat retrieval")
    void testCinemaHallSeatRetrieval() {
        CinemaHall hall = new CinemaHall(5, 10);

        Seat seat = hall.getSeat(2, 5);
        assertNotNull(seat);
        assertEquals(2, seat.getRow());
        assertEquals(5, seat.getNumber());

        // Out of bounds
        assertNull(hall.getSeat(-1, 5));
        assertNull(hall.getSeat(2, 20));
        assertNull(hall.getSeat(10, 5));
    }

    @Test
    @DisplayName("CinemaHall - Get all seats count")
    void testCinemaHallGetAllSeats() {
        CinemaHall hall = new CinemaHall(10, 15);
        List<Seat> allSeats = hall.getAllSeats();

        assertEquals(150, allSeats.size(),
                "Should have 150 seats (10 rows × 15 seats)");
    }

    @Test
    @DisplayName("BookingRequest - Constructor and getters")
    void testBookingRequest() {
        List<String> seatIds = Arrays.asList("0-0", "0-1", "0-2");
        BookingRequest request = new BookingRequest(seatIds, "John Doe");

        assertEquals(seatIds, request.getSeatIds());
        assertEquals("John Doe", request.getCustomerName());
    }

    @Test
    @DisplayName("BookingRequest - Setters")
    void testBookingRequestSetters() {
        BookingRequest request = new BookingRequest();

        List<String> seatIds = Arrays.asList("1-5", "1-6");
        request.setSeatIds(seatIds);
        request.setCustomerName("Jane Smith");

        assertEquals(seatIds, request.getSeatIds());
        assertEquals("Jane Smith", request.getCustomerName());
    }
}